#!/usr/bin/env sh
# Compile the app inside the build container. The project is bind-mounted, so
# the resulting APKs land on the host under app/build/outputs/apk/.
#
# Usage: scripts/indockerbuild.sh [gradle-tasks...]
#   default tasks: :app:assemblePlayDebug :app:assembleFdroidDebug
set -eu

REPO_ROOT=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)
IMAGE="openeksin-build:latest"

command -v docker >/dev/null 2>&1 || { echo "docker not found" >&2; exit 1; }

if [ "$#" -gt 0 ]; then
    TASKS="$*"
else
    TASKS=":app:assemblePlayDebug :app:assembleFdroidDebug"
fi

# Generate the Gradle wrapper on first run (so the repo gets a committable
# wrapper), then build. Gradle home is kept inside the project to stay writable
# under the mapped user.
INNER="set -e
export GRADLE_USER_HOME=/work/.gradle
if [ ! -f /work/gradlew ]; then
  gradle wrapper --gradle-version 8.7 --no-daemon
fi
./gradlew ${TASKS} --no-daemon --stacktrace"

echo "==> Building tasks: $TASKS"
docker run --rm \
    -u "$(id -u):$(id -g)" \
    -e HOME=/work \
    -v "$REPO_ROOT":/work \
    -w /work \
    "$IMAGE" \
    sh -c "$INNER"

echo "==> APK output:"
find "$REPO_ROOT/app/build/outputs/apk" -name '*.apk' 2>/dev/null || true
