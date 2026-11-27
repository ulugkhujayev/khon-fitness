# REV01-fix1: fix the review findings of the Review row

Task-ID: REV01-fix1
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/rev01
Base: d10034e
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/rev01/docs/rev01-tasks/REV01-fix1-REPORT.md
Coverage: required

## Why

The deep review of the Review row is in
`docs/rev01-tasks/REV01-review-from-history-DEEP-REPORT.md`. Read it,
then `docs/rev01-tasks/REV01-review-from-history.md` and its report.
Five findings are open: F1, F2, F4, F6, and F7. F3 and F5 did not
reproduce (their witnesses print `fixed` on the base); do not change
code for them, but keep them `fixed`. Each is a class: fix every instance, not only
the lines the review names.

Witness files, in
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/REV01-review-from-history-W/`:
`F1.json` to `F5.json`. Run
`~/.config/agents/tools/witness run <file> --sha <your commit>` for each
with `AGENTS_TASKS=/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks`
and `ANDROID_HOME="$HOME/Library/Android/sdk"` exported, and read each
receipt log. Each one that reproduced on the base (see the lead's note
at the end of this file) must print `fixed`. Do not change the witness
files; when one cannot pass for a reason outside the finding, report it
as blocked with the exact error.

A known fault of this machine: a test worker with native graphics
sometimes hangs at 100 % CPU. Run each Gradle test command under
`timeout` (240 s for one class, 900 s for the suite). On a hang,
`kill -9` the "Gradle Test Executor" process of this worktree and run
again, up to 3 times. Do not spend time on its cause.

## Scope and lead decisions

1. **F1, the tap reads the present state.** The action of the Review row
   decides at the tap, from the present state and not from the state of
   the last composition: it opens Review only when the session is
   finished, the gym screen is not in Edit mode, and the cardio form has
   no typed change (draft and interval fields). It goes through the same
   exit guard as Save, Back, and Edit, so that only one of them runs: a
   Review tap while Save leaves the form does nothing, and a Review tap
   after Edit took effect does nothing.
2. **F2, the scroll target of a block does not depend on the row.** A
   scroll to a block lands on that block when the Review row appears or
   goes away during the scroll (Edit and Done). Resolve the target from
   the block key, or give the row a place that does not shift the
   indexes. With no row the screen must look as it did before this
   branch (no extra gap).
3. **F4, the bar never waits.** The top bar with Back, the exit and Back
   guards, `LeaveIfSessionMissing`, and Finish or Save show and work
   from the first frame that has the session. Only the scrolling body
   waits for its sets or rounds, so that its first measure has its full
   height and the saved scroll place holds. Every hook that ran before
   the builder's early `return` runs again from the first frame. This
   applies to unfinished sessions too.
4. **F6, tests that fail when the result breaks.** Add or change: V4
   asserts the Review picture (`HEART_PICTURE_TAG`) and the cardio
   figures group on the opened Review; V5 has the bar Back from Review
   to the cardio form; V6 asserts visible History content, not only the
   route; V7 has a fill that commits while the cardio form is open and
   the row value follows. Give a break proof for each.
5. **F7, comments.** Remove the four comments the review names and any
   other comment of this branch that a name, a type, or a test can say.

No Room, DAO, backup, permission, dependency, or version change. No task
or review IDs in code. Load the `unslop` skill before you edit `SPEC.md`
or `DESIGN.md`. Make the screenshots of `ReviewRowShotsTest` again
(create its folder first) and look at each one yourself.

## Contract

| # | Finding | Case | Expected result |
|---|---|---|---|
| X1 | F1 | A typed note or interval value, then the Review action of the last composition; a Review tap while Save leaves the form; a Review tap after Edit took effect | Review does not open in each; the typed value stays; Save completes. Witness F1 prints `fixed`. |
| X2 | F2 | Open Block 2 and tap Edit before the scroll ends; the inverse with Done | The selected block shows. Witness F2 prints `fixed`. With no row the gym screen is pixel-equal to the base (a test or a picture proves it). |
| X3 | F3, F5 | The witnesses F3 and F5 at your final commit | Both still print `fixed`. The layout of the row does not change. |
| X4 | F4 | A gym and a cardio session whose sets or rounds load late, finished and unfinished | The bar with Back shows from the first frame; Back works; no hook is lost. Witness F4 prints `fixed`. |
| X5 | F6 | The four tests of decision 4 | Each exists, passes, and fails when its result breaks (break proof in the report). |
| X6 | F7 | The comments | Removed. `~/.config/agents/tools/comment-guard` (when it exists) passes on the branch. |
| X7 | All | The whole suite; the screenshots | The `Tests:` command passes. The 16 pictures made again and looked at. The existing tests of History, the session screens, Review, and the exit guards pass unchanged, or the report names each changed assertion with its reason. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of each class (instance, status, test, found by) and the witness results.
- The `Tests:` command passes on the final commit. Commit on branch `rev01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.

## Lead's note: witness results on the base

Ran by the lead at `bbd7536`: F1 `reproduces` (ComparisonFailure in `ReviewActionStateWitness`), F2 `reproduces` (AssertionError in `ReviewBlockScrollWitness`), F4 `reproduces` (AssertionError in `SessionHeaderLoadingWitness`), F3 `fixed`, F5 `fixed`.
