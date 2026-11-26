# HR01-P1-fixAB report

Branch `hr01-fixab`, base `407a906`, fix commit `84791cc` (`Keep ended timer results in a pending store and restore runs by elapsed time`). No push, no merge. The trunk was not merged into the branch: the task says the lead merges.

Tests command on `84791cc` (ran): `JAVA_HOME=... android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease` gave `BUILD SUCCESSFUL in 1m 53s`, `71 actionable tasks: 27 executed, 22 from cache, 22 up-to-date`, exit 0. The XML results: 79 suites, 739 tests, 0 failures, 0 errors. Timer suites: TimerCommitTest tests="17", TimerRunTest tests="13", TimerServiceTest tests="4", TimerSnapshotLockTest tests="5", TimerSnapshotTest tests="10", TimerStateTest tests="12", all failures="0".

Files changed outside the `timer` package: none. Only `timer/TimerSnapshot.kt`, `timer/TimerService.kt`, `timer/TimerRun.kt` and tests in `test/.../timer/`.

## Contract

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: `finish` publishes the end on Main and only when `sameRun(state.value, done)` (session, runId, run number), `TimerService.kt:340`. The commit of R1 settles only R1's pending entry (`settle`, `isRunOf`). | F, `TimerServiceTest.C15 a held older run that the service ended...`; `witness run AB-F1.json --sha 84791cc` | tests="4" failures="0"; `WITNESS AB-F1 fixed at=84791ccdbb89 receipt=r-20261126212722-3da342` |
| X2 | done: one lock (`TimerSnapshots.lock`) over save, end, load, clear, clearRun, settle, clearAll, pending and exists (`TimerSnapshot.kt:93-160`). A cleanup held after its read keeps the save of a newer run waiting. | F, `TimerSnapshotLockTest.X2 a save between the comparison...`, `X2 a result that ends between the comparison...` | tests="5" failures="0" |
| X3 | done: the stretch cleanup is now `clearRun(app, s)` (`TimerService.kt:460`); `clear(sessionId)` and the removal of an invalid slot in `load` run under the lock. | F, `TimerSnapshotLockTest.X3 the cleanup after a stretch run and the removal of an invalid snapshot...` (three cases) | tests="5" failures="0" |
| X4 | done: an ended cardio run goes to the pending store (`save` routes it to `endLocked`); Ready, Stop on Ready (no result) and another timer write only the live slot. `restore()` commits every entry; `finish` commits every entry after a run with a result. | F, `TimerServiceTest.X4 a failed result stays pending...`, `X4 Ready and Stop on Ready leave a pending result alone, and the next run's end commits it`; `witness run AB-F2.json` | tests="4" failures="0"; `WITNESS AB-F2 fixed at=84791ccdbb89 receipt=r-20261126212755-d00d0e` |
| X5 | done: `commitPending` commits each entry through `endTimerRun`; only a settled outcome removes it. | F, `TimerCommitTest.X5 results refused for good leave the pending store...` | `[RETRY, GONE, DROPPED, DROPPED]`, then `[COMMITTED]`; tests="17" failures="0" |
| X6 | done: `ServiceTimerControl.clear` calls `clearAll` (live slot and pending store) after the write. Failure: the entry stays, RETRY keeps it, one commit after restart. | F, `TimerCommitTest.AT19 a successful Replace or reset...`, `C17 AT19 a failed Replace or reset...` (now assert `pendingIds()`) | tests="17" failures="0" |
| X7 | done: `TimerSnapshot.savedElapsed`; `advancedAt` advances by elapsed time when the run is in step or the save pair is in step; without `savedElapsed` the run is `untrusted()` and the wall gap is used (`TimerSnapshot.kt:30-38`). | F, `TimerRunTest.X7 a trusted restore places every boundary...` (+1.5 s and -1.5 s, exact `RoundSpan` lists), `X7 a snapshot without the elapsed value...`; `witness run AB-F3.json` | tests="13" failures="0"; `WITNESS AB-F3 fixed at=84791ccdbb89 receipt=r-20261126212811-28c97f` |
| X8 | deviation: Part B tests pass, but three TimerRunTest lines changed for AB-F3, not for the pending store: `TimerSnapshot(inRound1/paused/beforeReboot, wall)` now passes the elapsed value (C12 x2, C13 reboot). Without it the lead's AB-F3 rule makes the snapshot untrusted. Pending-store changes: C15 `snapshot()?.runId == r2` to `pendingIds() == [r2]`; C16 and C17 AT19 `snapshot()?.runId == r1` to `pendingIds() == [r1]`; `commitPending` returns a list (`COMMITTED` to `[COMMITTED]`, `NOTHING` to `[]`); C16a AT26 builds its snapshot with the elapsed value; added `pendingIds()` empty checks in C14, C16, AT19. | F, full Tests command | 739 tests, failures 0 |
| X9 | done: no file outside `timer`; no Room entity, migration, text, dependency or version change. | `git diff --stat 407a906 84791cc -- android` | 7 files, all under `.../timer/`; 634 insertions, 51 deletions |

C15 and C17 run through the real service under Robolectric (`TimerServiceTest`). C15 ends R1 and R2 through the service loop's last second. C17 pauses the service in work round 1, fails the Replace, and delivers the restore intent that `ServiceTimerControl.resume` started to a new service; the same runId, run number, index, remaining time and round times come back. Limit: under Robolectric the elapsed clock is fake and the wall clock is real, so service runs are untrusted; trust is tested in TimerRunTest and TimerCommitTest.

## Coverage

| # | Instance | Status | Test | Found by |
|---|---|---|---|---|
| X1.1 | `finish`: publication of the end and `state = null`, by session id, on the IO scope (`TimerService.kt:340`) | fixed: Main, `sameRun` | TimerServiceTest C15; break B1 (session-id compare) fails C15 | review AB-F1 |
| X1.2 | `recordRun` cleanup after the commit (`TimerService.kt:466`) | fixed: `settle` of this run's entry plus `clearRun` | TimerCommitTest C14, C15; break B8 (no settle) fails C14, C15 | review AB-F1 |
| X1.3 | `restore()` cleanup of a stale or expired live snapshot: `clearRun(app, snap.state)` | kept: names the run; now atomic | TimerSnapshotLockTest X2 | sibling search |
| X1.4 | `beginRun` state update after the number write | kept: checks runId on Main | TimerServiceTest C15 | sibling search |
| X1.5 | Delayed Stop during the Start write (`TimerService.kt:148`) | kept: Stop acts on the live timer; Stop and a new Start arrive in order on Main | TimerServiceTest X4 | sibling search |
| X1.6 | `noisyReceiver`, Pause, Resume, Skip, Back: check and update in one Main step | kept | TimerServiceTest C17 | sibling search |
| X1.7 | `TimerScreen.kt:76`, `StretchScreen.kt:94` match `finished` by session id | kept: the service now publishes only the live run's end | TimerServiceTest C15 | sibling search |
| X2.1 | `clearRun` read then remove | fixed: under the lock | TimerSnapshotLockTest X2; break B2 (new lock per call) fails X2 x2, X3; break B10 (session-id compare) fails C15 service test | review AB-F1 |
| X2.2 | Pending `settle` read-modify-write against `end` of another run | fixed: under the lock, matches session, run number and runId | TimerSnapshotLockTest X2 pending; break B9 (session-id match) fails C15 and the live-slot test | sibling search |
| X3.1 | Stretch cleanup `clear(app, sessionId)` in `recordTimer` | fixed: `clearRun(app, s)` under the lock | TimerSnapshotLockTest X3 cases 0 and 1 | review AB-F1 |
| X3.2 | Removal of an invalid live slot in `load` | fixed: under the lock | TimerSnapshotLockTest X3 case 2 | review AB-F1 |
| X3.3 | An ended stretch run stays in the live slot until its session is written | kept: design keeps stretch runs on their old path | TimerSnapshotTest, StretchDemoTest | sibling search |
| X4.1 | `finish` saved the ended cardio run in the live slot | fixed: `save` sends it to the pending store | TimerServiceTest X4; break B3 fails 7 tests (C15, C16, C17 AT19, X5, X4 x2, X2 pending) | review AB-F2 |
| X4.2 | `restore()` past the end saved the ended run in the live slot | fixed: same `save` routing | TimerCommitTest C16a AT26 | review AB-F2 |
| X4.3 | Ready save and Stop on Ready save over the slot | fixed: they write only the live slot; Stop on Ready triggers no pending commit | TimerServiceTest X4 second test; break B7 fails it | review AB-F2 |
| X4.4 | Commit of every entry after each run's end | fixed | TimerServiceTest X4 second test; break B6 fails it | task decision |
| X4.5 | Ended cardio run left in the live slot by the 407a906 build | fixed: `load` moves it to the pending store | TimerSnapshotLockTest older-build test | sibling search |
| X4.6 | `TimerSnapshots.exists` (start check) | fixed: true also when only results are pending | TimerSnapshotLockTest older-build test | sibling search |
| X5.1 | `commitPending` outcomes GONE, DROPPED (newer run, committed), RETRY | fixed: per entry, settled only | TimerCommitTest X5 | task decision |
| X6.1 | `replacingTimer` clear after success and resume after failure | kept: `clearAll` now clears both stores | TimerCommitTest AT19, C17 AT19 | task decision |
| X7.1 | `advancedAt` wall gap for a trusted restore | fixed: elapsed gap | TimerRunTest X7 drift; break B4 fails it | review AB-F3 |
| X7.2 | Snapshot without `savedElapsed` | fixed: untrusted, wall gap | TimerRunTest X7 older; break B5 fails it | review AB-F3 |
| X7.3 | `continueExisting` restore (same `advancedAt`) | fixed by X7.1 | TimerRunTest X7 drift | sibling search |
| X7.4 | `expired(now)` uses the wall clock | kept: a staleness limit, not a clock fact | TimerSnapshotTest | sibling search |
| X8.1 | Part B rows C9 to C17, C16a, C16b, AT2, AT3, AT19, AT20, AT26 | kept: pass, changed assertions listed in X8 | TimerCommitTest, TimerRunTest | task |
| X9.1 | Files outside `timer` | kept: none changed | `git diff --stat` | task |

Witnesses (ran, `ANDROID_HOME` exported): AB-F1 `fixed` r-20261126212722-3da342, AB-F2 `fixed` r-20261126212755-d00d0e, AB-F3 `fixed` r-20261126212811-28c97f. Each receipt log shows `> Task :app:testDebugUnitTest` and `BUILD SUCCESSFUL` (a `--tests` filter with no match fails the build). Earlier records at 407a906: `reproduces`.

Break proofs (ran, each on the worktree, then `git checkout` back to 84791cc): B1 to B10 as named above, each `exit=1` with the failing tests listed. B2 at first replaced `synchronized(lock)` with `run`, which did not compile; the run that counts uses `private val lock get() = Any()`.

RESULT {"status":"deviation","head":"84791ccdbb89"}
