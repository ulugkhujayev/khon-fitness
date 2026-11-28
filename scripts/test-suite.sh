#!/bin/bash
# Runs the unit suite and the release checks. A Robolectric test worker hangs now and then, and Gradle ends it in
# one of two ways (see android/app/build.gradle.kts). The JVM aborts when a thread blocks a safepoint: it prints
# the timeout warning, then a fatal error report. Any other hang ends at the timeout of the test task. A run that
# ends in one of these two ways and reports no failed test runs again. This script sets no time limit of its own.
set -u
cd "$(dirname "$0")/.."
SAFEPOINT_TIMEOUT="SafepointSynchronize::begin: Timeout detected"
JVM_FATAL="A fatal error has been detected by the Java Runtime Environment"
TASK_TIMEOUT="as it has exceeded its configured timeout of"
MAX_RUNS=3
if [ $# -eq 0 ]; then
    set -- android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
fi
log=$(mktemp)
trap 'rm -f "$log"' EXIT

reports_failed_test() {
    grep -Eq -e '^[^>].* > .* FAILED$' -e 'tests completed, [1-9][0-9]* failed' "$log"
}

for run in $(seq 1 "$MAX_RUNS"); do
    "$@" < /dev/null 2>&1 | tee "$log"
    status=${PIPESTATUS[0]}
    [ "$status" -eq 0 ] && exit 0
    reports_failed_test && exit "$status"
    if grep -qF "$SAFEPOINT_TIMEOUT" "$log" && grep -qF "$JVM_FATAL" "$log"; then
        echo "test-suite: run $run ended on a blocked safepoint"
    elif grep -qF "$TASK_TIMEOUT" "$log"; then
        echo "test-suite: run $run passed the timeout of the test task"
    else
        exit "$status"
    fi
done
echo "test-suite: $MAX_RUNS runs hung or ended on a blocked safepoint"
exit 1
