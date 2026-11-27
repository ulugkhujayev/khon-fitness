# HR01-P1-data: clock times, the heart rate store, and the fill

Task-ID: HR01-P1-data
Class: Risky
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01
Base: 9d1ba17
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01/docs/hr01-tasks/HR01-P1-data-REPORT.md

## Why

See `docs/hr01-tasks/HR01-COMMON.md`. Read it first: it holds the decided
design and this file only adds the contract. The coverage list is
`docs/hr01-tasks/HR01-COVERAGE.md`. The attack cases at the end of this
file are tests you write.

## Scope

Phase 1 of 3. Data and engine only: no screen changes except what a
compile needs and the three trigger calls.

- Room 5: the new columns and the `session_hr` table, the migration with
  its two range writes, the exported schema `android/app/schemas/.../5.json`.
- `checkedAt` on every set path; the range write at gym Finish.
- The timer run: anchors, trust, round times, `commitTimerRun`, the
  snapshot rule (`timer/TimerState.kt`, `timer/TimerService.kt`,
  `timer/TimerSnapshot.kt`).
- `hrWindow`, `runRange`, `hrFacts`, and the round average as pure
  functions with no Android types.
- `HeartSource`: the interface, its Health Connect implementation in
  `health/`, a fake in tests.
- `fillHeartRate`, `rereadHeartRate`, `removeHeartRate`; the triggers gym
  Finish, cardio Save, app foreground.
- Backup export and import of `session_hr`, and the merge rule for clock
  fields.
- `SPEC.md`: the data model and timer sections, and a new "Heart rate"
  section that states the rules of `HR01-COMMON.md` in the spec's own
  style. Load the `unslop` skill before you write it.

The reader writes only `session_hr`. No code in this phase writes
`sessions.avgHr`, `sessions.distanceM`, or `interval_logs.avgHr`.

Do not change: `ImportScreen.kt`, `HealthImport.drafts`, `pendingCount`,
`importDraft` (phase 2), any layout or shown text (phase 3), the version
number, the manifest. Do not add a dependency or a background job.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| C1 | Migration | A version 4 database with sessions, set logs, interval logs, and day overrides opens at version 5 through the app's own builder | Every old column reads back equal; `session_hr` exists and is empty. Databases of versions 1, 2, and 3 also reach 5 with their rows. The test uses the exported schemas, the way the repo can run it under Robolectric. |
| C2 | Migration | Rows: finished gym of 9 min, 10 min, 4 h, 4 h 1 min; unfinished gym; finished imported cardio; finished app cardio | `timedFrom`, `timedTo` = `startedAt`, `finishedAt` for the imported cardio only. Null for every gym row and the app cardio. Every other new column is null. |
| C3 | Set check | Check a pending set in an unfinished session at T1; check it again at T2 | `status = done`, `checkedAt = T1` after both. The existing refusals (no reps, no weight) write nothing, also no time. Status and stamp change in one statement. |
| C4 | Set check | Check a set in a finished session (History edit) | `status = done`, `checkedAt` unchanged. |
| C5 | Set check | A done set becomes pending or skipped by any path; then it is checked again at T3 while unfinished | `checkedAt = null`, then T3. Finish turns pending sets to skipped and leaves every stamp of a done set. List each path in the report. |
| C5a | Set check | Check a skipped set directly at T2 in an unfinished session; check it again at T3; the same with a refused check (no weight) | Done with `checkedAt = T2`; T2 stays; the refused check leaves skipped and null. |
| C6 | Set copies | Edit weight or reps of a checked set from the screen; swap an exercise; start a new session prefilled from the last one; export and import | No path drops or rewrites `checkedAt`, `startedAt`, `endedAt` of an existing row. New rows start null. List every `SetLog` and `IntervalLog` write site in the report with how it keeps the times. |
| C7 | Gym Finish | Start 18:00, stamps 18:10 and 18:40, Finish 18:42; Finish 19:30; a stamp before `startedAt`; no done set; Finish called twice | `timedTo` = 18:42; = 18:45; the outside stamp is ignored; no done set: both null; the second Finish changes nothing. `timedFrom` = `startedAt`. |
| C7a | Gym clocks | Start 18:00; check at 18:08; the phone clock moves +40 min; check; Finish; the same with the clock moved back before Finish; a session without `startedElapsed`; a reboot between start and Finish | A check while the clocks are not in step is done with a null stamp. Finish with clocks not in step, or with no anchor: no range, and the session and its sets are saved as today. Clock moved back before Finish: the range ends at the last in-step stamp + 5 min. |
| C7b | Confirm range | `confirmRange` with the session's own bounds on: a finished gym session of 40 min without a range; one of 9 min; one of 4 h 1 min; one with a moved date; one that has a range; an unfinished one; a cardio session; a deleted id | First: `timedFrom`, `timedTo` = `startedAt`, `finishedAt`, and a fill is asked. Every other case: refused with its reason, nothing written. Bounds that are no longer the session's `startedAt` and `finishedAt` (a merge changed them after the screen read them): refused, no fill; a new call with the new bounds is accepted. |
| C8 | Gym Finish | After Finish: uncheck the last set in History; edit reps; add a set | `timedFrom` and `timedTo` do not change. |
| C9 | Timer | A full run of warmup, 4 work rounds, 3 rests at speed 1 with fake clocks | Start writes `timerRun = 1` (the next run 2). `timedFrom` is the anchor wall time, `timedTo` the end. Each round has start and end; end - start equals the work length within 1 s; rounds do not overlap and lie inside the run. |
| C10 | Timer | Pause 30 s inside round 2; Skip in round 3 work; Skip in a rest | Round 2's range includes the pause; later rounds shift. Round 3 ends at the Skip. The next round starts at the rest's Skip. |
| C11 | Timer | Stop in round 3; Stop in the warmup; open the timer and wait 40 min on Ready, then Start from the screen and, in another run, from the notification; Stop on Ready | Rounds 1 and 2 complete, round 3 ends at Stop, round 4 null. Stop in warmup: no round times, `timedFrom` and `timedTo` set. Ready: the range starts at the Start action on both paths; Stop on Ready stores no run and no times. |
| C12 | Timer | The process dies in round 1 and restores 10 minutes later on the same boot, clocks in step; the same from a paused snapshot | Each crossed boundary has its own time from the phase lengths; the run stays trusted. Paused: no phase time consumed, the round's range includes the pause. |
| C13 | Timer trust | Wall clock +1 h during a live run with elapsed-realtime continuous; a wall jump during a pause; restore after a reboot (elapsed-realtime below the anchor); a snapshot of the old app version without anchors | Each run is untrusted: the commit writes the duration as today and null for `timedFrom`, `timedTo`, and every round time. No crash; the old snapshot decodes. A drift of 1 s stays trusted. |
| C14 | Timer commit | Finish or Stop of a trusted run | One transaction writes the duration, `timedFrom`, `timedTo`, and all round times by index. A session deleted meanwhile: nothing written, snapshot cleared. Stretch runs unaffected. |
| C15 | Timer commit | Run R1 ends and its write is held; run R2 starts for the same session and saves its snapshot; R1's write and cleanup are released; R2 ends | R1 never clears R2's snapshot. After R2's commit the row holds R2's times only, `timerRun` and `timerDone` are R2's number. An R1 write that arrives after R2 started changes nothing, also when R2 was untrusted and left the clock fields null, when the owner typed a duration since, and when the phone clock went back between the two starts so that R1's wall times are later than R2's. A replay of R2 changes nothing. |
| C16 | Timer commit | The commit throws once; the owner types another duration and saves; the app restarts | Nothing partial after the throw; the snapshot stays. The next start commits the clock fields once, keeps the typed duration, and clears the snapshot. |
| C16a | Timer duration | A run starts with `timeSec` null and ends: commit. A process death, a restore that advances the run to its end, and its first commit held while the owner saves 25 minutes; then the commit is released. A run that starts with `timeSec` 1200 (an earlier run) and ends | First: the duration is written. Second: 25 minutes stays, the clock fields of a trusted run are written once. Third: the new duration replaces 1200, because nothing was typed since Start. |
| C16b | Old snapshot | A running snapshot of the old app version (no number, no anchor) is restored after the migration and ends; the same when the session has a `timeSec` or a `timerRun`; an old Ready snapshot gets Stop | First: the duration is written, every clock field stays null, `timerRun` stays null. Second: dropped, nothing written. Ready and Stop: nothing written. Each decodes without a crash. |
| C17 | Timer and import | Replace import or reset while a run is active or an ended snapshot waits; the same when the Room write of the Replace or reset fails | Success: the timer stops, the snapshot is cleared after the write, and no later command writes into the new data. Failure: the old rows and the snapshot stay, and the pending result commits once after a restart. |
| C17a | Timer and import | An ended run's commit call begins and is held; a Replace brings a session with the same id and run number; the commit is released. The same with a Merge | Replace: the commit is refused (generation), the imported rows are unchanged. Merge: refused once, the snapshot stays, and the retry at the next start commits once. |
| C18 | `hrWindow` | Every rule of "The range of a session" | One table-driven test, a row per rule and per bound (just inside and just outside): unfinished; no `timedFrom`; `timedFrom >= timedTo`; date moved; midnight crossing with the start date; a session made at 23:59 whose timer starts at 00:01 (range kept without a date edit); gym 9 and 10 min, 4 h and longer; app cardio `timedTo + 60 s` before and after `finishedAt`; imported cardio without the 60 s; 2 min; 6 h. |
| C19 | `hrFacts` | No range with a filled row; `off`; a row for the range in each state; no row; a filled row for another range | No range; removed; filled, waiting, none; not read; not read, and nothing of the stale row is exposed. |
| C20 | Round average | Buckets inside, at `startedAt`, at `endedAt`; one bucket inside; a round without times; a row that is not filled | The mean of the buckets in `[startedAt, endedAt)`, rounded half up; nothing for one bucket, no times, or a row that is not filled. |
| C21 | Reader | A record that crosses only `from`, another that crosses only `to`, and one that crosses both, each with samples inside and outside; two pages; samples at `from` and at `to`; bpm 29, 30, 230, 231; exact duplicates; two packages; two samples of one package at one instant; page 2 throws | Every crossing record contributes and their outside samples count only in `rawCount`; all pages read; `[from, to)`; 30 and 230 accepted; duplicates dropped; the package with more accepted samples wins (tie by name); `rawCount`, `sampleCount`, `origin`, `origins`, `firstMs`, `lastMs`, `maxGapSec` as designed; the throw is a failed read with no write. |
| C22 | Fill | 10 identical samples; 10 instants inside one minute of a 40-minute range; 10 instants over 20 minutes; 9 instants over the range | Only the third is `filled` with `avgHr` and `maxHr`. The others store samples and counts, state `waiting`, values null. |
| C23 | Fill | A complete read of a gym session and of a cardio session with typed `avgHr`, typed round values, and a typed distance | `session_hr` is written. The session row and every interval log are byte-equal to before. |
| C24 | Fill | Sparse now, complete one hour later; a range that ended 7 days ago exactly and just over; a failed read at day 8 | `waiting` then `filled`. Exactly 7 days: `waiting`; over: `none`. A failed read changes nothing. A pass does not read `none`. |
| C25 | Fill | Range end 30 days ago exactly and just over; in the future | Exactly: read. Just over, and future: not read by a pass. |
| C26 | Commit | During the read: the session is deleted; its date moves; a newer timer run changes `timedTo`; a Replace or a Merge import runs (also with a session of the same id and range); "Remove" runs; the phone's zone changes so that the session's date no longer matches | Each result is dropped: no row written or changed. After Remove the row is `off`. |
| C27 | Cleanup | A filled row whose session lost its range, or has another range now; the provider fails; between the pass's selection and its delete an explicit reread commits a row for the current range, or "Remove" sets the row `off` | The pass deletes the stale row without any read. An `off` row stays. The delete checks again in its transaction: the current row and the `off` row stay. |
| C28 | Explicit | `rereadHeartRate`: a failed read; a complete read; on `off`; on `none`; on a session older than 30 days; without a range; two calls at once | Failed: the old row stays. Complete: replaced. `off`, `none`, old: read. No range: returns that, writes nothing. Two calls: one read. |
| C29 | Explicit | `removeHeartRate` on a filled row; on no row | `off`, empty samples, null values, new `fetchedAt`; the session row is unchanged; no pass reads it. With no row and a range: an `off` row for that range. With no row and no range: nothing is written. |
| C30 | Runner | A fill runs; Finish of another session and a cardio Save arrive; foreground twice within 10 minutes; the wall clock jumps one hour | One read at a time; both new sessions are read in the extra run; one foreground start; the limit uses elapsed real time; Finish and Save return before the read ends. |
| C31 | Provider | No Health Connect; no heart rate permission; the permission returns later | No write, no crash. The next trigger after the permission returns fills. No distance permission is asked or used by the reader. |
| C32 | Delete | Delete a session by each path; reset; replace import; an ordinary session upsert or edit | The HR row goes with the session and with reset and replace. An upsert keeps it. |
| C33 | Backup | Export and import as replace and as merge; a 0.6.1 file; an HR row without its session; a malformed HR row; an HR row whose `state` is a JSON number; two HR rows with one id; an `off` row; a filled row with a sample of 999 bpm; one with an offset at the range's end; one with no sample; a `startedElapsed` in the file | New columns and valid rows survive, also `off`; the old file imports as before and its sessions have no range; bad, undecodable, and orphan rows are dropped and both the Restore preview and the import still succeed with every session and typed value; of duplicates the last wins; replace leaves no old row. The three rows with bad values are dropped in both import modes. `startedElapsed` is null after every import. |
| C34 | Backup merge | Merge over local rows that hold `checkedAt`, round times, `timedFrom`, `timedTo`, `timerRun`, `timerDone`, `linkedIds`, and an HR row: a 0.6.1 file; a file with other non-null values in each of those columns and another HR row; a file with a session that is new to this database with all of them | Existing rows: every local value and the local HR row stay in both cases. The new session takes the file's values and HR row. Replace takes the file as it is. |
| C34a | Backup merge | The file row of an existing session has another `startedAt`; another `finishedAt`; another `exerciseId`. The file row of a locally done and stamped set is pending; is skipped | The session loses `timedFrom`, `timedTo`, and its round times in the merge transaction; its HR row is stale, hidden by `hrFacts`, and cleaned by the next pass. The set has no `checkedAt`; a later check stamps the new time only. |
| C34b | Backup merge | Run 2 is started locally; merge a file that holds run 1's counters and bounds for that session, or none, or a committed run 2 of another history; then run 2 ends and its commit replays | The local counters stay. Run 2 commits once with its own times; the replay writes nothing; no bound or round time of the file appears. |
| C35 | Rules | Whole change | No new permission, dependency, network call, or write to Health Connect. No manifest change. No write of `sessions.avgHr`, `sessions.distanceM`, or `interval_logs.avgHr` by new code. `SPEC.md` updated as in Scope. |

## Attack cases

From attack round 8 (phase 1), with the lead's decisions applied. A case
that names a held call uses a hook or a fake that the test controls.

| ID | Steps | Expected result |
|---|---|---|
| AT1 | Start gym at 18:00. Check a set at 18:08. At true 18:10 move the wall clock forward 40 minutes. Check another set at true 18:15 and finish at true 18:17. Correct the clock. Offer readings from 18:24 to 18:54 and request a read. | Both checks and the session survive. The second check has no stamp. The session has no range or measured heart rate. |
| AT2 | Hold R1's first commit before the transaction. Start R2 on the same session. Change the clock during R2, end it, then save 25 minutes. Release R1 and replay R2. Repeat with trusted clocks during both runs, but set the wall clock back between them. | Both counters identify R2. Keep 25 minutes. R2's clock fields stay null in the first case and keep R2's verified values in the repeat. R1 cannot clear R2's snapshot or data. |
| AT3 | Open Ready at 18:00. Offer HR only from 18:03 to 18:35. Start from the notification at 18:40. Stop at 19:00 and save at 19:01. | The range starts at 18:40. No measured average or maximum appears. |
| AT5 | Read 18:00 to 18:30. Return records covering 17:59 to 18:12 and 18:18 to 18:31. Give 130 bpm each minute from 18:00 through 18:11 and 18:18 through 18:29. Add 200 bpm at 17:59 and 18:30. | Both records contribute. The result is filled. Raw count is 26, accepted count is 24, and average and maximum are 130. |
| AT6 | Export a session with a typed average and an HR row. Change the HR row's JSON state to a number. Preview and Replace from the file. | Preview and Replace succeed. The session and typed average survive. The invalid HR row is absent. |
| AT7 | Hold a complete read for a range starting at 00:15. Change the zone so both creation and range start fall on the prior date. Release the result. | The current zone invalidates the range. The result writes nothing. |
| AT18 | Export trusted run 1 before Save. Commit untrusted run 2 and merge the older file with the same session endpoints. Save and read facts. In a second fixture, restore the same run-1 backup in two databases. Finish run 2 in one. Merge that backup while a different run 2 is active in the other. End the local run. | First, keep run 2's counters, null range and null round times. Second, accept the pending local run's commit once and preserve its own verified clocks. |
| AT19 | End a run and fail its first commit. Fail a valid Replace inside its Room transaction. Repeat with reset. Restart after each rollback, then run the same operation successfully in another fixture. | After each rollback, the old rows and pending result survive. The result commits once after restart. A successful operation discards the old result. |
| AT22 | Read a gym session's possible confirmation range of 18:00 to 18:40. Merge the same ID with new bounds of 19:00 to 19:40. Confirm the first range. Then read and confirm the second range. | First confirmation refuses and triggers no fill. The second stores exactly its approved bounds and requests one fill. |
| AT25 | Check a live set at T1. Merge the same row as pending with a null stamp. Repeat with skipped. Return it to pending if needed and check again at T2. | The pending and skipped rows have no stamp. The new check stamps T2. T1 never returns. |
| AT26 | Run 20 minutes, then fail the result transaction. Save 25 minutes before restarting. Restore the ended snapshot and replay it. Repeat with a live snapshot that advances to Done on restore. Hold its first commit until after Save. | Both paths keep 25 minutes. The ended snapshot survives the first failure. The current run writes its verified clocks once and clears its own snapshot. |
| AT27 | Hold an ended run's commit. Replace with a file containing the same session ID and run number, but different facts. Release the result. Repeat by queuing Stop before Replace, starting a new timer afterward, then delivering the old Stop. | The replaced session, logs and timer state keep their imported values. Old work cannot write or remove a new snapshot. |
| AT29 | Start a read from a waiting row. Remove while the read waits, release it, and run an automatic pass. Then explicitly reread with a complete result. | The old read loses to Remove. The automatic pass skips the row. The new explicit read can replace `off` with filled. |
| AT30 | Block a provider read. Finish a second session and Save a third. Request foreground twice inside 10 minutes of elapsed time with a one-hour wall-clock step. Release the first read. | Finish and Save return while the provider is blocked. Reads stay sequential. The extra pass reaches both sessions. The wall-clock step does not refresh the foreground allowance. |
| AT31 | Start run 2, then merge a backup with no counters or with run 1 counters. End run 2 and replay it. Repeat after restoring a valid running snapshot on the same boot. | Run 2 stays current and commits once. Its range and every started round use the run's phase times. The replay writes nothing. |
| AT32 | Check a live set at T1, mark it skipped, and check directly at T2. Check again at T3. Repeat with missing weight at T2 for a set that requires weight. | Marking skipped clears T1. The successful check stamps T2 and the repeat keeps it. The refused check keeps skipped and a null stamp. |
| AT4 | Hold cleanup after it selects a stale filled row. Commit an explicit reread for the current range. Resume cleanup with a failed provider. | The current row stays filled and `hrFacts` returns its values. |
| AT17 | Export an unfinished gym session with its anchor. Import it as Replace and as Merge into an empty database. Check a set and finish. | `startedElapsed` is null after the import. The check has no stamp, Finish stores no range, and the session and its sets are saved. `confirmRange` then works. |
| AT20 | Restore a running snapshot of the old version and let it end; replay its end. Repeat on a session that already has a `timeSec`. Restore an old Ready snapshot and send Stop. | The duration is written once, with null clock fields and a null `timerRun`. On the session with a `timeSec`: nothing written. Ready and Stop: nothing written. |
| AT21 | Create Monday at 23:59. Start Tuesday at 00:01, stop at 00:21, and save Monday. Fill it, then edit Date to Tuesday, then to Wednesday. | Monday and Tuesday both keep the range and the measured facts. Wednesday: no range, no measured facts. |
| AT23 | Store a waiting row. Hold a read from it. Run a Merge import that holds another row for that session. Release the old result. Repeat with an explicit reread and inspect its return value. | The local row stays exactly (the merge does not replace it). Neither read commits: the generation changed. The explicit call reports that. |
| AT24 | Preview and import a filled row with samples at 999 bpm. Repeat with a valid bpm and an offset at the exclusive end. In another fixture, Remove on a finished gym session without a range; then on one with a range and no row; export, import as Replace and as Merge into an empty database. | Bad measured rows are dropped in both import modes; the session and typed values stay. Remove without a range writes nothing. The `off` row of the second session survives both imports and blocks the automatic read. |

## Build parts

Three builders do this task one after the other, each a fresh agent on
this branch. The prompt names your part. Do only its rows, read the
report of the parts before yours, and add your rows to the same report.
A case of your part that needs code of a later part: write the test with
a direct row write in place of that code, and say so in the report.

- **Part A, tables and gym.** C1 to C8 (with C5a, C7a, C7b), C18, C19,
  C20, C32, and AT1, AT21, AT22, AT32. Entities, DAO, migration 4 to 5,
  the verified clock helper, set stamps, gym Finish, `confirmRange`,
  `hrWindow`, `hrFacts`, the round average, deletion of HR rows with
  their sessions.
- **Part B, timer.** C9 to C17 (with C16a, C16b), and AT2, AT3, AT19,
  AT20, AT26. Run numbers, anchors, round times, `commitTimerRun`, the
  snapshot rules, Replace and reset against the timer. The generation
  counter starts here (the timer commit reads it).
- **Part C, reading and backup.** C17a, C21 to C31, C33, C34, C34a, C34b,
  C35, and AT4, AT5, AT6, AT7, AT17, AT18, AT23, AT24, AT25, AT27, AT29,
  AT30, AT31. `HeartSource`, the fill runner and its triggers,
  `rereadHeartRate`, `removeHeartRate`, backup export, Replace, and
  Merge. Part C also runs `task-result check` for the whole report.

Each part ends with the `Tests:` command green on its last commit.

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- Every attack case above is a test that passes.
- A test for new behavior fails on `9d1ba17` (or does not compile there) and passes on the branch.
- The `Tests:` command passes on the final commit. Commit on branch `hr01` (no push, no merge), one commit per logical part, messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row
and per attack case:

| # | Status | Check ran | Real output |
|---|---|---|---|
| C1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
