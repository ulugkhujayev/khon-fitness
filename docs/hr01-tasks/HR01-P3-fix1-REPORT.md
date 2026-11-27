# HR01-P3-fix1 report

Commits on `hr01-p3` (task base `4f3ea27`, branch head before this task `0b0bee7`): `decdf9c` (fixes, tests, SPEC.md), plus the commit that adds this report. No push, no merge.

Focused command (F): `JAVA_HOME=... android/gradlew -p android :app:testDebugUnitTest --tests '*HeartRulesUiTest' --tests '*HeartDataScreenTest' --rerun`

Tests command on `decdf9c`: `BUILD SUCCESSFUL in 1m 33s`, `71 actionable tasks: 11 executed, 5 from cache, 55 up-to-date`; JUnit XML: 804 tests, 0 failures, 0 errors, 21 skipped (shot tests of other folders). `HeartRulesUiTest 11/0`, `HeartDataScreenTest 19/0` (3 runs in a row with `--rerun`: 19/0 each), `HeartShotsTest 11/0`, `TapTargetTest 11/0`, `ContrastTest 11/0`.

## Witness F1

Patch repair (only the wait at line 63 changed). Line 63 is not the list-row wait. It is the wait for the held read: `compose.waitUntil(5_000) { fake.reached.isCompleted }`. That condition never lets the main looper run, so the screen's read does not reach the fake (the existing screen test uses `waitForIdle()` in the same wait). Diff of `F1.patch` (backup of the old one in the scratchpad):

```
68c68
< +            compose.waitUntil(5_000) { fake.reached.isCompleted }
---
> +            compose.waitUntil(5_000) { compose.waitForIdle(); fake.reached.isCompleted }
```

- Before the repair (in my tree, patch applied): the range case `ComposeTimeoutException ... HeartDataLateResultTest.kt:63`.
- Base `4f3ea27`, after the repair: `WITNESS F1 reproduces at=4f3ea27aa030 receipt=r-20261126222402-aef14e`; its log: `failedReadDoesNotAppearAfterRemove FAILED ... AssertionError at HeartDataLateResultTest.kt:78`, same for `...AfterRangeChange` at line 78 (the last assertion, `assertDoesNotExist` of "Health Connect did not answer"); `2 tests completed, 2 failed`.
- Head `decdf9c`: `WITNESS F1 fixed at=decdf9c66f43 receipt=r-20261126223621-b26609`.

## Break proof (each break put in the tree, F run, files restored with `git checkout`, tree clean after)

| Break | Failing tests |
|---|---|
| "Read again" without `enabled = id !in reading` | `the sheet's facts, one read for two taps...` (asserts Disabled while held) |
| `readResult` returns every result, sheet line without key check, no clear effect, no sheet counter (the old logic) | the 5 `a late failure after ...` screen tests, rules `a result belongs to its sheet, range and row...` |
| sheet "Average" from `shownAvgHr` | rules `the sheet shows measured values...`, screen `the sheet shows measured values where Review shows the typed ones` |
| `avgHrPoints` from `s.avgHr` | rules `typed and measured values reach both charts` (first chart) |
| `workHrPoints` from `it.avgHr` alone | same test: `expected [ChartPoint(..152.0..)...]` (second chart) |
| `withPermissions` also asks distance | `a read works with heart rate access only` |
| `remove` flag taken from the facts | rules `the sheet's model holds the facts and the action flags...` |
| `Repository.confirmRange` without `requestFill()` | `Use this time range ... and the sheet follows its own fill` |
| cardio form draft takes the measured placeholder | `a restored cardio form ...` (`expected null, but was:<128>`, line 525) and the old placeholder test |
| session average field change not written to the draft | `a restored cardio form ...` (`expected:<137> but was:<null>`, line 535) |

## Coverage

Witness F1: base `4f3ea27` reproduces (line 78, both cases), head `decdf9c` prints `fixed`.

| ID | Instance | Status | Test | Found by |
|---|---|---|---|---|
| V1.1 | late failure after Remove | fixed | `a late failure after Remove shows nothing`; witness F1 | review F1 |
| V1.2 | late failure after a range change | fixed | `a late failure after a range change shows nothing`; witness F1 | review F1 |
| V1.3 | late failure after a new row of the same state | fixed | `a late failure after a new row of the same state shows nothing` | review F1 |
| V1.4 | completion after close writes the result back | fixed | `a late failure after the sheet closed shows nothing when it opens again` | review F1 |
| V1.5 | completion after a delete, then the same session id again | fixed | `a late failure after a delete shows nothing, also when the session comes back` | fixer |
| V1.6 | filled or sparse result matched by state alone | fixed: the key holds the row the read left | rules `a result belongs to its sheet, range and row...` | review F1 |
| V1.7 | failure with nothing changed stays visible | kept | `the sheet's facts, one read for two taps, a failed read keeps the facts` | task |
| V2.1 | sheet round row measured (typed 150, measured 144) | kept: F2 refused, now tested | rules and screen `the sheet shows measured values...` | review F2 |
| V2.2 | sheet "Average" row took the typed value | fixed: `measuredAvgHr` | same tests | fixer |
| V2.3 | Review and charts keep typed first | kept | screen `...where Review shows the typed ones`; rules `typed and measured values reach both charts` | task |
| V3.1 | Remove flag from a raw row | fixed: `HrSheet.remove` | rules `the sheet's model holds the facts and the action flags...` | review F3 |
| V3.2 | Read again, Use this time range, facts, result line read in the composable | fixed: `HrSheet` fields | same | fixer |
| V3.3 | `rows.associateBy { it.sessionId }` in the composable | fixed: `factsById`, `rowOf` | V3 grep | fixer |
| V4.1 | confirm sentence "Read again brings it back." | fixed | `HeartShotsTest.confirmRemove`, image looked at | review F4 |
| V5.1 | action disabled while a read waits | fixed | `one read for two taps` Disabled checks | review F5 |
| V5.2 | late failing result after Remove, range change, delete | fixed | V1.1, V1.2, V1.5 tests | review F5 |
| V5.3 | mixed values in both charts | fixed | rules `typed and measured values reach both charts` | review F5 |
| V5.4 | restored cardio form, typed session average | fixed | `a restored cardio form saves no placeholder, and a typed session average is saved` | review F5 |
| V5.5 | Use this time range asks its own fill | fixed | `Use this time range ... and the sheet follows its own fill` | review F5 |
| V5.6 | read with heart rate access only | fixed | `a read works with heart rate access only` | review F5 |
| V5.7 | screen tests used the real Health Connect reader for the repository's fill; a leftover pass hit a closed database | fixed in setup | `HeartDataScreenTest` 3 runs 19/0 | fixer |
| V5.8 | Review test: the rule ends the activity after `@After`, the screen read a closed database | fixed in `@After` | same | fixer |
| V6.1 | added comments in `HeartRules.kt`, `HeartDataScreen.kt`, `HeartRulesUiTest.kt` | fixed: all removed | `comment-guard --base 4bc0e7c` guard=0 | review F6 |
| V6.2 | quirk comments in `HeartDataScreenTest.kt` (4) and `HeartShotsTest.kt` (1) | kept: Compose test and Robolectric quirks | V6 row | review F6 |
| V7.1 | old rows of the first task | kept | Tests command 804/0; 16 shots looked at | task |
| V8.1 | data, timer, health, manifest, dependencies, version | kept: no change | `git diff ... \| wc -l` = 0 | task |

## Contract

| # | Status | Check ran | Real output |
|---|---|---|---|
| V1 | done: `HrReadKey(sheet, sessionId, range, row)` and `HrReadResult(key, outcome, seen)` in `HeartRules.kt`. A completion writes only through `readResult`: nothing when the sheet, session or range changed; a failure only while the key is unchanged; a committed result keyed to the row read from the database after the read. The line shows only while the key equals the key of now; `after` clears a seen result when its key goes. Open and close bump the sheet counter and clear the result; Remove and a new tap clear it. | F; witness base and head | `HeartDataScreenTest tests="19" failures="0"`; `HeartRulesUiTest tests="11" failures="0"`; base `WITNESS F1 reproduces ... receipt=r-20261126222402-aef14e` (both cases fail at line 78); head `WITNESS F1 fixed at=decdf9c66f43 receipt=r-20261126223621-b26609` |
| V2 | done: sheet "Average" is `measuredAvgHr`, round rows `roundAverage` (measured). Review and the charts keep `shownAvgHr`/`shownRoundHr`. SPEC.md line added after the `unslop` skill. | F: rules `the sheet shows measured values...`; screen `the sheet shows measured values where Review shows the typed ones`; rules `typed and measured values reach both charts` | sheet `Round 1 144 bpm`, `Average 128 bpm`, no 150 or 141; Review `150 bpm`, `141 bpm`; charts `[150.0, 144.0]` and `[152.0, 155.0]`; all pass |
| V3 | done: `hrSheet(...)` returns `HrSheet(facts, reread, remove, useRange, line)`; `remove = row != null && row.state != OFF` in the rules. | rules `the sheet's model holds the facts and the action flags...`; `grep -nE "\.(fromMs\|toMs\|state\|samples\|rawCount\|sampleCount\|origin\|origins\|firstMs\|lastMs\|maxGapSec\|maxHr\|fetchedAt\|sessionId)\b" HeartDataScreen.kt`; `grep -nE "hrRow\.\|hrRows\.\|rows\.(find\|associate\|filter\|map)"` over HeartDataScreen, Review, Cardio, ExerciseDetail | two NotRead sessions: same facts, `remove` true only with the stale row; grep hits only `intervals.filter { it.sessionId == id }` (IntervalLog) and `bounds.fromMs/toMs` (HrRange); second grep: exit 1, no hit |
| V4 | done: `ConfirmSheet("Remove heart rate?", "", "Remove", ...)`; the component draws no body for "". | `HeartShotsTest.confirmRemove`, image looked at | `360-f1.0-10-confirm-remove.png`: title, red "Remove", "Cancel", no sentence |
| V5 | done: disabled while held: `assertTrue(disabled("Read again"))` after `reached()` and after a second tap. Late failure: 5 tests. Charts: `avgHrPoints`, `workHrPoints` (now used by `CardioTypeScreen`) with mixed values. Cardio form: `StateRestorationTester` restore, Save, both typed columns null; typed session average 137 saved. Time range: the confirm's own fill (`fake.calls == [40 min range]`) and the sheet shows "Maximum 131 bpm" with no Read again tap. Heart rate only: `HeartAccess.withPermissions(..., { setOf(HEART_READ) })`, no card, Read again fills; empty set shows the card. | F; break table above | each break fails its test (table) |
| V6 | done: all added comments removed from `HeartRules.kt`, `HeartDataScreen.kt` (0 left), `HeartRulesUiTest.kt` (0). The non-breaking spaces got a name, `labelsKeptWithNumbers`. Kept, each a quirk of Compose test or Robolectric: `HeartDataScreenTest:82` (the rule ends the activity after `@After`), `:113` (one setContent per rule), `:125` (Robolectric drops a touch on a settling sheet), `:128` (`waitUntil` does not run the main looper); `HeartShotsTest:74` (a sheet is its own window). Other files of the first change had no added comment. | `grep -cE '^\s*(//\|/\*\*\|\*)'`; `comment-guard --base 4bc0e7c` | 0, 0, 4, 0, 1; `guard=0` |
| V7 | done: old rows hold (full suite 804/0). Changed assertions: rules `canReread`/`canRemove` lines replaced by the projection test (functions gone); `one read for two taps` adds the Disabled checks; the old Use this time range test now expects the confirm's own fill (it no longer waits for "Not read" and a manual Read again, because the fill now reaches the fake); the separate new test was merged into it. 16 shots made on `decdf9c` (15 old + confirm) and looked at; layout as before. | Tests command | `BUILD SUCCESSFUL in 1m 33s`, 804 tests, 0 failures; `ls android/app/build/shots-hr \| wc -l` = 16 |
| V8 | done: no change in data, timer, health, manifest, `build.gradle.kts`, `libs.versions.toml`. | `git diff 4f3ea27 HEAD -- <those paths> \| wc -l` | `0` |

deviation: the witness repair changed the held-read wait at line 63, a line after the "Read again" tap. The task said line 63 waits for the list row; it does not. Only the wait's condition changed; no step and no assertion.
deviation: the list row on "Heart rate data" still shows the typed average first ("avg 141 · max 167"). Scope 2 names the sheet only.
deviation: `HeartAccess` keeps its `granted` constructor (the witness patch uses it); `HeartAccess.withPermissions` holds the heart-rate-only rule, and `of(context)` uses it.
deviation: test setup replaces `KhonApp.heartFill`'s lazy delegate with the fake fill (reflection), so the repository's fill request reaches the fake.
deviation: SPEC.md also got the F1 rule (the line belongs to the open sheet, the range read, and the row the read left).
deviation: no trunk merge before the final tests; the lead said do not merge.

## UI proof

The task header has no `UI: yes` line, so no ui-proof sheet. Before pictures (made on the base code): `/private/tmp/claude-501/-Users-mirzohidkhon-deepvision-personal/b250a2cc-4c9b-4163-ad7d-0c9cc08e0a2f/scratchpad/before/` (15). After pictures on `decdf9c`: `android/app/build/shots-hr/` and a copy in `.../scratchpad/after/` (16): `360-f1.0-{1-list,2-sheet-filled,4-sheet-waiting,5-no-permission,6-review-cardio,7-cardio-form-placeholders,8-review-gym,9-settings,10-confirm-remove}`, `360-f1.3-{1-list,2-sheet-filled,3-sheet-use-range}`, `411-f1.0-{1-list,2-sheet-filled}`, `411-f1.3-{1-list,2-sheet-filled}`. All looked at.
