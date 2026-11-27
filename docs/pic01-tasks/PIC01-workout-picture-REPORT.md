# PIC01-workout-picture report

Branch `pic01`, commit `210b154` (on base `2dbca2f`). No push, no merge.

Shared test command prefix: `JAVA_HOME=".../jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest`.
Final run of the `Tests:` line on `210b154`: `BUILD SUCCESSFUL in 1m 59s`, 781 tests, 0 failed, 21 skipped
(the skipped ones are other shot tests whose folders do not exist).

| # | Status | Check ran | Real output |
|---|---|---|---|
| H1 | done: `curveSegments` cuts gaps > 60 s; a one-bucket segment draws as a 4 dp dot | `--tests '*PictureRulesTest*'` | `curve segments join gaps of 60 s and cut gaps of more, a lone bucket is a dot ok` (sizes [30]; 1 segment at 60 s; [1,1] at 65 s; [20,1,20]) |
| H2 | done: `workoutPicture` uses only `pictureBuckets(hrFacts)`; waiting, none, off, stale range, no row give no curve, no axis, no chart; no range and unfinished give null | `--tests '*PictureRulesTest*'` | `no curve and no chart without a filled fact of the current range ok` |
| H3 | done: `setMarks` keeps done sets with `checkedAt` in `[from, to)`, sorted by time | `--tests '*PictureRulesTest*'` | `marks are the stamped done sets inside the range in time order ok` (ids [first, b, a]; marks [0, 10 min, 20 min]) |
| H4 | done: `setMarkRows`: clock, `minSec` since, highest bucket in `[prev, check)` with 2+ buckets; same rows with no bpm without a fact; one mark gives no rows | `--tests '*PictureRulesTest*'` | `set rows give the clock, the time since the mark before, and the highest bpm of the span ok` (clocks 18:10/18:12/18:15, since [null, 2:05, 2:55], sparse bpm [null, 151, null]) |
| H5 | deviation: "both inside the range" read as "start inside the range, end cut at the range end", because the contract cuts a round that ends after the range | `--tests '*PictureRulesTest*'` | `round bands start inside the range and are cut at its end ok` (4 bands + long one cut to range end; no-times and start=end give none) |
| H6 | deviation: steps 10 or 20 with 3 to 5 lines; when the span from the floor-10 tick is over 80 bpm no such grid exists, so the step grows to 30/40/50/100 | `--tests '*PictureRulesTest*'` | `the bpm axis has 3 to 5 lines in steps of 10 or 20 over every bucket ok` (92..164 gives 90..170 step 20; flat 120 gives 110..130; one bucket no crash) |
| H7 | done: pixel tests find the plot from the bottom grid line, then check accent pixels, tick runs at each mark column (±2.5 px) and no other, band runs at each round | `--tests '*PictureScreenTest*'`; deliberate break: `if (false)` before the tick loop, then before the curve loop in `ReviewPicture.kt`, file restored after | `the gym chart draws the curve and a tick at each set mark only ok`, `the cardio chart draws the curve over a band at each round ok`; ticks removed: `FAIL java.lang.AssertionError: ticks`; curve removed: `FAIL java.lang.AssertionError: curve` |
| H8 | done: Review gets 3 added lines; chart under the header figures, "Sets in order" after exercise groups and before Notes | `--tests '*PictureScreenTest*'` | 4 tests ok: filled gym shows chart + group (17 "since" rows, none clickable); stamps without heart rate group only, no "bpm"; cardio chart only; no range and unfinished show neither; existing Review texts present; `TodayScreenTest` 23/23 and `TapTargetTest` 10/10 pass |
| H9 | done: the screen collects `repo.sessionHr(id)`, so a commit and Remove update it in place | `--tests '*PictureScreenTest*'` (real `HeartFill` from `repo.heartFill(FakeHeart, scope)`) | `the chart appears when the fill commits and goes on Remove, without leaving Review ok` (`rereadHeartRate` = FILLED, chart appears; `removeHeartRate` = true, chart and "bpm" go, group stays) |
| H10 | deviation: each row is two lines (name with clock on the right, set text with time since and bpm on the right) instead of one right block of three values; at 360 dp x1.3 the one-line form cut names to 4 letters and wrapped the set text | `--tests '*PictureScreenTest*'` at 360 dp x1.0, 360 dp x1.3, 411 dp x1.3 with 30 sets and a 40-character name; shots viewed | 3 layout tests ok: 30 rows, no wrapped or clipped value, name before clock, set text before values, no overlap, long name ellipsized, last row scrolls into view; `ContrastTest` 11/11 and `TapTargetTest` 10/10 pass |
| H11 | done: chart: "Heart rate from 18:02 to 19:14, lowest 92 beats per minute, highest 164 beats per minute, 18 sets marked"; row: "Barbell Bench Press, 62.5 kilograms, 9 reps, checked at 18:09, 3 minutes 50 seconds since the set before, highest N beats per minute" | `--tests '*PictureRulesTest*'`, `--tests '*PictureScreenTest*'` | `the chart and a set row speak in full unit words ok`; screen test reads the same chart text from the node |
| H12 | done: muted and accent only, no verdict words, no new entity, DAO write, backup, permission, dependency, or version change; SPEC.md and DESIGN.md updated | `git diff --stat 2dbca2f..210b154`; `comment-guard --base 2dbca2f` | 9 files: DESIGN.md, SPEC.md, ReviewScreen.kt (+3), PictureRules.kt, ReviewPicture.kt (new), 4 new test files; guard exit 0 |

## Notes

- Repository read used: the existing `repo.sessionHr(id)`. No new read added.
- Chart height is 190 dp, not 180: DESIGN.md says every chart is 190; the task said "about 180".
- New rule tests do not compile on the base: `PictureRules.kt` does not exist in `2dbca2f` (`git cat-file -e` fails).
- The trunk was not merged into the branch: the lead said "do not merge".

## Screenshots

`android/app/build/shots-picture/review-{gym,cardio,plain}-{360,411}dp-x{1_0,1_3}.png` (12 files, made by
`PictureShotsTest` on the final run; all looked at, crops for the chart and the group at 360 x1.0, 360 x1.3, 411 x1.0, 411 x1.3).
