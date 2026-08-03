#!/bin/bash
# ==============================================================================
# Jasmine 一键部署脚本 (ops/docker/prod/up.sh)
#
# 确保脚本在本目录执行，需先: chmod +x up.sh down.sh
# ==============================================================================
set -euo pipefail

D="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ENV_FILE="$D/.env"

# ---- 载入 .env ----
if [ ! -f "$ENV_FILE" ]; then echo ">>> [错误] 缺少 $ENV_FILE"; exit 1; fi
set -a; . "$ENV_FILE"; set +a

# 兼容 sudo / 不同环境的 docker compose
if docker compose version &>/dev/null 2>&1; then
  DC="docker compose --env-file $ENV_FILE"
else
  DC="docker-compose -f $D/docker-compose.yml"
fi

echo "========================================="
echo " Jasmine 部署 阶段1/4: 中间件"
echo "========================================="

$DC pull
$DC up -d mysql redis rabbitmq nacos

echo ">>> 等待 Nacos healthy..."
until [ "$(docker inspect -f '{{.State.Health.Status}}' jasmine-prod-nacos 2>/dev/null)" = "healthy" ]; do
  sleep 5
done
echo ">>> [OK] Nacos 就绪"

# 补种 Nacos 管理员用户
if [ "$(docker exec jasmine-prod-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -s \
  -e "SELECT COUNT(*) FROM nacos.users WHERE username='nacos'" 2>/dev/null || echo 0)" = "0" ]; then
  echo ">>> 补种 Nacos 管理员用户..."
  docker exec jasmine-prod-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" nacos -e "
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
docker exec jasmine-prod-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" nacos -e \
  "INSERT IGNORE INTO tenant_info (kp,tenant_id,tenant_name,tenant_desc,create_source,gmt_create,gmt_modified)
   VALUES ('1','$NS','$NS','Jasmine $NS','empty',$TS,$TS)"
echo ">>> [OK] 命名空间 $NS"

# 密码自愈 + 导入配置
NPH="${NACOS_PORT:-8848}"
IMPORT_SCRIPT="$(CDPATH= cd -- "$D/../.." && pwd)/nacos-config/import.sh"
bash "$IMPORT_SCRIPT" "127.0.0.1:$NPH" "$NS"

echo "========================================="
echo " Jasmine 部署 阶段3/4: Schema 迁移"
echo "========================================="

$DC up -d jasmine-schema
echo ">>> 等待 Schema 完成..."
while [ "$(docker inspect -f '{{.State.Status}}' jasmine-prod-schema 2>/dev/null)" != "exited" ]; do
  sleep 3
done
EXIT=$(docker inspect -f '{{.State.ExitCode}}' jasmine-prod-schema 2>/dev/null)
if [ "$EXIT" != "0" ]; then
  echo ">>> [错误] Schema 迁移失败 (exit=$EXIT)"
  docker logs jasmine-prod-schema --tail=20 2>&1
  exit 1
fi
echo ">>> [OK] Schema 迁移成功"

echo "========================================="
echo " Jasmine 部署 阶段4/4: 启动服务"
echo "========================================="

$DC up -d
sleep 5
docker exec jasmine-prod-frontend nginx -s reload 2>/dev/null || true

echo ""
echo "========================================="
echo " 部署完成！验证:"
echo " curl -X POST http://localhost:8080/user/login \\"
echo "   -H 'Content-Type: application/json' \\"
echo "   -d '{\"username\":\"admin\",\"password\":\"123456\"}'"
echo "========================================="
