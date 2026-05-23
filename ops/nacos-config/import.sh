#!/bin/bash
# ============================================================
# Nacos 3.x 配置导入脚本
# 用法: ./import.sh <nacos-addr> [namespace]
# 示例: ./import.sh 127.0.0.1:8848 prod
# 可选: NACOS_SCHEME=https ./import.sh nacos.example.com prod
#
# 通过环境变量传入鉴权信息（不要把账号密码写进仓库）：
#   NACOS_USERNAME / NACOS_PASSWORD
#
# 适配: Nacos 3.0.3（命名空间创建走 MySQL tenant_info 表，
#       因为 /v1/console/namespaces 接口在 3.x 返回 404）
# ============================================================
set -euo pipefail

NACOS_ADDR="${1:?用法: $0 <nacos-addr> [namespace]}"
NAMESPACE="${2:-prod}"
GROUP="JASMINE"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NACOS_SCHEME="${NACOS_SCHEME:-http}"

NACOS_USER="${NACOS_USERNAME:-nacos}"
NACOS_PASS="${NACOS_PASSWORD:-nacos}"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"

BASE="$NACOS_SCHEME://$NACOS_ADDR/nacos"

echo "=== 导入 Nacos 配置 (3.x) ==="
echo "地址: $BASE | 命名空间: $NAMESPACE | 分组: $GROUP"
echo ""

# -----------------------------------------------------------
# 鉴权：尝试登录获取 accessToken（auth 关闭时快速跳过）
# -----------------------------------------------------------
ACCESS_TOKEN=""

if [ "${NACOS_AUTH_ENABLED:-true}" != "false" ]; then
  echo ">>> 尝试登录 Nacos 获取 accessToken..."
  for i in {1..5}; do
    LOGIN_RESP="$(curl -sS -X POST "$BASE/v1/auth/users/login" \
      --data-urlencode "username=$NACOS_USER" \
      --data-urlencode "password=$NACOS_PASS" || true)"

    if echo "$LOGIN_RESP" | grep -q '"accessToken"'; then
      ACCESS_TOKEN="$(echo "$LOGIN_RESP" | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
      echo ">>> [OK] 登录成功"
      break
    fi

    echo ">>> 登录尝试 $i/5 失败: ${LOGIN_RESP:-'(无内容)'}"
    [ $i -lt 5 ] && sleep 3
  done

  if [ -z "$ACCESS_TOKEN" ]; then
    echo ">>> [警告] 登录失败，后续请求将以未鉴权方式发送..."
  fi
else
  echo ">>> Nacos 鉴权已关闭 (NACOS_AUTH_ENABLED=false)，跳过登录"
fi

# 统一拼装鉴权参数
auth_query() {
  if [ -n "$ACCESS_TOKEN" ]; then
    printf 'accessToken=%s' "$ACCESS_TOKEN"
  fi
}

# -----------------------------------------------------------
# 命名空间创建：Nacos 3.x 的 console API 返回 404，
# 直接通过 MySQL INSERT tenant_info 表来创建命名空间。
# -----------------------------------------------------------
create_namespace_via_mysql() {
  local ns_id="$1"

  # 检查 mysql 命令是否可用
  if ! command -v mysql &>/dev/null; then
    echo ">>> [警告] 未找到 mysql 客户端命令，无法自动创建命名空间 '$ns_id'"
    echo ">>> 请手动在 Nacos 控制台或 MySQL 中创建命名空间后重试"
    return 1
  fi

  # 检查是否已存在
  local exists
  exists=$(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" \
    -N -e "SELECT COUNT(*) FROM nacos.tenant_info WHERE tenant_id='$ns_id'" 2>/dev/null || echo "0")

  if [ "$exists" -gt 0 ]; then
    echo ">>> 命名空间已存在: $ns_id"
    return 0
  fi

  echo ">>> 通过 MySQL 创建命名空间: $ns_id"
  local ts
  ts=$(date +%s)000

  mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" \
    -e "INSERT INTO nacos.tenant_info (kp, tenant_id, tenant_name, tenant_desc, create_source, gmt_create, gmt_modified)
        VALUES ('1', '$ns_id', '$ns_id', 'Jasmine $ns_id', 'empty', $ts, $ts);" 2>/dev/null

  if [ $? -eq 0 ]; then
    echo ">>> 命名空间创建成功: $ns_id"
    return 0
  else
    echo ">>> [错误] 命令空间创建失败，请检查 MySQL 连接参数"
    return 1
  fi
}

echo ">>> 检查/创建命名空间: $NAMESPACE"
create_namespace_via_mysql "$NAMESPACE"
echo ""

# -----------------------------------------------------------
# 导入 YAML 配置
# -----------------------------------------------------------
echo ">>> 开始导入配置文件 ..."
for file in "$SCRIPT_DIR"/*.yml "$SCRIPT_DIR"/*.yaml; do
  [ -f "$file" ] || continue
  data_id="$(basename "$file")"
  echo ">>> 导入: $data_id"
  CFG_QUERY="$(auth_query)"
  curl -fsS -X POST "$BASE/v1/cs/configs${CFG_QUERY:+?$CFG_QUERY}" \
    --data-urlencode "dataId=$data_id" \
    --data-urlencode "group=$GROUP" \
    --data-urlencode "tenant=$NAMESPACE" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@$file" | head -1
  echo ""
done

echo "=== 导入完成 ==="
echo "验证: 打开 Nacos 控制台 $BASE  (命名空间: $NAMESPACE | 分组: $GROUP)"
