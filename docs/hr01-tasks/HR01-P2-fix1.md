# HR01-P2-fix1: fix the review findings of the band import

Task-ID: HR01-P2-fix1
Class: Risky
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-p2
Base: 79531bd
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-p2/docs/hr01-tasks/HR01-P2-fix1-REPORT.md
Coverage: required

## Why

The deep review of the band import is in
`docs/hr01-tasks/HR01-P2-DEEP-REPORT.md`. Read it, then
`docs/hr01-tasks/HR01-P2-import.md` (with the lead decisions at its end)
and its report. Three findings are open. Each is a class: fix every
instance, not only the lines the review names.

One witness, in
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/HR01-P2-import-W/`:
`F1.json` with `F1.patch`. It prints `reproduces` on the base (both
tests fail at their assertion). Run
`~/.config/agents/tools/witness run <file> --sha <your commit>` with
`AGENTS_TASKS=/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks`
and `ANDROID_HOME="$HOME/Library/Android/sdk"` exported, and read the
receipt log. It must print `fixed`. Do not change the witness files.

## Scope

1. **F1, the pending check at the wrong time.** An import is refused
   with "timer result pending" when a result was pending at its submit
   **or** is pending at its write: read the pending state when
   `importDraft` is called, before it waits for the repository timer
   lock, and read it again inside the Room write transaction, after the
   local rows are read and before any insert. A true value at either
   point refuses the import and writes nothing. The claim and coverage
   results keep their precedence over each other as today; decide and
   state in the report where "timer result pending" stands in that
   order, and keep it the same at both points. Search every other value
   that the import reads before its transaction and uses inside it (the
   generation, the covered decision, the already-imported decision, the
   exercise) for the same class, and close each instance.
2. **F2, tests that skip their step.** Each point of the review's F2
   gets its test step: the timer's arrival at the admission boundary is
   signalled, and the test proves it cannot pass the lock before the
   import releases it (no sleep as proof); both submit actions of the
   Import screen are driven and the reload and the kept draft are
   asserted; a file-backed Room database is closed and opened again for
   the restart case; the fill request on open is counted at the call,
   with a permission callback and a recomposition of the screen. Where
   the Robolectric harness cannot do a step, say so as a deviation with
   the exact error, and test the nearest step that it can do.
3. **F3, comments.** Remove every added comment that a name, a type, or
   a test can say, in every file of the first change (tests too). Move a
   rule into a fixture or test name. Keep a comment only for a quirk of
   something we cannot change, and list each kept one with its reason.

Do not change: the rules of the design file, the "covered" rule, the
grouping, the Import screen's layout and texts, Room, the version. In
the `timer` package change only what Scope 1 needs, and name each
changed line. No new dependency. No task or review IDs in code.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| W1 | Pending at write | An import waits behind another Room transaction; a covering timer run ends and publishes its result before the import reads its rows | The import returns "timer result pending", inserts nothing, and asks for no fill. After the result commits, a new submit gives "covered". Witness F1 `fixed`. |
| W2 | Pending at submit | A result is pending at the submit; the import waits for the lock behind that result's commit; the commit has no covering range | The import returns "timer result pending" and inserts nothing. A new submit after it saves. |
| W3 | Import first | An import passes both checks and holds the lock; a timer run ends | The import saves; the timer commit waits for the lock and then commits (the task file's row D7c). |
| W4 | Class | Every value read before the transaction and used inside it | Listed in the Coverage table, each fixed with a test or kept with the reason. |
| W5 | Test steps | Each point of Scope 2 | One test each; each fails when its guard is removed (break proof in the report). |
| W6 | Comments | The files of the first change | No comment that a name, a type, or a test can say; kept ones listed. |
| W7 | Old rows | The rows and attack cases of the first task | Still hold; each changed assertion is listed with its reason. |
| W8 | Rules | Whole change | Changed `timer` lines are named. No layout, text, Room, dependency, or version change. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of each class (instance, status, test, found by) and the witness result.
- The `Tests:` command passes on the final commit. Commit on branch `hr01-p2` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| W1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
