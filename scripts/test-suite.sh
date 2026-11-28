#!/bin/bash
# Runs the unit suite and the release checks. A Robolectric test worker hangs now and then. The JVM ends one that
# blocks a safepoint (see android/app/build.gradle.kts): it prints the timeout warning, then a fatal error report,
# either "Safepoint sync time longer than" or a SIGILL crash. Other hangs end at RUN_LIMIT_SECONDS, counted until the
# output closes: the command runs in its own process group, and the whole group ends there. A hung run never passes.
# A run that ends in one of these two ways and reports no failed test runs again.
set -u
cd "$(dirname "$0")/.."
ROOT=$(pwd -P)
SAFEPOINT_TIMEOUT="SafepointSynchronize::begin: Timeout detected"
JVM_FATAL="A fatal error has been detected by the Java Runtime Environment"
WORKER_CLASS="worker.org.gradle.process.internal.worker.GradleWorkerMain"
WORKER_MARK="-Dorg.gradle.internal.worker.tmpdir=$ROOT/android/app/build/"
LIMIT=${RUN_LIMIT_SECONDS:-900}
GRACE_SECONDS=5
MAX_RUNS=3
if [ $# -eq 0 ]; then
    set -- android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
fi
work=$(mktemp -d)
log="$work/log"
out="$work/out"
hung="$work/hung"
cmd=""
teed=""
watchdog=""

wait_gone() {
    local waited=0
    while kill -0 -- "$1" 2>/dev/null && [ "$waited" -lt "$GRACE_SECONDS" ]; do
        sleep 1
        waited=$((waited + 1))
    done
}

end_workers() {
    local pid
    for pid in $(ps -axww -o pid= -o command= | CLASS="$WORKER_CLASS" MARK="$WORKER_MARK" awk '
        { line = $0 " " }
        index(line, " " ENVIRON["CLASS"] " ") && index(line, " " ENVIRON["MARK"]) { print $1 }'); do
        [ "$pid" != "$$" ] && kill -KILL "$pid" 2>/dev/null
    done
}

watch_run() {
    local start=$SECONDS
    while kill -0 "$teed" 2>/dev/null; do
        if [ $((SECONDS - start)) -ge "$LIMIT" ]; then
            touch "$hung"
            kill -TERM -- "-$cmd" 2>/dev/null
            wait_gone "-$cmd"
            kill -KILL -- "-$cmd" 2>/dev/null
            wait_gone "$teed"
            kill -KILL "$teed" 2>/dev/null
            return
        fi
        sleep 1
    done
}

reports_failed_test() {
    grep -Eq -e '^[^>].* > .* FAILED$' -e 'tests completed, [1-9][0-9]* failed' "$log"
}

stop() {
    [ -n "$watchdog" ] && kill "$watchdog" 2>/dev/null
    [ -n "$cmd" ] && kill -TERM -- "-$cmd" 2>/dev/null
    [ -n "$teed" ] && kill -TERM "$teed" 2>/dev/null
    exit "$1"
}
trap 'rm -rf "$work"' EXIT
trap 'stop 130' INT
trap 'stop 143' TERM

for run in $(seq 1 "$MAX_RUNS"); do
    rm -f "$hung" "$out"
    mkfifo "$out"
    tee "$log" < "$out" &
    teed=$!
    set -m
    "$@" < /dev/null > "$out" 2>&1 &
    cmd=$!
    set +m
    watch_run > /dev/null 2>&1 &
    watchdog=$!
    wait "$cmd"
    status=$?
    wait "$teed"
    wait "$watchdog"
    cmd=""
    teed=""
    watchdog=""
    if [ -e "$hung" ]; then
        end_workers
        echo "test-suite: run $run passed the time limit of $LIMIT s"
        if reports_failed_test; then
            [ "$status" -eq 0 ] && status=1
            exit "$status"
        fi
        continue
    fi
    [ "$status" -eq 0 ] && exit 0
    reports_failed_test && exit "$status"
    if grep -qF "$SAFEPOINT_TIMEOUT" "$log" && grep -qF "$JVM_FATAL" "$log"; then
        echo "test-suite: run $run ended on a blocked safepoint"
    else
        exit "$status"
    fi
done
echo "test-suite: $MAX_RUNS runs hung or ended on a blocked safepoint"
exit 1
