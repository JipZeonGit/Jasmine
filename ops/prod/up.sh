#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
OPS_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

docker compose --env-file "$OPS_DIR/.env" -f "$SCRIPT_DIR/docker-compose.yml" pull
docker compose --env-file "$OPS_DIR/.env" -f "$SCRIPT_DIR/docker-compose.yml" up -d
