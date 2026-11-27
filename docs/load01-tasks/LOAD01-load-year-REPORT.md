# LOAD01-load-year report

Branch `load01`, base 4171ab0. Commits: 5fdac9f (rules, screen, entry, rule tests), e5ec2ca (screen tests, screenshot test, chart steps, SPEC.md, DESIGN.md).

| # | Status | Check ran | Real output |
|---|---|---|---|
| L1 | done: `loadWeight` in `LoadRules.kt` counts the bounds 50, 60, 70, 80, 90 % with `bpm * 100 >= max * percent` | `gradlew :app:testDebugUnitTest --tests '*LoadRulesTest'`, `weightsByTheIntegerRule` | max 200: 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 5; max 187: 93→0, 94→1, 112→1, 113→2, 130→2, 131→3, 149→3, 150→4, 168→4, 169→5; LoadRulesTest 10 0 0 0 |
| L2 | done: `bucketsLoad` sums weight × 5 s over `pictureBuckets`, then `floorDiv(2 * sec + 60, 120)` | same, `loadIsWeightedMinutesRoundedHalfUp` | 12 × w5: 5; 6 × w1: 1; 5 × w1: 0; 40 × w0: 0; 12 buckets with gaps plus 2 outside the range: 5; pass |
| L3 | done: `sessionLoad` returns null unless the session is finished, the fact is `HrFacts.Filled` and a maximum is in use | same, `noLoadWithoutAFilledFactOrAMaximum` | waiting, none, off (removed), stale range, no row, null fact, no maximum, unfinished: all null; pass |
| L4 | done: `loadYear` groups finished sessions by `date`, week start from `weekOf` (Monday) | same, `dayAndWeekSums`, `weekAcrossAMonthAndAYearEnd` | `LoadDay(2026-11-24, 2, 40)`; no-load day: sessions 1, load null; week of 2025-12-29 holds Dec 31 and Jan 1, load 16, Sunday Dec 28 in the week before; pass |
| L5 | done: `loadStep` compares `4 * load`, `2 * load`, `4 * load` with `3 * highest`; no division. A highest of 0 gives step 1 (in SPEC.md) | same, `stepsAgainstTheHighestDay` | 1, 1, 2, 2, 3, 4, 4; one loaded day: step 4; all 0: step 1; pass |
| L6 | done: 52 weeks, current first; days after today left out; window from Monday 51 weeks back. A session on that Monday shows, one on the day before does not | same, `yearIs52WeeksNewestFirst`; `LoadScreenTest.cellsFillByStepAndSumsStandOnTheirRows` | 52 Mondays newest first; Wednesday today: 3 days in week 0; oldest Monday counted, day before and tomorrow not; 52 row nodes; no cell for tomorrow; pass |
| L7 | done: `ListRow("Load")` under "Compare two periods" in `HistoryProgress.kt` (1 line), shown when `rememberLoadYear(...).anyLoad`; `Routes.LOAD` and its `composable` in `KhonNav.kt` (2 lines) | `LoadRulesTest.anyLoadNeedsALoadedSessionInTheYear`; `LoadScreenTest.entryIsAbsentWithoutALoadAndOpensTheScreen` | no heart rate rows: no "Load"; rows written: "Load" appears, tap: route `load`, Back: route `history`; pass |
| L8 | done: `loadChart` takes the weeks with a value among the last 12, oldest first, needs 2; `LineChart` titled "Load per week" with `ChartTitle` (made `internal` in `ExerciseDetail.kt`, 1 word) | `LoadRulesTest.chartOfTheLast12Weeks`; `LoadScreenTest.chartDrawsTheLineOfTheWeeksWithAValue`, `oneWeekWithAValueHasNoChart`; pixel test again with the `LineChart` call replaced by an empty Box, then restored | 5 points in week order, week 13 left out; 1 value: no chart; pass. Line removed: `chartDrawsTheLineOfTheWeeksWithAValue: AssertionError: accent in 0 of 984 columns` |
| L9 | done: cells accent at alpha 0.30, 0.50, 0.75, 1.0 by step; Surface; Surface with 1 dp muted border | `LoadScreenTest.cellsFillByStepAndSumsStandOnTheirRows` (150-session fixture); again with the accent fill replaced by Surface, then restored | one cell per step matches Accent at its alpha over Bg (±6 per channel); no-load cell: Surface center, Muted edge; empty: Surface both; week sums found inside their row nodes; pass. Fill removed: `AssertionError: step 1 on 2026-11-21: ff1c1c1e` |
| L10 | done: `rememberLoadYear` reads `vm.sessions`, `vm.allSessionHr` and `rememberMaxInUse` | `LoadScreenTest.gridAndChartFollowTheMaximumAndAFill` | typed max 186 → 150: week row speech changes to the higher load; a filled row written for the session without one: its week changes from "no load" to a load; pass |
| L11 | done: month and load columns as wide as the widest text ("0000" or longer); cells `weight(1f)` | `LoadScreenTest.fitsAt360`, `fitsAt360Scale13`, `fitsAt411`, `fitsAt411Scale13`; TapTargetTest, ContrastTest in the full run; pictures viewed | heavy week load 1668: no clipped text, cell widths differ by ≤ 1 px, sum right of Sunday and inside its row, rows do not overlap; TapTargetTest 12 0 0 0; ContrastTest 11 0 0 0 |
| L12 | done: `loadWeekSpoken`; the row merges its texts and speaks the description, like `ReviewScores`; header letters are silent; the chart is the plain `LineChart` (it has no speech of its own, as on the other screens) | `LoadRulesTest.weekRowsSpeakInFullWords`; speech asserts in `LoadScreenTest` | "Week of November 23, load 412, 4 days"; "Week of November 23, no load, 1 day"; "…, no load, 0 days"; pass |
| L13 | done: muted text, accent fills by step only, no verdict or target; no Room, DAO, backup, setting, permission, dependency or version change; SPEC.md and DESIGN.md updated | `~/.config/agents/tools/comment-guard --base 4171ab0`; `git diff 4171ab0 --stat` | comment-guard exit 0; changed existing code files: KhonNav.kt +2, HistoryProgress.kt +1, ExerciseDetail.kt 1 word, LineChart.kt 2 lines (see deviations) |

## Tests

`JAVA_HOME=".../jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease` on e5ec2ca:
exit 0, `BUILD SUCCESSFUL in 2m 39s`, 109 test classes, 1026 tests, 0 failures, 0 errors, 61 skipped (the shot tests whose folder does not exist, LoadShotsTest 12 among them).

New tests on the base: `LoadRules.kt` and `LoadScreen.kt` do not exist at 4171ab0 (`git cat-file -e`: "exists on disk, but not in '4171ab0'"), and `niceStep` is private there, so every new test does not compile on the base.

## Screenshots

`android/app/build/load01-shots/` (12 PNG, 1080 or 1233 px wide, 9600 px tall): `load-year-*`, `load-few-*` and `progress-*`, each at 360dp and 411dp, x1_0 and x1_3. I viewed each kind. Make them again with `mkdir -p android/app/build/shots-load` and one `LoadShotsTest` method group per run (see blocked below).

## Deviations

- deviation: `LineChart.niceStep` stopped at a step of 100, so a week load of 1668 drew 17 crowded axis labels. It now also has 200, 250, 500 and 1000 and is `internal` for `LineChartStepTest`. Spans up to 300 keep their old steps.
- deviation: "a session 52 weeks ago" read as a session in the oldest of the 52 rows (Monday 51 weeks before the current Monday); the day before it is in the 53rd week and is not shown.
- blocked: `gradlew :app:testDebugUnitTest --tests '*LoadShotsTest'` with `build/shots-load` present hangs: the test JVM spins at 100 % CPU (killed after 5 min, `timeout 300` exit 124; the lead saw the main thread in compiled code that never reaches a safepoint). The same tests pass in parts: `few*` and `year*` (8 tests in one JVM, exit 0), `progress360`, then `progress360x13` and `progress411*` (5.9 to 8.6 s each). The hang needs both groups in one JVM; I found no long loop in the new code. The full run skips the class because the folder does not exist.
- The trunk was not merged into the branch: the lead said no merge.
