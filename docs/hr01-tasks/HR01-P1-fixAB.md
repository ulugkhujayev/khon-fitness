# HR01-P1-fixAB: fix the deep review findings of the timer part

Task-ID: HR01-P1-fixAB
Class: Risky
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-fixab
Base: 407a906
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-fixab/docs/hr01-tasks/HR01-P1-fixAB-REPORT.md
Coverage: required

## Why

The deep review of the first two parts of the data task is in
`docs/hr01-tasks/HR01-P1-AB-DEEP-REPORT.md`. Read it, the design file
`docs/hr01-tasks/HR01-COMMON.md` (sections "Verified clocks" and "Clock
times"), and the Part B rows of `docs/hr01-tasks/HR01-P1-data-REPORT.md`
first. This task is step 6 of the protocol: fix each class, every
instance. It runs on its own branch (`hr01-fixab`, from the commit the
review read) while another builder works on branch `hr01` in the data
package; the lead merges the two. So keep your changes inside the timer
code where you can, and name in the report every file you change
outside the `timer` package, with the lines.

Witness files (each with a test patch next to it), in
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/HR01-P1-data-W/`:
`AB-F1.json`, `AB-F2.json`, `AB-F3.json`. Run
`~/.config/agents/tools/witness run <file> --sha <your commit>` for each
with `ANDROID_HOME="$HOME/Library/Android/sdk"` exported (the witness
clone has no `local.properties`; without the variable Gradle stops with
"SDK location not found" and the tool prints a false `reproduces`). Read
each receipt log to confirm that the test ran. Show `fixed` for the
three in the Coverage table.

## Scope

Lead decisions per finding:

- **AB-F1, late cleanup acts on a newer run (fix).** Class: a cleanup or
  a state update that names its target by session id, or that checks and
  then removes in two steps. Every cleanup and every publication of a
  finished run names the run (session id and run number) and does
  nothing when the live state or the stored snapshot is another run.
  The snapshot store gets one lock: save, compare-and-remove, and read
  for restore run under it, so a save cannot land between a comparison
  and its removal. The same for the cleanup after a stretch run and for
  the removal of an invalid snapshot: neither may remove a newer cardio
  snapshot. State updates happen on the main thread and check the run
  first.
- **AB-F2, one snapshot slot holds two things (fix, decided design).**
  The live snapshot slot holds only a timer that waits, runs, or is
  paused. When a run ends, its result moves, under the snapshot lock, to
  a second store of **pending results** (a small list, one entry per
  session id and run number, with everything `commitTimerRun` needs). A
  result leaves that store only when its commit returned: accepted, or
  refused for good (the session is gone, a newer run owns the session,
  or it is already committed). A commit that failed, or that was refused
  only because the generation changed, keeps its entry. The app commits
  every pending entry at its start and after each timer end. Opening
  another timer, Ready, and Stop on Ready never touch the pending store.
  Replace and reset clear the pending store after their write succeeded,
  as they clear the live slot. One read for other code: whether any
  result is pending (the band import of a later task refuses while one
  is).
- **AB-F3, restore places round boundaries with the wall clock (fix).**
  The snapshot stores the elapsed-realtime value of its last save. A
  restore that is in step advances the run by the elapsed-realtime
  difference, so every boundary comes from the anchor and the phase
  lengths, and an allowed drift of the wall clock moves nothing. A
  snapshot without that value (an older one) keeps the wall-clock path
  and is untrusted, as today.
- **Tests the review named as weak.** C15: end the run through the
  service's own completion path, not a helper. C17: a real service
  paused in an active round, the Room write of the Replace fails, and
  the same run resumes. Add both when the test harness can drive the
  service under Robolectric; when it cannot, say why in the report and
  test the nearest function the service calls.

Search for siblings of each class in the whole timer package and in the
code that Part B added to the repository, and list every instance in the
Coverage table, also those that need no change.

Do not change: the rules of the design file, Room entities, the
migration, texts, the version number. No new dependency. No comment
that a name, a type, or a test can say; no task or review IDs in code.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| X1 | Late cleanup | Run 1 ends and its cleanup is held; run 2 starts for the same session and saves its snapshot; the cleanup is released | Run 2's live state and snapshot are untouched. Witness AB-F1 `fixed`. |
| X2 | Snapshot lock | A save lands between a cleanup's comparison and its removal (the test holds the cleanup at that point) | The save waits or wins whole; the newer snapshot is never removed. |
| X3 | Stretch and invalid | A stretch run's cleanup, and the removal of an invalid snapshot, each released after a newer cardio snapshot was saved | The cardio snapshot stays. |
| X4 | Pending results | A run ends and its commit fails; the owner opens another timer (Ready), stops on Ready, starts and ends a run for another session; the app restarts | The first result is still pending and commits once at the start. The second run commits by itself. Witness AB-F2 `fixed`. |
| X5 | Pending results | A pending result whose session was deleted; whose session has a newer run; that was already committed; whose commit is refused by the generation | The first three leave the store without a write. The fourth stays and commits at the next try. |
| X6 | Pending results | Replace and reset with a pending result: the write succeeds; the write fails | Success: the store is empty after the write. Failure: the entry stays and commits once after a restart. |
| X7 | Restore | A trusted snapshot restored after 10 minutes with the wall clock 1.5 s ahead of the elapsed clock; the same 1.5 s behind; an older snapshot without the elapsed value | Every round boundary equals anchor plus phase lengths exactly, in both drift cases. The older snapshot is untrusted and writes no clock field. Witness AB-F3 `fixed`. |
| X8 | Old rows | Every row of Part B in the data task (C9 to C17, C16a, C16b, AT2, AT3, AT19, AT20, AT26) | Still holds: the Part B tests pass, changed only where the pending store replaces the ended snapshot; each changed assertion is listed with its reason. |
| X9 | Rules | Whole change | Files outside the `timer` package are named with their changed lines. No Room, migration, text, dependency, or version change. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of each class (instance, status, test, found by), the three witness results, and a break proof for each changed guard (break it in a temp copy and show that a test fails).
- The `Tests:` command passes on the final commit. Commit on branch `hr01-fixab` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
