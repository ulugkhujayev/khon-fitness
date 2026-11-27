# FIG05-fix1: fix the findings of the deep review of the photo player

Task-ID: FIG05-fix1
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/fig05
Base: ef05d83e
Capabilities: static
Coverage: required
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/fig05/docs/fig05-tasks/FIG05-fix1-REPORT.md

## Why

Sol's deep review of the photo stretch player (task
`docs/fig05-tasks/FIG05-photo-player.md`, read it first) gave five
findings. The lead fixed F1 and wrote F5 into the report. This task
fixes F2, F3, and F4, and confirms F1. You are a fresh fixer: follow
step 6 of the protocol.

The witness files are in
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/FIG05-photo-player-W/`
(`F1.json`, `F2.json`, `F3.json`, each with a test patch). Read each
one: it holds the steps, the expected result, and a test you can adapt.

## The findings (each one is a class: search for every instance)

- **F1, a missing photo file crashed the load.** Fixed by the lead in
  `ef05d83e`: `decodePhoto` returns null. Check every other place that
  opens a demonstration asset (the list thumbnail, the editor preview,
  the prefetch, `DemoCatalog`) for the same class and fix what is left.
- **F2, the instruction of a rep changes before the photo.** In an
  exercise with reps, the visible instruction and the TalkBack text
  change to "hold" when the motion value passes 0.5, while the panel
  still shows the start photo until the new photo is decoded. Rule: the
  instruction and the TalkBack text that go with a photo show when the
  panel shows that photo. Until then the old pair stays. The same class:
  a preview step change and a view change (check them; the text of a
  preview step must not run ahead of its photo either).
- **F3, the prefetch holds more photos than the rule allows.** Rule 8 of
  the task file, as corrected at its end: in the exercise a set holds
  the two rep poses (or the one hold pose) of the shown view, plus the
  photo that fades out. The set-up photo of Cat cow belongs to the
  preview only. In the preview a set holds the poses that its steps
  name, plus the photo that fades out. A view change must not keep more
  than that.
- **F4, two tests pass when the drawing breaks.** The right-side test
  checks a semantics text, not pixels: add a test that compares rendered
  pixels (the right side is the left side flipped; a photo that is not
  symmetric proves it). The loading test stops when the new photo's
  label shows: follow the cross-fade to its end and check at each frame
  that the panel draws a photo (never an empty black panel). Use the
  capture helper `nodeImage` (`NodeImage.kt`) and native graphics, as
  `LoadScreenTest` and `HistoryTitleTabsTest` do.

## Not in scope

The photos, `guides.json` texts, the layout of the player (P8 is
accepted), a dependency or version change. No comment that a name, a
type, or a test can say; no task, finding, or review IDs in code or in
test names.

A known fault of this machine: a test worker with native graphics
sometimes hangs at 100 % CPU. Run each Gradle test command under
`timeout` (240 s for one class, 600 s for the suite). On a hang,
`kill -9` the "Gradle Test Executor" process of this worktree only and
run again, up to 3 times. Export `ANDROID_HOME="$HOME/Library/Android/sdk"`.
One test outside this change, `TargetGroupTest` G11, sometimes fails in
the full suite under load with a 5 s timeout and passes alone: report
it, do not change it. Check free disk before a test run: below 30 GB,
stop and report.

## Contract

| # | Finding | Case | Expected result |
|---|---|---|---|
| X1 | F1 | Every reader of a demonstration asset, with a file that is missing | No crash; the panel, the thumbnail, and the editor preview show what they show with no photo. |
| X2 | F2 | A rep exercise at motion 0.49 then 0.5, with the hold photo not decoded yet; then decoded | The start photo with the start instruction and TalkBack text; then the hold photo with the hold instruction and text. Never a mixed pair. The same for a preview step change. |
| X3 | F3 | Cat cow reps, then a view change during the exercise; a preview of Cat cow | At most the two rep photos of the new view plus the one photo that fades out; the preview holds its step poses plus the one that fades out. |
| X4 | F4 | The right side of a stretch; a change of pose | A pixel test proves the flip. A frame-by-frame test proves that the panel draws a photo through the whole cross-fade. |
| X5 | All | The witnesses; the suite | `~/.config/agents/tools/witness run <file> --sha <your commit>` (run it from /Users/mirzohidkhon/deepvision/personal/khon-fitness, with `AGENTS_TASKS=/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks`) prints `fixed` for F1, F2, and F3. The `Tests:` command passes on the final commit, or fails only on `TargetGroupTest` G11, which then passes alone. |

## Definition of done

- Every contract row holds, with a test that fails when the rule breaks: for each changed guard or test, break the fix in a temporary copy and show the failing output.
- The report has the contract table and a Coverage table: one row per instance of each class (instance with file and line, status, test, found by).
- You opened the pictures of `android/app/build/shots-stretch` after the change and they still look right.
- Commit on branch `fig05` (no push, no merge). Messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
