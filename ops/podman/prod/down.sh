#!/bin/sh
# ==============================================================================
# Jasmine 一键停止脚本 —— Podman + Docker Compose 版 (ops/podman/down.sh)
# ==============================================================================
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

cd "$SCRIPT_DIR"
docker compose --env-file .env down
echo ">>> Jasmine (Podman) 环境已停止"
