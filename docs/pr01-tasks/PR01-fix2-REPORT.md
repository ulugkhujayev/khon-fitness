# PR01-fix2 report

Fix commit: `42ce880` on branch `pr01` (base `c1a3cd2`). No push, no merge.

| # | Status | Check ran | Real output |
|---|---|---|---|
| Y1 | done: `FinishRequest.start` starts no read while one is pending | `android/gradlew -p android :app:testDebugUnitTest --tests '*FinishSaveOnceTest'` | `two Finish taps while the read is held read once and open one sheet` passes: reads 1 after a double tap and a third tap, 0 sheets before the release, 1 sheet after it. FinishSaveOnceTest: tests="15" failures="0" |
| Y2 | done: the sheet's dismiss calls `finishRead.drop()`, which cancels the pending read; `ensureActive()` after the read stops a cancelled one | same | `a sheet dismissed before a later read returns stays closed` passes: second read started while the sheet was open (reads 2), sheet dismissed, read 2 released, 0 sheets |
| Y3 | done: after the sets read, the session is read; missing or finished gives no sheet. A `LaunchedEffect` drops the read when the observed session is missing or finished. Leaving the screen cancels its scope | same | `a session deleted while the read is held opens no sheet` (screen back on Today, 0 sheets, no crash) and `a session finished while the read is held opens no sheet` (screen stays, 0 sheets) pass |
| Y4 | done: the time is taken at the tap (`clock()` before `start`); the session's own sets are `all.filter { it.sessionId == s.id }` from the one `readSets()` result | same | `a set checked before the held query runs counts in every figure, at the tap's time` → Time "10 min", "2 of 2", New bests "1", Rep records "0"; `a set checked after the query ran counts in no figure, at the tap's time` → "10 min", "1 of 2", "0", "1". The clock moved to 40 min before the release in both |
| Y5 | done: the witness still gives `fixed` | `ANDROID_HOME=$HOME/Library/Android/sdk witness run .../F2.json --sha 42ce880...` | `WITNESS F2 fixed at=42ce880293d4 receipt=r-20261126201833-5fe756`. Receipt log: `Task :app:compileDebugUnitTestKotlin`, `Task :app:testDebugUnitTest` (not from cache), `BUILD SUCCESSFUL in 13s` |
| Y6 | done: `assertMainLine` needs at least 1 dp of accent pixels in every band column, for no trend, a trend inside, a trend above, and an empty trend | break script `breaks.py` (temp edit, test class, `git checkout --`) | main stroke removed: fails at LineChartTrendTest.kt:93; trend stroke removed: fails at LineChartTrendTest.kt:111; passes on `42ce880` |
| Y7 | done: the last check compares the no-argument chart and the empty-trend chart for accent and muted counts over the whole width, plus the main-line check on both | `--tests '*LineChartTrendTest'` | passes, tests="1" failures="0" |
| Y8 | done: only `SessionScreen.kt`, four test files and this report change. `SessionScreen` gets two parameters with defaults (`readSets`, `clock`); `KhonNav.kt` and the witness patch call it unchanged | `git show --stat 42ce880` | SessionScreen.kt, LineChartTrendTest.kt, FinishSaveOnceTest.kt, HistoryScreenTest.kt, RepRecordsTest.kt |

Tests command on `42ce880`: `JAVA_HOME=... android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease` → `Task :app:testDebugUnitTest`, `Task :app:assembleDebug`, `Task :app:lintVitalRelease`, `BUILD SUCCESSFUL in 1m 27s`, exit 0. Test XML totals: 703 tests, 25 skipped, 0 failures, 0 errors.

## Break proofs

Each break was a temp edit on `42ce880`, one test class run, then `git checkout --`; the tree was clean after the script.

- F2-once: the pending check removed → `two Finish taps...` fails (FinishSaveOnceTest.kt:226).
- F2-dismiss: dismiss without `drop()` → `a sheet dismissed...` fails (FinishSaveOnceTest.kt:249).
- F2-gone-both: the `LaunchedEffect` drop and the post-read session check both removed → `a session finished...` fails (FinishSaveOnceTest.kt:274).
- F2-gone-effect-only and F2-gone-check-only: exit 0. Each guard alone catches the test case. The post-read check covers a read that returns before the session flow emits; the effect drops the read as soon as the screen sees the change.
- F2-time-after-read: the time taken after the read → both Y4 tests fail (lines 307, 314).
- F2-own-from-screen: own sets from the screen's `logs` → `a set checked after the query ran...` fails (line 314).
- F2-no-ensureActive: exit 0. `drop()` cancels the job, and both the real read and the test's held read stop at a cancellable suspension, so this guard has no reachable failure in a test.
- F4-main-stroke: fails at LineChartTrendTest.kt:93. F4-trend-stroke: fails at LineChartTrendTest.kt:111.
- F4-rows-no-date: the date line removed from `RepBestRow` → most reps test fails (HistoryScreenTest.kt:259).
- F4-log-wide-span: green span widened over " · 1 PR" → one Log row test fails (HistoryScreenTest.kt:285, no exact span).
- F4-log-prefix: "x " before the session line → one Log row test fails (timeout waiting for the whole line).
- F4-trend-text-no-mean: `trendText` drops the mean when a rate exists → the rounding test fails, with two other RepRecordsTest tests.

## Coverage

| # | Instance | Status | Test | Found by |
|---|---|---|---|---|
| Y1.1 | `SessionScreen.kt` `openSummary` → `FinishRequest.start`, second tap while pending | fixed: one pending read | FinishSaveOnceTest `two Finish taps while the read is held...` | finding F2 |
| Y2.1 | `SessionScreen.kt` finish sheet `onDismiss` | fixed: `finishRead.drop()` | FinishSaveOnceTest `a sheet dismissed before a later read returns stays closed` | finding F2 |
| Y2.2 | `FinishRequest.start` `ensureActive()` after the read | kept: the cancel already stops the read at its suspension (break F2-no-ensureActive passes) | FinishSaveOnceTest `a sheet dismissed...` | finding F2 |
| Y3.1 | `openSummary` read, session deleted | fixed: post-read session check; screen leave cancels the scope | FinishSaveOnceTest `a session deleted while the read is held opens no sheet` | finding F2 |
| Y3.2 | `openSummary` read, session finished | fixed: post-read session check and `LaunchedEffect(gone)` drop | FinishSaveOnceTest `a session finished while the read is held opens no sheet` | finding F2 |
| Y4.1 | `openSummary` own set list | fixed: from the same `readSets()` result as the other sessions' sets | FinishSaveOnceTest `a set checked after the query ran...` | finding F2 |
| Y4.2 | `openSummary` time | fixed: `clock()` at the tap | both Y4 tests | finding F2 |
| Y4.3 | Other reads that publish into UI state later, in the branch diff | kept: none. `git diff 9d1ba17 HEAD -- android/app/src/main \| grep -E "launch\|LaunchedEffect\|\.first\(\)\|vm\.run\|produceState\|collectAsState\|withContext\|suspend "` gives only the `openSummary` lines | FinishSaveOnceTest Y1–Y4 tests | grep over the branch diff (merge base `9d1ba17`) |
| Y5.1 | Fresh view model, one tap | kept: `fixed` | witness F2, receipt r-20261126201833-5fe756 | witness file |
| Y6.1 | `LineChartTrendTest` main line check | fixed: `assertMainLine` on every band column | LineChartTrendTest (breaks F4-main-stroke, F4-trend-stroke) | finding F4 |
| Y7.1 | `LineChartTrendTest` last assertion | fixed: accent and muted counts, whole width, both charts | LineChartTrendTest | finding F4 |
| Y7.2 | `HistoryScreenTest.kt:243` most reps rows | fixed: compares every text of each row (weight, date, reps), not first and last | HistoryScreenTest `most reps by weight...` (break F4-rows-no-date) | finding F4 list |
| Y7.3 | `HistoryScreenTest.kt:267` Log row text | fixed: the whole line, exact match, not a substring | HistoryScreenTest `one Log row holds...` (break F4-log-prefix) | finding F4 list |
| Y7.4 | `HistoryScreenTest` `stylesOf` span match | fixed: span must cover exactly the part; the older rep record test still passes with it | HistoryScreenTest `one Log row holds...` (break F4-log-wide-span) | read of the helper |
| Y7.5 | `RepRecordsTest` rounding test, `substringAfter(" · ")` | fixed: the whole `trendText` result per case | RepRecordsTest `the rate rounds its size half up...` (break F4-trend-text-no-mean) | finding F4 list |
| Y7.6 | `RepRecordsTest` Log line test | kept: checks the whole text and exact spans already | RepRecordsTest `the Log line keeps each dot...` | finding F4 list |
| Y7.7 | `FinishSaveOnceTest` fix1 test `rowValue` | kept: one digit-only text on the row line | FinishSaveOnceTest `the finish sheet counts rep records...` | read |
| Y8.1 | Whole change | kept: no Room, layout, text, rule, version or dependency change | full `Tests:` command | `git show --stat 42ce880` |
