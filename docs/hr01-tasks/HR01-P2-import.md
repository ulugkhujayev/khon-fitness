# HR01-P2-import: covered band workouts and the owner's merge of old pairs

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
- Old pairs: `pairCandidates` (pure), the "Keep both" set, and
  `mergePairs` with the safety copy and one transaction. Nothing calls
  `mergePairs` in this phase; phase 3 adds the owner's screen. No merge
  runs at app start.
- A fill request when the Import screen opens.
- `SPEC.md`: the import section. Load the `unslop` skill first.

Do not change: the exercise choice of a draft, "Import all" as a feature,
band strength records staying hidden, any layout, the version number, the
manifest. No "Add to" (cut from this task). No new dependency.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| D1 | Covered | A 40-minute band run; an app run session with a run range that overlaps 36 minutes of it, finished, and also unfinished (timer ended, form not saved) | Covered in both: not in `drafts()`, not counted by `pendingCount()`. |
| D1a | Covered | An app run made at 23:59, timer 00:01 to 00:21 with clocks in step, saved without a date edit; the band's run 00:00 to 00:22 | The run range exists and covers the record, also at the import write check. |
| D2 | Covered | The same band run against: a gym session over the whole time; an app bike session; an app run without a run range; an imported run | Not covered in each case: the draft stays. |
| D3 | Covered | Overlap exactly half of both ranges; half of the record and 40 % of the session; a 2-minute record inside a 60-minute session of the same modality; a record that crosses midnight | Half of both: covered. Each of the two partial cases: not covered. Midnight: decided on epoch ranges, covered when the rule holds. |
| D4 | Covered | A record with unknown modality; a band strength record | Unknown: never covered, stays a draft as today. Strength: hidden as today. |
| D5 | Grouping | Two overlapping records of two packages for one workout, one covered and one not; three records in a chain; two records that are not covered alone while their union would be | The check runs on each original record. Covered records leave before grouping; the rest group as today; the union never hides a group; list and count agree; order of input does not change the result. |
| D6 | Linked ids | A record whose id is in some session's `linkedIds` | Never a draft, also when no session covers it. |
| D7 | Import | A stale draft is saved after the app session that covers it was saved; a stale grouped draft of which one record is covered now; a draft is saved twice; "Import all" with one stale draft among three | The stale draft writes nothing and returns its reason (the check is per original record, inside the write); the others save; the screen shows the fresh list, where the uncovered record of the group is still offered. A saved import has `timedFrom`, `timedTo` = the band's bounds and asks for a fill; `avgHr` and round values are written as today. |
| D8 | Candidates | A timer 4x4 app session and its imported copy (same exercise, 90 % overlap) | `pairCandidates` lists the pair, not blocked. Nothing is merged or deleted by listing. No code path merges at app start or without `mergePairs`. |
| D9 | Candidates | Same modality but another exercise; another modality; overlap 60 % of one and 40 % of the other; A with a moved date; B with a moved date; A unfinished; A with `linkedIds`; A without `timedFrom` and without `timeSec` | Same modality: a candidate. Every other case: not listed. |
| D9a | Candidates | A has no `timedFrom`, `startedAt` 18:00, `timeSec` 2100, saved 19:40; band run B 18:50 to 19:30; the same A with a band copy 18:02 to 18:40 | A's range is `[startedAt, startedAt + timeSec)`, never up to the Save time: the first B is not listed; the copy is listed. |
| D10 | Candidates | Two imports overlap one app session; one import overlaps two app sessions | Every edge is listed. "Merge all" skips each of them; a merge of one edge by hand works, and then the other edge of that A is gone from the list. |
| D11 | Blocked | Owner-only fields (`effort`, `watts`, `stroke`, round `watts`) set on both and different; B has a round index A lacks | Listed as blocked with the reason; `mergePairs` refuses it; both sessions stay whole. |
| D12 | Merge values | Per field: A null and B set; both equal; both set and different (band-derived). Notes: A "No knee pain" and B "knee pain"; B equal to A; B equal to one line of A; B empty. B alone has effort, session watts, stroke, and a round's watts | Band-derived fields (`avgHr`, `distanceM`, `laps`, `poolLength`, `timeSec`, round `avgHr`): A's stays, null takes B's. Owner-only fields: null on A takes B's, all four at once too. Notes: both kept on two lines in the first case; no copy in the other three. |
| D13 | Merge result | A has no `timedFrom`; A has one; B has an HR row | Without: `timedFrom`, `timedTo` become B's `startedAt`, `finishedAt`, and the next fill reads that range. With: unchanged. `linkedIds` holds B's `sourceId`. B, its logs, and its HR row are gone; one session remains, the app one. |
| D14 | Safety copy | `mergePairs` with one pair; the copy cannot be written or does not parse; the transaction throws | Before any delete, `backups/before-merge-<time>.json` exists in the folder of the "before-import" copies, parses as a backup, and holds both sessions and their logs. On a failure: nothing changed, the result says so, and an earlier copy is never overwritten. |
| D15 | Fresh rows | Between the verified copy and the transaction: a note is typed on B; a round's watts on B changes while both session rows stay equal; a session of the pair is deleted; the pair stops being a candidate | The transaction checks each asked pair on fresh rows against the candidate rule and against the rows in the parsed copy. Each such pair is skipped with its reason and stays whole; the other asked pairs merge. |
| D15a | Shown facts | The owner opens a pair; then B's note changes; then the owner confirms and the verified copy already holds the new note | Refused: the fresh rows differ from the rows the owner saw. Nothing changes; the result says so. |
| D15c | Shown heart rate | The owner opens a pair; a fill then writes or changes B's HR row; the owner confirms | Refused: B's HR row differs from the row the owner saw. B keeps its new row. |
| D15b | Merge all | Two one-to-one pairs are confirmed; before the transaction a new imported session gives A of the first pair a second candidate | The first pair is skipped with its reason; the second merges; the new session is never merged. |
| D16 | Keep both | The owner keeps a pair; the list is read again; the app restarts; one of the two sessions is deleted | The pair is not listed again, also after a restart. A stale entry for a deleted session does no harm. |
| D17 | Twice | `mergePairs` is called twice for one pair (double tap); a merged pair's safety copy is restored later | The second call merges nothing and reports it. After the restore the pair is listed again as a candidate, and nothing merges by itself. |
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
