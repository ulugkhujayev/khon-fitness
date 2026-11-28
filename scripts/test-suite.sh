#!/bin/bash
# Runs the unit suite and the release checks. A test worker that the JVM ended for blocking a safepoint
# (see android/app/build.gradle.kts) is a fault of the runtime, not of a test: only that case runs again.
set -u
cd "$(dirname "$0")/.."
ABORT_MARK="Safepoint sync time longer than"
MAX_RUNS=3
if [ $# -eq 0 ]; then
    set -- android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
fi
log=$(mktemp)
trap 'rm -f "$log"' EXIT
for run in $(seq 1 "$MAX_RUNS"); do
    "$@" 2>&1 | tee "$log"
    status=${PIPESTATUS[0]}
    [ "$status" -eq 0 ] && exit 0
    if ! grep -q "$ABORT_MARK" "$log"; then
        exit "$status"
    fi
    echo "test-suite: run $run ended on a blocked safepoint"
done
echo "test-suite: $MAX_RUNS runs ended on a blocked safepoint"
exit 1
