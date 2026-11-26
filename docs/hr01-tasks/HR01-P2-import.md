# HR01-P2-import: covered band workouts and the one-time merge of old pairs

Task-ID: HR01-P2-import
Class: Risky
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01
Base: 9d1ba17
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01/docs/hr01-tasks/HR01-P2-import-REPORT.md

## Why

See `docs/hr01-tasks/HR01-COMMON.md`, section "Band workouts (phase 2)".
Read the whole file first. Phase 1 is already on this branch: read its
report `docs/hr01-tasks/HR01-P1-data-REPORT.md` and use its functions
(`hrWindow`, `fillHeartRate`, the generation, `linkedIds`); do not write a
second copy of any of them. The attack cases at the end of this file are
tests you write.

## Scope

Phase 2 of 3. Logic only; the Import screen keeps its layout.

- One pure function decides "covered" for a band record; `drafts()` and
  `pendingCount()` both use it, before the existing grouping.
- `linkedIds` count as imported everywhere `sourceId` does for drafts.
- `importDraft` returns what happened (saved, already imported, covered)
  and checks inside its write. `ImportScreen.kt` reads the list again
  after a refusal, also for "Import all"; no new text.
- The old-pair merge: pure plan function, the safety copy, one
  transaction, the DataStore flag, its start in `KhonApp` off the main
  thread before the first fill.
- A fill request when the Import screen opens.
- `SPEC.md`: the import section. Load the `unslop` skill first.

Do not change: the exercise choice of a draft, "Import all" as a feature,
band strength records staying hidden, any layout, the version number, the
manifest. No "Add to" (cut from this task). No new dependency.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| D1 | Covered | A 40-minute band run; an app run session with a run range that overlaps 36 minutes of it, finished, and also unfinished (timer ended, form not saved) | Covered in both: not in `drafts()`, not counted by `pendingCount()`. |
| D2 | Covered | The same band run against: a gym session over the whole time; an app bike session; an app run without a run range; an imported run | Not covered in each case: the draft stays. |
| D3 | Covered | Overlap exactly half of both ranges; half of the record and 40 % of the session; a 2-minute record inside a 60-minute session of the same modality; a record that crosses midnight | Half of both: covered. Each of the two partial cases: not covered. Midnight: decided on epoch ranges, covered when the rule holds. |
| D4 | Covered | A record with unknown modality; a band strength record | Unknown: never covered, stays a draft as today. Strength: hidden as today. |
| D5 | Grouping | Two overlapping records of two packages for one workout, one covered and one not; three records in a chain; two records that are not covered alone while their union would be | The check runs on each original record. Covered records leave before grouping; the rest group as today; the union never hides a group; list and count agree; order of input does not change the result. |
| D6 | Linked ids | A record whose id is in some session's `linkedIds` | Never a draft, also when no session covers it. |
| D7 | Import | A stale draft is saved after the app session that covers it was saved; a stale grouped draft of which one record is covered now; a draft is saved twice; "Import all" with one stale draft among three | The stale draft writes nothing and returns its reason (the check is per original record, inside the write); the others save; the screen shows the fresh list, where the uncovered record of the group is still offered. A saved import has `timedFrom`, `timedTo` = the band's bounds and asks for a fill; `avgHr` and round values are written as today. |
| D8 | Old pairs: plan | A timer 4x4 app session and its imported copy (same exercise, 90 % overlap) | One pair. After the merge: one session, the app one; values as designed; `linkedIds` holds the copy's `sourceId`; the copy, its logs, and its HR row are gone. |
| D9 | Old pairs: plan | Same modality but another exercise; another modality; overlap 60 % of one and 40 % of the other; the app session has a moved date; the imported copy has a moved date; the app session is unfinished; the app session already has `linkedIds` | Same modality: a pair. Every other case: no pair, nothing changes. |
| D10 | Old pairs: one to one | Two imports overlap one app session; one import overlaps two app sessions; two crossing pairs in any input order | No merge for any session with more than one candidate. The result does not depend on order. |
| D11 | Old pairs: values | Per field: A null and B set; both equal; both set and different. Notes: A "No knee pain" and B "knee pain"; B equal to A; B equal to one line of A; B empty | Band-derived fields (`avgHr`, `distanceM`, `laps`, `poolLength`, `timeSec`, round `avgHr`): A's stays, null takes B's. Owner-only fields (`effort`, `watts`, `stroke`, round `watts`): a null on A takes B's (also all four at once); a difference blocks the pair and both sessions stay whole; so does a round index B has and A lacks. Notes: both kept on two lines in the first case; no copy in the other three. |
| D12 | Old pairs: range | A has no `timedFrom`; A has one; B has an HR row | Without: `timedFrom` and `timedTo` become B's `startedAt` and `finishedAt`, and the next fill reads that range. With: unchanged. B's HR row is deleted with B. |
| D13 | Old pairs: safety copy | A pass with one pair | Before any delete, `backups/before-merge-<time>.json` exists in the folder of the "before-import" copies, parses as a backup, and holds both sessions and their logs. With no pair, no file is written and the flag is set. |
| D14 | Old pairs: failure | The copy cannot be written or does not parse; the transaction throws | No delete, no flag; the next start tries again. An earlier good copy is never overwritten. |
| D15 | Old pairs: restart | The app dies after the transaction and before the flag; it starts twice more | The second run finds no pair, writes no second file, sets the flag. Later starts do nothing. |
| D16 | Old pairs: restore | After the pass, the owner restores the safety copy or imports an old backup with pairs | The pairs stay as two sessions; the pass does not run again. |
| D17 | Old pairs: fresh rows | Between the verified copy and the transaction: a note is typed on B; a round's watts on B changes while both session rows stay equal; a session of a pair is deleted; another imported session becomes a second candidate for A while A and B do not change | The transaction builds the whole plan again from fresh rows and merges only pairs whose A, B, and logs equal the rows in the parsed copy. Each of these pairs is skipped and stays whole; the other pairs merge. |
| D18 | Fill | The Import screen opens | One fill request through the runner of phase 1. |
| D19 | Linked ids and backup | After a merge, merge-import a 0.6.1 file that holds only the surviving app session | `linkedIds` stays (phase 1 rule); its band records are still not drafts. |
| D20 | Rules | Whole change | No layout change, no new text on the Import screen, no new permission or dependency, no manifest change. `SPEC.md` updated. |

## Attack cases

<!-- ATTACK-CASES -->

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- Every attack case above is a test that passes.
- A test for new behavior fails on the phase 1 tip (or does not compile there) and passes on the branch.
- The `Tests:` command passes on the final commit. Commit on branch `hr01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row
and per attack case:

| # | Status | Check ran | Real output |
|---|---|---|---|
| D1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
