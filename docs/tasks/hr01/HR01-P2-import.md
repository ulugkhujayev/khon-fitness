# HR01-P2-import: band workouts covered by app sessions, and the one-time merge of old pairs

Task-ID: HR01-P2-import
Class: Risky
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01
Base: 9d1ba17
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01/docs/tasks/hr01/HR01-P2-import-REPORT.md

## Why

See `docs/tasks/hr01/HR01-COMMON.md`, section "Band workouts (phase 2)".
Read COMMON first. Phase 1 (`HR01-P1-data.md`) is already on this branch:
`hrWindow`, `session_hr`, and `fillHeartRate` exist. The coverage list is
`HR01-COVERAGE.md`; the attack cases are in `HR01-ATTACK.md`, and each one
that names this phase is a test you write.

## Scope

Phase 2 of 3. Logic only. The screens change only as far as they must to
call the new functions; phase 3 does the visible parts.

- A pure function `covered(draft, sessions…)` next to `hrWindow`, used by
  both `HealthImport.drafts` and `HealthImport.pendingCount`.
- Repository function for "Add to": attach a band workout to an app cardio
  session (window override, fill at once, record ids in `sourceId`).
  Decide where the window override lives so that `hrWindow` stays pure:
  the `session_hr` row's `fromMs` and `toMs` written first in state
  `waiting`, with one more state value or column that marks the range as
  fixed by the user. State the choice in the report.
- The one-time merge of old pairs, its DataStore flag, and its safety file.
- `fillHeartRate` also runs when the Import screen opens.
- `SPEC.md`: the Import and Backup sections.

Do not change: layouts, strings on screen other than what a compile needs,
the version number. Do not add a dependency.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| D1 | Covered | A band bike workout 18:00 to 18:40; an app 4x4 session with live window 18:02 to 18:37 | The workout is covered: not in `drafts`, not in `pendingCount`. |
| D2 | Covered | A band strength workout over a gym session; a band strength workout with no gym session | Both stay out of the Import list, as today (strength is never a draft). The strength count line still counts only what it counts today, minus covered ones. |
| D3 | Covered | Overlap is 40 % of the shorter range; the app session is backdated (no window); the app session is a different day | Not covered: the workout stays a draft. |
| D4 | Covered | A band run that overlaps a gym session by more than half | Covered (any app session counts, gym or cardio). |
| D5 | Covered | The covering app session is deleted | The workout is a draft again on the next read. |
| D6 | Add to | Candidates for a draft | The app's cardio sessions whose `date` is the draft's date and that have no `session_hr` row in state `filled`. Gym sessions and sessions with `sourceId` are not candidates. |
| D7 | Add to | Pick a session | Its `session_hr` range becomes the workout's range and is filled at once from the draft's samples; `sourceId` holds the draft's record ids; `avgHr`, `maxHr`, `distanceM` fill only where null; interval rounds without stored times get no per-round value. The draft is gone on the next read. A second tap writes nothing. |
| D8 | Add to | The session was deleted, or got a `sourceId`, before the write | Nothing is written and the caller gets a refusal it can show. |
| D9 | Old pairs | Three pairs: same exercise; two exercises of the same type; exercises of different types | The first two merge, the third does not. |
| D10 | Old pairs | Merge of one pair where the app session has `avgHr` null, a typed `distanceM`, an empty note, and 4 interval rows with round 2 typed | It takes the copy's `avgHr`, keeps its own `distanceM`, takes the copy's note unless that note starts with "From ", fills rounds 1, 3, 4 from the copy and keeps round 2. The copy and its interval logs are deleted. Each pair is one transaction. |
| D11 | Old pairs | The app session has no live window, or the overlap is under half of the shorter range | No merge. |
| D12 | Old pairs | Safety file | Before the first delete, one file from `exportJson` is in the app's private files folder. No pair, no file. A failure to write the file stops the pass before any delete, and the pass runs again at the next start. |
| D13 | Old pairs | Run twice; process dies between two pairs | The flag is set only after the whole pass; a second run after the flag does nothing; a rerun after a death merges the rest and deletes nothing twice. |
| D14 | Old pairs | After the pass | The merged watch workouts do not come back as drafts (they are covered by the app session's window), and History shows one session per workout. |
| D15 | Fill | Import screen opens | A fill starts, under the same single-run rule as phase 1. |
| D16 | Rules | Whole change | No new permission, no new dependency, no network call. `SPEC.md` updated as in Scope. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- Every attack case in `HR01-ATTACK.md` for this phase is a test that passes.
- The `Tests:` command passes on the final commit. Commit on branch `hr01` (no push), messages hold only the change.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| D1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
