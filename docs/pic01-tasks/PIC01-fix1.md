# PIC01-fix1: fix the review findings of the workout picture

Task-ID: PIC01-fix1
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/pic01
Base: 2100daf
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/pic01/docs/pic01-tasks/PIC01-fix1-REPORT.md
Coverage: required

## Why

The deep review of the workout picture is in
`docs/pic01-tasks/PIC01-DEEP-REPORT.md`. Read it, then
`docs/pic01-tasks/PIC01-workout-picture.md` (with the lead decisions at
its end) and its report. Three findings are open. Each is a class: fix
every instance, not only the lines the review names.

Witness files, in
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/PIC01-workout-picture-W/`:
`F1.json`, `F2.json`, `F3.json`. All three print `reproduces` on the
base. Run `~/.config/agents/tools/witness run <file> --sha <your commit>`
for each with
`AGENTS_TASKS=/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks`
and `ANDROID_HOME="$HOME/Library/Android/sdk"` exported, and read each
receipt log to confirm what ran. All three must print `fixed`.

## Scope

1. **F1, a bucket in the last partial second.** A bucket belongs to the
   picture when its time in milliseconds is inside the half-open range:
   `fromMs + offsetSec * 1000 < toMs`, computed without overflow. Search
   every other place of the picture rules that turns the range or a time
   into whole seconds (set marks, spans of set rows, round bands, the
   axis, the spoken text) for the same loss at either end of the range,
   and close each instance.
2. **F2, pixel tests that accept a wrong drawing.** The tests of the
   chart take their expected positions from the fixture's times and bpm:
   curve points at their x and y (some pixels of tolerance), no accent
   pixel inside a gap, the lone dot at its place, both ends of each tick
   (it stands on the bottom of the plot), each band at three heights and
   at both of its edges. Each of the four mutations of the F2 witness
   (curve y position, tick y position, band vertical span, lone dot)
   must fail a test, and so must a curve shifted in x and a band shifted
   in x.
3. **F3, comments.** Remove every comment block the review lists, and
   every other comment in the files of this change that a name, a type,
   or a test can say. Where a comment holds a rule, a unit, or a reason
   that the code cannot show, move it into a name (a constant, a
   function, a test name). Keep a comment only for a quirk of something
   we cannot change (Android, Compose, Robolectric), and list each kept
   one in the report with its reason.

Do not change what the screen shows, texts, `SPEC.md`, or `DESIGN.md`
(except a line that F1 makes wrong). No Room, DAO, repository, backup,
dependency, or version change. Do not change the `timer` or `data`
packages. No task or review IDs in code.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| X1 | Range end | A range of 600,002 ms with a bucket at 600 s that is the highest; a bucket exactly at the range length; a set checked in the last partial second | The 600 s bucket is in the curve, the axis, the spoken text, and the set row's bpm; the bucket at the length is not; the set has its mark. Witness F1 `fixed`. |
| X2 | Pixel tests | Each of the six mutations of Scope 2 | Each fails a test (break proof in the report); the tests pass on the unchanged drawing. Witness F2 `fixed`. |
| X3 | Comments | The files of this change | No comment that a name, a type, or a test can say; kept ones listed. Witness F3 `fixed`. |
| X4 | Old rows | Rows H1 to H12 of the first task | Still hold; each changed assertion is listed with its reason. The 12 screenshots are made again and looked at; they show no change. |
| X5 | Rules | Whole change | No change outside the files of the first change and the report. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of each class (instance, status, test, found by) and the three witness results.
- The `Tests:` command passes on the final commit. Commit on branch `pic01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
