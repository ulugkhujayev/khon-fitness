# PR01-fix3: the Finish sheet is computed from collected state, with no read at the tap

Task-ID: PR01-fix3
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/pr01
Base: 98c76bf
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/pr01/docs/pr01-tasks/PR01-fix3-REPORT.md
Coverage: required

## Why

Finding F2 failed its verify round twice
(`docs/pr01-tasks/PR01-fix1-VERIFY-REPORT.md`,
`docs/pr01-tasks/PR01-fix2-VERIFY-REPORT.md`; read both). Each fix
guarded one more way in which a late read result can reach the screen
(a second tap, a dismissal, a deletion, and now Back). The approach is
wrong: a read that starts at the tap and publishes later has to be
guarded on every exit. This task removes the late publication.

Witness file for F2:
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/PR01-rep-records-weight-trend-W/F2.json`.
Run `~/.config/agents/tools/witness run <file> --sha <your commit>` with
`ANDROID_HOME="$HOME/Library/Android/sdk"` exported, and read the receipt
log to confirm that the test ran. It must stay `fixed`.

## Scope

Decided design, in `SessionScreen.kt`:

- The screen collects every set (`vm.repo.allSetLogs`, lifecycle-aware,
  initial value "not loaded") while it is shown. No coroutine is started
  by the Finish tap, and nothing sets the sheet state from a callback.
- The Finish tap writes one piece of state synchronously: the tap time.
  Dismiss and Save clear it. Nothing else writes it.
- The sheet is shown when the tap time is set, the sets are loaded, and
  the session is unfinished. Its figures are computed once from the
  session's own sets and the other sessions' sets of that same collected
  list, and from the tap time; they stay as computed while the sheet is
  open (as today: "the figures as of opening").
- A tap before the sets are loaded shows the sheet when they arrive, if
  the screen is still shown and the tap was not cleared.
- Remove `finishRead`, its job, the `readSets` parameter if it has no use
  left, the second session read, and `ensureActive()`. Keep the `clock`
  parameter if the tests need it.
- Tests in `FinishSaveOnceTest.kt`: keep the cases of the six held-read
  tests in the new form (the test holds the flow's first value where a
  held read stood), and add the Back case: tap Finish before the sets
  are loaded, press Back, then deliver the sets while the composition is
  still alive: no sheet state is set and no sheet is shown.
- Search the branch diff again for any other coroutine that publishes UI
  state after an await, and list each in the Coverage table.

Do not change: the rep record rule, texts, layout, Room, the version
number. No new dependency. No comment that a name, a type, or a test can
say; no task or review IDs in code.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| Z1 | Finish | A fresh view model where no other screen collects the sets; one tap (the F2 witness) | The sheet shows the right "New bests" and "Rep records". Witness `fixed`. |
| Z2 | Finish | Two fast taps | One sheet with the first tap's time; a tap while the tap time is set changes nothing. |
| Z3 | Finish | Tap before the sets are loaded; then they load | The sheet shows once, with figures from that list and the tap time. |
| Z4 | Finish | Tap before the load; dismiss is not possible yet; press Back; the sets load while the composition is alive | No sheet, and the sheet state stays clear. |
| Z5 | Finish | The sheet is open; the session is deleted or finished elsewhere | The sheet goes; no crash. |
| Z6 | Finish | The sheet is open; a set of another session changes | The shown figures do not change. |
| Z7 | Code | `SessionScreen.kt` | No `launch` and no suspend call on the Finish path. Break proof for each new test. |
| Z8 | Rules | Whole change | Only `SessionScreen.kt`, its tests, and the report change, unless a caller needs the removed parameter; each extra file is named with its reason. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of the class (a coroutine that publishes UI state after an await) with status, test, and found by.
- The `Tests:` command passes on the final commit. Commit on branch `pr01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| Z1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
