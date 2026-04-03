#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$(cd "$ROOT_DIR/.." && pwd)/xs-bbs-frontend"

if [ ! -d "$FRONTEND_DIR" ]; then
  echo "Missing frontend directory: $FRONTEND_DIR" >&2
  exit 1
fi

cd "$FRONTEND_DIR"
npm run dev -- "$@"
