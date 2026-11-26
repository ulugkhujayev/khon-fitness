# HR01-P2-import report

Commits on `hr01-p2` (base `a899bd2`, branch start `c15ea8f`): `29fb556` (code and tests), `492159a` (`SPEC.md`), and this report. No push, no merge.

Focused command (F): `JAVA_HOME=... android/gradlew -p android :app:testDebugUnitTest --tests '*BandImportTest' --tests '*ImportAdmissionTest' --tests '*ImportOpenFillTest'`
Real output of F: `BandImportTest tests="16" failures="0"`, `ImportAdmissionTest tests="10" failures="0"`, `ImportOpenFillTest tests="1" failures="0"`.

Tests command on `492159a`: `BUILD SUCCESSFUL in 1m 52s`, `71 actionable tasks: 27 executed, 22 from cache, 22 up-to-date`; JUnit XML: 834 tests, 0 failures, 0 errors.

On the phase 1 tip: `git checkout a899bd2 -- android/app/src/main` (BandCover.kt removed), then `compileDebugUnitTestKotlin`: 116 errors in `BandImportTest.kt`, 37 in `ImportAdmissionTest.kt`, 2 in `TimerCommitTest.kt` (first: `Unresolved reference 'offered'`, `'importLocal'`, `'countOffered'`). Tree restored with `git checkout HEAD -- android/app/src/main`.

Break proof (each break in the tree, F run with old XML deleted, file restored with `git checkout`; tree clean after): read not in one transaction: 1 failed (AT12a); half of the run not required: 3; linkedIds not claimed: 1; ids matched as substrings: 1; grouping keeps input order: 2; no pending check: 6; live snapshot not checked: 2; write does not check covered: 12; pending checked before covered: 1; "Import all" stops at a failed write: 1; fill on every result: 13; no fill on screen open: 1 (ImportOpenFillTest); run range ignores the date: 1.

Timer package lines changed: `timer/TimerSnapshot.kt:137-145` (new `TimerSnapshots.resultPending`, read only), `timer/TimerService.kt:547` (`ServiceTimerControl.resultPending`). No existing timer line changed. `data/TimerCommit.kt`: `TimerControl.resultPending()` added. `ImportScreen.kt`: 12 changed lines (open fill, list read, one `import` for single and batch).

| # | Status | Check ran | Real output |
|---|---|---|---|
| D1 | done: `covered` (`data/BandCover.kt:31`), `runRange` (`data/HeartRate.kt`); finished and unfinished app run 18:04-18:44 against band 18:00-18:40: not listed, count 0, write `COVERED`. | F, `BandImportTest.D1 ...`, `ImportAdmissionTest.AT01 D1 D1a ...` | tests="16" failures="0"; tests="10" failures="0" |
| D1a | done: session made 23:59, timer 00:01-00:21 next day (real timer path), band 00:00-00:22: run range exists, list and count exclude, write `COVERED`, before and after Save without a date edit. | F, `ImportAdmissionTest.AT01 D1 D1a ...`, `BandImportTest.D1a ...` | tests="10" failures="0" |
| D2 | done: gym over the whole time, bike session, run without range, imported run: draft stays in list and count, write `SAVED`. | F, `BandImportTest.D2 ...` | tests="16" failures="0" |
| D3 | done: half of both covered; half and 40 %, 2 min in 60 min, one side below half: not covered; midnight decided on epoch ranges: covered. List, count, and write agree for each fixture. | F, `BandImportTest.D3 AT08 AT08a ...` | tests="16" failures="0" |
| D4 | done: unknown modality never covered, offered and counted; standalone strength record hidden from list and count. | F, `BandImportTest.D3 AT08 AT08a ...` | tests="16" failures="0" |
| D5 | done: `HealthImport.offered` filters each original record, then `mergeOverlaps` groups (sorted by start, end, id; output newest first). Covered member leaves, uncovered pair whose union would be covered still shows, every input order gives the same groups, count = visible groups. | F, `BandImportTest.D5 ...`, `AT03 ...`, `AT03a ...` | tests="16" failures="0" |
| D6 | done: `claimedIds` splits `sourceId` and `linkedIds` into trimmed whole ids; `id1` linked hides `id1` only, `id10` stays and saves once. | F, `BandImportTest.D6 AT04 ...` | tests="16" failures="0" |
| D6a | done: `KhonDao.importLocal` is one `@Transaction` (exercises, then sessions); a held read with a queued replacing write gives 0 offered during and after, for list and count. | F, `BandImportTest.D6a AT12 ...`, `AT12a ...` | tests="16" failures="0" |
| D7a | done: `HealthImport.importEach` catches a failed write and goes on; the screen reads the list after every batch. Trigger fails rounds of draft `bad`: no session, rounds, claim or fill; two later drafts save; retry saves `bad` once. | F, `BandImportTest.D7a AT09 ...` | tests="16" failures="0" |
| D7b | done: result held before its write, or failed and waiting for retry: `TIMER_PENDING`, nothing written, draft still offered; after commit `COVERED`; running timer: `SAVED`. | F, `ImportAdmissionTest.D7b AT07 ...`, `AT07 AT07a a failed timer write ...`, `AT07 an import while the timer still runs saves` | tests="10" failures="0" |
| D7c | deviation: `importDraft` takes `timerLock` for its pending check and its Room write, and the timer commit takes the same lock. The timer does not take the Mutex at "run ended" (`finish()` runs on the main thread and cannot wait for it); the pending store entry, written at once at the run end, stands for it. The import does not wait for the startup restore; it counts a live cardio snapshot that has ended, or reaches its end now, as pending (`TimerSnapshots.resultPending`). Outcomes match the row: pending first refused, import first saves and the commit waits for the lock, cold start refused until the restore commits. | F, `ImportAdmissionTest.AT07a ...` (five cases) | tests="10" failures="0" |
| D18 | done: `LaunchedEffect(Unit) { vm.repo.fillHeartRate() }` in `ImportScreen`; one pass per visit through the app runner. | F, `ImportOpenFillTest.D18 AT24 ...` | tests="1" failures="0" |
| D20 | done: no layout change, no new screen text, no permission, dependency, manifest or version change; `SPEC.md` import section updated (`492159a`). | `git diff --stat a899bd2 HEAD -- android/app/src/main/AndroidManifest.xml android/app/build.gradle.kts gradle` | empty output |
| P2-AT01 | done: real timer path, old draft read while the result waits, commit, unsaved form: list and count exclude, old draft `COVERED`. | F, `ImportAdmissionTest.AT01 D1 D1a ...` | tests="10" failures="0" |
| P2-AT02 | done: old group `COVERED`, refresh offers `g2` only, count 1, retry stores 18:15-18:45 with `sourceId` `g2`. | F, `BandImportTest.AT02 ...` | tests="16" failures="0" |
| P2-AT03 | done: six orders of the chain, tied starts, union against 18:00-18:40; grouped import stores 18:00-18:26. | F, `BandImportTest.AT03 ...` | tests="16" failures="0" |
| P2-AT03a | done: tied-start bridge in four orders one group; tied pairs (equal sample counts) give avg 120 in both orders. | F, `BandImportTest.AT03a ...` | tests="16" failures="0" |
| P2-AT04 | done: linked `id1` and imported `id1`: old group `ALREADY_IMPORTED`, `id10` saves once, other sessions unchanged. | F, `BandImportTest.D6 AT04 ...` | tests="16" failures="0" |
| P2-AT05 | done: Import all `[SAVED, COVERED, SAVED]` with band bounds, replays `ALREADY_IMPORTED`, two parallel submits give one `SAVED` and one `ALREADY_IMPORTED`. | F, `BandImportTest.AT05 ...` | tests="16" failures="0" |
| P2-AT07 | done: pending refusal, covered after commit, failed timer write keeps the draft, running timer saves. | F, `ImportAdmissionTest` (D7b AT07, AT07 AT07a, AT07 live) | tests="10" failures="0" |
| P2-AT07a | done: held before admission `TIMER_PENDING`; admitted import saves while the commit waits (not done after 500 ms), then commits; cold start ended and reaching-end snapshots `TIMER_PENDING`, then `COVERED`; second submit before retry pending; covering result with cleanup not run `COVERED`; result without a range frees the import, saves once. | F, `ImportAdmissionTest.AT07a ...` | tests="10" failures="0" |
| P2-AT08 | done: covered table and DB fixtures; touching ends, two runs together, gym, bike, imported, no range: not covered. | F, `BandImportTest.D3 AT08 AT08a ...`, `D2 ...` | tests="16" failures="0" |
| P2-AT08a | done: touching ends zero overlap; unknown workout against no-modality session offered and counted; strength hidden. | F, `BandImportTest.D3 AT08 AT08a ...` | tests="16" failures="0" |
| P2-AT09 | done: single and batch runs with failing rounds; retry from the refreshed list saves once. | F, `BandImportTest.D7a AT09 ...` | tests="16" failures="0" |
| P2-AT10 | done: import with a fill that never runs; new repository and fake heart source fill the stored bounds (`filled`, 18:00-18:40); replay `ALREADY_IMPORTED`, no fill. | F, `BandImportTest.AT10 ...` | tests="16" failures="0" |
| P2-AT11 | done: archived exercise still covers; date moved to 2026-11-25: no run range, record listed and counted. | F, `BandImportTest.AT11 ...` | tests="16" failures="0" |
| P2-AT12 | done: held list and count reads against an atomic replace of the run by a claiming import: 0 during and after. | F, `BandImportTest.D6a AT12 ...` | tests="16" failures="0" |
| P2-AT12a | done: writer queued behind the held read, released without waiting; Run to overlapping Bike in one transaction: offered and counted (1, 1) in both states, no import. | F, `BandImportTest.AT12a ...` | tests="16" failures="0" |
| P2-AT24 | done: one pass per visit, none for recomposition or Check again; one fill after a save, none for the refused repeat. The permission refresh path (`refresh++`) restarts only the list effect; the test reaches Check again, not the permission launcher. | F, `ImportOpenFillTest.D18 AT24 ...`, `BandImportTest.AT24 ...` | tests="1" failures="0"; tests="16" failures="0" |

Other deviations:

- `ImportDraft` gained `records` (the original records with ranges), so the write can check each original record.
- On a tie of sample counts the group's base is now the earliest record by start, end and id, not the record that came first in the descending list. AT03a needs one answer for every input order.
- `Repository.importedSourceIds` and `KhonDao.importedSourceIds` were removed; `importLocal` replaces them.
- No trunk merge: the lead said not to merge.
