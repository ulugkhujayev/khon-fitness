# HR01-P1-fixAB2: repair the timer fixes after their verify round

Task-ID: HR01-P1-fixAB2
Class: Risky
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-fixab
Base: fbe803d
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-fixab/docs/hr01-tasks/HR01-P1-fixAB2-REPORT.md
Coverage: required

## Why

The verify round of the timer fixes is in
`docs/hr01-tasks/HR01-P1-fixAB-VERIFY-REPORT.md`. Read it, then
`docs/hr01-tasks/HR01-P1-fixAB.md` and its report. Six points are open:
one missed instance of AB-F1 and five defects inside the fixes of AB-F2
and AB-F3. This task repairs them and nothing else. A second failed
verify round is a design alarm, so for each point also search for its
siblings and close them.

Witness files, in
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/HR01-P1-data-W/`:
`AB-F1.json`, `AB-F2.json`, `AB-F3.json`. Run
`~/.config/agents/tools/witness run <file> --sha <your commit>` for each
with `ANDROID_HOME="$HOME/Library/Android/sdk"` exported, and read each
receipt log to confirm that the test ran. All three must stay `fixed`.

Another builder works on branch `hr01` in the data package; the lead
merges. Stay inside the `timer` package where you can and name every
other file you change, with its lines.

## Scope

Lead decisions, one per point of the verify report, in its order:

1. **Stop finishes whichever run is live (AB-F1).** Every command that
   acts later than it was given (Stop, Skip, Pause, Resume, the end of a
   wait for a Start write) captures the stable id of the run it was
   given for, and after any wait it acts only when the live run still
   has that id. A run that got its number during the wait is the same
   run. List every such command in the Coverage table.
2. **Runs without a number share one pending key (AB-F2).** A pending
   entry is keyed by the run's stable id, never by the session alone, so
   no entry replaces another. The commit order of two entries for one
   session is the order of their ends. An entry without a number follows
   the old-version rule of the design file (the duration only, when the
   session's `timeSec` and `timerRun` are both null; else it leaves the
   store as dropped).
3. **The move to the pending store is not durable (AB-F2).** The edit
   that adds the pending entry and removes the live slot is written
   synchronously (`commit`, off the main thread) before the end counts
   as staged. When that write fails, the live snapshot stays and the end
   is tried again; nothing is lost and nothing is staged twice.
4. **A restore with an elapsed value below the saved one stays trusted
   (AB-F3).** When elapsed realtime at the restore is below the saved
   value, the run is untrusted for good before any fallback, also when
   it is still above the anchor.
5. **The saved elapsed value is sampled apart from the state (AB-F3).**
   A state carries the elapsed-realtime instant it stands for, taken in
   the same step that computed its remaining time; the snapshot stores
   that value and never a later sample. A restore publishes the advanced
   state with its own instant, so a second restore loses no time.
6. **A restored completion skips the retry path (AB-F2).** One function
   handles the end of a run, live or restored: stage the result, then
   commit every pending entry. The startup order is: restore the live
   snapshot first, then one drain.

Do not change: the rules of the design file, Room, texts, the version
number. No new dependency. No comment that a name, a type, or a test can
say; no task or review IDs in code.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| Y1 | Late command | A's Start write is held; Stop is given; timer B opens on Ready; the write is released | B and its snapshot are untouched; A ends once. The same for Skip, Pause, and Resume given for A. |
| Y2 | Pending key | Two runs of one session whose Start writes failed (no numbers); A's result fails to commit; B ends | Both entries exist; neither replaced the other; they commit in the order of their ends under the old-version rule (the first writes the duration, the second is dropped). |
| Y3 | Durable move | The process dies after the end of a run and before the staging write returned (the test makes the write fail or never return) | After a restart the live snapshot is still there and the run ends once, at its real end; a Stop given before the death is not lost when its staging write had returned. |
| Y4 | Restore trust | Elapsed 9000 at Start, 10000 at the save, 9500 at the restore, wall clock in step with the anchor | The run is untrusted; no clock field is written; the duration follows its rule. |
| Y5 | Saved instant | A real service save with a delay between the tick and the write; then a restore; then a second restore right after the first, before the foreground call | The next round boundary equals anchor plus phase lengths exactly in both restores. |
| Y6 | Startup | A pending entry A that is refused by the generation at the startup drain; a live snapshot B that reaches its end during the restore | Both are staged and tried in one pass; A is not left for the next launch. |
| Y7 | Old rows | Rows X1 to X9 of the first fix task and the Part B rows of the data task | Still hold; each changed assertion is listed with its reason. Witnesses AB-F1, AB-F2, AB-F3 `fixed`. |
| Y8 | Rules | Whole change | Files outside the `timer` package are named with their changed lines. No Room, text, dependency, or version change. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of each class (instance, status, test, found by), the three witness results, and a break proof for each changed guard.
- The `Tests:` command passes on the final commit. Commit on branch `hr01-fixab` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| Y1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
