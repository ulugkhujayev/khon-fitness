# SC01-fix1: fix the review findings of the cardio heart rate figures

Task-ID: SC01-fix1
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/sc01
Base: 2248c44
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/sc01/docs/sc01-tasks/SC01-fix1-REPORT.md
Coverage: required

## Why

The deep review of the cardio heart rate figures is in
`docs/sc01-tasks/SC01-DEEP-REPORT.md`. Read it, then
`docs/sc01-tasks/SC01-cardio-scores.md` (with the lead decisions at its
end) and its report. Seven findings are open. Each is a class: fix every
instance, not only the lines the review names.

Witness files, in
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/SC01-cardio-scores-W/`:
`F1.json` to `F5.json`. All five print `reproduces` on the base, each at
an assertion. Run
`~/.config/agents/tools/witness run <file> --sha <your commit>` for each
with `AGENTS_TASKS=/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks`
and `ANDROID_HOME="$HOME/Library/Android/sdk"` exported, and read each
receipt log. All five must print `fixed`. Do not change the witness
files; when one cannot pass for a reason outside the finding, report it
as blocked with the exact error.

## Scope

Lead decisions:

1. **F1, the window of the measured maximum.** A session counts when its
   finish is inside `[now - 365 days, now]`, both ends inside. The value
   is computed again when a screen that shows it resumes and when its
   inputs change, so an expired or a future value never stays on screen.
2. **F2, the typed value.** The sheet judges the text as typed: only an
   empty field clears; only a whole number of digits from 100 to 230
   can be saved; every other text (letters, a sign, a fraction, mixed
   text, spaces inside) leaves Save off and stores nothing. The field
   does not rewrite what the owner typed into another number.
3. **F3, one bucket counted more than once.** Two parts. (a) The figures
   and the charts take their buckets from one normalized list: one
   bucket per offset (the first), in time order, inside the range; use
   or extend the helper that the Review chart uses, and make every
   caller of this change use it. (b) The check of a heart rate row from
   a backup file (`validFileHr` in `data/HeartRead.kt`) also refuses a
   row whose offsets are not strictly rising multiples of the bucket
   length. This one function is the only change allowed in the `data`
   package besides the settings store; give it tests for Replace and
   Merge in the way the existing backup tests do it.
4. **F4, chart ticks outside the possible values.** The two charts of
   this change never show a tick below 0, and "In zone" never one above
   100. Equal values (all 0, all 100, all the same) still give a chart
   with a visible span. The point labels stay exact.
5. **F5, rounds whose clocks disagree.** A round whose times overlap
   another round of the session, or whose start is not after the end of
   the round before it in index order, has no peak and no drop, and the
   round before it has no drop.
6. **F6, comments.** Remove every added comment that a name, a type, or
   a test can say, in every file of the first change. Keep a comment
   only for a quirk of something we cannot change, and list each kept
   one with its reason.
7. **F7, the layout matrix.** The layout assertions and the screenshots
   cover 10 rounds at 360 and 411 dp and font scale 1.0 and 1.3.

Do not change: what the screens show otherwise, texts, Room, the
`timer` and `health` packages, `HeartRules.kt`, `HeartDataScreen.kt`,
dependencies, the version. Other fixers work on the heart rate screens
and the band import on other branches. No task or review IDs in code.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| T1 | Window | A filled session that finishes tomorrow; one 365 days ago to the millisecond; one 1 ms older; the screen open while the only session passes 365 days, then a resume | Not counted; counted; not counted; after the resume the value is gone and "Not set" (or the typed one) shows. Witness F1 `fixed`. |
| T2 | Input | Saved 186, then the field holds: empty; "abc"; "200.5"; "-201"; "2x02"; " 150"; "150"; "99"; "231" | Clear is offered only for empty; Save is on only for "150"; nothing else is stored and 186 stays. Witness F2 `fixed`. |
| T3 | Buckets | A row with offsets 0, 0, 5 in the database; the same in a backup file for Replace and for Merge; offsets 0, 3; offsets 10, 5 | The figures count one bucket per offset; both imports drop the three bad rows and keep every session. Witness F3 `fixed`. |
| T4 | Ticks | Two sessions with 0:00; two with 0 %; two with 100 %; values 40 % and 60 % | No tick below 0 or above 100; a visible span; exact point labels. Witness F4 `fixed`. |
| T5 | Rounds | Round 2 inside round 1; round 3 that starts before round 2 ends; rounds in order | No figures for the rounds whose clocks disagree and no drop for the round before; the ordered session is unchanged. Witness F5 `fixed`. |
| T6 | Comments | The files of the first change | No comment that a name, a type, or a test can say; kept ones listed. |
| T7 | Layout | 10 rounds at the four sizes | Asserted and shown in screenshots that you looked at. |
| T8 | Old rows | Rows S1 to S13 of the first task | Still hold; each changed assertion is listed with its reason. |
| T9 | Rules | Whole change | In `data`: only `validFileHr` (and its tests) and the settings store. No Room, text, dependency, or version change. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of each class (instance, status, test, found by) and the five witness results.
- The `Tests:` command passes on the final commit. Commit on branch `sc01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| T1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
