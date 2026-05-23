#!/bin/sh
# Jasmine 一键停止脚本 (ops/prod/down.sh)
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

docker compose --env-file "$SCRIPT_DIR/.env" down
echo ">>> Jasmine 生产环境已停止"
