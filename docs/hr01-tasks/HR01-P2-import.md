# HR01-P2-import: band workouts that an app session already covers

Task-ID: HR01-P2-import
Class: Risky
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01
Base: 9d1ba17
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01/docs/hr01-tasks/HR01-P2-import-REPORT.md

## Why

See `docs/hr01-tasks/HR01-COMMON.md`, section "Band workouts (phase 2)".
Read the whole file first. Phase 1 is already on this branch: read its
report `docs/hr01-tasks/HR01-P1-data-REPORT.md` and use its functions
(`hrWindow`, `fillHeartRate`, the generation, `linkedIds`); do not write a
second copy of any of them. The attack cases at the end of this file are
tests you write.

## Scope

Phase 2 of 3. Logic only; the Import screen keeps its layout.

- One pure function decides "covered" for a band record; `drafts()` and
  `pendingCount()` both use it, before the existing grouping, on local
  rows read in one Room read transaction.
- `linkedIds` count as imported everywhere `sourceId` does for drafts.
- `importDraft` returns what happened (saved, already imported, covered,
  timer result pending) and checks inside its write. It is refused
  while an ended timer run still waits for its commit. `ImportScreen.kt` reads the list again
  after a refusal, also for "Import all"; no new text.
- Old double sessions are not in this phase. A later task (HR02) builds
  their list and the owner's merge; this phase writes nothing into
  `linkedIds` and deletes no session.
- A fill request when the Import screen opens.
- `SPEC.md`: the import section. Load the `unslop` skill first.

Do not change: the exercise choice of a draft, "Import all" as a feature,
band strength records staying hidden, any layout, the version number, the
manifest. No "Add to" (cut from this task). No new dependency. What the
import does today with a band record or an exercise that changed after
the list was read stays as it is (lead decision).

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| D1 | Covered | A 40-minute band run; an app run session with a run range that overlaps 36 minutes of it, finished, and also unfinished (timer ended, form not saved) | Covered in both: not in `drafts()`, not counted by `pendingCount()`. |
| D1a | Covered | An app run made at 23:59, timer 00:01 to 00:21 with clocks in step, saved without a date edit; the band's run 00:00 to 00:22 | The run range exists and covers the record, also at the import write check. |
| D2 | Covered | The same band run against: a gym session over the whole time; an app bike session; an app run without a run range; an imported run | Not covered in each case: the draft stays. |
| D3 | Covered | Overlap exactly half of both ranges; half of the record and 40 % of the session; a 2-minute record inside a 60-minute session of the same modality; a record that crosses midnight | Half of both: covered. Each of the two partial cases: not covered. Midnight: decided on epoch ranges, covered when the rule holds. |
| D4 | Covered | A record with unknown modality; a band strength record | Unknown: never covered, stays a draft as today. Strength: hidden as today. |
| D5 | Grouping | Two overlapping records of two packages for one workout, one covered and one not; three records in a chain; two records that are not covered alone while their union would be | The check runs on each original record. Covered records leave before grouping; the rest group as today; the union never hides a group; list and count agree; order of input does not change the result. |
| D6 | Linked ids | A record whose id is in some session's `linkedIds` (written by a test; ids `id1` and `id10` in one group, `id1` linked) | Never a draft, also when no session covers it. Ids match as whole tokens: `id10` stays available. |
| D6a | One snapshot | A Room change replaces a covering app run with an import that claims the same band id while a list read or a count read is in progress | Each read takes the claimed ids, the session ranges, and the modalities of their exercises in one Room read transaction. With either committed state the record is not offered. |
| D7a | Failed write | The session insert of an import succeeds and its round insert fails; single import, and "Import all" with two valid drafts after the failing one | Nothing of the failed draft is saved: no session, no rounds, no claimed id, no fill request. The screen stays open and reads the list again; the two other drafts save; the failed draft is offered again and a new try saves it once. |
| D7b | Pending timer result | A timer run of a cardio session has ended and its result is not yet in Room (held, or failed and waiting for the retry); the owner submits a band draft. The same while a timer still runs | Pending: the import writes nothing and returns "timer result pending"; the draft stays. After the result commits, the write check decides on fresh rows (covered or saved). A timer that still runs does not block the import (accepted: lead decision). |
| D18 | Fill | The Import screen opens | One fill request through the runner of phase 1. |
| D20 | Rules | Whole change | No layout change, no new text on the Import screen, no new permission or dependency, no manifest change. `SPEC.md` updated. |

## Attack cases

From attack round 2 of this phase, with the lead's decisions applied. A
case that names a held call uses a hook or a fake that the test controls.

| ID | Steps | Expected result |
|---|---|---|
| P2-AT01 | Create a run at 23:59. Run the timer from 00:01 to 00:21. Read a draft for 00:00 to 00:22 while the result waits. Commit the timer result. Leave the form unsaved, read list and count again, then submit the old draft. | The committed range covers the record before Save. Both reads exclude it. The old draft returns covered and writes nothing. |
| P2-AT02 | Open a group for runs from 18:00 to 18:20 and 18:15 to 18:45. Commit an app run from 18:00 to 18:20. Submit the old group. Import the group offered after refresh. | The old group returns covered and writes nothing. The refresh offers only the second member. A fresh count gives one. The retry stores 18:15 to 18:45 and claims only that member. |
| P2-AT03 | Feed 18:10–18:35, 18:20–18:25 and 18:30–18:40 to both readers in all six orders. Repeat with two records that start together. For the union check, put 18:00–18:15 and 18:10–18:26 against an app run from 18:00 to 18:40. | List group membership stays stable. The count equals the number of visible groups. The union cannot hide its members. A successful grouped import stores the earliest start and latest end of its members. |
| P2-AT04 | Open a group with IDs `id1` and `id10`. Give an unrelated session the token `id1` in `linkedIds`. Submit the old group. Refresh and retry. Repeat with `id1` in the `sourceId` of a finished import. | The old draft returns already imported. Only `id10` remains eligible and it saves once. Neither attempt changes the existing claim. |
| P2-AT05 | Open three drafts. Commit a covering run for one draft, then use Import all. Replay the successful drafts. In a separate run, submit one eligible draft twice at once. | The stale draft writes nothing. Both other drafts save with the band bounds. The list refreshes. Each replay returns already imported. The simultaneous submissions produce one save and one already-imported result. |
| P2-AT08 | Read a band run against separate fixtures with exact half overlap, one side below half, a 2-minute record inside a 60-minute run, and two app runs whose combined overlap alone passes. Include a gym session, a bike session, an imported run, and an app run without a valid range. | Coverage requires one eligible app run that overlaps at least half of both ranges. Equal endpoints alone give no overlap. The list, count and write use the same decision. |
| P2-AT09 | Fail one eligible draft's interval insert after its session insert. Try single import and Import all in separate runs. In the batch, place two valid drafts after the failed one. Remove the failure and retry from the refreshed list. | The screen stays open. The failed draft leaves no session, rounds, claim or fill request. Both valid drafts save. The refreshed list offers the failed draft, and the retry saves it once. |
| P2-AT10 | End the process after a recent import commits, before its fill request. Restart with a readable HR source. Open Import and replay the old draft in the repository test. | The commit and ID survive. The phase 1 foreground or screen-open request can fill the stored bounds. The replay creates no second import or fill request. |
| P2-AT11 | Commit a covering app run. Archive its exercise and read again. Then move the session to an unrelated date and read again. | The archived exercise still gives the run its modality. The date edit removes its eligible range and the band record appears in both reads. |
| P2-AT12 | Hold a list read between its local inputs. Atomically replace a covering app run with an import that claims the band ID. Resume the read. Repeat for the count. | A result based on either committed state excludes the record. Neither read can combine the old ID set with the new range set to offer it. |
| P2-AT24 | Count fill requests with a fake runner. Open Import, recompose, and refresh permissions in the same visit. Save a draft and resubmit it. Leave and open Import again. | Each visit requests one fill. A successful save requests one after commit. Recomposition, permission refresh and the refused repeat make no extra request. |
| P2-AT07 | Open Import before notification Stop. Stop a trusted covering run and hold its result before the write. Submit the draft before the release. Repeat after startup restores an ended snapshot. In separate runs, fail the timer write once, or submit while the timer still runs. | While the result is pending the import writes nothing and returns "timer result pending". After the result commits, a new submit is refused as covered. After a failed timer write the draft stays available. An import while the timer still runs can save. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- Every attack case above is a test that passes.
- A test for new behavior fails on the phase 1 tip (or does not compile there) and passes on the branch.
- The `Tests:` command passes on the final commit. Commit on branch `hr01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row
and per attack case:

| # | Status | Check ran | Real output |
|---|---|---|---|
| D1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
