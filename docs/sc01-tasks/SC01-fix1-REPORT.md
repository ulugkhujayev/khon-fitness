# SC01-fix1 report

Branch `sc01`, base 2248c44. Commits: 3af2e68 (fixes and tests), 568d00a (ten-round screenshots).

| # | Status | Check ran | Real output |
|---|---|---|---|
| T1 | done: `measuredMaxHr` counts a finish in `[now - 365 days, now]`; `rememberMeasuredMax` reads `LocalWallClock` again on each resume (`LifecycleResumeEffect`) and on each input change | `gradlew :app:testDebugUnitTest --tests '*ScoreRulesTest' --tests '*ScoreScreenTest'` (`measuredMaximumCountsFinishesFromNowLess365DaysToNow`, `measuredMaximumLeavesOnResumeOnceItIsOlderThan365Days`); `witness run F1.json --sha 568d00a` | finish = now: 183; finish 1 ms in the future: null; exactly 365 days: 183; 1 ms older: null; tomorrow: null. Screen: "measured N" stays after the clock passes 365 days, "not set" after STARTED -> RESUMED. ScoreRulesTest 20 0 0, ScoreScreenTest 16 0 0. `WITNESS F1 fixed at=568d00ab23dc receipt=r-20261126231211-a85ce9`, log `BUILD SUCCESSFUL in 12s` |
| T2 | done: `maxEntry(text)`: "" clears, `[1-9][0-9]*` from 100 to 230 stores, every other text is refused; `NumberField` has an optional `onTextChange` mode that keeps the text as typed (no cleaning, no rewrite on blur or from outside); the sheet uses it | `ScoreRulesTest.sheetEntryJudgesTheTextAsTyped`, `ScoreScreenTest.sheetJudgesTheTextAsTypedAndKeepsTheSavedValue`, `sheetRefusesBadValuesAndClears`; `witness run F2.json` | Saved 186; inputs "", "abc", "200.5", "-201", "2x02", " 150", "150", "99", "231": the field shows each as typed, Save on only for `["", "150"]`, a tap on a disabled Save keeps 186. `WITNESS F2 fixed at=568d00ab23dc receipt=r-20261126231225-ed118f`, log `BUILD SUCCESSFUL in 16s` |
| T3 | done: (a) `scoreBuckets` = `pictureBuckets` (range, time order) then the first bucket of each offset; `scoreRows` and `scoreCharts` (every caller of this change) use it. (b) `validFileHr` also refuses offsets that are not whole buckets or not strictly rising | `ScoreRulesTest.oneBucketPerOffsetTheFirstInTimeOrder`, `ScoreScreenTest.aRowWithARepeatedOffsetCountsTheBucketOnce`, `BackupHrTest` (`validFileHr takes only strictly rising offsets that are whole buckets`, `rows with a repeated, an unaligned or a falling offset are dropped in Replace and Merge and every session stays`); `witness run F3.json` | Database row 0,0,5 (+10 at 120): "At or above 159 beats per minute, 10 seconds"; first of each offset kept; one-bucket round has no row; chart points 5 s and 10 s. Replace and Merge: rows 0,0,5 / 0,3 / 10,5 dropped, 4 sessions kept, only `ok` row stays. BackupHrTest 14 0 0. `WITNESS F3 fixed at=568d00ab23dc receipt=r-20261126231243-2e95e5` |
| T4 | blocked: the code holds T4 (ticks proven below), but witness F4 cannot print `fixed` while the exact point labels "0:00" and "100" stay; its pixel matcher reads them as "-1:00" and "101" | `ScoreRulesTest.chartTicksStayInsideThePossibleValues` (`valueAxis`); `witness run F4.json --sha 568d00a`; a probe test (removed) that drew one label and ran the witness's `containsLabel` | Ticks: 0:00 -> 0:00 1:00 2:00; 0 % -> 0 1 2; 100 % -> 98 99 100; 40/60 -> 40 50 60; every pair 0..100 step 5 inside 0..100 with a span; no domain: -1 0 1 (other charts unchanged). Witness: `WITNESS F4 reproduces at=568d00ab23dc receipt=r-20261126231254-79f09f`; log `ScoreChartAxisWitnessTest > fullZoneShareHasNoOver100Tick FAILED / java.lang.AssertionError at ScoreChartAxisWitnessTest.kt:127`, same for `zeroHardTimeHasNoNegativeTick`; `zeroZoneShareHasNoNegativeTick` passes. Probe: `1:00 vs -1:00 at 10: true`, `0:00 vs -1:00 at 4: true`, `2:00 vs -1:00 at 4: true`, `100 vs 101 at 0: true`, `99 vs 101 at 0: false`, `100 vs -1 at 0: false` |
| T5 | done: `roundScores` returns one `RoundScore` per round (peak nullable); a round that overlaps another or starts at or before the end of the timed round before it gets no peak and no drop; the round before it gets no drop | `ScoreRulesTest.aRoundInsideAnotherHasNoFiguresAndTheRoundBeforeHasNoDrop`, `aRoundThatStartsBeforeTheRoundBeforeEndsHasNoFigures`, `orderedRoundsGiveTheSameFiguresInAnyListOrder`, `roundDrop`, `dropOfTheLastRoundRunsToTheRangeEnd`; `witness run F5.json` | Round inside round 1: rounds 2, 3 `RoundScore(n, null, null)`, round 1 drop null, round 4 unchanged; overlap, early start, start = previous end: no figures; ordered session equal in any list order; S6 values 42, -4, 38 unchanged. `WITNESS F5 fixed at=568d00ab23dc receipt=r-20261126231310-3e3b42` |
| T6 | done: removed the 2 KDocs of `MaxHeartRate.kt`, the `percentShares` KDoc of `ScoreRules.kt`, the `scoreSession` KDoc of `ScoreFixtures.kt`; the new code adds no comment | `~/.config/agents/tools/comment-guard --base 2248c44`; `git diff 2248c44 -- android \| grep -E '^\+.*(//\|/\*)'` | guard exit 0; grep: no line. Kept in the first-change files: none. |
| T7 | done: `tenRoundsFitAt411Scale1` added (now 360 x1.0, 360 x1.3, 411 x1.0, 411 x1.3); `ScoreShotsTest` shoots a 10-round session `review-hiit10-*` at the four sizes | `ScoreScreenTest`; `mkdir -p android/app/build/shots-score` + `--tests '*ScoreShotsTest'` | 4 layout tests pass (11 rows, no clip, no overlap); 32 PNGs, the four `review-hiit10-*` viewed: "Round 10 peak 165 · drop 42" on one line at each size |
| T8 | done: S1-S13 tests pass; changed assertions listed under "Changed assertions" | Tests command | 899 tests, 0 failures, 0 errors |
| T9 | done: in `data` only `validFileHr` (rule and its KDoc), its tests, and `MaxHeartRate.kt` (comments only) | `git diff 2248c44 --stat -- android` | data: HeartRead.kt +4 -2, MaxHeartRate.kt -5, BackupHrTest.kt +31 -1. No Room, DAO, text, dependency or version change |

## Coverage

| ID | Instance | Status | Test | Found by |
|---|---|---|---|---|
| T1.1 | F1: `measuredMaxHr` had a lower bound only | fixed: window `[now - 365 days, now]` | `measuredMaximumCountsFinishesFromNowLess365DaysToNow`; witness F1 fixed | review, witness F1 |
| T1.2 | F1: the Settings row kept an expired value (`rememberMeasuredMax` cache) | fixed: read again on resume | `measuredMaximumLeavesOnResumeOnceItIsOlderThan365Days` | review |
| T1.3 | F1: Review group and type chart (`rememberMaxInUse` calls `rememberMeasuredMax`) | fixed: same function | `measuredMaximumLeavesOnResumeOnceItIsOlderThan365Days` | fixer |
| T2.1 | F2: `maxEntry` judged the cleaned number | fixed: judges the text | `sheetEntryJudgesTheTextAsTyped`; witness F2 fixed | review, witness F2 |
| T2.2 | F2: `NumberField` cleaning rewrote the typed text | fixed: as-typed mode | `sheetJudgesTheTextAsTypedAndKeepsTheSavedValue` | review |
| T2.3 | F2: blur and an outside value rewrote the text | fixed: skipped in as-typed mode | `sheetJudgesTheTextAsTypedAndKeepsTheSavedValue` | fixer |
| T3.1 | F3: `scoreRows` buckets (hard time, shares, rounds) | fixed: `scoreBuckets` | `oneBucketPerOffsetTheFirstInTimeOrder`, `aRowWithARepeatedOffsetCountsTheBucketOnce`; witness F3 fixed | review, witness F3 |
| T3.2 | F3: `scoreCharts` buckets | fixed: `scoreBuckets` | `oneBucketPerOffsetTheFirstInTimeOrder` | fixer |
| T3.3 | F3: `validFileHr` accepted 0,0,5 / 0,3 / 10,5 | fixed: strictly rising whole buckets | `validFileHr takes only strictly rising offsets that are whole buckets`, `rows with a repeated, an unaligned or a falling offset are dropped in Replace and Merge and every session stays` | review |
| T4.1 | F4: hard chart tick below 0 | fixed: `valueAxis` with domain 0 to infinity | `chartTicksStayInsideThePossibleValues` | review, witness F4 |
| T4.2 | F4: zone chart tick below 0 | fixed: domain 0 to 100 | `chartTicksStayInsideThePossibleValues`; witness test `zeroZoneShareHasNoNegativeTick` passes | review, witness F4 |
| T4.3 | F4: zone chart tick above 100 | fixed: domain 0 to 100 | `chartTicksStayInsideThePossibleValues` | review, witness F4 |
| T4.4 | F4: the witness itself | blocked: its pixel matcher reads the exact labels "0:00" and "100" as "-1:00" and "101" | none | fixer probe |
| T5.1 | F5: a round inside another round | fixed: no figures, round before has no drop | `aRoundInsideAnotherHasNoFiguresAndTheRoundBeforeHasNoDrop`; witness F5 fixed | review, witness F5 |
| T5.2 | F5: a round that starts before the round before ends | fixed: no figures | `aRoundThatStartsBeforeTheRoundBeforeEndsHasNoFigures` | fixer |
| T5.3 | F5: a round that starts at the end of the round before | fixed: no figures | `aRoundThatStartsBeforeTheRoundBeforeEndsHasNoFigures` | fixer |
| T5.4 | F5: a rest that holds another round's work | fixed: no drop | `aRoundThatStartsBeforeTheRoundBeforeEndsHasNoFigures` | fixer |
| T6.1 | F6: MaxHeartRate.kt 2 KDocs, ScoreRules.kt 1, ScoreFixtures.kt 1 | fixed: removed | `comment-guard --base 2248c44` | review |
| T7.1 | F7: 411 dp at 1.0 with 10 rounds | fixed: test added | `tenRoundsFitAt411Scale1` | review |
| T7.2 | F7: screenshots with 10 rounds | fixed: `review-hiit10-*` at four sizes | `ScoreShotsTest` | review |
| T8.1 | S1-S13 of the first task | kept: changed assertions listed below | Tests command, 899 tests 0 failures | fixer |
| T9.1 | `data` package limits | kept: only `validFileHr`, its tests, and the settings store | `git diff 2248c44 --stat -- android` | fixer |

`measuredMaxHr` reads the stored `maxHr` and not the buckets, so a repeated offset cannot change it. It needs no change for F3.

Witness results on 568d00a: F1 fixed, F2 fixed, F3 fixed, F4 reproduces (blocked, see T4), F5 fixed. Same results on 3af2e68.

## Changed assertions

- `ScoreRulesTest.roundPeak`: three `emptyList()` results are now `RoundScore(n, null, null)` lists, because `roundScores` returns one entry per round (the F5 witness reads round 2 by number). `roundRow` gives no row for them, so the screens are unchanged.
- `ScoreRulesTest.sheetEntryRefusesValuesOutsideTheRange` is now `sheetEntryJudgesTheTextAsTyped`: `maxEntry` takes the text. 99, 231 and a fraction stay refused.
- `ScoreScreenTest.sheetRefusesBadValuesAndClears`: the check that "abc" is cleaned to "" is gone, because the field now keeps the typed text (lead decision 2).
- `BackupHrTest`: "2395:30,0:230" is now "0:230,2395:30" (falling offsets are refused); "0:130,2399:130" is now "0:130,2395:130" (2399 is not a whole bucket); "0:130,2401:130" is now "0:130,2405:130", so that case still tests the length bound.

## Deviations

- blocked: witness F4. Exact error: `ScoreChartAxisWitnessTest > fullZoneShareHasNoOver100Tick FAILED java.lang.AssertionError at ScoreChartAxisWitnessTest.kt:127` and the same for `zeroHardTimeHasNoNegativeTick`. The matcher passes a forbidden label when 80 % of its gray pixels match. A lone "0:00", "1:00" or "2:00" label at 4 dp or more from the chart edge matches "-1:00". A lone "100" matches "101" at every position. The first point label sits at the plot edge (about 38 dp) in the left quarter, and it must stay exact ("0:00", "100"). So no fix that keeps exact labels can pass. The witness needs a check of the tick values, not of the pixels.
- deviation: F5 adds one rule beyond the lead decision. A round whose rest holds another round's work gets no drop. Without it, a later round placed inside an earlier rest still counts as rest.
- deviation: F2 refuses a leading zero ("0150") and a plus sign. Saving 150 from "0150" would change what was typed.
- deviation: two shared components got optional parameters: `NumberField(onTextChange)` and `LineChart(domain)`. The tick math moved to the pure `valueAxis`. Other callers pass neither, and their ticks do not change (test: no domain gives -1, 0, 1).
- deviation: F3(a) adds `scoreBuckets` on top of `pictureBuckets` and does not change it. Changing `pictureBuckets` would change the Review chart, which this task must not change.
- F1 follows the decision: a refresh on resume and on input change, with no timer while the screen stays open.
- The KDoc of `validFileHr` now names the new rule.

## Tests

`JAVA_HOME=".../jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease` on 568d00a:
exit 0, `BUILD SUCCESSFUL in 1m 58s`, 94 test classes, 899 tests, 0 failures, 0 errors, 35 skipped. All the skips are screenshot tests whose folder did not exist.

## Screenshots

`android/app/build/shots-score/` holds 32 PNGs. `review-hiit10-{360,411}dp-x{1_0,1_3}.png` are new. I viewed all four, plus `type-run-360dp-x1_0` and `settings-sheet-360dp-x1_3`.
