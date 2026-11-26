# CMP01-fix1 report

Fix commit: `14a949d` on branch `cmp01` (base of the fix `6025dc8`). No push, no merge.
The work tree also has `docs/cmp01-tasks/CMP01-compare-periods-ROUNDS.tsv` changed; another agent changed it, this task did not touch or commit it.

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: `CompareScreen` reads `vm.sessions`, `vm.allSetLogs`, `vm.stretchSessions`, `vm.bodyweights` directly instead of `rememberHistoryData`; History callers unchanged | `witness run F1.json --sha 14a949d`; `gradlew :app:testDebugUnitTest --tests '*CompareScreenTest'`; break: put `rememberHistoryData` back | `WITNESS F1 fixed at=14a949de6e52 receipt=r-20261126200335-9b4aca`; break run: `a custom period counts every record of its days after today FAILED`, `a program period reaches its last finished session, also after today FAILED`, `9 tests completed, 2 failed` |
| X2 | done: new private `PeriodRow` with a `RangeThenName` layout (range takes its full width first, name gets the rest, one line with ellipsis); new `StackedValues` uses `Arrangement.spacedBy(CELL_GAP)` between two `weight(1f)` cells | `witness run F2.json --sha 14a949d`; breaks: old ListRow layout, old right-cell padding | `WITNESS F2 fixed at=14a949de6e52 receipt=r-20261126200354-679c10`; breaks: `a long program name leaves the whole date range on the period row FAILED` (1 of 9); `the stacked values take two cells of the same width FAILED` (1 of 9) |
| X3 | done: `muscleVolume` is the base loop again; the base and `muscleSets` share only `forEachMuscleShare` (1 primary, 0.5 secondary) | `witness run F3.json --sha 14a949d`; break: `git show d9fb265:.../HistoryScreen.kt` | `WITNESS F3 fixed at=14a949de6e52 receipt=r-20261126200413-36b107`; break: 3 `ProgressBaseResultsTest` weekly tests FAILED, `49 tests completed, 3 failed` |
| X4 | done: `cardioTypes` uses the base lower bound `date >= today - days` again, and shares `cardioType` aggregation; `Period` keeps `require(from <= to)` | `witness run F4.json --sha 14a949d`; break: the `Period(today - days, today)` span back | `WITNESS F4 fixed at=14a949de6e52 receipt=r-20261126200425-fc8feb`; break: 2 `ProgressBaseResultsTest` cardio tests FAILED, `35 tests completed, 2 failed` |
| X5 | done: removed 16 comments that repeat code or tests and cut one to its run condition; kept the `cellText` text-wrapping comment | `~/.config/agents/tools/comment-guard --base 9d1ba17` | `comment-guard exit=0` (no output) |
| X6 | done: C8 adds a time-only run `r4` and asserts per-type sessions, seconds, meters and pace for run, and seconds, meters, pace for the time-only erg type | break: `cardioTypesIn` returns `.copy(seconds = 0)`; then the branch run | break: `C8 cardio per type with the existing pace, a row of two dashes left out FAILED`, `14 tests completed, 1 failed`; branch: passes in the full run below |
| X7 | done: only the six fixes; no Room, DAO, repository, backup, version or manifest change | `JAVA_HOME=... android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease` on `14a949d` | `BUILD SUCCESSFUL in 1m 36s`, gradle exit 0; JUnit XML: `tests 707 failed 0 skipped 26` |

Witness note: each receipt log shows `> Task :app:testDebugUnitTest` executed (not up to date) and `BUILD SUCCESSFUL`; Gradle stops with "No tests found" when a `--tests` filter matches nothing, so each witness test ran.

Robolectric note: the default graphics mode measures text as almost zero width. The two new layout tests use `@GraphicsMode(NATIVE)`. The F2 witness runs in the default mode; `RangeThenName` gives the range a fixed width, so it is correct in both modes.

## Coverage

| # | Instance | Status | Test | Found by |
|---|---|---|---|---|
| X1.1 | `CompareScreen.kt` sessions input (period resolve, `programChoices`, `periodStats`) | fixed: `vm.sessions` unfiltered | `CompareScreenTest.a program period reaches its last finished session, also after today`; witness F1 | grep `data\.` in `CompareScreen.kt` |
| X1.2 | `CompareScreen.kt` set logs input (`periodStats` sets, lifted, muscles, best sets) | fixed: `vm.allSetLogs` unfiltered | `CompareScreenTest.a custom period counts every record of its days after today` (Sets, Bench press, Chest rows) | grep `data\.` |
| X1.3 | `CompareScreen.kt` stretch sessions input | fixed: `vm.stretchSessions` unfiltered | same custom-period test (Stretching row) | grep `data\.` |
| X1.4 | `CompareScreen.kt` bodyweights input | fixed: `vm.bodyweights` unfiltered | same custom-period test (Average row) | grep `data\.` |
| X1.5 | `CompareScreen.kt` exercises, modalities, programs inputs | kept: read from `vm` flows, never through `historyData` | `CompareScreenTest` (all 9 pass) | caller trace of `historyData` |
| X1.6 | History callers of `rememberHistoryData` (`HistoryLog.kt:55`, `HistoryProgress.kt:54`, `ExerciseDetail.kt:39,69,127`) | kept: unchanged, filter up to today | `HistoryRulesTest.ranges stop at today - later sessions and weights count nowhere`, `HistoryScreenTest` | grep `rememberHistoryData` |
| X2.1 | `CompareScreen.kt` left period row | fixed: `PeriodRow` / `RangeThenName` | `CompareScreenTest.a long program name leaves the whole date range on the period row`; witness F2 | review F2 |
| X2.2 | `CompareScreen.kt` right period row | fixed: same `PeriodRow` | same test (right column) | review F2 |
| X2.3 | `CompareScreen.kt` stacked value cells | fixed: `StackedValues` with `spacedBy(CELL_GAP)` | `CompareScreenTest.the stacked values take two cells of the same width` | review F2 |
| X2.4 | `CompareScreen.kt` side-by-side value cells | kept: both cells have `padding(start = CELL_GAP).width(column)`, equal by construction | `CompareScreenTest` (all pass) | read of `FactRow` |
| X3.1 | `HistoryScreen.kt` `muscleVolume` session choice for a repeated id | fixed: base `associate` of finished sessions | `ProgressBaseResultsTest.weekly muscles count a repeated session id once, by its last finished snapshot`; witness F3 | review F3 |
| X3.2 | `HistoryScreen.kt` `muscleVolume` row order for equal totals | fixed: base insertion in log order | `ProgressBaseResultsTest.weekly muscles keep the base order for tied rows`, `...equal the base result for generated inputs` (300 cases vs a copy of the base function) | review F3 |
| X3.3 | `CompareRules.kt` `muscleSets` | kept: uses the shared `forEachMuscleShare` | `CompareRulesTest` C6 and the weekly comparison test | grep `secondaryList` |
| X4.1 | `HistoryRules.kt` `cardioTypes` with `days < 0` | fixed: base lower bound, no `Period` | `ProgressBaseResultsTest.the cardio range with a negative days value keeps every historical type with zero totals`; witness F4 | review F4 |
| X4.2 | `HistoryRules.kt` `cardioTypes` with a type that has only older sessions | kept: base result | same test (swim at -40 days), `...equals the base result for generated inputs` (300 cases, days -10 to 59) | review F4 |
| X4.3 | `CompareRules.kt` `Period` `from <= to` rule | kept: `require` unchanged | `CompareRulesTest` (all pass) | task scope |
| X5.1 | `CompareScreen.kt` screen KDoc | fixed: removed | comment-guard | review F5 |
| X5.2 | `CompareScreen.kt` `FactGroups` layout KDoc | fixed: removed | comment-guard | review F5 |
| X5.3 | `CompareScreen.kt` `PeriodSheet` KDoc | fixed: removed | comment-guard | review F5 |
| X5.4 | `CompareScreen.kt` `TwoLineChoiceRow` KDoc | fixed: removed | comment-guard | review F5 |
| X5.5 | `CompareScreen.kt` `cellText` text-wrapping comment | kept: explains a line-breaking quirk | comment-guard | task scope |
| X5.6 | `CompareRules.kt` KDoc of `countsIn`, `programChoices`, `PeriodChoice`, `choiceKey`, `PeriodStats`, `NO_VALUE`, `CompareRow`, `CompareGroup`, `muscleSets` | fixed: removed (9) | comment-guard | grep of added `/**` in `git diff 9d1ba17` |
| X5.7 | `CompareScreenTest.kt` class summary | fixed: removed | comment-guard | review F5 |
| X5.8 | `CompareShotsTest.kt` `shot` and `seed` KDoc | fixed: removed | comment-guard | review F5 |
| X5.9 | `CompareShotsTest.kt` class KDoc | fixed: cut to the run condition (`app/build/shots-compare`) | comment-guard | grep of added `/**` |
| X6.1 | `CompareRulesTest.kt` C8 | fixed: per-type sessions, seconds, meters, pace asserts | `CompareRulesTest.C8 cardio per type with the existing pace, a row of two dashes left out` (break proof above) | review F6 |
