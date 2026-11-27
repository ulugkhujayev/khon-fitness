# SC01-cardio-scores report

Branch `sc01`, base 6139309. Commits: 493b53b (code and tests), b2c741a (screenshot test), f755a9e (SPEC.md, DESIGN.md).

| # | Status | Check ran | Real output |
|---|---|---|---|
| S1 | done: setting in the settings DataStore (`data/MaxHeartRate.kt`, key `max_heart_rate`); `measuredMaxHr`, `maxInUse`, `maxEntry`; Settings row and sheet (`MaxHeartSheet.kt`); Save off for 99, 231; letters never reach the field | `gradlew :app:testDebugUnitTest --tests '*ScoreRulesTest' --tests '*MaxHeartRateTest' --tests '*ScoreScreenTest'` (tests `maximumInUse`, `measuredMaximumOfTheLast365Days`, `sheetEntryRefusesValuesOutsideTheRange`, `storesClearsAndRefuses`, `sheetRefusesBadValuesAndClears`, `settingsRowShowsTypedMeasuredOrNotSet`) | ScoreRulesTest 14 0 0 0; MaxHeartRateTest 2 0 0 0; ScoreScreenTest 12 0 0 0 (tests failures errors skipped) |
| S2 | done: `hardLine` = ceil(85 %) in integer math; `hardSeconds` | same, `hardLineIsTheCeilingOf85Percent` | 159 for 186 and 187; only the 159 bucket counts; 139 buckets give "11:35"; pass |
| S3 | done: `hrZone`, `zoneShares` | same, `zoneFrom60To70Percent` | HrZone(112, 130); ZoneShares(25, 50, 25); pass |
| S4 | done: `percentShares`, largest remainder, tie order below, inside, above (documented in SPEC.md) | same, `sharesSumTo100` | 34, 33, 33; every count 0..7 sums to 100; 0 buckets: null shares, no rows; pass |
| S5 | done: `roundScores` peak | same, `roundPeak` | 48 buckets: peak 168; 1 bucket, no times, start before range: no row; pass |
| S6 | done: `roundScores` drop | same, `roundDrop`, `dropOfTheLastRoundRunsToTheRangeEnd` | 42; 1 rest bucket: null; last round, range end 40 s on: 38 from the tail buckets; -4 shown as "−4"; pass |
| S7 | done: all figures go through `hrFacts` and `pictureBuckets` | same, `noFiguresWithoutAFilledFactOfAFinishedCardioSession` | waiting, none, not read, removed, stale, unfinished, gym: no rows, no chart points; pass |
| S8 | done: `ReviewScores` after the cardio figures and rounds in `ReviewScreen.kt` (1 added line) | `ScoreScreenTest` `highIntensityWithFourRounds`, `lowIntensitySteady`, `highIntensityWithoutRounds`, `noMaximumShowsNothing`; `ScoreRulesTest.reviewRowsByIntensity`; full suite | hard row + Round 1..4; 3 zone rows; hard row only; nothing; PictureScreenTest 10 0 0 0, full suite 887 tests 0 failures |
| S9 | done: rows read the typed flow, the session row flow and all rows | `ScoreScreenTest.rowsFollowTheTypedMaximumAndTheFill` | waiting row: no group; fill commits: "At or above 159 bpm"; typed 200: "At or above 170 bpm"; typed cleared: measured line; pass |
| S10 | done: `ScoreChartsView` in `CardioTypeScreen` (1 added line), `scoreCharts` | `ScoreRulesTest.typeChartPointsInDateOrder`; `ScoreScreenTest.typeScreenChartDrawsTheLine`, `typeScreenWithOneFilledSessionHasNoChart`; the pixel test again with the `drawPath(line, ...)` call of `LineChart.kt` removed, then restored with `git checkout` | 3 points in date order; 1 filled: no chart; pass. Line removed: `FAIL typeScreenChartDrawsTheLine : java.lang.AssertionError: accent in 79 of 984 columns` |
| S11 | done: rows with weight label and wrap value; value lost `TextAlign.End` after the overflow check caught it | `ScoreScreenTest.tenRoundsFitAtScale1`, `tenRoundsFitAtScale13`, `tenRoundsFitAt411Scale13`; `ScoreShotsTest` at 360 and 411 dp, 1.0 and 1.3 (28 PNGs, each viewed); TapTargetTest, ContrastTest | 11 rows, no overflow, no overlap; TapTargetTest 11 0 0 0; ContrastTest 11 0 0 0 |
| S12 | done: spoken texts per row; Settings row "Maximum heart rate, 186 beats per minute" / "measured 183 ..." / "not set" | `ScoreRulesTest.spokenTextsUseFullWords`, `ScoreScreenTest` speech asserts | "At or above 159 beats per minute, 11 minutes 35 seconds"; "112 to 130 beats per minute, 50 percent"; "Round 1, peak 168, drop 41 beats per minute"; pass |
| S13 | done: facts only, no colors per value, no `mark`ed chart point; no Room, DAO write, permission, dependency or version change; SPEC.md and DESIGN.md updated | `~/.config/agents/tools/comment-guard --base 6139309`; `git diff 6139309 --stat`; Tests command | comment-guard exit 0; 0 added `//` lines in main; changed existing files: KhonApp.kt +2, AppViewModel.kt +2, ReviewScreen.kt +1, ExerciseDetail.kt +1, PlanScreen.kt +5 -2 |

## Tests

`JAVA_HOME=".../jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease` on f755a9e:
exit 0, `BUILD SUCCESSFUL in 2m 35s`, 94 test classes, 887 tests, 0 failures, 0 errors, 33 skipped (skips are the shot tests whose folder does not exist).

New tests on the base: `ScoreRules.kt`, `ReviewScores.kt`, `MaxHeartSheet.kt`, `MaxHeartRate.kt` do not exist at 6139309 (`git cat-file -e` says "exists on disk, but not in '6139309'"), so every new test does not compile there.

## Deviations

- deviation: no number-entry sheet exists to reuse. The sheet is the existing `Sheet` with one `NumberField` and a `PrimaryButton` Save, which is off for a value outside 100 to 230. The field does not clamp, so a typed 99 is refused, not saved as 100.
- deviation: the settings DataStore is outside the backup, so the backup does not carry the setting. Reset to built-in data clears it (`PlanScreen.kt`, one call), because its confirm text says every setting goes back.
- deviation: the task names "the screen that lists the sessions of one cardio exercise". The app has `CardioTypeScreen`, one cardio type (modality). It shows "At or above 85 %" for the sessions of high-intensity exercises of that type and "In zone" for low-intensity ones, so a type with both gets two charts.
- deviation: moderate intensity, or an exercise that no longer exists, shows no group and no chart point. The task names only high and low.
- deviation: the rest of a round ends at the next round's `startedAt` by index; when that next round has no `startedAt`, the round has no drop. No value is made up.
- deviation: a negative drop shows with a minus sign ("−4") and speaks "minus 4".
- deviation: I did not merge the trunk into `sc01` before the final tests. The lead said "do not merge".

## Screenshots

`android/app/build/shots-score/` (28 PNG, 1080 or 1233 px wide, 9600 px tall), names `<screen>-<width>dp-x<scale>.png`: `review-hiit`, `review-z2`, `type-bike`, `type-run`, `settings`, `settings-measured`, `settings-sheet`, each at 360dp and 411dp, x1_0 and x1_3. Make them again with `mkdir -p android/app/build/shots-score` and the `ScoreShotsTest` run.
