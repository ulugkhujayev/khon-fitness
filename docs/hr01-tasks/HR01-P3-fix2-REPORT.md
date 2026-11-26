# HR01-P3-fix2 report

Commits on `hr01-p3` (task base `e375cbd`, branch head before this task `65ce408`): `e62d21a` (code and tests), `325cf64` (SPEC.md, DESIGN.md, failure-line screenshot), plus the commit that adds this report. No push, no merge.

Focused command (F): `JAVA_HOME=... android/gradlew -p android :app:testDebugUnitTest --tests '*HeartRulesUiTest' --tests '*HeartDataScreenTest' --rerun`, result on `e62d21a`: `HeartRulesUiTest tests="10" failures="0"`, `HeartDataScreenTest tests="25" failures="0"` (2 runs in a row).

Tests command on `325cf64`: exit 0, `BUILD SUCCESSFUL in 1m 55s`, `71 actionable tasks: 9 executed, 62 up-to-date`; JUnit XML: `tests=810 failures=0 errors=0 skipped=21`.

## Design

`HeartDataScreen` keeps only `open`, `sheetNo` and `confirmRemove` (now `rememberSaveable`, so the sheet survives a rotation). The sheet is a private composable `HeartSheet` under `key(sheetNo)`, composed only while the sheet is open and the Remove confirmation is not. It holds:

- `reading`, `confirming`: plain `remember`.
- `asked` (the current request token): `remember(range, row)`.
- `failedAt` (hash of range and row at the tap): `rememberSaveable(range, row)`; the line shows only while `failedAt == hash(range, row of now)`.
- `rememberCoroutineScope()`: "Read again" runs `heart.rereadHeartRate(id)` there. Close, Remove tap, delete, and leaving the screen take the sheet out of the composition and cancel the read.

A completion writes `failedAt` only for `HrReread.FAILED` and only when `asked === request`. Any change of range or row makes new state holders (also when it changes back). "Read again" and "Use this time range" set `asked = null`, `failedAt = null`. `HrReadKey`, `HrReadResult`, `readResult`, `after`, `rereadLine` and the texts "No data yet" and the values line are gone. `hrSheet(..., failed: Boolean, ...)`.

Charts: `cardioHrCharts(...)` in `HeartRules.kt` returns the list of (title, points); `ExerciseDetail` draws each pair in a loop.

## Break proof (each break put in the tree, F run, files restored with `git checkout`)

| Break | Failing tests |
|---|---|
| B1: "Read again" in `vm.run` instead of the sheet scope | `an old request after leaving the screen and a range change shows nothing, the new request shows its own outcome` |
| B2: `remember`/`rememberSaveable` without `(range, row)` keys | `a late failure after a range change and a change back shows nothing` |
| B2b: B2 plus `failed = failedAt != null` (no hash check) | change back; `an automatic pass that replaces the row as the explicit read returns leaves no line`; `a late failure after a new row of the same state`; `a late failure after a range change` |
| B4: the sheet stays composed under the Remove confirmation (no `return`) | `a late failure after Remove tapped and cancelled`; `a late failure after Remove confirmed before Room publishes the off row`; `a late failure after Remove`; `a late result after Remove or after a range change...`; `Remove shows Removed everywhere...` (B4 also puts two sheets on screen, so part of these failures may come from duplicate nodes) |
| B4 + B1 + no `key(sheetNo)` | the 5 above plus the leave-the-screen test |
| B5: `open`, `sheetNo`, `failedAt` not saveable | `a failure stays after a recomposition and a rotation` |
| B6: line for every outcome but DROPPED | none fails: every committed outcome writes a new row (new `fetchedAt`), so the row key resets the line. The FAILED filter is the only guard against a short flash before Room publishes the row; no test sees that frame |
| B7: chart series swapped in `cardioHrCharts` | `HeartRulesUiTest > typed and measured values reach both charts of a cardio type` |

## Coverage

Witness F1: base of this task `e375cbd` prints `fixed` (lead record); `e62d21a`: `WITNESS F1 fixed at=e62d21a7b59b receipt=r-20261126230348-ecdbf6`; head `325cf64`: `WITNESS F1 fixed at=325cf645d817 receipt=r-20261126230757-2dec8c`.

| ID | Instance | Status | Test | Found by |
|---|---|---|---|---|
| U1.1 | committed row replaced by an automatic pass before the lookup | fixed: no lookup, no line for committed outcomes | `an automatic pass that replaces the row as the explicit read returns leaves no line` | verify F1 (V1.6) |
| U3.1 | range change and back before the row appears | fixed: holders keyed by range and row | `a late failure after a range change and a change back shows nothing` (B2) | verify V1.2 |
| U3.2 | Remove confirmed, failure before Room publishes `off` | fixed: Remove tap disposes the sheet and cancels the read | `a late failure after Remove confirmed before Room publishes the off row shows nothing` (B4) | verify V1.1 |
| U3.3 | leave, change range, return, tap before the held read ends | fixed: read cancelled with the sheet; the new tap reads its own range | `an old request after leaving the screen...` (B1) | verify V1.4 sibling |
| U3.4 | Remove tapped, then Cancel | fixed | `a late failure after Remove tapped and cancelled shows nothing` (B4) | fixer |
| U4.1 | session-id sets `reading` and `confirming` in the screen | fixed: both are sheet state | U4 search | fixer |
| U2.1 | rotation lost the open sheet and its line | fixed: `rememberSaveable` with a hash check | `a failure stays after a recomposition and a rotation` (B5) | contract U2 |
| U3.5 | "Use this time range" with a read in flight | fixed in code (`asked = null`); the two actions never show together (Read again needs a range, Use this time range needs none) | `HeartShotsTest.sheetsOfOtherStates` (`360-f1.3-3-sheet-use-range`: only "Use this time range") | fixer |
| U3.6 | old cases: range change, new row, close and reopen, delete, Remove | kept | the 5 `a late failure after ...` tests, `a late result after Remove...` | fix1 |
| U3.7 | refused: two ranges sharing one data-layer read | kept: refused by the lead; through the UI it no longer occurs because the old read is cancelled when the sheet goes | U3.3 test | lead |
| U5.1 | charts swapped at the screen | fixed: one list of (title, points) | B7 | verify F5 (V5.3) |
| U3.8 | close and delete tests reopened only after release | fixed by U3.3 test (second request before release) | U3.3 | verify V5.2 |
| U6.1 | old rows of the first task and the first fix | kept: except the removed line texts (listed in U6) | Tests command 810/0; 17 shots looked at | task |
| U7.1 | data, timer, health, Room, manifest, dependencies, version | kept | U7 diff | task |

## Contract

| # | Status | Check ran | Real output |
|---|---|---|---|
| U1 | done: no line for filled, waiting, none, dropped; the rows show the flow's row | F: `the sheet's facts, one read for two taps...` (filled, asserts no `avg 140 · max 140 bpm`), `a sparse read shows the waiting row...` (asserts no "No data yet"), `an automatic pass...` (waiting then filled, no line), `a late result after Remove...` (dropped: off row, no line); rules `the sheet's only line is the failure line` | all pass: 10/0 and 25/0 |
| U2 | done: failure line shows and stays after a recomposition (new session row in the list) and `StateRestorationTester.emulateSavedInstanceStateRestore()` | F: `a failure stays after a recomposition and a rotation` | pass; fails under B5 |
| U3 | done: every listed case shows no old line | F: the 5 old and 5 new late-failure tests, `a sparse read ... session deleted ... closes the sheet`; witness F1 | all pass; `WITNESS F1 fixed at=325cf645d817` |
| U4 | done: no request or result state outside the sheet | `grep -nE "reading\|confirming\|result\|HrRead\|asked\|failedAt\|mutableStateOf" HeartDataScreen.kt`; same over AppViewModel.kt, KhonNav.kt | screen level: only `status`, `granted`, `open`, `confirmRemove` (lines 73-83); `asked`, `failedAt`, `reading`, `confirming` at lines 133-136 inside `HeartSheet`; AppViewModel/KhonNav: no hit |
| U5 | done: `cardioHrCharts` result asserted: `Average heart rate` [150, 144], `Heart rate in work intervals` [152, 155]; a one-chart case too | F: rules `typed and measured values reach both charts...`; B7 | pass; B7 fails it |
| U6 | done: old rows hold except the removed texts. Changed assertions: `one read for two taps` waits for `140 bpm` (was the line `avg 140 · max 140 bpm`) and asserts that line is absent; sparse test drops `shows("No data yet")`, adds `readDone()` and asserts it is absent (renamed); `Remove shows Removed...` and `a read works with heart rate access only` wait for `128 bpm` / `133 bpm` in the facts (were the line); rules tests of `readResult`/`rereadLine` replaced by `the sheet's only line is the failure line`. 17 shots made on `325cf64` (16 old + `360-f1.0-11-sheet-failed`) and looked at; layout as before | Tests command; `HeartShotsTest` | `tests=810 failures=0`; `ls android/app/build/shots-hr \| wc -l` = 17 |
| U7 | done: no change in the restricted paths | `git diff e375cbd HEAD --stat -- data timer health AndroidManifest.xml build.gradle.kts libs.versions.toml schemas \| wc -l` | `0` |

deviation: `confirming` also moved into the sheet (a busy flag kept by session id in the screen); closing the sheet during a range confirm lets a new sheet tap again. `confirmRange` still runs in the view model scope, so a Room write is never cancelled.
deviation: `open`, `sheetNo`, `confirmRemove` are now `rememberSaveable`; U2 needs the sheet to survive a rotation (the manifest has no `configChanges`, so the activity is recreated).
deviation: `ExerciseDetail.kt` changed: the two heart rate charts come from `cardioHrCharts`; layout unchanged. Needed for U5.
deviation: a Remove tap (not only confirm) cancels a running read; after Cancel, "Read again" is enabled again. This follows Scope 2 (the call runs in the sheet scope).
deviation: B6 has no failing test (see break table).
deviation: no trunk merge before the final tests; the lead said do not merge.
deviation: one new shot test `sheetFailed` in `HeartShotsTest.kt`.

## UI proof

The task header has no `UI: yes` line, so no ui-proof sheet. After pictures on `325cf64`: `android/app/build/shots-hr/` and a copy in `/private/tmp/claude-501/-Users-mirzohidkhon-deepvision-personal/162677c2-faf3-443f-b976-34a4a6fdcd1f/scratchpad/after/` (17), all looked at.
