# PR01-fix3 report

Commit: ca3d5b3 (code and tests). Base: 98c76bf.

| # | Status | Check ran | Real output |
|---|---|---|---|
| Z1 | done: the screen collects `allSets` (default `vm.repo.allSetLogs`) with `collectAsStateWithLifecycle(null)`; the sheet is computed from that list | `witness run …/F2.json --sha ca3d5b3` | `WITNESS F2 fixed at=ca3d5b31c8a3 receipt=r-20261126211220-472e05`; receipt log line 45 `> Task :app:testDebugUnitTest` (executed, not from cache), line 47 `BUILD SUCCESSFUL in 11s` |
| Z2 | done: Finish sets `finishTap` only while it is null; test `Finish taps before the sets load open one sheet at the first tap's time` (double tap at 10 min, third tap at 30 min, one sheet, Time `10 min`, one collection) | focused run; break B2 `finishTap = clock()` | pass; B2: `FAIL Finish taps before the sets load open one sheet at the first tap's time` |
| Z3 | done: `remember(tap, loaded == null)` computes once when both are set; tests `a set checked before the held query runs …` and `a set checked after the query ran …` (first list held, tap time `10 min`, `sheets() == 1`) | focused run; break B4 `remember(tap, loaded)` | pass; B4: `FAIL a set checked after the query ran counts in no figure, at the tap's time` |
| Z4 | done: test `sets that load after Back while the screen is still composed open no sheet` (clock paused, Back, `Bench Press` still composed and route `today`, release the first list, 5 frames, `sheets() == 0`, then 0 after disposal). The tap time stays as tapped (only Dismiss and Save write it); the computed sheet stays null because the popped entry stops collecting | focused run; break B1 `collectAsState(null)` | pass; B1: `FAIL sets that load after Back while the screen is still composed open no sheet` |
| Z5 | done: the sheet is composed only `if (running)`, and a null session returns before it; tests `an open sheet goes when the session is finished elsewhere`, `… deleted elsewhere`, `a session finished/deleted before the sets load opens no sheet` | focused run; break B3 removes `if (running)` | pass; B3: `FAIL an open sheet goes when the session is finished elsewhere`, `FAIL a session finished before the sets load opens no sheet`. The two deleted cases have no break the compiler allows (`session ?: return`); they guard against a crash |
| Z6 | done: test `an open sheet keeps its figures when a set of another session changes` (s0 set 80 × 6 becomes 80 × 10, Rep records stays `1`) | focused run; break B4 | pass; B4: `FAIL an open sheet keeps its figures when a set of another session changes` |
| Z7 | done: `FinishRequest`, `finishRead`, its job, `readSets`, the second session read, `ensureActive()` and the `gone` effect are removed. The Finish path is `{ if (finishTap == null) finishTap = clock() }` | `grep -n "launch\|ensureActive\|FinishRequest\|readSets" SessionScreen.kt` | only `scope.launch { listState.animateScrollToItem(…) }` in `open()` (scroll, not Finish) |
| Z8 | done: only `SessionScreen.kt`, `FinishSaveOnceTest.kt` and this report change. `readSets` became `allSets: Flow<List<SetLog>>`: the tests hold the flow's first value through it. No other caller used `readSets` (`grep -rn readSets android/app/src` empty) | `git diff --stat 98c76bf` | 2 code files and the report |
| Dismiss | done: test `a dismissed sheet stays closed when the sets change` | break B5 `onDismiss = { }` | B5: `FAIL a dismissed sheet stays closed when the sets change` |

Tests: the `Tests:` command on ca3d5b3. First run: stopped by the 30-minute background limit inside `:app:testDebugUnitTest`, with no test failure in the log (machine shared with other Gradle builds). Second run: `> Task :app:testDebugUnitTest`, `> Task :app:assembleDebug`, `> Task :app:lintVitalRelease`, `BUILD SUCCESSFUL in 1m 26s`, `71 actionable tasks: 8 executed, 63 up-to-date`; XML results: 707 tests, 0 failures, 0 errors, 25 skipped. Focused `FinishSaveOnceTest`: 19 tests, 0 failures.

## Coverage

Class: a coroutine that publishes UI state after an await. Search: `git diff 9d1ba17 -- android/app/src/main` (merge base with `main`), added lines with `launch`, `LaunchedEffect`, `vm.run`, `produceState`, `async`, `first()`, `withContext`, `suspend`, `collect`, `await`.

| # | Instance | Status | Test | Found by |
|---|---|---|---|---|
| Z1.1 | Finish read (`FinishRequest.start` → `summary = it`), fresh view model | fixed: removed; the sheet derives from `allSets` collected by the screen | witness F2 receipt r-20261126211220-472e05; FinishSaveOnceTest `the finish sheet counts rep records against sessions no other screen has loaded` | finding F2 |
| Z2.1 | Second Finish tap while a tap is pending | fixed: `if (finishTap == null) finishTap = clock()` | FinishSaveOnceTest `Finish taps before the sets load open one sheet at the first tap's time` (break B2) | finding F2 |
| Z3.1 | Tap before the sets load | fixed: `remember(tap, loaded == null)` computes once | FinishSaveOnceTest `a set checked before/after the held query …` (break B4) | finding F2 |
| Z4.1 | Sets that arrive after Back, composition alive | fixed: lifecycle-aware collection; no callback sets the sheet | FinishSaveOnceTest `sets that load after Back while the screen is still composed open no sheet` (break B1) | verify R3 |
| Z5.1 | Session finished elsewhere, sheet open or pending | fixed: sheet composed only `if (running)` | FinishSaveOnceTest `an open sheet goes when the session is finished elsewhere`, `a session finished before the sets load opens no sheet` (break B3) | finding F2 |
| Z5.2 | Session deleted elsewhere, sheet open or pending | kept: `session ?: return` before the sheet | FinishSaveOnceTest `an open sheet goes when the session is deleted elsewhere`, `a session deleted before the sets load opens no sheet` | finding F2 |
| Z6.1 | Set of another session changes while the sheet is open | fixed: figures keyed on tap time and loaded state only | FinishSaveOnceTest `an open sheet keeps its figures when a set of another session changes` (break B4) | contract Z6 |
| Z6.2 | Dismiss, then the sets change | fixed: Dismiss clears the tap time | FinishSaveOnceTest `a dismissed sheet stays closed when the sets change` (break B5) | finding F2 |
| Z7.1 | Save: `vm.run { finishSession }` | kept: `finishTap = null` runs synchronously outside `vm.run` | FinishSaveOnceTest `a double tap on Save …` (2 tests) | branch diff search |
| Z7.2 | `LaunchedEffect(s.id, s.programId) { previous = … }`, toggleSet `vm.run { refused = …; open(…) }`, swap `vm.run { swapNotice = … }` | kept: pre-existing, not in the branch diff, outside scope | branch diff search shows no change to these lines (`grep -c` = 0); check tests cover toggleSet | branch diff search |
| Z7.3 | `ExerciseDetail.kt`, `HistoryLog.kt`, other diff files | kept: no instance; only `remember(history) { repBests(history) }`, synchronous | branch diff search | branch diff search |
| Z8.1 | `readSets` parameter callers | fixed: replaced by `allSets: Flow<List<SetLog>>`; only `FinishSaveOnceTest` passed it | `grep -rn readSets android/app/src` empty; Tests command | contract Z8 |
