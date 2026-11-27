# PR01-fix1 report

Fix commit: `0680a11` on branch `pr01` (base `c0a6ace`). No push, no merge.

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: `trendText` sums the entries as `BigDecimal.valueOf(kg)` and rounds the mean and the rate size with `HALF_UP` on the exact decimal value, then puts the sign; "±" only when the rounded size is 0.0. The card line and the Today status call `trendText`. | `android/gradlew -p android :app:testDebugUnitTest --tests '*RepRecordsTest'`; `witness run .../F1.json --sha 0680a11` | Test `the rate rounds its size half up before its sign, and the average rounds half up as entered` passes (−0.25 → "−0.3", +0.25 → "+0.3", ±0.05 → "−0.1"/"+0.1", ±0.04 → "±0.0", mean 79.45 → "79.5 kg"). `WITNESS F1 fixed at=0680a1195e7d receipt=r-20261126195436-8c38c4` |
| X2 | done: `SessionScreen.openSummary` reads `vm.repo.allSetLogs.first()` in a coroutine, then sets the summary. It no longer reads `vm.allSetLogs.value`. | `--tests '*FinishSaveOnceTest'`; `witness run .../F2.json --sha 0680a11` | Test `the finish sheet counts rep records against sessions no other screen has loaded` passes: "Rep records" 1, "New bests" 0, with a fresh view model. `WITNESS F2 fixed at=0680a1195e7d receipt=r-20261126195457-aa6e3d` |
| X3 | done: three test gaps closed: `HistoryScreenTest` `most reps by weight shows the heaviest six of seven sets, none of them tappable` and `one Log row holds a weight PR and a rep PR, each in its own style`; new `LineChartTrendTest` (native graphics, pixel counts). | break script: one break at a time in the worktree, the test run, then `git checkout -- <file>` | Every break fails its test (Gradle exit 1), see "Break proofs". All pass on `0680a11`. |
| X4 | done: `doneLine` in `HistoryLog.kt` appends `" · "` before "N PR" and "N rep PR" (normal space before "·"). Test of the built string added. | `--tests '*RepRecordsTest'`; `RepRecordShotsTest.logAndDetail` with `build/shots-reps` | Test `the Log line keeps each dot with the count after it...` passes. Screenshot `1-log-rep-pr.png`: Mon "4 of 4 sets · 2.4 t · 58 min" / "· 2 rep PR"; Wed "... 58 min · 1 PR" / "· 1 rep PR". No line ends with "·". |
| X5 | done: only the fixes above; the `Tests:` command passes on `0680a11`. | `JAVA_HOME=... android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease` | Run 1: tests and assembleDebug done, then "Gradle build daemon disappeared unexpectedly" in lint (low free memory, sibling builds). Run 2, same command: `testDebugUnitTest UP-TO-DATE`, `assembleDebug UP-TO-DATE`, `lintVitalRelease`, `BUILD SUCCESSFUL in 16s`. Test XML totals: 697 tests, 25 skipped, 0 failures, 0 errors. |

## Break proofs

Each line: the break, the test that failed (Gradle exit 1 in every case).

- F1-half-even: `HALF_UP` → `HALF_EVEN` in `trendText`. `RepRecordsTest` rounding test fails (ComparisonFailure at RepRecordsTest.kt:177).
- F2-value: `vm.repo.allSetLogs.first()` → `vm.allSetLogs.value`. `FinishSaveOnceTest` new test fails (ComparisonFailure at FinishSaveOnceTest.kt:170, the "Rep records" value).
- F4a-cap: `REP_BESTS_SHOWN = 7`. Most reps test fails at HistoryScreenTest.kt:258 (row list).
- F4a-order: `repBests` sorted lightest first. Most reps test fails (row list).
- F4a-click: `.clickable {}` on `RepBestRow`. Most reps test fails at HistoryScreenTest.kt:264 (click action).
- F4b-color: "N PR" in `K.Text`. One Log row test fails.
- F4b-weight: "N rep PR" without SemiBold. One Log row test fails.
- F4c-nodraw: trend path not drawn. `LineChartTrendTest` fails at line 97 (muted pixels).
- F4c-scale: `valueBounds` ignores the trend. `LineChartTrendTest` fails at line 100 (points do not move down).
- L1-space: `" · "` → `" · "`. `RepRecordsTest` Log line test fails (ComparisonFailure at RepRecordsTest.kt:215).

## Coverage

| # | Instance | Status | Test | Found by |
|---|---|---|---|---|
| X1.1 | `HistoryRules.kt:394` `trendText` mean | fixed: decimal sum, `HALF_UP` at scale 1 | RepRecordsTest `the rate rounds its size half up...` | finding F1 |
| X1.2 | `HistoryRules.kt:397` `trendText` rate | fixed: exact rate `(S1·n0 − S0·n1)/(n1·n0)`, size rounded `HALF_UP`, then sign | RepRecordsTest `the rate rounds its size half up...`; witness F1 | finding F1 |
| X1.3 | `HistoryProgress.kt:133` card trend line | fixed: calls `trendText` | RepRecordsTest `the rate rounds its size half up...` | grep `trendText(` |
| X1.4 | `TodayRules.kt:277` `bodyweightStatus` | fixed: calls `trendText` | RepRecordsTest `the rate rounds...` (asserts `bodyweightStatus` "7-day avg 79.5 kg") | grep `trendText(` |
| X1.5 | `HistoryRules.kt:375` `weekAverage` | kept: returns a Double, formats no text; text goes through `trendText`, the chart through `trendPoints` | RepRecordsTest `the week average needs 3 entries...` | task list, grep `weekAverage(` |
| X1.6 | `HistoryRules.kt:378` `weekRate` | kept: formats no text; after the fix no main-code caller is left (tests only) | RepRecordsTest `the week average needs 3 entries...` | task list, grep `weekRate(` |
| X1.7 | `HistoryRules.kt:405` `trendPoints` / `LineChart` trend | kept: plotted only; `LineChart` labels only the main points | LineChartTrendTest `the trend draws as a muted line...` | task list, `LineChart.kt` read |
| X1.8 | `ExerciseDetail.kt:201` `RepBestRow` weight "80 kg" | kept: uses the base `fmt` as the record rows do; rounding of base text not changed | HistoryScreenTest `most reps by weight shows the heaviest six...` ("100 kg"…"50 kg") | grep `fmt(` / `format(` in branch diff |
| X2.1 | `SessionScreen.kt:137` `openSummary` read of `vm.allSetLogs.value` | fixed: `vm.repo.allSetLogs.first()` when Finish is tapped | FinishSaveOnceTest `the finish sheet counts rep records...`; witness F2 | finding F2 |
| X2.2 | other `.value` reads of `stream(...)` flows in `ui/` | kept: none found (`grep -rnE "\b(exercises\|…\|planData)\.value\b"` gives only X2.1) | FinishSaveOnceTest `the finish sheet counts rep records...` | grep over all 17 stream names |
| X3.1 | "Most reps by weight": 7 sets → 6 rows, heaviest six, no click | fixed: new test | HistoryScreenTest `most reps by weight shows the heaviest six of seven sets, none of them tappable` | finding F4 |
| X3.2 | One Log row with "1 PR" and "1 rep PR" and their styles | fixed: new test | HistoryScreenTest `one Log row holds a weight PR and a rep PR, each in its own style` | finding F4 |
| X3.3 | Chart with and without trend: scale and drawn series | fixed: new pixel test | LineChartTrendTest `the trend draws as a muted line on the same scale, and a trend outside the points widens the scale` | finding F4 |
| X4.1 | `HistoryLog.kt:120` `doneLine` separators before "N PR" and "N rep PR" | fixed: `" · "` | RepRecordsTest `the Log line keeps each dot with the count after it...`; HistoryScreenTest Log assertions updated to `· ` | lead finding L1 |
| X5.1 | Whole change | fixed: only the files above; no Room, DAO, repository, backup, version, or manifest change | full `Tests:` command | `git diff --stat c0a6ace 0680a11` |

## Notes

- F3: no change, as the lead decided.
- `HistoryLog.kt` now has `doneLine(String, Int, Int)`; `TodayRules.kt:200` has an older `doneLine(Session, List<SetLog>)` in the same package. They are overloads and compile; rename one if the reviewer wants.
- Existing test text changed for L1: HistoryScreenTest lines 171, 222, 223, 224 now expect `· ` before the count.
