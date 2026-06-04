#!/usr/bin/env sh
# Build the Android build-environment Docker image.
set -eu

REPO_ROOT=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)
IMAGE="openeksin-build:latest"

command -v docker >/dev/null 2>&1 || { echo "docker not found" >&2; exit 1; }

echo "==> Building Docker image: $IMAGE"
docker build -t "$IMAGE" -f "$REPO_ROOT/docker/Dockerfile" "$REPO_ROOT/docker"
echo "==> Done: $IMAGE"
