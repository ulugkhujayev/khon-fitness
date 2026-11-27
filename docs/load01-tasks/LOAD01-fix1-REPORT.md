# LOAD01-fix1 report

Branch `load01`, base 1e8dfe0. Commits: affc4bb (code and tests), ed7f78a (SPEC.md, DESIGN.md).

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | blocked: the rule is fixed (`rowBuckets` keeps the first stored sample of each offset; `pictureBuckets`, `roundAverage` and the load read through it), but witness F1 cannot print `fixed`. Its row goes through `importedHr`, and `validFileHr` (merged from int07, `HeartRead.kt:149`) refuses offsets that do not rise, so the import drops the row and the load is null. Error: `java.lang.AssertionError: expected:<5> but was:<null>` at `LoadDuplicateOffsetWitness.kt:19` | `witness run F1.json --sha affc4bb…` (receipt r-20261127005035-01a5b9); the witness test applied locally with the import bypassed; `LoadRulesTest.eachOffsetCountsOnceTheFirstStored`, `ScoreRulesTest.repeatedOffsetsCountOnceInEveryCardioFigure`, `PictureRulesTest` "the picture draws the first stored sample of each offset" | witness: `WITNESS F1 reproduces at=affc4bb2e3de`. Local copy with `hrFacts(session, row, zone)`: `assertEquals(null, imported)`, `validFileHr(row) == false`, load 5: tests=1 failures=0. Rules: 12 offsets twice at 190, max 200: load 5; pairs 120 then 190: load 2, 190 then 120: load 5; hard line "1:00" (pairs 120 first: "0:00"); zone shares 0/100/0 %; round peak 120, round average 120; type chart 1.0, 1.0 min; picture curve 12 buckets, "highest 120". LoadRulesTest 13 0 0, ScoreRulesTest 22 0 0, PictureRulesTest 10 0 0 |
| X2 | blocked: the step rule is fixed (`niceStep` goes on with 1, 2, 2.5, 5 × 10^k above 1000; `ValueAxis.ticks` counts its ticks), but witness F2 cannot print `fixed`. Robolectric draws a Canvas only when a test takes an image, and the witness takes none, so the formatter is never called. Error: `java.lang.AssertionError: The chart did not draw numeric labels` at `LoadWideAxisWitness.kt:41` | `witness run F2.json --sha affc4bb…` (receipt r-20261127005048-a35c9b); the witness test applied locally plus `compose.nodeImage(view, compose.onRoot())`; `LineChartStepTest` | witness: `WITNESS F2 reproduces at=affc4bb2e3de`. Local copy with the image: tests=1 failures=0, `LABELS [0.0, 5000.0, 10000.0, 12600.0, 15000.0]`. Steps: 3000→1000, 3001→2000, 12600→5000, 1e6→500000, 1e9→5e8; spans 0..3000 equal the old list; ticks 1 to 5 for 13 spans × 4 lows up to 1e300; old step test unchanged. LineChartStepTest 4 0 0 |
| X3 | done: pure `pointLabelBox` (the old x and y rule) and `drawnLabels` (last, highest, then newest to oldest; a box grown by 2 dp must meet no drawn box); `LineChart` draws only the chosen labels | `LineChartLabelsTest`; `LoadScreenTest.equalWeekLabelsStayApartAt360Scale13`, `equalWeekLabelsStayApartAt411` (measured boxes) | seven 4-digit equal values at 360 dp, 1.3: boxes 0 and 1 overlap, drawn [6, 0, 4, 3, 2]; two equal points: both when apart, only the last when they meet; highest next to last: only the last when they meet, both when apart. Screen, 7 weeks of 1668: 360 dp 1.3 draws 5 labels, 411 dp 1.0 draws 7, no two drawn boxes intersect, all inside the canvas. LineChartLabelsTest 5 0 0 |
| X4 | done: `untilNextDay(now: ZonedDateTime)` uses `Duration.between` of two zoned times (instants); `rememberToday` passes `ZonedDateTime.now(clock)` | `HistoryRulesTest` "a day of 23 or 25 hours…"; `TodayRolloverTest`; `witness run F4.json` | New York 2026-03-08 00:00: 23 h 1 s; 2026-11-01: 25 h 1 s; Tashkent: 24 h 1 s. Rollover after 23 h 5 s (New York) and 24 h 5 s (Tashkent): pass. `WITNESS F4 fixed at=affc4bb2e3de receipt=r-20261127005105-9ab499` |
| X5 | done: `loadMonth` reads `week.days` | `LoadRulesTest.monthLabelOnlyFromDrawnDays`; `witness run F5.json` | Mon 2026-11-30: null; 2026-12-01: "Dec"; full week of Nov 30 on Dec 9: "Dec". `WITNESS F5 fixed at=affc4bb2e3de receipt=r-20261127005122-831512` |
| X6 | done: `LoadWeek.load` is `Long?`; `dayLoad` sums as Long and holds at `Int.MAX_VALUE`; `loadStep`, `loadWeight`, `bucketsLoad` use Long math | `LoadRulesTest.sumsAndThresholdsDoNotWrap`; `witness run F6.json` | week of 2,147,483,647 + 1: 2147483648, spoken "load 2147483648", chart value 2.147483648E9; `loadStep(600000000, 600000000)` = 4; `dayLoad(MAX−1, MAX−1)` = 2147483647. `WITNESS F6 fixed at=affc4bb2e3de receipt=r-20261127005135-fa3f8c` |
| X7 | done: L7 uses `khonScreens` (the production graph, now a `NavGraphBuilder` function of `KhonNav.kt`); L8 reads the drawn points through `LocalChartDrawn`; L9 has literal alphas; L10 fills a session in week 8 and waits for the chart; L11 checks the measured label and tick boxes | each mutation, the test, then `git checkout` of the file (log below) | see "Break proofs" |
| X8 | done: full suite, 12 pictures viewed, no verdict word, color, suggestion or target added | the `Tests:` command with `--rerun-tasks`; LoadShotsTest in two runs | see "Tests" and "Screenshots" |

## Break proofs

Each mutation ran with `timeout 240 android/gradlew -p android :app:testDebugUnitTest --tests '*LoadScreenTest.<test>'`, then the file was restored.

- L7, `composable(Routes.LOAD) { … }` removed from `khonScreens`: `entryIsAbsentWithoutALoadAndOpensTheScreen FAILED`, `IllegalArgumentException: Navigation destination that matches route load cannot be found in the navigation graph`.
- L8, `LineChart(chart.asReversed(), …)` in `LoadScreen`: `chartDrawsTheLineOfTheWeeksWithAValue FAILED`, `AssertionError: want [ChartPoint(date=2026-09-07, value=342.0 …` (drawn points in reverse).
- L9, `LOAD_STEP_ALPHAS` first value 0.30 → 0.40: `cellsFillByStepAndSumsStandOnTheirRows FAILED`, `AssertionError: step 1 on 2026-11-21: ff663f04`.
- L10, `remember(year) { loadChart(year) }` → `remember { loadChart(year) }`: `gridAndChartFollowTheMaximumAndAFill FAILED`, `ComposeTimeoutException: Condition still not satisfied after 15000 ms` (the chart stays as first composed).
- L11, `drawnLabels` keeps every box: `equalWeekLabelsStayApartAt360Scale13 FAILED`, `AssertionError: Rect.fromLTRB(839.0, 200.5, 960.0, 262.5) meets Rect.fromLTRB(768.7, 209.5, 874.7, 265.5)`.
- After the restores: `git status` clean, LoadScreenTest tests=11 skipped=0 failures=0 errors=0.

## Coverage

| # | Instance | Status | Test | Found by |
|---|---|---|---|---|
| X1.1 | `pictureBuckets` (PictureRules.kt) reads `rowBuckets` | fixed: first sample per offset | `PictureRulesTest` first stored sample | review F1 |
| X1.2 | load `bucketsLoad` via `pictureBuckets` | fixed | `LoadRulesTest.eachOffsetCountsOnceTheFirstStored` | review F1 |
| X1.3 | cardio figures: hard time, zone shares, round peak and drop, type charts (`scoreBuckets` removed, `pictureBuckets` used) | fixed | `ScoreRulesTest.repeatedOffsetsCountOnceInEveryCardioFigure`, `oneBucketPerOffsetTheFirstInTimeOrder` | review F1, lead decision 1 |
| X1.4 | Review picture `workoutPicture` (curve, axis, set rows, speech) via `pictureBuckets` | fixed | `PictureRulesTest` first stored sample | lead decision 1 |
| X1.5 | `roundAverage` (HeartRate.kt) read `parseSamples` | fixed: reads `rowBuckets` | `ScoreRulesTest.repeatedOffsetsCountOnceInEveryCardioFigure` | grep of `parseSamples` readers |
| X1.6 | `measuredMaxHr` reads the stored `maxHr`, no sample | kept: no bucket count | `ScoreRulesTest.measuredMaximumOfTheLast365Days` | grep of `parseSamples` readers |
| X1.7 | `validFileHr` (HeartRead.kt) reads `parseSamples` to check a file row | kept: refuses offsets that do not rise | `BackupHrTest` | grep of `parseSamples` readers |
| X1.8 | `PictureScreenTest.kt:146` and `HeartRateRulesTest` read `parseSamples` | kept: test code | the same tests | grep of `parseSamples` readers |
| X1.9 | witness F1 | blocked: `expected:<5> but was:<null>`, the import refuses the row | none | review F1 |
| X2.1 | `niceStep` above 1000 | fixed | `LineChartStepTest` | review F2 |
| X2.2 | `ValueAxis.ticks` counted by index, at most 5 | fixed | `LineChartStepTest.everyFiniteSpanHasAtMostFiveTicks` | lead decision 2 |
| X2.3 | witness F2 | blocked: `The chart did not draw numeric labels`, no draw without an image | none | review F2 |
| X3.1 | `LineChart` point labels, every chart | fixed: `drawnLabels` | `LineChartLabelsTest`, `LoadScreenTest.equalWeekLabels*` | review F3 |
| X3.2 | pictures that lose a label (other shot tests run once with a print) | kept: wanted result | `ScoreShotsTest`, `SetupShotsTest` | shot run |
| X4.1 | `untilNextDay` | fixed | `HistoryRulesTest` | review F4 |
| X4.2 | `rememberToday` callers: History, Load, Compare, Heart data, lift and cardio type detail | fixed through `rememberToday` | `TodayRolloverTest`, witness F4 fixed | review F4 |
| X5.1 | `loadMonth` | fixed | `LoadRulesTest.monthLabelOnlyFromDrawnDays`, witness F5 fixed | review F5 |
| X6.1 | `LoadWeek.load` Long | fixed | `LoadRulesTest.sumsAndThresholdsDoNotWrap`, witness F6 fixed | review F6 |
| X6.2 | day sum `dayLoad` held at `Int.MAX_VALUE` | fixed | same | review F6 |
| X6.3 | `loadStep` thresholds | fixed | same | review F6 |
| X6.4 | `loadWeight` `bpm * 100` against `max * percent` | fixed: Long | same | lead decision 6 |
| X6.5 | `bucketsLoad` seconds sum | fixed: Long, held at `Int.MAX_VALUE` | `LoadRulesTest.loadIsWeightedMinutesRoundedHalfUp` | lead decision 6 |
| X6.6 | chart value and spoken week text | fixed: follow the Long | `LoadRulesTest.sumsAndThresholdsDoNotWrap` | lead decision 6 |
| X7.1 | L7 production graph | fixed | `entryIsAbsentWithoutALoadAndOpensTheScreen` | review F7 |
| X7.2 | L8 drawn point order | fixed | `chartDrawsTheLineOfTheWeeksWithAValue` | review F7 |
| X7.3 | L9 literal alphas | fixed | `cellsFillByStepAndSumsStandOnTheirRows` | review F7 |
| X7.4 | L10 fill in the chart weeks | fixed | `gridAndChartFollowTheMaximumAndAFill` | review F7 |
| X7.5 | L11 label bounds | fixed | `fitsAt*`, `equalWeekLabels*` | review F7 |
| X8.1 | whole suite and screenshots | kept | the `Tests:` command | task |

Witness results at affc4bb: F1 reproduces (blocked, above), F2 reproduces (blocked, above), F4 fixed, F5 fixed, F6 fixed.

## Tests

The `Tests:` command on ed7f78a (the code is the same as affc4bb), run three times:

1. With `--rerun-tasks`: `timeout 900` ended it, exit 124. One "Gradle Test Executor" of this worktree spun at 99 % CPU for 14:38 (the known fault). I killed it with `kill -9`.
2. Without `--rerun`: exit 1, `BUILD FAILED in 1m 49s`, 1108 tests, 1 failure: `CardioIntervalsKeptTest > a cleared field writes null and keeps the other field's imports`, `ComposeTimeoutException: Condition still not satisfied after 15000 ms`. That class alone then passed: `BUILD SUCCESSFUL in 6s`, tests=8 failures=0.
3. With `--rerun`: exit 0, `BUILD SUCCESSFUL in 2m 1s`, 114 test classes, 1108 tests, 0 failures, 0 errors, 63 skipped (the shot tests whose folder does not exist, LoadShotsTest 12 among them).

Run 2 had a timing failure in a class I did not change. CardioScreen does call `roundAverage`, which I changed. It passed alone and in run 3. I did not find its cause.

New tests on the base: `LineChartLabelsTest`, `TodayRolloverTest` and the new `LoadRulesTest` and `LoadScreenTest` cases use names that do not exist at 1e8dfe0 (`drawnLabels`, `pointLabelBox`, `dayLoad`, `rowBuckets`, `LocalChartDrawn`, `khonScreens`, `untilNextDay(ZonedDateTime)`), so they do not compile there (read). On the code side the break proofs above show the L7 to L11 failures, and witnesses F4, F5 and F6 reproduced on 8fb54d6 (the lead's receipts) and are fixed on affc4bb.

## Screenshots

`android/app/build/load01-fix1-shots/` (renamed from `shots-load` after the run, so the full suite skips LoadShotsTest as on the base): 12 PNG, `load-year-*`, `load-few-*`, `progress-*` at 360dp and 411dp, x1_0 and x1_3, 1080 or 1233 by 9600 px. Made in two runs (`year*` and `few*`: BUILD SUCCESSFUL in 18s; `progress*`: in 20s). I viewed all 12. The year chart axis shows 0, 1000, 2000; the last (130) and highest (1668) labels draw in every picture; no Load picture lost a label.

Pictures of other screens that now leave out a label, found by one run of the chart shot tests with a temporary print in `LineChart` (removed):
- `shots-score/type-bike-360dp-x1_3.png` and `type-bike-411dp-x1_3.png`: "At or above 85 %" leaves out "7:20" (it met "9:50 min").
- `shots-setup/5-row-long-program-font-1.3.png`: the Cable Seated Row chart leaves out one "57.5".
I deleted those shot folders after viewing.

## Deviations

- deviation: `roundAverage` is in `data/` and does not import `ui/`, so it reads `rowBuckets` (the one-per-offset rule that `pictureBuckets` itself calls), not `pictureBuckets`.
- deviation: `LineChart` gets an internal `LocalChartDrawn` (null by default) that reports the drawn points and label boxes; L8, L10 and L11 read it. The navigation graph moved into `NavGraphBuilder.khonScreens(vm, nav, clock)` so L7 opens the production graph with the test clock.
- Seen, not changed: at font scale 1.3 the date labels under some charts touch ("Nov 13Nov 16" on type-bike 360 dp). Decision 3 covers point labels only.
