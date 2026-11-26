# HR01-P1-data: clock times, the heart rate store, and the fill

Task-ID: HR01-P1-data
Class: Risky
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01
Base: 9d1ba17
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01/docs/tasks/hr01/HR01-P1-data-REPORT.md

## Why

See `docs/tasks/hr01/HR01-COMMON.md`. Read it first: it holds the decided
design and this file only adds the contract. The coverage list is
`docs/tasks/hr01/HR01-COVERAGE.md`; the attack cases are in
`docs/tasks/hr01/HR01-ATTACK.md` and each one is also a test you write.

## Scope

Phase 1 of 3. Data and engine only: no screen changes except what a
compile needs.

- Room 5: the new columns and the `session_hr` table, the migration, the
  exported schema `android/app/schemas/.../5.json`.
- `checkedAt` on the set check path.
- Work round times in the timer (`timer/TimerState.kt`,
  `timer/TimerService.kt`, `timer/TimerSnapshot.kt`) and their write to
  `interval_logs`.
- `hrWindow` as a pure function with no Android types.
- A small interface for the Health Connect reads (heart rate samples in a
  range, distance total in a range), its real implementation in `health/`,
  and a fake in tests.
- `fillHeartRate` and its three triggers that are not the Import screen:
  gym Finish, cardio Save of an unfinished session, app foreground (at most
  once per 10 minutes).
- Backup export and import of `session_hr`.
- `SPEC.md`: the data model and timer sections, and a new "Heart rate"
  section that states the rules of `HR01-COMMON.md` in the spec's own
  style.

Do not change: `ImportScreen.kt` behavior, `HealthImport.drafts` and
`pendingCount` (phase 2), any layout, the version number. Do not add a
dependency. Do not add a background job.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| C1 | Migration | A version 4 database file with sessions, set logs, interval logs, and day overrides opens at version 5 | Every old row reads back equal; the new columns are null; `session_hr` exists and is empty. A test proves it with a real version 4 schema, the way the repo can run it under Robolectric. |
| C2 | Set check | Check a set in an unfinished session at time T | `status = done`, `checkedAt = T`. The existing refusals (no reps, no weight) still write nothing, also no time. |
| C3 | Set check | Check a set in a finished session (History edit) | `status = done`, `checkedAt` unchanged (null stays null). |
| C4 | Set check | A done set becomes pending or skipped by any path that exists | `checkedAt = null`. List each path in the report. |
| C5 | Set copies | Swap an exercise, finish a session, export and import a backup; start a new session whose rows are prefilled from the last one (`programLogs`) | No path drops or rewrites `checkedAt`, `startedAt`, `endedAt` of an existing row. The rows of a new session start with `checkedAt` null. |
| C6 | Timer | A full run of warmup, 4 work rounds, 3 rests | Each work round has start and end; end - start equals the work length within 1 s at speed 1; rounds do not overlap. |
| C7 | Timer | Pause for 30 s inside round 2, resume | Round 2's range includes the pause; later rounds shift by 30 s. |
| C8 | Timer | Stop in the middle of round 3 | Rounds 1 and 2 complete, round 3 ends at the Stop time, round 4 has no times. |
| C9 | Timer | One tick that crosses several phases (restore after the process died for 10 minutes) | Each crossed boundary gets its own time, computed from the snapshot time and the phase lengths; no two rounds share one timestamp. |
| C10 | Timer | Finish or Stop | The service writes the ranges to `interval_logs` by session and index where it writes the duration; only those two columns change; a session deleted meanwhile writes nothing. A snapshot saved by the old app version still decodes. Stretch runs are not affected. |
| C11 | `hrWindow` | Each rule of "The time range of a session" in COMMON | One table-driven test with a row per rule, including: backdated, gym with and without `checkedAt`, gym longer than 4 h without checks, interval cardio, typed-in cardio (short form time against a long `timeSec`), unfinished with stopped timer, unfinished without time, shorter than 60 s. |
| C12 | Fill | A finished gym session, 300 samples in its window | `session_hr` row `filled` with 5-second buckets; `avgHr` and `maxHr` set from the raw samples. |
| C13 | Fill | The session already has a typed `avgHr` | `avgHr` unchanged; `maxHr` set; the row is `filled`. |
| C14 | Fill | Interval cardio with round times; round 2 has a typed `avgHr` | Rounds 1, 3, 4 get the mean of the samples inside their own range; round 2 unchanged. |
| C15 | Fill | Cardio whose exercise logs distance; `distanceM` null, then typed | Null takes the range total when above 0; a typed value stays. An exercise without the distance field never gets one. |
| C16 | Fill | 4 samples now, 300 samples one hour later | First run writes `waiting`; second run writes `filled`. A window that ended more than 7 days ago with fewer than 10 samples becomes `none` and is not read again. |
| C17 | Fill | The session is deleted while its read runs | Nothing is written. Deleting a session by any path also deletes its `session_hr` row. |
| C18 | Fill | Health Connect missing, or heart rate permission missing, or the read throws | No write, no crash, no state change. |
| C19 | Fill | Triggers | Gym Finish, cardio Save of an unfinished session, and app foreground each start a fill; foreground twice within 10 minutes starts one. A fill already running is not started twice. |
| C20 | Fill | A window that ended 40 days ago with no row | Not a candidate. |
| C21 | Backup | Export, then import as replace and as merge; import a backup made by 0.6.1 | `session_hr` rows and the new columns survive export and import; an old file imports as before; rows whose session is missing are dropped; replace leaves no old row. |
| C22 | Rules | Whole change | No new permission, no new dependency, no network call. `SPEC.md` updated as in Scope. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- Every attack case in `HR01-ATTACK.md` is a test that passes.
- A test for new behavior fails on `9d1ba17` and passes on the branch.
- The `Tests:` command passes on the final commit. Commit on branch `hr01` (no push), one commit per logical part, messages hold only the change.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| C1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
