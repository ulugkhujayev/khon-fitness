# HR01-P3-fix1: fix the review findings of the heart rate UI

Task-ID: HR01-P3-fix1
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-p3
Base: 4f3ea27
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-p3/docs/hr01-tasks/HR01-P3-fix1-REPORT.md
Coverage: required

## Why

The deep review of the heart rate UI is in
`docs/hr01-tasks/HR01-P3-DEEP-REPORT.md`. Read it, then
`docs/hr01-tasks/HR01-P3-ui.md` and its report. Five findings are open
(F1, F3, F4, F5, F6); the lead refused F2 (see Scope 2). Each is a class:
fix every instance, not only the lines the review names.

One witness, in
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/HR01-P3-ui-W/`:
`F1.json` with `F1.patch`. Run
`~/.config/agents/tools/witness run <file> --sha <commit>` with
`AGENTS_TASKS=/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks`
and `ANDROID_HOME="$HOME/Library/Android/sdk"` exported. On the base it
fails too early: `ComposeTimeoutException` at line 63 of its test, where
it waits for the list row, before the case of the finding. First repair
only the setup of that test patch (how it finds and opens the row; do
not change the steps after "Read again" or its last assertions), run it
on the base, and show in the report that it now fails at its last
assertions. After your fix it must print `fixed`.

## Scope

1. **F1, a result that outlives its request.** The result line of "Read
   again" belongs to one request: the opened sheet, the session, the
   range that was read, and the row that the read left. It is shown only
   while all of these are still current. Remove, a range change, a new
   row, a closed sheet, or a deleted session clears it, and a completion
   that arrives later writes nothing. A failure stays visible while its
   request is still current.
2. **F2 is refused.** The "Heart rate data" sheet shows what the band
   sent: its round rows and its "Average" row are measured values, also
   when a typed value exists. Make this explicit: one test that a round
   with a typed value shows the measured one in the sheet, and one line
   in `SPEC.md` (load the `unslop` skill first). Review, the charts, and
   the cardio form keep "typed first".
3. **F3, a raw row in the presentation code.** The rules give the sheet
   a projected model: the fact rows, and flags for "Read again",
   "Remove", and "Use this time range". The composables read no
   `SessionHr` field. "Remove" stays present for every row that exists
   and is not `off`, also a stale one.
4. **F4, the confirmation text.** The Remove confirmation has its title
   and its two actions and no sentence.
5. **F5, weak tests.** Each point of the review's F5 gets its test: the
   action is disabled while a read waits (assert the UI, not the call
   count); a late failing result after Remove, after a range change, and
   after a delete; mixed typed and measured values that reach both
   charts; a restored cardio form that saves null in both typed columns,
   and a typed session average that is saved; "Use this time range" asks
   for its own fill and the sheet follows it; a read with heart rate
   access only. Each new test fails when its guard is removed (break
   proof in the report).
6. **F6, comments.** Remove every added comment that a name, a type, or
   a test can say, in every file of the first change. Keep a comment
   only for a quirk of something we cannot change, and list each kept
   one with its reason.

Do not change: the data layer, the `timer` and `health` packages, Room,
the manifest, dependencies, the version, the layout of the screens
(except the removed sentence). No task or review IDs in code.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| V1 | Late result | A held read that fails, with before its end: Remove; a range change; a new row of the same state; the sheet closed and opened again; the session deleted | No result line of the old request in any case. A failure with nothing changed stays visible. Witness F1 fails at its assertions on the base and prints `fixed` on the branch. |
| V2 | Sheet values | A round with a typed 150 and a measured 144; a session with a typed average | The sheet shows 144 and the measured average; Review and the chart show 150 and the typed average. |
| V3 | Projection | Two sessions with the same "Not read" fact, one with a stale row and one without | The composables get a model with flags; "Remove" is present only for the one with a row. No composable reads a `SessionHr` field (show the search in the report). |
| V4 | Text | The Remove confirmation | Title and two actions, no sentence. |
| V5 | Tests | Each point of Scope 5 | One test each, with its break proof. |
| V6 | Comments | The files of the first change | No comment that a name, a type, or a test can say; kept ones listed. |
| V7 | Old rows | The rows of the first task | Still hold; each changed assertion is listed with its reason. The 15 screenshots are made again and looked at. |
| V8 | Rules | Whole change | No change in the data layer, `timer`, `health`, Room, the manifest, dependencies, or the version. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of each class (instance, status, test, found by) and the witness result.
- The `Tests:` command passes on the final commit. Commit on branch `hr01-p3` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| V1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
