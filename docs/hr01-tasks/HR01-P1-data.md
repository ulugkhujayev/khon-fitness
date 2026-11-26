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

- Room 5: the new columns and the `session_hr` table, the migration, the
  exported schema `android/app/schemas/.../5.json`.
- `checkedAt` on every set path.
- The timer run: `timedFrom`, `timedTo`, round times, the trusted anchor,
  `commitTimerRun`, the snapshot rule (`timer/TimerState.kt`,
  `timer/TimerService.kt`, `timer/TimerSnapshot.kt`).
- `hrWindow` as a pure function with no Android types.
- `HeartSource`: the interface, its Health Connect implementation in
  `health/`, a fake in tests.
- `fillHeartRate`, `rereadHeartRate`, `removeHeartRate`, the ownership
  marks, withdraw, reconcile; the triggers gym Finish, cardio Save, app
  foreground.
- Backup export and import of `session_hr`.
- `SPEC.md`: the data model and timer sections, and a new "Heart rate"
  section that states the rules of `HR01-COMMON.md` in the spec's own
  style. Load the `unslop` skill before you write it.

Do not change: `ImportScreen.kt`, `HealthImport.drafts` and `pendingCount`
(phase 2), any layout, the version number, the manifest. Do not add a
dependency or a background job. The old-pair merge is phase 2.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| C1 | Migration | A version 4 database with sessions, set logs, interval logs, and day overrides opens at version 5 through the app's own builder | Every old row reads back equal; the new columns are null; `session_hr` exists and is empty. Databases of versions 1, 2, and 3 also reach 5 with their rows. The test uses the exported schemas, the way the repo can run it under Robolectric. |
| C2 | Set check | Check a pending set in an unfinished session at T1; check it again at T2 | `status = done`, `checkedAt = T1` after both. The existing refusals (no reps, no weight) write nothing, also no time. Status and stamp change in one statement. |
| C3 | Set check | Check a set in a finished session (History edit) | `status = done`, `checkedAt` unchanged. |
| C4 | Set check | A done set becomes pending or skipped by any path; then it is checked again at T3 while unfinished | `checkedAt = null`, then T3. Finish turns pending sets to skipped and leaves every stamp of a done set. List each path in the report. |
| C5 | Set copies | Edit weight or reps of a checked set from the screen; swap an exercise; finish; start a new session prefilled from the last one; export and import | No path drops or rewrites `checkedAt`, `startedAt`, `endedAt` of an existing row. New rows start null. List every `SetLog` and `IntervalLog` write site in the report with how it keeps the times. |
| C6 | Timer | A full run of warmup, 4 work rounds, 3 rests at speed 1 | `timedFrom` is the run start, `timedTo` its end. Each round has start and end; end - start equals the work length within 1 s; rounds do not overlap and lie inside the run. |
| C7 | Timer | Pause 30 s inside round 2; Skip in round 3 work; Skip in a rest | Round 2's range includes the pause; later rounds shift. Round 3 ends at the Skip. The next round starts at the rest's Skip. |
| C8 | Timer | Stop in round 3; Stop in the warmup; Stop before the run counts | Rounds 1 and 2 complete, round 3 ends at Stop, round 4 null. Stop in warmup: no round times, `timedFrom` and `timedTo` set. Never counted: nothing written beyond today's behavior. |
| C9 | Timer | One tick crosses several boundaries after a restore, with a trusted anchor | Each boundary gets its own computed time; no two rounds share one; the sum matches the phase lengths. |
| C10 | Timer | The same with a wall clock jump, a reboot (elapsed-realtime smaller than the anchor's), a speed change, or a snapshot of the old app version | Every round touched by the gap has no times; rounds seen live afterwards have theirs; no crash; the old snapshot decodes. |
| C11 | Timer | Restore from a paused snapshot after 10 minutes | No time is consumed; the round continues; its range includes the pause. |
| C12 | Timer commit | Finish or Stop | One transaction writes the duration as today, `timedFrom`, `timedTo`, and all round times by index. A session deleted meanwhile: nothing written, snapshot cleared. A second run on the same session replaces all round times. Stretch runs unaffected. |
| C13 | Timer commit | The commit throws once; the app restarts | Nothing partial. The snapshot is still there; the next start commits once. A replay after the owner typed a new duration changes nothing. |
| C14 | Timer and import | Replace import or reset while a run is active or a finished snapshot waits | The timer stops and the snapshot is cleared before the write; no later command writes into the new data. |
| C15 | `hrWindow` | Every rule of "The time range of a session" | One table-driven test, a row per rule and per bound (just inside and just outside): unfinished; date moved; gym with stamps; a stamp outside the session; gym without any stamp; gym 9 and 10 minutes, 4 hours and longer; app cardio with and without a timer run; `timedTo + 60 s` past `finishedAt`; imported cardio; 2 minutes; 6 hours; a start after the end; midnight crossing with the start date. |
| C16 | Reader | Two pages; samples at `from` and at `to`; bpm 29, 30, 230, 231; exact duplicates; two packages; two samples of one package at one instant; page 2 throws | All pages read; `[from, to)`; bounds inclusive at 30 and 230; duplicates dropped; the package with more accepted samples wins (tie by name); `rawCount`, `sampleCount`, `origin`, `origins`, `firstMs`, `lastMs`, `maxGapSec` as designed; the throw is a failed read with no write. |
| C17 | Fill | 10 identical samples; 10 instants inside one minute of a 40-minute range; 10 instants over 20 minutes; 9 instants over the range | Only the third is complete. The others store samples and counts, state `waiting`, measured values null, and fill nothing. |
| C18 | Fill | A complete read of a gym session; the same with a typed `avgHr` | `filled`, 5-second buckets, `avgHr` and `maxHr` in the row; the session gets both, or only `maxHr` when `avgHr` was typed; marks as designed. Rounding is half up. |
| C19 | Fill | Interval cardio; round 2 has a typed `avgHr`; round 4 has one accepted instant; round 3 has no times | Round 1 gets its mean; 2 unchanged; 3 and 4 get nothing. Samples exactly at `endedAt` are outside. |
| C20 | Fill | Distance: exercise with and without the field; null and typed; no permission; the distance read throws; total 0 | Null takes the total of the same package when above 0; a typed value stays; no field, no permission, a throw, or 0: no distance, and the heart rate result commits. |
| C21 | Fill | Sparse now, complete one hour later; a range that ended 7 days ago exactly and just over; a failed read at day 8 | `waiting` then `filled`. At exactly 7 days still `waiting`; over: `none` after a sparse read. A failed read never changes the state. `none` is not read again by a pass. |
| C22 | Fill | Range end 30 days ago exactly and just over; range end in the future | Just over 30 days: not read by a pass; exactly: read. Future: not read. |
| C23 | Commit | During the read: the session is deleted; its date moves; a round time changes; replace import brings a session with the same id | Each result is dropped: no row, no value written. |
| C24 | Ownership | Reader fills `avgHr`; the owner types another value, or clears it; a new read arrives | The typed value or the blank stays; the mark is user; later reads never write it. A value the owner did not touch follows the new read, also to null. |
| C25 | Ownership | A session loses its range (date moved) after a fill | The pass withdraws: reader-owned values that still hold the reader's result become null, others stay, the row is deleted. |
| C26 | Explicit | `rereadHeartRate`: a failed read; a complete read; on an `off` row; on a session older than 30 days; on a session without a range | Failed: the old row and values stay. Complete: replaced. `off`: read again. Old: read. No range: returns that, writes nothing. |
| C27 | Explicit | `removeHeartRate` | Withdraw, state `off`, empty samples; no pass reads it. |
| C28 | Runner | A fill runs; Finish of another session and a cardio Save arrive; foreground twice within 10 minutes; the wall clock jumps one hour | One read at a time; both new sessions are read in the extra run; one foreground start; the limit uses elapsed real time; Finish and Save return before the read ends. |
| C29 | Provider | No Health Connect; no heart rate permission; heart rate permission without distance; the permission returns later | No write, no crash. Heart rate alone is enough. The next trigger after the permission returns fills. |
| C30 | Delete | Delete a session by each path; reset; replace import; an ordinary session upsert or edit | The HR row goes with the session and with reset and replace. An upsert keeps it. |
| C31 | Backup | Export and import as replace and as merge; a 0.6.1 file; an HR row without its session; a malformed HR row; two HR rows with one id | New columns and valid rows survive; the old file imports as before; bad and orphan rows are dropped and the import still succeeds; of duplicates the last wins; replace leaves no old row. |
| C32 | Backup | Merge an old file whose session row replaces a filled local session | Reconcile: the session's null values take the reader's results again and the marks are reader. |
| C33 | Rules | Whole change | No new permission, dependency, network call, or write to Health Connect. No manifest change. `SPEC.md` updated as in Scope. |

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
