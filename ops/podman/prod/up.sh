#!/bin/bash
# ==============================================================================
# Jasmine 一键部署脚本 —— Podman + Docker Compose 版 (ops/podman/up.sh)
#
# 适配环境：Fedora 等使用 Podman 作为容器运行时，但用 docker compose CLI
#           （非 podman compose）通过 Docker 兼容后端驱动 Podman。
#
# 前置：
#   1) docker compose 能连通 Podman（见 README.md 第 4 节，常见方式：
#      systemctl --user start podman.socket 后导出
#      DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock）
#   2) 本脚本内对容器的查询/执行操作使用 podman CLI，请确保已安装 podman。
#
# 确保脚本在本目录执行，需先: chmod +x up.sh down.sh
# ==============================================================================
set -euo pipefail

D="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$D/.env"

# ---- 载入 .env ----
if [ ! -f "$ENV_FILE" ]; then echo ">>> [错误] 缺少 $ENV_FILE，请先 cp .env.example .env"; exit 1; fi
set -a; . "$ENV_FILE"; set +a

# ---- docker compose 命令（指向 Podman 兼容后端） ----
DC="docker compose --env-file $ENV_FILE"

# podman CLI（用于容器查询/执行/日志）
PM="${PODMAN_BIN:-podman}"

echo "========================================="
echo " Jasmine 部署 阶段1/4: 中间件 (Podman)"
echo "========================================="

$DC pull || true
$DC up -d mysql redis rabbitmq nacos

echo ">>> 等待 Nacos healthy..."
until [ "$($PM inspect -f '{{.State.Health.Status}}' jasmine-podman-nacos 2>/dev/null)" = "healthy" ]; do
  sleep 5
done
echo ">>> [OK] Nacos 就绪"

# 补种 Nacos 管理员用户
if [ "$($PM exec jasmine-podman-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -s \
  -e "SELECT COUNT(*) FROM nacos.users WHERE username='nacos'" 2>/dev/null || echo 0)" = "0" ]; then
  echo ">>> 补种 Nacos 管理员用户..."
  $PM exec jasmine-podman-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" nacos -e "
    INSERT IGNORE INTO users (username,password,enabled) VALUES ('nacos','\$2a\$10\$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu',TRUE);
    INSERT IGNORE INTO roles (username,role) VALUES ('nacos','ROLE_ADMIN');
  "
fi

echo "========================================="
echo " Jasmine 部署 阶段2/4: Nacos 配置"
echo "========================================="

# 创建 prod 命名空间
TS=$(date +%s)000
NS="${NACOS_NAMESPACE:-prod}"
$PM exec jasmine-podman-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" nacos -e \
  "INSERT IGNORE INTO tenant_info (kp,tenant_id,tenant_name,tenant_desc,create_source,gmt_create,gmt_modified)
   VALUES ('1','$NS','$NS','Jasmine $NS','empty',$TS,$TS)"
echo ">>> [OK] 命名空间 $NS"

# 导入配置（鉴权信息通过环境变量传给 import.sh）
NPH="${NACOS_PORT:-8848}"
IMPORT_SCRIPT="$(CDPATH= cd -- "$D/../.." && pwd)/nacos-config/import.sh"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}" NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}" \
  bash "$IMPORT_SCRIPT" "127.0.0.1:$NPH" "$NS"

echo "========================================="
echo " Jasmine 部署 阶段3/4: Schema 迁移"
echo "========================================="

$DC up -d jasmine-schema
echo ">>> 等待 Schema 完成..."
while [ "$($PM inspect -f '{{.State.Status}}' jasmine-podman-schema 2>/dev/null)" != "exited" ]; do
  sleep 3
done
EXIT=$($PM inspect -f '{{.State.ExitCode}}' jasmine-podman-schema 2>/dev/null)
if [ "$EXIT" != "0" ]; then
  echo ">>> [错误] Schema 迁移失败 (exit=$EXIT)"
  $PM logs jasmine-podman-schema --tail=20 2>&1
  exit 1
fi
echo ">>> [OK] Schema 迁移成功"

echo "========================================="
echo " Jasmine 部署 阶段4/4: 启动服务"
echo "========================================="

$DC up -d
sleep 5
$PM exec jasmine-podman-frontend nginx -s reload 2>/dev/null || true

echo ""
echo "========================================="
echo " 部署完成！验证:"
echo " curl -X POST http://localhost:${GATEWAY_PORT:-8080}/user/login \\"
echo "   -H 'Content-Type: application/json' \\"
echo "   -d '{\"username\":\"admin\",\"password\":\"123456\"}'"
echo " 前端: http://localhost:${FRONTEND_PORT:-8081}/"
echo "========================================="
