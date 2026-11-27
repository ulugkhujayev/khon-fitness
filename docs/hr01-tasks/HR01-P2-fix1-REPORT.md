# HR01-P2-fix1 report

Commit on `hr01-p2` (base `79531bd`, branch tip before the fix `58135d9`): `79cb97b` (code and tests), and this report. No push, no merge, no trunk merge (the lead said do not merge).

Focused command (F): `JAVA_HOME=... android/gradlew -p android :app:testDebugUnitTest --tests '*ImportScreenTest' --tests '*ImportAdmissionTest' --tests '*BandImportTest'`
Real output of F on `79cb97b`: `BUILD SUCCESSFUL in 16s`; JUnit XML `ImportAdmissionTest tests="12" failures="0"`, `BandImportTest tests="17" failures="0"`, `ImportScreenTest tests="3" failures="0"`.

Tests command on `79cb97b`: exit 0, `BUILD SUCCESSFUL in 1m 41s`, `71 actionable tasks: 11 executed, 5 from cache, 55 up-to-date`; JUnit XML: 839 tests, 0 failures, 0 errors (834 before).

Witness: `witness run .../HR01-P2-import-W/F1.json --sha 79cb97b` printed `WITNESS F1 fixed at=79cb97b1be74 receipt=r-20261126230652-666d30`. Receipt log: `BUILD SUCCESSFUL in 15s`, receipt JSON `"exit": 0`, `"tree_clean": true`.

`comment-guard --base 79531bd`: exit 0.

Precedence (Scope 1): already imported, then covered, then timer result pending. The same at both points: the value read at submit and the value read inside the write are one condition (`pendingAtSubmit || timer.resultPending()`) at the third place (`Dao.kt:349`). Reason: a committed covering result in Room must win over a pending entry whose cleanup has not run (test `AT07a a covering result in Room wins over its pending entry when the cleanup has not run`, unchanged).

Break proof (each break in the tree, F run with old XML deleted, file restored with `git checkout`; tree clean after, script output):
- B1 no pending read inside the write: 2 failed (`D7b a covering run that ends while the admitted import waits for Room ...`, `AT07a an import held before admission ...`).
- B2 no pending read at submit: 1 failed (`D7b a result pending at submit refuses the import that waits behind its commit ...`).
- B3 import without the timer lock: 1 failed (`AT07a D7c a timer commit that reached the lock passes it only after the admitted import released it`).
- B4 zone read before the transaction: 1 failed (`the write reads the time zone inside its transaction, not at submit`).
- B5 screen does not reload after an import: 3 failed (AT09, AT05, D18 AT24 screen tests).
- B6 fill asked with every list read: 1 failed (`D18 AT24 ...`).
- B7 permission result does not read the list again: 1 failed (`D18 AT24 ...`).
- B8 foreground asks no fill: 1 failed (`AT10 after a restart of the database file ...`).

Timer package (main): no line changed (`git diff --stat 79531bd HEAD -- .../timer` is empty). Timer test package: `timer/ImportAdmissionTest.kt` changed (tests only).

| # | Status | Check ran | Real output |
|---|---|---|---|
| W1 | done: `Repository.kt:431,434` reads the pending state at submit and passes `{ pendingAtSubmit \|\| timer.resultPending() }`; `Dao.kt:349` calls it inside the write transaction after the local rows, before the insert. Room held by a test transaction, import queued behind it, covering run ends: `TIMER_PENDING`, nothing written, no fill; after the commit `COVERED`. | F, `ImportAdmissionTest.D7b a covering run that ends while the admitted import waits for Room refuses it at its write`; witness F1 | tests="12" failures="0"; `WITNESS F1 fixed at=79cb97b1be74` |
| W2 | done: the timer commit holds the lock (latch inside its `ended` call), a result is pending at submit, the commit has no covering range (`timedFrom` null, pending store empty after it): the import that waited for the lock returns `TIMER_PENDING`, writes nothing; a new submit `SAVED`, one fill. | F, `ImportAdmissionTest.D7b a result pending at submit refuses the import that waits behind its commit, also when the commit covers nothing`; witness F1 | tests="12" failures="0"; `WITNESS F1 fixed` |
| W3 | done: the import holds the lock after its checks; the timer commit signals its arrival (`beforeTimerCommit`) and starts undispatched, so it runs until the lock; no "timer passed the lock" event before the release; order after: `[import released, timer passed the lock]`; import `SAVED`, commit `COMMITTED`, pending store empty. No sleep. | F, `ImportAdmissionTest.AT07a D7c a timer commit that reached the lock passes it only after the admitted import released it` | tests="12" failures="0"; B3 break: 1 failed |
| W4 | done: every value read before the transaction and used inside it is listed in Coverage (W4.1 to W4.7); the zone was a second instance and is fixed (`Dao.kt:346` calls the zone provider inside the transaction). | F, `BandImportTest.the write reads the time zone inside its transaction, not at submit` | tests="17" failures="0"; B4 break: 1 failed |
| W5 | done: one test per point of the review's F2 (W5.1 to W5.4 in Coverage); each fails when its guard is removed (B3, B5, B6, B7, B8). | F | tests="12"/"17"/"3" failures="0" |
| W6 | done: every comment that the first change added or rewrote is removed or set back to its old text, in main and test files; rules moved into names (`HoldFirstCommitBeforeLock`, `readHeldWhileRoomQueuesWrite`, test names). Kept: one comment, see W6.2. | `git diff 29fb556^ HEAD -U0 -- 'android/app/src/main/*.kt' \| grep added comment lines`; `comment-guard --base 79531bd` | only `timer/TimerSnapshot.kt:137-140` left; guard exit 0 |
| W7 | done: all old rows and attack tests still pass. Changed assertions: AT07a admitted import (500 ms wait replaced by the lock-arrival proof; production `endRun` replaced by `repo.endTimerRun` with the same settle and clear as `recordRun`, to see the lock pass); AT10 (in-memory reuse replaced by a closed and opened `khon.db`; `fillHeartRate()` replaced by `foreground()`; `fills == 0` replaced by `asked to fill.asked == 0 to 1`); D18 AT24 (counted passes replaced by requests counted at the call; Check again, Allow, recomposition now asserted). Repository AT05 and AT09 tests unchanged. | Tests command | 839 tests, 0 failures, 0 errors |
| W8 | done: no `timer` main line changed; no manifest, Gradle, version, Room schema, dependency change; Import screen layout and texts unchanged (only the `source` seam). | `git diff --stat 79531bd HEAD -- .../timer AndroidManifest.xml app/build.gradle.kts gradle build.gradle.kts app/schemas` | empty output |

## Coverage

| # | Instance | Status | Test | Found by |
|---|---|---|---|---|
| W1.1 | pending state, read only before the Room transaction (`Repository.kt:442-445` at base) | fixed: read again inside the write, `Dao.kt:349` | `ImportAdmissionTest.D7b a covering run that ends while the admitted import waits for Room ...`; witness F1 `fixed` | R4 deep review F1 |
| W2.1 | pending state, read only after the lock, after a commit that held the lock cleared it | fixed: read at submit, `Repository.kt:431` | `ImportAdmissionTest.D7b a result pending at submit ...`; witness F1 `fixed` | R4 deep review F1 |
| W3.1 | import first: the lock order of an admitted import and a timer commit | fixed: test proves the lock without a sleep | `ImportAdmissionTest.AT07a D7c a timer commit that reached the lock ...` | R4 deep review F2 (AT07a) |
| W4.1 | time zone, read at the call before the transaction, used by `runRange` inside | fixed: provider called inside the transaction, `Dao.kt:346` | `BandImportTest.the write reads the time zone inside its transaction, not at submit` | fixer search |
| W4.2 | generation | kept: the import reads no generation; Replace and reset take the same timer lock (`Repository.kt:471`), and the write reads fresh rows | `BandImportTest.D6a AT12 ...`, `AT02 ...` | task file list |
| W4.3 | covered decision of the list read | kept: decided again on fresh rows inside the write (`Dao.kt:348`) | `ImportAdmissionTest.AT01 D1 D1a ...`, `BandImportTest.AT02 ...`, `AT05 ...` | task file list |
| W4.4 | already-imported decision of the list read | kept: decided again inside the write (`Dao.kt:347`) | `BandImportTest.D6 AT04 ...`, `AT05 ...` (two submits at once) | task file list |
| W4.5 | the exercise (`id`, `name`, `fieldList`, intervals) | kept: lead decision of HR01-P2-import, an exercise that changed after the list was read stays as today | `BandImportTest.AT11 ...` | task file list |
| W4.6 | the band records and the draft date from the list read | kept: same lead decision (a band record that changed after the list read stays as today) | `BandImportTest.AT03 ...` | fixer search |
| W4.7 | session row and interval logs built before the lock (`newId`, `intervalHeartRates`) | kept: made only from the draft and the exercise (W4.5, W4.6), no Room value | `BandImportTest.D7a AT09 ...` | fixer search |
| W5.1 | AT07a: 500 ms wait as proof | fixed: arrival signal and undispatched start, no sleep | `ImportAdmissionTest.AT07a D7c a timer commit that reached the lock ...` (B3) | R4 deep review F2 |
| W5.2 | AT05 and AT09: batch helper instead of the screen | fixed: screen test drives Import all and the single import of the sheet, asserts the list read count and the kept draft | `ImportScreenTest.AT09 D7a ...`, `ImportScreenTest.AT05 ...` (B5) | R4 deep review F2 |
| W5.3 | AT10: same open in-memory database | fixed: `KhonDatabase.build` file closed and opened, fill through `HeartFill.foreground()` (the `MainActivity.onStart` entry), old draft replayed | `BandImportTest.AT10 after a restart of the database file ...` (B8) | R4 deep review F2 |
| W5.4 | AT24: click loop that can run zero times, passes counted | fixed: requests counted at the call (`HeartFill.asked`), Check again and Allow clicked and asserted, permission result dispatched by a test registry, screen recomposed by an exercise rename | `ImportScreenTest.D18 AT24 ...` (B5, B6, B7) | R4 deep review F2 |
| W6.1 | added comments in `BandCover.kt`, `Dao.kt` (also the detached old comment at base `Dao.kt:333-336`), `HeartRate.kt`, `Repository.kt`, `TimerCommit.kt`, `HealthImport.kt`, `ImportScreen.kt`, `BandImportTest.kt`, `ImportAdmissionTest.kt`, `ImportOpenFillTest.kt` | fixed: removed, or the old text set back where the first change rewrote it | `comment-guard --base 79531bd` exit 0 | R4 deep review F3 |
| W6.2 | `timer/TimerSnapshot.kt:137-140` (KDoc of `resultPending`) | kept: the task allows only Scope 1 changes in the `timer` package, and Scope 1 does not need this line | `comment-guard --base 79531bd` exit 0 | fixer search |
| W7.1 | old rows and attack cases of HR01-P2-import | fixed: changed assertions listed in row W7 | Tests command, 839 tests 0 failures | task file |
| W8.1 | rules: timer lines, layout, texts, Room, dependency, version | kept: no change | `git diff --stat` empty | task file |

Deviations:

- deviation: `TimerSnapshot.kt:137-140` comment kept. Scope 3 says every file, the rules say only Scope 1 changes in `timer`. The rule won.
- deviation: `ImportScreen` became `internal` with an optional `source: ImportSource = HealthConnectImport` (`ImportScreen.kt:40-54`). This is the test seam for the screen steps. No layout or text change.
- deviation: `HeartFill.asked` (`HeartFill.kt:52-53,64`) counts fill requests at the call. Production code for a test count, like the existing `passes`.
- deviation: the sheet's exercise row is clicked with the semantics `OnClick` action. With `performClick` the Robolectric harness did not reach the row's `onClick` (the sheet stayed open, the list read count stayed 1): `androidx.compose.ui.test.ComposeTimeoutException: Condition still not satisfied after 5000 ms`. The draft row tap that opens the sheet and "Import all" use `performClick`.
- deviation: the screen test's wait idles the main looper (`ShadowLooper.idleMainLooper()`) in its condition. Without it, "Import all" stopped after its first saved draft (only `ok2` saved, list read count 1) and the wait failed with the same `ComposeTimeoutException`.
- deviation: `ImportOpenFillTest.kt` is replaced by `ImportScreenTest.kt` (D18 AT24 plus the AT05 and AT09 screen tests).
- deviation: W1 and W2 have no fail run of the new tests on `79531bd` (the base lacks `HeartFill.asked` and `ImportSource`, so the test set does not compile there). The proof on the base is the lead's witness run (`reproduces`); B1 and B2 break each read on the branch.
