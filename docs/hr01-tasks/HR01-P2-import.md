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
  `pendingCount()` both use it, before the existing grouping.
- `linkedIds` count as imported everywhere `sourceId` does for drafts.
- `importDraft` returns what happened (saved, already imported, covered)
  and checks inside its write. `ImportScreen.kt` reads the list again
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
| D7a | Import | The session insert of an import succeeds and its round insert fails | Nothing of that draft is saved: no session, no rounds, no claimed id. A new try can save it. |
| D7 | Import | A stale draft is saved after the app session that covers it was saved; a stale grouped draft of which one record is covered now; a draft is saved twice; "Import all" with one stale draft among three | The stale draft writes nothing and returns its reason (the check is per original record, inside the write); the others save; the screen shows the fresh list, where the uncovered record of the group is still offered. A saved import has `timedFrom`, `timedTo` = the band's bounds and asks for a fill; `avgHr` and round values are written as today. |
| D18 | Fill | The Import screen opens | One fill request through the runner of phase 1. |
| D20 | Rules | Whole change | No layout change, no new text on the Import screen, no new permission or dependency, no manifest change. `SPEC.md` updated. |

## Attack cases

From the phase 2 attack, the cases of this phase's scope. A case that
names a held call uses a hook or a fake that the test controls.

| ID | Steps | Expected result |
|---|---|---|
| P2-AT01 | Create a run at 23:59. Start the timer at 00:01 and stop at 00:21. Hold the timer commit. At 00:23 load the band draft for 00:00–00:22. Release the commit and leave the form unsaved. Read the list and count again, then submit the old draft. | The record is covered before Save. Both reads exclude it. The earlier draft writes nothing and returns covered. |
| P2-AT02 | Open a grouped draft for runs from 18:00–18:20 and 18:15–18:45. Commit an app run from 18:00–18:20. Submit the old group. | The whole old write is refused. The refreshed list offers only the second record. Today counts one draft. |
| P2-AT03 | Pass a three-record overlap chain and a pair with tied starts through list and count in all input orders. Include an overlapping strength record and an unknown workout. Include a group whose union would pass coverage while each original fails. | Group membership stays stable across input orders. List and count agree. Strength stays hidden and the unknown workout stays available. Coverage checks each original range. |
| P2-AT04 | Open a group with source IDs `id1` and `id10`. Link `id1` to an app session whose date makes its range ineligible. Submit the old group. Refresh and retry with only `id10`. | The old group is refused as already imported. Token matching leaves `id10` available. The retry saves it once. |
| P2-AT05 | Submit three drafts through Import all. One became covered after the list opened. Submit each successful draft again. In a separate run, fail one session's interval insert after its session insert. | The covered draft writes nothing. The two valid drafts save once each and carry the band's bounds. A failed insert leaves no session, logs, or claimed IDs for that draft. A fresh retry can save it. |
| P2-AT24 | Open Import with a fake phase 1 fill runner. Save an eligible draft. Submit it again after it is imported. | Opening asks for one fill. A successful save asks for a fill after the write. The refused repeat creates no session or fill request. |

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
