# HR01-P1-fixC: fix the review findings of the heart rate reader and the backup check

Task-ID: HR01-P1-fixC
Class: Risky
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-fixc
Base: d9b444e
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-fixc/docs/hr01-tasks/HR01-P1-fixC-REPORT.md
Coverage: required

## Why

The deep review of Part C of the data task is in
`docs/hr01-tasks/HR01-P1-C-DEEP-REPORT.md`. Read it, then the Part C
rows of `docs/hr01-tasks/HR01-P1-data.md` and of its report. Three
findings are open. Each is a class: fix every instance, not only the
lines the review names.

Witness files, in
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/HR01-P1-data-W/`:
`C-F1.json`, `C-F2.json`, `C-F3.json`. All three print `reproduces`
on the base. Run
`~/.config/agents/tools/witness run <file> --sha <your commit>` for each
with `AGENTS_TASKS=/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks`
and `ANDROID_HOME="$HOME/Library/Android/sdk"` exported, and read each
receipt log to confirm that the test ran and passed. All three must
print `fixed`.

Other builders work on the `timer` package and on the UI on other
branches; the lead merges. Do not change the `timer` package,
`data/TimerCommit.kt`, or any file under `ui/`.

## Scope

Lead decisions:

1. **C-F1, a zone read before the transaction.** Every write that checks
   a session's range (`hrWindow`) reads the zone inside its own
   transaction, at the check: the fill commit, the cleanup delete,
   `removeHeartRate`, and every other DAO or repository write that takes
   a `ZoneId` today (search for them; `confirmRange`, the set stamp, and
   the backup merge clears are candidates). Pass a `() -> ZoneId`, not a
   value. A pass also picks its stale rows with the zone of the delete,
   so a row that became current is kept.
2. **C-F3, records that begin before the range.** Health Connect filters
   records by their start time. The adapter asks from 24 hours before the
   range start up to the range end, and keeps only the records that touch
   the range (`endTime > from` and `startTime < to`). The pure reader and
   its counts do not change: `rawCount` still counts every sample of
   every record that touches the range. The selection is a function that
   a unit test can call with record bounds; the AT5 test goes through it.
3. **C-F2, an overflow in the file check.** The check of a heart rate row
   from a file uses no sum that can overflow: an offset is valid when
   `0 <= offsetSec` and `offsetSec < ceil((toMs - fromMs) / 1000)`,
   computed without overflow, and `fromMs` must be at least 0. Search the
   other value checks of file rows of this task (`checkedAt`, round
   times, `timedFrom`, `timedTo`, `fetchedAt`, `firstMs`, `lastMs`) and
   every range computation (`hrWindow`, `lengthMs`, the cardio tail) for
   the same class, and close each instance.
4. **Tests the review named as weak.** The zone test changes the zone
   inside the transaction wait, not before the provider returns. The
   Remove test isolates the stamp check: a held reread of an `off` row,
   then a second Remove.

Do not change: the rules of the design file, the Room schema, texts, the
version number. No new dependency. No comment that a name, a type, or a
test can say; no task or review IDs in code.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| Z1 | Zone | The zone changes as Room starts the transaction of: a fill commit; a cleanup delete; a Remove with no row; each other write that checks a range | The check uses the new zone: the result whose range went away is dropped (`DROPPED`); the row that became current stays; Remove writes nothing. Witness C-F1 `fixed`. |
| Z2 | Provider | A platform that returns only records whose start is inside the asked range; the AT5 records (17:59 to 18:12, 18:18 to 18:31); a record that ends exactly at `from`; one that starts exactly at `to`; one from 20 hours before that ends inside the range | The AT5 result: filled, raw 26, accepted 24, average and maximum 130. The two records at the bounds are not kept. The long record is kept. Witness C-F3 `fixed`. |
| Z3 | File check | A row with `toMs` equal to `Long.MAX_VALUE` and an offset past the range; a row with a negative `fromMs`; an offset of `Int.MAX_VALUE`; a valid row at a normal time | The first three are dropped in Replace and in Merge and the import still succeeds with every session; the valid row is kept. Witness C-F2 `fixed`. |
| Z4 | Class | Every instance found by the searches of Scope 1 and 3 | Listed in the Coverage table, each fixed with a test or kept with the reason. |
| Z5 | Weak tests | The two tests of Scope 4 | Each fails when its guard is removed (break proof in the report). |
| Z6 | Old rows | The Part C rows and attack cases of the data task | Still hold; each changed assertion is listed with its reason. |
| Z7 | Rules | Whole change | No change in `timer`, `data/TimerCommit.kt`, or `ui/`. No Room schema, text, dependency, or version change. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of each class (instance, status, test, found by) and the three witness results.
- The `Tests:` command passes on the final commit. Commit on branch `hr01-fixc` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| Z1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
