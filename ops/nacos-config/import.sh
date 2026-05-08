#!/bin/bash
# ============================================================
# Nacos 配置导入脚本
# 用法: ./import.sh <nacos-addr> [namespace]
# 示例: ./import.sh 192.168.31.26:8848 dev
# ============================================================
set -euo pipefail

NACOS_ADDR="${1:?用法: $0 <nacos-addr> [namespace]}"
NAMESPACE="${2:-dev}"
GROUP="JASMINE"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Nacos 鉴权（与 docker-compose 中的默认值一致）
NACOS_USER="${NACOS_USERNAME:-nacos}"
NACOS_PASS="${NACOS_PASSWORD:-nacos}"

echo "=== 导入 Nacos 配置 ==="
echo "地址: $NACOS_ADDR | 命名空间: $NAMESPACE | 分组: $GROUP"
echo ""

# 获取 namespace ID（Nacos 用 namespace ID 而非名称）
NS_ID=$(curl -s "http://$NACOS_ADDR/nacos/v1/console/namespaces?accessToken=&namespace=$NAMESPACE" \
  --user "$NACOS_USER:$NACOS_PASS" 2>/dev/null | grep -o "\"namespace\":\"$NAMESPACE\"" || true)

if [ -z "$NS_ID" ]; then
  echo ">>> 创建命名空间: $NAMESPACE"
  curl -s -X POST "http://$NACOS_ADDR/nacos/v1/console/namespaces" \
    --user "$NACOS_USER:$NACOS_PASS" \
    -d "customNamespaceId=$NAMESPACE&namespaceName=$NAMESPACE&namespaceDesc=Jasmine+dev+environment" > /dev/null
  echo ""
fi

for file in "$SCRIPT_DIR"/*.yml "$SCRIPT_DIR"/*.yaml; do
  [ -f "$file" ] || continue
  data_id="$(basename "$file")"
  content="$(cat "$file")"

  echo ">>> 导入: $data_id"
  curl -s -X POST "http://$NACOS_ADDR/nacos/v1/cs/configs" \
    --user "$NACOS_USER:$NACOS_PASS" \
    -d "dataId=$data_id&group=$GROUP&tenant=$NAMESPACE&type=yaml&content=$content" | head -1
  echo ""
done

echo "=== 导入完成 ==="
echo ""
echo "验证: 打开 Nacos 控制台 http://$NACOS_ADDR/nacos"
echo "       命名空间: $NAMESPACE | 分组: $GROUP"
