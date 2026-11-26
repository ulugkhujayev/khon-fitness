# HR01-P3-fix2: the "Read again" result line as a failure line only

Task-ID: HR01-P3-fix2
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-p3
Base: e375cbd
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-p3/docs/hr01-tasks/HR01-P3-fix2-REPORT.md
Coverage: required

## Why

The verify round of the heart rate UI fixes is in
`docs/hr01-tasks/HR01-P3-fix1-VERIFY-REPORT.md`. Read it, then
`docs/hr01-tasks/HR01-P3-fix1.md` and its report. Two findings are open:
F1 (a result of "Read again" that outlives its request: four more
schedules) and F5 (two weak tests). The first fix tied the result to
the sheet, the range, and a row found by a later lookup, and that
lookup is the weak point. This task changes the design so that the
class cannot occur, and a second failed verify would be a design alarm:
also search for siblings.

The witness
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/HR01-P3-ui-W/F1.json`
prints `fixed` on the base of this task and must still print `fixed`
(run it with
`AGENTS_TASKS=/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks`
and `ANDROID_HOME="$HOME/Library/Android/sdk"` exported). Do not change
the witness files.

## Scope

Lead decision, a simpler design:

1. **The rows say the state; the line says only a failure.** After
   "Read again" the sheet shows what the flows give: the fact rows of
   the row that exists now, or the state row ("Waiting for data", "No
   data", "Not read"). The outcomes filled, waiting, none, no range, and
   dropped write no line and keep no state. The texts "No data yet" and
   the line with the new values go away.
2. **The failure line.** Only the outcome "failed" shows a line: "Health
   Connect did not answer". It belongs to one request. A request is
   current while all of this holds: the same sheet instance is open
   (same session, not closed and opened again); the session's range
   (`hrFacts`) is the one it had at the tap; the row that the flow gives
   is the one it gave at the tap; the owner did not tap Remove, confirm
   Remove, tap "Use this time range", or tap "Read again" again. The
   call runs in the sheet's own scope, so it is cancelled when the sheet
   leaves the composition, and a completion checks its request against
   the current one before it shows the line. Any change of the list
   above clears the line and makes the request not current. Nothing
   about a request is kept outside the sheet (no map by session id in
   the screen or the view model).
3. **Not in scope (refused by the lead):** two requests of one session
   for two ranges that share one running read in the data layer; the
   later request then shows the failure of that read. The data layer
   does not change here.
4. **F5, tests.** The mixed typed and measured fixture asserts the
   values that each of the two charts receives (the work chart gets 152
   and 155, the average chart its own values): a swap of the two series
   must fail the test. New tests for the schedules of the verify report:
   a row replaced by an automatic pass while the explicit read returns;
   a range change and a change back before the row appears; Remove
   confirmed while a read waits, then a provider failure; leave the
   screen, change the range, come back, tap "Read again" before the
   held read ends (expected here: no line from the old request; the new
   request shows its own outcome). Each new test fails when its guard is
   removed (break proof in the report).
5. `SPEC.md` and `DESIGN.md`: the result line as it is now (load the
   `unslop` skill first).

Do not change: the data layer, the `timer` and `health` packages, Room,
the manifest, dependencies, the version, the layout (except the removed
line texts). No comment that a name, a type, or a test can say; no task
or review IDs in code.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| U1 | Rows | "Read again" that ends filled; waiting; none; dropped | No result line. The sheet shows the row that the flow gives. |
| U2 | Failure | A failed read with nothing changed; the same after a recomposition and a rotation | "Health Connect did not answer" shows and stays. |
| U3 | Not current | A held failing read with, before its end: Remove tapped; Remove confirmed (before Room publishes the `off` row); a range change; a range change and back; a row replaced by an automatic pass; the sheet closed; closed and opened again; the session deleted; the screen left and opened again; a second "Read again" | No line from the old request in any case. Witness F1 `fixed`. |
| U4 | No kept state | The screen and view model code | No result or request state outside the sheet's composition (show the search in the report). |
| U5 | Charts | The mixed fixture | Each chart's received values are asserted; a swap of the series fails the test. |
| U6 | Old rows | The rows of the first task and of the first fix | Still hold, except the two removed line texts; each changed assertion is listed with its reason. The screenshots are made again and looked at. |
| U7 | Rules | Whole change | No change in the data layer, `timer`, `health`, Room, the manifest, dependencies, or the version. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of each class (instance, status, test, found by) and the witness result.
- The `Tests:` command passes on the final commit. Commit on branch `hr01-p3` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| U1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
