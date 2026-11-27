# LOAD01-fix1: fix the review findings of the Load screen

Task-ID: LOAD01-fix1
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/load01
Base: __BASE__
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/load01/docs/load01-tasks/LOAD01-fix1-REPORT.md
Coverage: required

## Why

The deep review of the Load screen is in
`docs/load01-tasks/LOAD01-load-year-DEEP-REPORT.md`. Read it, then
`docs/load01-tasks/LOAD01-load-year.md` and its report. Seven findings
are open. Each is a class: fix every instance, not only the lines the
review names.

Witness files, in
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/LOAD01-load-year-W/`:
`F1.json`, `F2.json`, `F4.json`, `F5.json`, `F6.json`. Run
`~/.config/agents/tools/witness run <file> --sha <your commit>` for each
with `AGENTS_TASKS=/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks`
and `ANDROID_HOME="$HOME/Library/Android/sdk"` exported, and read each
receipt log. All five must print `fixed`. Do not change the witness
files; when one cannot pass for a reason outside the finding, report it
as blocked with the exact error.

The branch now holds a merge of the integration branch (after the
review). It brought the cardio figures (`ui/screens/ScoreRules.kt`) and a
`valueAxis` function in `ui/components/LineChart.kt`. The chart
component is yours to change in this task.

A known fault of this machine: a test worker with native graphics
sometimes hangs at 100 % CPU (seen in `LoadShotsTest` and in full runs,
at a different test each time; no cause in app code was found). Run each
Gradle test command under `timeout` (240 s for one class, 900 s for the
suite). On a hang, `kill -9` the "Gradle Test Executor" process of this
worktree and run again, up to 3 times. Do not spend time on its cause.

## Scope and lead decisions

1. **F1, one bucket per offset.** `pictureBuckets` keeps the first
   sample of each offset (in the stored order) and drops the others.
   Every figure that counts buckets must read them through it: the load,
   the cardio figures (time at or above the hard line, zone shares,
   round peak and drop), the measured maximum when it reads samples, and
   the picture of the Review screen. List every reader of
   `parseSamples` in the Coverage table with its status.
2. **F2, the chart step has no upper end.** Above the present list,
   `niceStep` continues with 2000, 2500, 5000, 10000, 20000, 25000, …
   (1, 2, 2.5, 5 times a power of ten): the smallest such step that is
   at or above `span / target`. For every finite span the value axis has
   at most 5 ticks. The steps of spans up to 3000 do not change (keep
   the present step tests and add the new ones).
3. **F3, point labels do not overlap (every chart).** Keep the present
   position rule of a label (x and y). A pure function chooses the
   labels that draw, from the measured boxes: take the labels in the
   order last point, highest point, then the other labelled points from
   the newest to the oldest; a label draws only when its box, grown by
   2 dp on each side, does not intersect the box of a label that already
   draws. Unit-test the function with boxes (the case of the review:
   seven equal values of four digits at 360 dp and font scale 1.3; two
   points with equal values; a highest point next to the last point).
   Check that no existing chart test or screenshot breaks; a screenshot
   that loses a label because it overlapped is the wanted result: name
   each such picture in the report.
4. **F4, the delay to the next midnight uses instants.** `untilNextDay`
   computes the time between two instants in the zone of the clock, so a
   day of 23 or 25 hours gives the right delay. Every caller of
   `rememberToday` gets the fix through it.
5. **F5, the month label.** `loadMonth` reads only the days that the
   week row draws.
6. **F6, wide sums.** The week load is a `Long`. The day load stays an
   `Int`: its sum is computed wide and held at `Int.MAX_VALUE` when it
   is larger. `loadStep` and every other threshold or sum of the load
   rules use arithmetic that cannot wrap. The chart value and the spoken
   week text follow the `Long`.
7. **F7, tests that fail when the rule breaks.** For each of the five
   weak tests the review names: change it so that the named mutation of
   production code makes it fail, and give the break proof (the mutation,
   the failing output, the passing output after the revert):
   - L7 opens the Load screen through the production navigation graph;
   - L8 fails when the screen passes the points in reverse order;
   - L9 holds its expected alpha values as literals of the test;
   - L10 writes a fill inside the 12 chart weeks and checks the chart
     too;
   - L11 checks the bounds of the chart labels (through the pure
     function of decision 3 or the measured boxes).

No Room, DAO, backup format, permission, dependency, or version change.
No comment that a name, a type, or a test can say; no task or review IDs
in code. Update `SPEC.md` and `DESIGN.md` where a rule changes (load the
`unslop` skill first). Make the 12 screenshots of `LoadShotsTest` again
(folder `android/app/build/shots-load`, create it first) and look at
each one yourself.

## Contract

| # | Finding | Case | Expected result |
|---|---|---|---|
| X1 | F1 | 12 offsets, each twice, at 190 bpm with a maximum of 200; the same row for the cardio figures of a high session; two samples of one offset with 120 and 190 bpm | Load 5, not 10; the time at or above the hard line is 1:00, not 2:00; the first sample counts. Witness F1 prints `fixed`. |
| X2 | F2 | Spans of 3000, 3001, 12600, 1e6, 1e9; a week load of 12,600 next to one of 0 | Steps 1000, 2000, 5000, 500000, 500000000; at most 5 ticks for each; the old step tests pass unchanged. Witness F2 prints `fixed`. |
| X3 | F3 | Seven weeks of 1,668 at 360 dp and font scale 1.3; two equal points; the highest point next to the last | No two drawn label boxes intersect; the last label always draws; the highest draws when it fits. |
| X4 | F4 | New York, 2026-03-08 00:00 (a day of 23 hours) and 2026-11-01 00:00 (25 hours); Tashkent on a plain day | Delays of 23 h, 25 h, 24 h, each plus one second. Witness F4 prints `fixed`. |
| X5 | F5 | Today is Monday 2026-11-30; today is 2026-12-01; a full week that holds the first of a month | No month label; "Dec"; that month. Witness F5 prints `fixed`. |
| X6 | F6 | Day loads of 2,147,483,647 and 1 in one week; `loadStep(600000000, 600000000)`; two session loads on one day whose sum passes `Int.MAX_VALUE` | Week load 2,147,483,648; step 4; day load `Int.MAX_VALUE`. Witness F6 prints `fixed`. |
| X7 | F7 | The five tests of decision 7 | Each fails under its mutation and passes after the revert (break proof in the report). |
| X8 | All | The whole suite; the screenshots; the hard rules | The `Tests:` command passes. 12 pictures made and looked at. Facts only: no verdict word or color, no suggestion, no target. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of each class (instance, status, test, found by) and the five witness results.
- The `Tests:` command passes on the final commit. Commit on branch `load01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
