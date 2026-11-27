# HR01-P1-fixAB2 report

Branch `hr01-fixab`, base `fbe803d`. Commits: `f8be8b1` (`Stage ended timer runs durably and keep late commands on their run`) and `8d49409` (`Test the end order of one session's results and the restore instant of a paused run`). No push, no merge. I did not merge a trunk into the branch, because the task says the lead merges.

Tests command on `8d49409` (ran): `JAVA_HOME=... android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease` exited 0 with `BUILD SUCCESSFUL in 1m 52s` and `71 actionable tasks: 11 executed, 3 from cache, 57 up-to-date`. The XML results show 79 suites, 753 tests, 0 failures, 0 errors. The timer suites: TimerCommitTest 20, TimerRunTest 15, TimerServiceTest 11, TimerSnapshotLockTest 7, TimerSnapshotTest 10, TimerStateTest 12, all at 0 failures. `comment-guard --base fbe803d` exited 0.

Files changed outside the `timer` package: none. The diff `fbe803d..8d49409 -- android` has 8 files, all under `.../timer/`, with 583 insertions and 93 deletions.

## Contract

| # | Status | Check ran | Real output |
|---|---|---|---|
| Y1 | done: Stop, Skip, Back, Pause and Resume go through `forLiveRun` (`TimerService.kt:219`). It captures the live run when the command arrives, waits for the Start write and for earlier queued commands, and acts only when `sameRunId` (session and runId, number ignored) still holds. The Start write checks `sameRunId` after its wait (`:240`). The Start guard is per run (`:235`), so B's Start no longer drops while A's write is held. `halt` cancels queued commands (`:252`). | F, `TimerServiceTest.Y1 a Stop given while A's Start write is held leaves timer B...` (loops Stop, Skip, Pause, Resume), `Y1 a Stop or a Pause given while the run's own Start write is held acts on that run once it has its number`, `Y1 timer B opened during A's held Start write begins with its own write`. A thread holds a Room write transaction to hold the Start write. | TimerServiceTest tests="11" failures="0" |
| Y2 | done: `PendingRun.sameSlot` matches session and runId (`TimerSnapshot.kt:67`). Staging appends and replaces only the same run in its place (`:124`). `commitPass` goes in list order (the order of the ends), and a session whose earlier entry did not settle skips its later entries in that pass (`TimerService.kt:533`). `drain` runs one at a time under a mutex (`:515`). The DAO's old-version rule then gives: the first commits the duration, the second is DROPPED. | F, `TimerServiceTest.Y2 two results without numbers for one session keep their own entries...` (A fails, B ends while A still fails), `Y2 a result without a number whose commit is in flight...` (A's commit held while B ends), `TimerSnapshotLockTest.Y2 two runs of one session without numbers keep one entry each...` | `[COMMITTED, DROPPED]`, session timeSec 5 (A's), timerRun null; all suites failures="0" |
| Y3 | done: `TimerSnapshots.stage` writes the move with `commit()` under the lock and returns its result (`TimerSnapshot.kt:116`). `endRun` stages on `Dispatchers.IO`, tries up to 5 times 500 ms apart, and drains only after a staged write (`TimerService.kt:483`). On failure the disk keeps the live snapshot. The move on Main (`save`, `apply`) stays, so a later Ready save of another run cannot overwrite an unstaged end in memory. | F, `TimerCommitTest.Y3 a run whose staging write never returned is still live after a restart and ends once at its real end`, `Y3 a failed staging write is tried again, and the run commits once`, `Y3 a Stop whose staging write returned survives a death before its commit`. A wrapped context drops the writes and makes `commit` return false. | after restart: timeSec 1560, timerDone 1, timedTo = start + 1563 s, a second pass gives `[]`; tests="20" failures="0" |
| Y4 | done: `advancedAt` sets `sameBoot = savedElapsed != null && elapsedMs >= savedElapsed`. Without it the run is `untrusted()` before the wall fallback (`TimerSnapshot.kt:33`). | F, `TimerRunTest.Y4 a restore at an elapsed value below the saved one is untrusted for good, also above the anchor` (9000 / 10000 / 9500, `inStep` with the anchor asserted true) | broken, preparation advanced 0.5 s by wall, from/to null, rounds empty, duration 1560 |
| Y5 | done: `TimerState.instant` is set by `tickAt` (both exits), `beginRun`, `skipAt`, Pause, Resume and the noisy pause. `save` stores `savedElapsed = instant` and `savedAt = now - (elapsed - instant)` (`TimerSnapshot.kt:105`). `advancedAt` returns the state at its own instant (`:37`). `runLoop` starts its tick clock from the state's instant (`TimerService.kt:282`). | F, `TimerServiceTest.Y5 a real service restore that saves after a slow foreground start, restored again at once...` (a Robolectric shadow adds 700 ms to `createNotificationChannel`), `TimerSnapshotLockTest.Y5 a save after a delay...`, `TimerRunTest.Y5 a restore stands at its own instant...` | rounds = `[RoundTime(1, A+3000, A+63000), RoundTime(2, A+93000, A+153000)]`, end A+153000, exact |
| Y6 | done: `restore` restores the live snapshot first. A run that ended runs `endRun` (stage, then drain). Every other exit runs one `drainPending` (`TimerService.kt:187`, `:210`). `drain` tries a RETRY again in the same drain, up to 3 passes, against the data that the import left. | F, `TimerServiceTest.Y6 a startup drain refused by the generation and a run that ended while the process was dead commit in one pass` (merge import while A's commit is held) | while held: pending `[A, B]` and no live slot; after: a and b timerDone 1, pending empty |
| Y7 | done: X1 to X9 and the Part B tests pass. One assertion changed: `TimerRunTest.the timed tick moves the timer exactly as the plain tick` now compares `copy(clock = null, instant = null)`, because the timed tick carries its instant and the plain tick does not. It also asserts `timed.instant == clocks.elapsed` while the run is not done. | F, full Tests command; `witness run` x3 at `8d49409` | 753 tests, 0 failures; AB-F1, AB-F2, AB-F3 `fixed` |
| Y8 | done: no file outside `timer`. No Room, text, dependency or version change. The tests use a new seam, `TimerService.wall: Clocks = SystemClocks` (`TimerService.kt:450`). The service reads every clock fact's wall time from it, and Y5 and Y6 set a wall clock that moves with Robolectric's fake elapsed clock. Robolectric keeps `System.currentTimeMillis()` real: a probe showed `wall=0 elapsed=60000` after `advanceBy(60 s)`. | `git diff --name-only fbe803d HEAD`; `comment-guard --base fbe803d` | only `.../timer/` files plus the two task docs; exit 0 |

## Coverage

| # | Instance | Status | Test | Found by |
|---|---|---|---|---|
| Y1.1 | Stop waits for the Start write, then finished the live run | fixed: `forLiveRun`, `sameRunId` | Y1 B-opened; break B1 fails it | verify AB-F1 |
| Y1.2 | Skip, Back, Pause, Resume given during a Start write | fixed: queued the same way, so Pause given during A's write pauses A once it has its number | Y1 both tests | sibling search |
| Y1.3 | The end of the Start write wait | kept: checks runId; now `sameRunId` | Y1 B-opened | sibling search |
| Y1.4 | Start of B dropped while A's write was held (one guard for all runs) | fixed: guard per run | Y1 B begins; break B11 fails it | sibling search |
| Y1.5 | `restore` publication after its IO wait | kept: acts only while `state.value == null` | Y6, C17 | sibling search |
| Y1.6 | Finish publication after the drain | kept: `sameRun` on Main | C15 service, witness AB-F1 | sibling search |
| Y1.7 | `teardown` stopSelf after 1.8 s | kept: `runLoop` cancels it for a new run | C15 service | sibling search |
| Y1.8 | Noisy pause, ACTION_BEGIN | kept: act at once in one Main step, no wait | TimerServiceTest | sibling search |
| Y2.1 | Pending key by session and number | fixed: by session and runId | Y2 x3; break B2 fails 8 tests | verify AB-F2 |
| Y2.2 | A direct commit at the end raced the pending order | fixed: no direct commit, the end stages and drains | Y2 in flight | verify AB-F2 |
| Y2.3 | Two drains at once | fixed: one mutex | Y2 in flight | sibling search |
| Y2.4 | A later entry of a session committed while an earlier one failed | fixed: per-session wait in a pass | Y2 A still fails; break B9 fails it | sibling search |
| Y2.5 | Re-staging one run moved it to the end | fixed: replaced in place | LockTest Y2; break B10 fails it | sibling search |
| Y2.6 | Two runs with empty runId (snapshots older than run ids) | kept: only one such snapshot can exist, and all newer runs get `newId()` | C13 C16b | sibling search |
| Y3.1 | The pending move used `apply` | fixed: `stage` with `commit()` off Main, retried | Y3 x3; break B3 fails 2, break B13 fails 1 | verify AB-F2 |
| Y3.2 | `clearAll` after a Replace or reset used `apply` (a stale entry could come back after a death) | fixed: `commit()`. It runs on the import's calling thread. | AT19, C17 AT19 | sibling search |
| Y3.3 | `settle`, `clearRun`, Ready save, Stop on Ready removal, and the older-build move in `load` use `apply` | kept: replays settle as DROPPED or GONE, and a lost removal only brings back a live or Ready slot | X5, C16 | sibling search |
| Y4.1 | Restore below the saved elapsed value stayed trusted | fixed | Y4; break B4 fails it | verify AB-F3 |
| Y4.2 | `inStep` with elapsed below the anchor, and the save pair check | kept: both already return false | C13 reboot | sibling search |
| Y5.1 | Collector save sampled elapsed after the tick | fixed: `save` uses `instant` | Y5 x3; break B5 fails 2 | verify AB-F3 |
| Y5.2 | Restore published after the foreground start, and the loop restarted at "now" | fixed: state at its own instant; `runLoop` starts from it | Y5 service; break B6 fails it | verify AB-F3 |
| Y5.3 | A paused or Ready restore kept the old instant | fixed: `advancedAt` sets `instant = elapsedMs` | TimerRunTest Y5; break B12 fails it | sibling search |
| Y5.4 | Resume and Pause | fixed: stamp the command's instant | Y1 Pause | sibling search |
| Y5.5 | `continueExisting` restore | fixed by Y5.2 (same `advancedAt` and `runLoop`) | X7 | sibling search |
| Y6.1 | Startup drained before the restore | fixed: restore first, then one drain | Y6; break B8 fails it | verify AB-F2 |
| Y6.2 | A restored end called only the commit of its own run | fixed: `endRun` is the one end path, live or restored | Y6 | verify AB-F2 |
| Y6.3 | RETRY left an entry for the next launch | fixed: the same drain tries it again, up to 3 passes | Y6; break B7 fails it | sibling search |
| Y6.4 | Restore exits (no snapshot, IO failure, stale, foreground refused, live) | fixed: each one drains once | Y6, X4 | sibling search |
| Y7.1 | Rows X1 to X9 and the Part B rows (C9 to C17, C16a, C16b, AT2, AT3, AT19, AT20, AT26) | kept: pass; one changed assertion (timed tick, instant), listed in Y7 | full Tests command, 753 tests | task |
| Y7.2 | Witnesses AB-F1, AB-F2, AB-F3 | kept: `fixed` at `8d49409` | witness receipts above | task |
| Y8.1 | Files outside `timer` | kept: none changed | `git diff --name-only fbe803d HEAD` | task |
| Y8.2 | Test seam `TimerService.wall` | kept: production default is `SystemClocks`; the seam is inside `timer` | Y5 service, Y6 | task |

Witnesses (ran with `ANDROID_HOME` and `AGENTS_TASKS` exported, `--sha 8d49409`): AB-F1 `fixed` r-20261126220401-c7d214, AB-F2 `fixed` r-20261126220421-7520c0, AB-F3 `fixed` r-20261126220433-85fa73. Each receipt log shows `> Task :app:testDebugUnitTest` and `BUILD SUCCESSFUL`, and each receipt JSON has `"exit": 0`.

Break proofs (ran, each on the worktree with the named suites, then `git checkout` of the file, and the tree was clean at the end). B1 forLiveRun without the run check fails Y1 B-opened. B2 old `sameSlot` fails 8 tests (Y2 x3, Y1, C15, X4 x2, Y6). B3 non-durable stage fails Y3 x2. B4 `sameBoot` without the elapsed check fails Y4. B5 sampled elapsed in `save` fails Y5 service and Y5 store. B6 loop from now fails Y5 service. B7 no RETRY pass fails Y6. B8 drain before restore fails Y6. B9 no per-session wait fails Y2 (after I made the trigger fail only A's duration, because the first version also failed B and did not catch B9). B10 append on re-stage fails LockTest Y2. B11 one Start guard for all runs fails Y1 B begins. B12 no instant in `advancedAt` fails TimerRunTest Y5 (after I added a paused restore, because the first version did not catch B12). B13 one staging try fails Y3 retry. Each break that counts exited 1.

Not done: a Y3 variant where the write never returns. `stage` holds the snapshot lock during `commit()`, so a commit that blocks in a test also blocks the restart's `load` in the same JVM. A real death releases the lock. The failing-write variant covers the same disk state.

RESULT {"status":"done","head":"8d49409e8d7d"}
