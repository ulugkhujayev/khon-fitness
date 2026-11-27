# PR01-rep-records-weight-trend report

Branch `pr01`, final commit `a3d5ad8` (base `9d1ba17`, `origin/main` is `9d1ba17`, so the trunk is already in the branch).

Commits:

- `c995b09` Show rep records next to weight records, and a 7-day bodyweight trend
- `99651e3` Describe rep records and the bodyweight trend in SPEC and DESIGN
- `a3d5ad8` Pass the session row's chip color as a Color the contrast check can trace

Rule tests are in `android/app/src/test/java/dev/mirzohidkhon/khonfitness/ui/screens/RepRecordsTest.kt` (one test per row, named by the case). Screen tests are in `HistoryScreenTest.kt`.

| # | Status | Check ran | Real output |
|---|---|---|---|
| C1 | done: `repRecordsOf` + `LiftSession.repRecords` filled by `liftHistory`; `repRecordCount` | `gradlew :app:testDebugUnitTest --tests '*RepRecordsTest'` | `RepRecordsTest tests="12" skipped="0" failures="0" errors="0"`; test `more reps under a heavier earlier record is a rep record, not a weight record` |
| C2 | done: equal reps and a set matched by a heavier earlier set give none | same | test `equal reps or a lighter set matched at a heavier weight is no rep record` passes |
| C3 | deviation: as written (earlier 80x6, 70x7, no heavier set), today's 80x8 beats 80x6 at the same weight, so it is the weight record (Scope rule 4 excludes it) and only 70x10 is a rep record. I kept the Scope rule. The test checks that reading and the intended one with an earlier 85x3: then 80x8 alone is a rep record, and 80x8 + 70x10 are both | same | test `a set matched by a heavier set of the same session is no rep record` passes |
| C4 | done: weight record set green with chip "PR"; 80x8 in `repRecords`; `repRecordCount` 0, `recordCount` 1 | same + `--tests '*HistoryScreenTest'` | `HistoryScreenTest tests="17" failures="0"`; detail row "Fri" shows `PR` chip, `65×3` span K.Green, `60×10` span K.Text SemiBold |
| C5 | done: first session, 0 kg (also 0 kg under an earlier weighted set: a set must be above 0 kg), skipped/0-rep sets, open sessions, two sessions on one date by `startedAt` | RepRecordsTest | test `the first session, 0 kg, uncounted sets and open sessions never give or serve as rep records` passes |
| C6 | done: of equal sets the first in set order stays | RepRecordsTest | test `of two equal sets above the earlier best, ...` passes (`repRecords.single() === sets[0]`) |
| C7 | done: `Summary.repRecords`; finish sheet row "Rep records" under "New bests"; exercises in `bests` excluded; compares with all other sessions | RepRecordsTest; screenshot | test `the finish sheet counts new bests and rep records apart, ...` passes; shot shows "New bests 1", "Rep records 1" |
| C8 | done: `repBests` (Pareto set, earliest of equals, heaviest first); group "Most reps by weight", max 6, only with 2+ rows, rows not tappable | RepRecordsTest + HistoryScreenTest | `repBests` = 85x3, 80x8, 60x15 with sessions a, b, c; screen test `the exercise detail has no most reps group with one such set` passes |
| C9 | done: Log line " · N rep PR" in K.Text SemiBold (no-break spaces inside, so it never splits); detail chip "Rep PR" via `LabelChip(text, K.Text)`; one chip per row | HistoryScreenTest | test `a rep record shows next to the weight record in the Log and the exercise detail` passes: "1 of 1 set · 0.5 t · 58 min · 1 rep PR", span K.Text/SemiBold, "Rep PR" and "PR" chips |
| C10 | done: `weekAverage`, `weekRate` | RepRecordsTest | test `the week average needs 3 entries in 7 days, the rate needs two such weeks` passes (null; avg only; avg only; avg + rate; later entry ignored; today-6 in, today-7 in the older window) |
| C11 | done: `trendText` rounds the rate to 0.1 first, then "+", "−" or "±" | RepRecordsTest | "+0.3 kg/week", "−0.3 kg/week", "±0.0 kg/week" x2 pass |
| C12 | done: `bodyweightStatus` returns `trendText` after today's save when it exists | `--tests '*TodayRulesTest'` + RepRecordsTest | `TodayRulesTest tests="21" failures="0"`. Changed asserts: the 3 old "Saved for today · …" lines used Oct 1, Oct 5, Oct 7 (3 entries in 7 days), so they now are the new case; they keep their text with the Oct 1 entry dropped, and 2 asserts check "7-day avg 80.3 kg" / "80.1 kg" for the old input |
| C13 | done: `trendPoints`; `LineChart(trend = …)` 1.5 dp K.Muted line, no points; `trendAt` + `valueBounds` set the shared scale; default `emptyList()` keeps other callers | RepRecordsTest | test `the trend points use entries before the range ...` passes; Progress screenshot shows the gray average line under the orange line |
| C14 | done: no Room, DAO, repository, backup, permission, dependency, or version change; no suggestion text; SPEC.md and DESIGN.md updated | `git diff --stat 9d1ba17 HEAD`; `comment-guard --base 9d1ba17` | diff touches only ui/screens, ui/components/LineChart.kt, tests, SPEC.md, DESIGN.md; `guard=0` |

## Base check

New tests do not compile on `9d1ba17` (main sources checked out from the base, tests from the branch, then restored):

```
e: RepRecordsTest.kt:10:51 Unresolved reference 'trendAt'.
e: RepRecordsTest.kt:110:27 Unresolved reference 'repRecords'.
e: RepRecordsTest.kt:136:21 Unresolved reference 'repBests'.
e: RepRecordsTest.kt:149:20 Unresolved reference 'weekAverage'.
```

## Full suite on a3d5ad8

`JAVA_HOME=… android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease --rerun-tasks`

```
BUILD SUCCESSFUL in 1m 40s
71 actionable tasks: 71 executed
suites=73 tests=691 skipped=21 failures=0 errors=0
```

One earlier full run failed once in `PlanScreenTest` "Plan keeps its own scroll position across a tab switch" (expected 1664.0, was 2000.0). Plan is not in this change. It passed 3 of 3 runs alone and in the final full run, so it is flaky under load.

## Screens looked at

`RepRecordShotsTest` (runs only when `android/app/build/shots-reps` exists) wrote these; I viewed each one:

- `android/app/build/shots-reps/1-log-rep-pr.png`: "2 rep PR", "1 PR · 1 rep PR", "1 PR" rows
- `android/app/build/shots-reps/2-exercise-detail.png`: "Most reps by weight" (4 rows), "PR" and "Rep PR" chips, bold rep record sets
- `android/app/build/shots-reps/3-progress-bodyweight.png`: "7-day avg 82.6 kg · −0.1 kg/week" line, gray trend line
- `android/app/build/shots-reps/4-today-bodyweight.png`: Today status "7-day avg 82.6 kg · −0.1 kg/week"
- `android/app/build/shots-reps/5-finish-sheet.png`: "Rep records 1" under "New bests 1"

## Notes

- deviation: a set at 0 kg is never a rep record, also in an exercise with weighted sets. C5 says "no rep record ... at 0 kg"; Scope's list did not state it for mixed exercises.
- With tabular figures, Inter draws "7-day" with a wide hyphen ("7 - day"). The task asks for tabular text; I kept it.
- `android/local.properties` (ignored by git) was added so Gradle finds the SDK. Remove: `rm android/local.properties`.
