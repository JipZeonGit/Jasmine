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

# Nacos 2.x 智能鉴权与密码自愈同步逻辑
ACCESS_TOKEN=""

echo ">>> 尝试使用配置的密码进行登录..."
for i in {1..5}; do
  LOGIN_RESP="$(curl -sS -X POST "$BASE/v1/auth/users/login" \
    --data-urlencode "username=$NACOS_USER" \
    --data-urlencode "password=$NACOS_PASS" || true)"
  
  if echo "$LOGIN_RESP" | grep -q '"accessToken"'; then
    ACCESS_TOKEN="$(echo "$LOGIN_RESP" | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
    echo ">>> [OK] 成功使用配置的自定义密码登录并获取 accessToken！"
    break
  fi
  
  echo ">>> [配置密码尝试 $i/5] 接口返回内容: ${LOGIN_RESP:-'(无内容)'}"
  if [ $i -lt 5 ]; then
    echo ">>> Nacos 服务或数据库连接池可能在初始化中，等待3秒后重试..."
    sleep 3
  fi
done

if [ -z "$ACCESS_TOKEN" ]; then
  echo ">>> [提示] 使用配置的密码登录失败，尝试使用 Nacos 默认密码 'nacos' 进行登录..."
  for i in {1..5}; do
    DEFAULT_LOGIN_RESP="$(curl -sS -X POST "$BASE/v1/auth/users/login" \
      --data-urlencode "username=nacos" \
      --data-urlencode "password=nacos" || true)"
    
    if echo "$DEFAULT_LOGIN_RESP" | grep -q '"accessToken"'; then
      DEFAULT_TOKEN="$(echo "$DEFAULT_LOGIN_RESP" | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
      echo ">>> [OK] 成功使用默认密码登录！"
      
      # 如果用户配置了自定义密码，且目前数据库里还是默认密码，则启动自愈同步
      if [ "$NACOS_PASS" != "nacos" ]; then
        echo ">>> [自愈启动] 正在自动将 Nacos 数据库中的管理员密码同步修改为你 .env 中的自定义密码..."
        # Nacos 2.x 修改密码 API (PUT 表单传参 username, newPassword)
        curl -fsS -X PUT "$BASE/v1/auth/users?accessToken=$DEFAULT_TOKEN" \
          --data-urlencode "username=nacos" \
          --data-urlencode "newPassword=$NACOS_PASS" > /dev/null
        
        echo ">>> [自愈成功] Nacos 数据库密码已热更新！正在重新以最新密码获取 accessToken..."
        # 重新使用新密码登录获取 token
        NEW_LOGIN_RESP="$(curl -sS -X POST "$BASE/v1/auth/users/login" \
          --data-urlencode "username=$NACOS_USER" \
          --data-urlencode "password=$NACOS_PASS" || true)"
        
        if echo "$NEW_LOGIN_RESP" | grep -q '"accessToken"'; then
          ACCESS_TOKEN="$(echo "$NEW_LOGIN_RESP" | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
          echo ">>> [OK] 成功获取最新的自定义密码 accessToken！"
        else
          echo ">>> [警告] 重新获取 accessToken 失败，接口返回: $NEW_LOGIN_RESP"
        fi
      else
        ACCESS_TOKEN="$DEFAULT_TOKEN"
      fi
      break
    fi
    
    echo ">>> [默认密码尝试 $i/5] 接口返回内容: ${DEFAULT_LOGIN_RESP:-'(无内容)'}"
    if [ $i -lt 5 ]; then
      echo ">>> 正在等待 Nacos 鉴权服务就绪，等待3秒后重试..."
      sleep 3
    fi
  done
fi

if [ -z "$ACCESS_TOKEN" ]; then
  echo ">>> [警告] 无法使用配置密码或默认密码登录。后续请求将以未鉴权方式发送..."
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
