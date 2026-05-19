#!/bin/bash
# ============================================================
# Nacos 配置导入脚本
# 用法: ./import.sh <nacos-addr> [namespace]
# 示例: ./import.sh 127.0.0.1:8848 dev
# 可选: NACOS_SCHEME=https ./import.sh nacos.example.com prod
#
# 通过环境变量传入鉴权信息（不要把账号密码写进仓库）：
#   NACOS_USERNAME / NACOS_PASSWORD
# ============================================================
set -euo pipefail

NACOS_ADDR="${1:?用法: $0 <nacos-addr> [namespace]}"
NAMESPACE="${2:-dev}"
GROUP="JASMINE"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NACOS_SCHEME="${NACOS_SCHEME:-http}"

NACOS_USER="${NACOS_USERNAME:-nacos}"
NACOS_PASS="${NACOS_PASSWORD:-nacos}"

BASE="$NACOS_SCHEME://$NACOS_ADDR/nacos"

echo "=== 导入 Nacos 配置 ==="
echo "地址: $BASE | 命名空间: $NAMESPACE | 分组: $GROUP"
echo ""

# Nacos 2.x 在开启 auth 时必须先用账号密码换 accessToken
# dev / 本地未开启 auth 时跳过登录
ACCESS_TOKEN=""
LOGIN_RESP="$(curl -sS -X POST "$BASE/v1/auth/users/login" \
  --data-urlencode "username=$NACOS_USER" \
  --data-urlencode "password=$NACOS_PASS" || true)"
if echo "$LOGIN_RESP" | grep -q '"accessToken"'; then
  ACCESS_TOKEN="$(echo "$LOGIN_RESP" | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
  echo ">>> 已获取 accessToken"
else
  echo ">>> 未获取到 accessToken（可能未开启 auth），后续请求按未鉴权方式发送"
fi

# 统一拼装鉴权参数（开启 auth 时附带 accessToken，否则为空）
auth_query() {
  if [ -n "$ACCESS_TOKEN" ]; then
    printf 'accessToken=%s' "$ACCESS_TOKEN"
  fi
}

# 检查命名空间是否存在；不存在则创建
NS_QUERY="$(auth_query)"
NS_RESP="$(curl -fsS "$BASE/v1/console/namespaces${NS_QUERY:+?$NS_QUERY}")"
if ! echo "$NS_RESP" | grep -q "\"namespace\":\"$NAMESPACE\""; then
  echo ">>> 创建命名空间: $NAMESPACE"
  curl -fsS -X POST "$BASE/v1/console/namespaces${NS_QUERY:+?$NS_QUERY}" \
    --data-urlencode "customNamespaceId=$NAMESPACE" \
    --data-urlencode "namespaceName=$NAMESPACE" \
    --data-urlencode "namespaceDesc=Jasmine $NAMESPACE environment" > /dev/null
fi

# 逐个导入 YAML，使用 --data-urlencode + content@file 保持原文不被表单语义破坏
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
