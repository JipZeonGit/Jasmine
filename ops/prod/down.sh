#!/bin/sh
# Jasmine 一键停止脚本 (ops/prod/down.sh)
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

cd "$SCRIPT_DIR"
docker compose --env-file .env down 2>/dev/null || docker-compose -f docker-compose.yml down 2>/dev/null || docker compose -f docker-compose.yml down
echo ">>> Jasmine 生产环境已停止"
