# UI05 P5-FLAKY: screen tests that fail only in the full test run

Class: Normal (test infrastructure). Use the `diagnosing-bugs` playbook.
Read `COMMON.md` in this folder first. Base: `4a44cd7` on `ui05-port`.
The final merge check runs the full suite once, so these failures block
the 0.5.0 release.

## Symptom

`./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease`
(from `android/`, JAVA_HOME as in `COMMON.md`) failed in 2 of the last 3
full runs with the same 11 failures (389 tests):

- `OpenPickKeptTest` (4) and `PendingFocusTest` (6): `AppNotIdleException:
  Compose did not get idle after ~10,000,000 attempts in 60 SECONDS`, from
  `waitForIdle`.
- `StretchDeleteScreenTest` "a delete refused at confirm time keeps the
  stretch and shows the reason" (`StretchDeleteScreenTest.kt:69,104`):
  "the screen did not reach the expected state".

The same idle failure appeared once earlier on `a931162` (394 tests).

## Ruled out (lead, ran)

- `OpenPickKeptTest` + `PendingFocusTest` alone, with `--rerun-tasks`:
  10 tests, 0 failures.
- Each with `NewRoutineScreenTest`, `NumberFieldFitTest`, or
  `CardioIntervalsKeptTest` before it: 0 failures.
- Gradle runs all classes in one JVM (no `maxParallelForks` or
  `forkEvery` in `app/build.gradle.kts`). In the failing run, the order
  was: `NewRoutineScreenTest` first (16:37:20), then the data, health,
  timer, ui and components classes, then `OpenPickKeptTest` at 16:37:33.

## Goal

Find the cause with evidence: a reproduction that fails reliably (for
example a filtered run of the classes before it, or a loop), then the
state that leaks or the clock or loop that keeps Compose busy. Fix the
cause in the tests or the app code it shows. Retries, longer timeouts,
`@Ignore`, or `forkEvery` alone are not a fix. If the cause is in app
code, report it before you change behavior.

## Done

The reproduction fails before the fix and passes after it. Three full
runs of the command above pass in a row, with 0 failures. Report under
20 lines: the cause with file:line, the reproduction command and its
output before and after, the commits, and the three run results. Do not
end your turn while a check you started is still running.
