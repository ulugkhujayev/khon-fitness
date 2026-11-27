# CMP01-compare-periods report

Branch `cmp01`, base 9d1ba17. Commits: f46b468 (rules), 53fcf77 (screen), 2cd6c42 (SPEC and DESIGN), plus this report.
`origin/main` had no commits past the base (`git fetch origin; git log HEAD..origin/main` printed nothing), so no trunk merge was needed.

| # | Status | Check ran | Real output |
|---|---|---|---|
| C1 | done: `Period`, `Session.countsIn`; a session on `from` and `to` counts, one a day outside or unfinished does not | `gradlew :app:testDebugUnitTest --tests '*CompareRulesTest'` | `CompareRulesTest tests="14" skipped="0" failures="0" errors="0"` (test `C1 ...`) |
| C2 | done: `Preset.period(today)`; values for 2026-11-27 as in the contract; neighbors meet day to day | same | test `C2 presets from Nov 27, 2026, ...` passes |
| C3 | done: `periodStats`; counted sets use `countsAsDone`; swim meters = laps x pool length via `meters()` | same | test `C3 ...` passes: 2 gym, 3 sets, 1355 kg ("1.4 t"), 2 cardio, 3300 s ("55 m"), 6000 m ("6.0 km"), 1 stretch |
| C4 | done: `setsPerWeek` = sets / (days / 7), one decimal | same | test `C4 ...` passes: "3.5" and "9.8" |
| C5 | done: mean and count of the period's entries; "–" and "0" without entries; group absent when both periods have none | same | test `C5 ...` passes |
| C6 | done: new `muscleSets(period, ...)`; `muscleVolume` now calls it for this and last week | same + `--tests '*HistoryRulesTest' '*VolumeSharesTest'` | test `C6 ...` passes (equals `muscleVolume(...).lastWeek`; two weeks = half the sum); `HistoryRulesTest tests="30" failures="0"` |
| C7 | done: `bestSet` over the period's sets per exercise; "–" for the other period; uncounted-only exercise absent | same | test `C7 ...` passes |
| C8 | done: new `cardioTypesIn(period, ...)`; `cardioTypes` and it share one `cardioType` builder; pace is `pace()` | same | test `C8 ...` passes; a type with no distance shows only its sessions row |
| C9 | done: `programPeriod`, `programChoices`; a program without a finished session is not in the sheet | rules test + `CompareScreenTest` | test `C9 ...` passes; screen test: sheet shows "Push day / Oct 10 – Nov 20", not "Legs" |
| C10 | done: `withFrom` / `withTo`; screen test picks Oct 31 as From in the Material picker over To Oct 30 | `--tests '*CompareScreenTest'` | `CompareScreenTest tests="5" failures="0"`; after the pick the row speaks "Days, 1 in Oct 31, 28 in Oct 31 – Nov 27" |
| C11 | done: entry row on Progress, route `compare`, sheet, groups, empty database | `--tests '*CompareScreenTest'` | 5/5 pass: entry opens `compare`; spoken rows match; empty DB shows only the first group, no titled group |
| C12 | done: columns = widest value (max 30 % of the row each); labels wrap 2 lines then ellipsis; rows stack only when an unbreakable value part does not fit | `--tests '*TapTargetTest'`, `ContrastTest` (full suite), `CompareShotsTest` (360/411 dp x font 1.0/1.3) | `TapTargetTest tests="11" failures="0"` with the new `the compare screen and its period sheet have 48 dp targets`; shots viewed, no clipped value (see UI check below) |
| C13 | done: no verdict words or colors; one text color for both columns; no Room, DAO, repository, backup, permission, dependency or version change; SPEC.md and DESIGN.md updated | `git diff 9d1ba17 --name-only \| grep -iE "Dao\|Repository\|Entities\|Database\|Backup\|build.gradle\|Manifest\|libs.versions"`; rules test `C13 no verdict words` | grep printed nothing; test passes; `HistoryRulesTest` 30/30 and `VolumeSharesTest` pass unchanged |

## Base proof

The new rules test does not compile on the base. Ran in a temporary detached worktree at 9d1ba17 (removed after):
`gradlew :app:compileDebugUnitTestKotlin` gave `e: .../CompareRulesTest.kt:42:26 Unresolved reference 'Period'.` and `Unresolved reference 'periodStats'`.

## Full test command (final commit 2cd6c42)

`JAVA_HOME=... android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease` ran with exit 0: `BUILD SUCCESSFUL in 2m 21s`.
The XML results add up to 74 classes, 698 tests, 26 skipped, 0 failures and 0 errors. The skipped tests are the screenshot tests, which run only when their folder exists.
`comment-guard --base 9d1ba17` ran with exit 0.

## UI check

The task header has no `UI: yes`, so I made no ui-proof sheet. I looked at these Robolectric captures from `CompareShotsTest`:
before: `before-progress.png` (Progress before the change); after: `w360|w411 x f1.0|f1.3` for `1-progress-entry`, `2-compare` and `3-sheet-custom`, plus `w360-f1.0-4-empty`.
All files are in `/private/tmp/claude-501/-Users-mirzohidkhon-deepvision-personal/84f11edb-c39d-4431-b8db-64992a6d9ee0/scratchpad/cmp01/`.

## Deviations and decisions

- deviation: decision: "Cardio time" uses the hour text from the Progress cardio line ("10 h 6 m"), now `durationText()`. It is not the per-session "58 min".
- deviation: decision: values that the task gives "one decimal" always show one decimal ("10.0", "6.0 km", "83.0 kg"), so the columns line up.
- deviation: decision: sets per muscle keeps the existing rule (status done, a 0-rep done set counts). "Sets" uses `countsAsDone` as the task says.
- deviation: decision: muscle rows follow `MUSCLE_GROUPS` order. Cardio types follow the modality list order, as Progress does.
- deviation: decision: a To before the From moves the From to the same day, as the mirror of the From rule.
- deviation: decision: the spoken text reads "none" for "–". The top rows are `ListRow`s: the range, with the choice name in muted text.
- deviation: decision: `DateRow` has a new `label` parameter (default "Date") for the From and To rows. `ProgressTitle` is now `internal`, so the screen can reuse the section title.
- deviation: decision: the program row in the sheet (name over its range, with a check) is a small private row. `ChoiceRow` has only one line.
- deviation: decision: when one value part does not fit the 30 % column, all rows put the label on its own line over the two columns. In the captures this happens at 360 dp with font 1.3.
- Not checked: a real device or emulator run. The captures come from Robolectric native graphics.
