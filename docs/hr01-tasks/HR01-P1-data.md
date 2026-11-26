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
| C2 | Migration | Rows: finished gym of 9 min, 10 min, 4 h, 4 h 1 min; unfinished gym; finished imported cardio; finished app cardio | `timedFrom`, `timedTo` = `startedAt`, `finishedAt` for the 10 min and 4 h gym sessions and the imported cardio. Null for every other row. Every other new column is null. |
| C3 | Set check | Check a pending set in an unfinished session at T1; check it again at T2 | `status = done`, `checkedAt = T1` after both. The existing refusals (no reps, no weight) write nothing, also no time. Status and stamp change in one statement. |
| C4 | Set check | Check a set in a finished session (History edit) | `status = done`, `checkedAt` unchanged. |
| C5 | Set check | A done set becomes pending or skipped by any path; then it is checked again at T3 while unfinished | `checkedAt = null`, then T3. Finish turns pending sets to skipped and leaves every stamp of a done set. List each path in the report. |
| C6 | Set copies | Edit weight or reps of a checked set from the screen; swap an exercise; start a new session prefilled from the last one; export and import | No path drops or rewrites `checkedAt`, `startedAt`, `endedAt` of an existing row. New rows start null. List every `SetLog` and `IntervalLog` write site in the report with how it keeps the times. |
| C7 | Gym Finish | Start 18:00, stamps 18:10 and 18:40, Finish 18:42; Finish 19:30; a stamp before `startedAt`; no done set; Finish called twice | `timedTo` = 18:42; = 18:45; the outside stamp is ignored; no done set: both null; the second Finish changes nothing. `timedFrom` = `startedAt`. |
| C7a | Gym clocks | Start 18:00; check at 18:08; the phone clock moves +40 min; check; Finish; the same with the clock moved back before Finish; a session without `startedElapsed`; a reboot between start and Finish | A check while the clocks are not in step is done with a null stamp. Finish with clocks not in step, or with no anchor: no range, and the session and its sets are saved as today. Clock moved back before Finish: the range ends at the last in-step stamp + 5 min. |
| C8 | Gym Finish | After Finish: uncheck the last set in History; edit reps; add a set | `timedFrom` and `timedTo` do not change. |
| C9 | Timer | A full run of warmup, 4 work rounds, 3 rests at speed 1 with fake clocks | `timedFrom` is the anchor wall time, `timedTo` the end. Each round has start and end; end - start equals the work length within 1 s; rounds do not overlap and lie inside the run. |
| C10 | Timer | Pause 30 s inside round 2; Skip in round 3 work; Skip in a rest | Round 2's range includes the pause; later rounds shift. Round 3 ends at the Skip. The next round starts at the rest's Skip. |
| C11 | Timer | Stop in round 3; Stop in the warmup; open the timer and wait 40 min on Ready, then Start from the screen and, in another run, from the notification; Stop on Ready | Rounds 1 and 2 complete, round 3 ends at Stop, round 4 null. Stop in warmup: no round times, `timedFrom` and `timedTo` set. Ready: the range starts at the Start action on both paths; Stop on Ready stores no run and no times. |
| C12 | Timer | The process dies in round 1 and restores 10 minutes later on the same boot, clocks in step; the same from a paused snapshot | Each crossed boundary has its own time from the phase lengths; the run stays trusted. Paused: no phase time consumed, the round's range includes the pause. |
| C13 | Timer trust | Wall clock +1 h during a live run with elapsed-realtime continuous; a wall jump during a pause; restore after a reboot (elapsed-realtime below the anchor); a snapshot of the old app version without anchors | Each run is untrusted: the commit writes the duration as today and null for `timedFrom`, `timedTo`, and every round time. No crash; the old snapshot decodes. A drift of 1 s stays trusted. |
| C14 | Timer commit | Finish or Stop of a trusted run | One transaction writes the duration, `timedFrom`, `timedTo`, and all round times by index. A session deleted meanwhile: nothing written, snapshot cleared. Stretch runs unaffected. |
| C15 | Timer commit | Run R1 ends and its write is held; run R2 starts for the same session and saves its snapshot; R1's write and cleanup are released; R2 ends | R1 never clears R2's snapshot. After R2's commit the row holds R2's times only and `timerRun` is R2. An R1 write that arrives after R2's commit changes nothing, also when R2 was untrusted and left the clock fields null and the owner typed a duration since. A replay of R2 changes nothing. |
| C16 | Timer commit | The commit throws once; the owner types another duration and saves; the app restarts | Nothing partial after the throw; the snapshot stays. The next start commits the clock fields once, keeps the typed duration, and clears the snapshot. |
| C17 | Timer and import | Replace import or reset while a run is active or an ended snapshot waits | The timer stops and the snapshot is cleared before the write; no later command writes into the new data. |
| C18 | `hrWindow` | Every rule of "The range of a session" | One table-driven test, a row per rule and per bound (just inside and just outside): unfinished; no `timedFrom`; `timedFrom >= timedTo`; date moved; midnight crossing with the start date; gym 9 and 10 min, 4 h and longer; app cardio `timedTo + 60 s` before and after `finishedAt`; imported cardio without the 60 s; 2 min; 6 h. |
| C19 | `hrFacts` | No range with a filled row; `off`; a row for the range in each state; no row; a filled row for another range | No range; removed; filled, waiting, none; not read; not read, and nothing of the stale row is exposed. |
| C20 | Round average | Buckets inside, at `startedAt`, at `endedAt`; one bucket inside; a round without times; a row that is not filled | The mean of the buckets in `[startedAt, endedAt)`, rounded half up; nothing for one bucket, no times, or a row that is not filled. |
| C21 | Reader | Two records that begin before `from` and end after `to` with samples inside and outside; two pages; samples at `from` and at `to`; bpm 29, 30, 230, 231; exact duplicates; two packages; two samples of one package at one instant; page 2 throws | Both crossing records contribute and their outside samples count only in `rawCount`; all pages read; `[from, to)`; 30 and 230 accepted; duplicates dropped; the package with more accepted samples wins (tie by name); `rawCount`, `sampleCount`, `origin`, `origins`, `firstMs`, `lastMs`, `maxGapSec` as designed; the throw is a failed read with no write. |
| C22 | Fill | 10 identical samples; 10 instants inside one minute of a 40-minute range; 10 instants over 20 minutes; 9 instants over the range | Only the third is `filled` with `avgHr` and `maxHr`. The others store samples and counts, state `waiting`, values null. |
| C23 | Fill | A complete read of a gym session and of a cardio session with typed `avgHr`, typed round values, and a typed distance | `session_hr` is written. The session row and every interval log are byte-equal to before. |
| C24 | Fill | Sparse now, complete one hour later; a range that ended 7 days ago exactly and just over; a failed read at day 8 | `waiting` then `filled`. Exactly 7 days: `waiting`; over: `none`. A failed read changes nothing. A pass does not read `none`. |
| C25 | Fill | Range end 30 days ago exactly and just over; in the future | Exactly: read. Just over, and future: not read by a pass. |
| C26 | Commit | During the read: the session is deleted; its date moves; a newer timer run changes `timedTo`; replace import brings a session with the same id and range; "Remove" runs; the phone's zone changes so that the session's date no longer matches | Each result is dropped: no row written or changed. After Remove the row is `off`. |
| C27 | Cleanup | A filled row whose session lost its range, or has another range now; the provider fails; between the pass's selection and its delete a merge import installs a row for the current range, or "Remove" sets the row `off` | The pass deletes the stale row without any read. An `off` row stays. The delete checks again in its transaction: the current row and the `off` row stay. |
| C28 | Explicit | `rereadHeartRate`: a failed read; a complete read; on `off`; on `none`; on a session older than 30 days; without a range; two calls at once | Failed: the old row stays. Complete: replaced. `off`, `none`, old: read. No range: returns that, writes nothing. Two calls: one read. |
| C29 | Explicit | `removeHeartRate` on a filled row; on no row | `off`, empty samples, null values, new `fetchedAt`; the session row is unchanged; no pass reads it. With no row: an `off` row is written when the session exists. |
| C30 | Runner | A fill runs; Finish of another session and a cardio Save arrive; foreground twice within 10 minutes; the wall clock jumps one hour | One read at a time; both new sessions are read in the extra run; one foreground start; the limit uses elapsed real time; Finish and Save return before the read ends. |
| C31 | Provider | No Health Connect; no heart rate permission; the permission returns later | No write, no crash. The next trigger after the permission returns fills. No distance permission is asked or used by the reader. |
| C32 | Delete | Delete a session by each path; reset; replace import; an ordinary session upsert or edit | The HR row goes with the session and with reset and replace. An upsert keeps it. |
| C33 | Backup | Export and import as replace and as merge; a 0.6.1 file; an HR row without its session; a malformed HR row; an HR row whose `state` is a JSON number; two HR rows with one id; an `off` row | New columns and valid rows survive, also `off`; the old file imports as before and its sessions have no range; bad, undecodable, and orphan rows are dropped and both the Restore preview and the import still succeed with every session and typed value; of duplicates the last wins; replace leaves no old row. |
| C34 | Backup merge | Merge a 0.6.1 file over local rows that hold `checkedAt`, round times, `timedFrom`, `timedTo`, `linkedIds`; the same with a file row whose `startedAt` differs | Null in the file never erases the local clock fields or `linkedIds`. With another `startedAt` or `finishedAt` the local `timedFrom` and `timedTo` are cleared. Replace takes the file as it is. |
| C35 | Rules | Whole change | No new permission, dependency, network call, or write to Health Connect. No manifest change. No write of `sessions.avgHr`, `sessions.distanceM`, or `interval_logs.avgHr` by new code. `SPEC.md` updated as in Scope. |

## Attack cases

<!-- ATTACK-CASES -->

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
