#!/bin/bash
# ==============================================================================
# Jasmine 一键部署脚本 (ops/prod/up.sh)
#
# 流程:
#   1. 读取 ops/prod/.env
#   2. 拉取最新镜像 + 启动核心中间件
#   3. 等待 Nacos healthy → 创建 prod 命名空间 → 密码自愈 → 导入 YAML 配置
#   4. 启动 Schema 迁移 (Flyway 分库) + 等待完成
#   5. 启动所有业务服务 + 前端
#   6. 刷新前端 nginx DNS 缓存 + 验证登录
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

# Docker Compose 兼容 (sudo 下 PATH 可能不同)
dcomp() { docker compose "$@" 2>/dev/null || docker-compose "$@"; }
dcomp_env() { dcomp_env "$@" 2>/dev/null || docker-compose -f "$SCRIPT_DIR/docker-compose.yml" "$@"; }

# ---- 1. 载入 .env ----
if [ -f "$ENV_FILE" ]; then
  echo ">>> 载入环境变量: $ENV_FILE"
  set -a
  . "$ENV_FILE"
  set +a
else
  echo ">>> [错误] 未找到 $ENV_FILE，请从 .env.example 复制并配置。"
  exit 1
fi

echo "========================================="
echo "  Jasmine 部署 - 阶段 1: 中间件"
echo "========================================="

# ---- 2. 拉取并启动中间件 ----
dcomp_env pull
dcomp_env up -d mysql redis rabbitmq nacos

# ---- 3. 等待 Nacos healthy ----
echo ">>> 等待 Nacos 就绪..."
while true; do
  STATUS=$(docker inspect --format='{{json .State.Health.Status}}' jasmine-prod-nacos 2>/dev/null || echo '"starting"')
  if [ "$STATUS" = "\"healthy\"" ]; then break; fi
  echo ">>> Nacos 状态: $STATUS ... 5 秒后重试"
  sleep 5
done
echo ">>> [OK] Nacos 已就绪"

# ---- 4. Nacos 管理员用户补种 ----
NACOS_USER_EXISTS=$(docker exec jasmine-prod-mysql \
  mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -N -s -e \
  "SELECT COUNT(*) FROM nacos.users WHERE username='nacos';" 2>/dev/null || echo "0")

if [ "$NACOS_USER_EXISTS" = "0" ]; then
  echo ">>> [自愈] 补种 Nacos 管理员用户..."
  docker exec jasmine-prod-mysql \
    mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" nacos -e "
      INSERT IGNORE INTO users (username, password, enabled)
        VALUES ('nacos', '\$2a\$10\$EuWPZHzz32dJN7jexM34EKsLrV7glS.aBGD66ZoNs5qi23.A277t.', TRUE);
      INSERT IGNORE INTO roles (username, role) VALUES ('nacos', 'ROLE_ADMIN');
    "
  echo ">>> [OK] Nacos 管理员已补种"
fi

echo "========================================="
echo "  Jasmine 部署 - 阶段 2: Nacos 配置导入"
echo "========================================="

# ---- 5. 创建 prod 命名空间（Nacos 3.x 需通过 MySQL） ----
TS=$(date +%s)000
docker exec jasmine-prod-mysql \
  mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" nacos -e \
  "INSERT IGNORE INTO tenant_info (kp, tenant_id, tenant_name, tenant_desc, create_source, gmt_create, gmt_modified)
   VALUES ('1', '${NACOS_NAMESPACE:-prod}', '${NACOS_NAMESPACE:-prod}', 'Jasmine ${NACOS_NAMESPACE:-prod}', 'empty', $TS, $TS)"
echo ">>> [OK] Nacos 命名空间 ${NACOS_NAMESPACE:-prod} 已创建"

# ---- 6. 导入 YAML 配置（密码自愈 + 导入） ----
NACOS_PORT_HOST="${NACOS_PORT:-8848}"
bash "$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)/nacos-config/import.sh" \
  "127.0.0.1:$NACOS_PORT_HOST" "${NACOS_NAMESPACE:-prod}"

echo "========================================="
echo "  Jasmine 部署 - 阶段 3: Schema 迁移"
echo "========================================="

# ---- 7. 启动 Schema 服务并等待完成 ----
dcomp_env up -d jasmine-schema
echo ">>> 等待 Schema 迁移完成..."
while true; do
  STATE=$(docker inspect --format='{{.State.Status}}' jasmine-prod-schema 2>/dev/null || echo "running")
  EXIT_CODE=$(docker inspect --format='{{.State.ExitCode}}' jasmine-prod-schema 2>/dev/null || echo "-1")
  if [ "$STATE" = "exited" ]; then
    if [ "$EXIT_CODE" = "0" ]; then
      echo ">>> [OK] Schema 迁移成功"
      break
    else
      echo ">>> [错误] Schema 迁移失败 (exit=$EXIT_CODE)，请查看日志: docker logs jasmine-prod-schema"
      exit 1
    fi
  fi
  sleep 5
done

echo "========================================="
echo "  Jasmine 部署 - 阶段 4: 启动所有服务"
echo "========================================="

# ---- 8. 全栈启动 ----
dcomp_env up -d

# ---- 9. 刷新前端 nginx DNS 缓存 ----
sleep 5
docker exec jasmine-prod-frontend nginx -s reload 2>/dev/null || true

echo ""
echo "========================================="
echo "  部署完成！验证登录:"
echo "  curl -X POST http://localhost:8080/user/login \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"username\":\"admin\",\"password\":\"123456\"}'"
echo "========================================="
