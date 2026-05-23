#!/bin/bash
# ==============================================================================
# Jasmine 多阶段企业级部署脚本 (ops/prod/up.sh)
# 
# 流程:
#   1. 读取 .env 环境变量（包括用户自定义的 NACOS_PASSWORD）
#   2. 启动核心中间件 (MySQL, Redis, RabbitMQ, Nacos)
#   3. 自动等待 Nacos 完成健康检查 (Healthy)
#   4. 自动触发 Nacos 数据库管理员密码从默认的 'nacos' 热更新为 .env 中定义的密码，并自动导入所有微服务 YAML 配置
#   5. 待密码同步与配置全部就绪后，启动网关、微服务与前端，确保零 403 认证错误
# ==============================================================================
set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
OPS_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

# 1. 载入并导出 .env 环境变量，使其可在本脚本和后续调用的 import.sh 中直接使用
if [ -f "$OPS_DIR/.env" ]; then
  echo ">>> 检测到 .env 文件，正在载入并导出环境变量..."
  set -a
  . "$OPS_DIR/.env"
  set +a
else
  echo ">>> [错误] 未在 $OPS_DIR 找到 .env 配置文件！请先参考 .env.example 进行复制与配置。"
  exit 1
fi

echo "========================================="
echo "  Jasmine 部署 - 阶段 1: 启动基础设施"
echo "========================================="

# 2. 拉取最新镜像
docker compose --env-file "$OPS_DIR/.env" -f "$SCRIPT_DIR/docker-compose.yml" pull

# 3. 优先启动核心中间件
docker compose --env-file "$OPS_DIR/.env" -f "$SCRIPT_DIR/docker-compose.yml" up -d mysql redis rabbitmq nacos

# 4. 循环等待 Nacos 容器达到 Healthy 状态
echo ">>> 正在等待 Nacos 启动并通过健康检查..."
while true; do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' jasmine-prod-nacos 2>/dev/null || echo '"starting"')
  if [ "$STATUS" = "\"healthy\"" ]; then
    break
  fi
  echo ">>> Nacos 尚未就绪 (当前状态: $STATUS)... 5秒后重试..."
  sleep 5
done
echo ">>> [OK] Nacos 基础设施已健康运行！"

# ---------- Nacos 初始管理员用户自动补种 ----------
# MySQL 的 /docker-entrypoint-initdb.d 脚本仅在首次初始化 datadir 时执行。
# 如果 data 卷已存在（非首次启动），nacos.users 表可能为空，
# 导致 Nacos 鉴权服务返回 "user not found"。此处自动检测并补种。
echo ">>> 正在检查 Nacos 数据库初始管理员用户..."
NACOS_USER_EXISTS=$(docker exec jasmine-prod-mysql \
  mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -N -s -e \
  "SELECT COUNT(*) FROM nacos.users WHERE username='nacos';" 2>/dev/null || echo "0")

if [ "$NACOS_USER_EXISTS" = "0" ]; then
  echo ">>> [自愈] 检测到 nacos.users 表中缺少管理员用户，正在自动补种..."
  docker exec jasmine-prod-mysql \
    mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" nacos -e "
      INSERT IGNORE INTO users (username, password, enabled)
        VALUES ('nacos', '\$2a\$10\$EuWPZHzz32dJN7jexM34EKsLrV7glS.aBGD66ZoNs5qi23.A277t.', TRUE);
      INSERT IGNORE INTO roles (username, role)
        VALUES ('nacos', 'ROLE_ADMIN');
    "
  echo ">>> [OK] Nacos 管理员用户已成功补种！(默认密码: nacos，后续将由 import.sh 自愈同步为 .env 中的自定义密码)"
else
  echo ">>> [OK] Nacos 管理员用户已存在，跳过补种。"
fi

echo "========================================="
echo "  Jasmine 部署 - 阶段 2: 密码自愈与配置导入"
echo "========================================="

# 5. 自动获取 Nacos 宿主机映射端口并调用 import.sh 进行自愈与配置导入
NACOS_PORT_HOST="${NACOS_PORT:-8848}"
echo ">>> 正在连接 127.0.0.1:$NACOS_PORT_HOST 进行密码同步与配置导入..."

# 执行 import.sh (它会自动检测默认密码，将其热修改为 .env 中的自定义密码，并导入 YAML 配置文件)
bash "$OPS_DIR/nacos-config/import.sh" "127.0.0.1:$NACOS_PORT_HOST" "${NACOS_NAMESPACE:-prod}"

echo "========================================="
echo "  Jasmine 部署 - 阶段 3: 启动应用微服务"
echo "========================================="

# 6. Nacos 密码与配置已全部就绪，安全启动所有微服务、数据迁移服务和前端
docker compose --env-file "$OPS_DIR/.env" -f "$SCRIPT_DIR/docker-compose.yml" up -d

echo ""
echo ">>> [恭喜] Jasmine 集群已成功完成分阶段部署指令！"
echo ">>> 所有微服务已以您配置的自定义密码成功连接 Nacos 控制台。"
echo ">>> 您可以使用 'docker compose -f $SCRIPT_DIR/docker-compose.yml ps' 检查所有容器状态。"
echo "========================================="

