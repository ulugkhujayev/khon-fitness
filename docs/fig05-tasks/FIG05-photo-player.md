# FIG05-photo-player: the stretch demonstrations become photos

Task-ID: FIG05-photo-player
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/fig05
Base: ce74ff0
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/fig05/docs/fig05-tasks/FIG05-photo-player-REPORT.md

## Why

The owner rejected the rendered 3D figures of the stretch demonstrations.
He chose a set of studio photos: one man, per stretch a start pose and a
hold pose, each from the side and from the front. He also chose the
layout: one large photo, as the player is today; the photo changes with
the steps, and the round button changes the camera view.

The picture he chose from:
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/docs/tasks/fig03/player-options.html`
(option P-A; the timer and buttons there are a sketch, only the photo
area is the design). Open it, or the photos, before you start.

The photos, 38 WebP files of 720 x 820 px on black:
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/docs/tasks/fig-final/webp/`.
Names: `<key>-<pose>-<side|front>.webp`. Keys: wgs, needle, ninety, plow,
hipflexor, fold, elephant, catcow, shoulderir. Poses: `start` and `hold`;
catcow also has `setup`.

## What exists

- `android/app/src/main/assets/stretch_demos/`: per key two sprite sheets
  of 64 frames (`<key>-0.webp`, `<key>-1.webp`), two posters, and
  `guides.json` (name, frame grid, steps with `frame`, `loopStart`,
  `moving`, `repHoldFraction`).
- `data/StretchDemo.kt`: the model and the frame rules.
- `ui/components/StretchDemonstration.kt`: `DemoCatalog`, the sheet
  cache, `DemoFrame`, `DemoPoster`, `StretchDemonstration` (preview with
  play and pause and a step timeline; during the exercise the frame
  follows the rep progress), the view button.
- Callers: `ui/screens/StretchScreen.kt` (the player),
  `ui/screens/StretchEditors.kt` (compact preview),
  `ui/components/Figure.kt` (list thumbnail), `ui/components/StretchParts.kt`.
- Tests: `timer/DemoClockTest`, `timer/StretchDemoTest`,
  `ui/screens/StretchDemoClockTest`.
- `tools/stretch-demo/` renders the old sprites. Leave it as it is; the
  README of that folder gets one line that the app no longer ships its
  output.

## Scope and lead decisions

1. **Assets.** Copy the 38 photos into
   `android/app/src/main/assets/stretch_demos/`. Delete the 36 old sprite
   and poster files. No separate poster files: the list thumbnail decodes
   the hold photo of the side view at a reduced size.
2. **`guides.json`.** Each step names its photo in place of a frame.
   Remove the frame grid fields. Keep the names, titles, and
   instructions letter for letter. The photo of each step:

   | Key | Steps in order |
   |---|---|
   | wgs | start, start, hold |
   | needle | start, hold, hold |
   | ninety | start, hold, hold, hold |
   | plow | start, start, hold, hold |
   | hipflexor | start, start, hold |
   | fold | start, hold, hold |
   | elephant | start, hold, hold |
   | catcow | setup, start, hold |
   | shoulderir | start, hold, hold |

   A stretch that moves in reps (`moving`: wgs, elephant, catcow) also
   names its two rep photos: `start` and `hold` for all three.
3. **Preview (before Start).** As today: the steps play in turn (2.6 s
   per step), the pause and play button, the step timeline, a tap on a
   step selects it and stops the play, the instruction text of the shown
   step. The photo is the photo of the shown step. When the photo
   changes, it cross-fades in 300 ms. The line of the timeline fills
   toward the next step during a step, as today.
4. **During the exercise.** A timed hold shows the photo of the last
   step. A rep stretch shows the first rep photo in the first half of
   each half-cycle and the second one in the second half: with the
   present `motion` value (0 to 1 and back), `hold` when it is 0.5 or
   more, else `start`, with the same cross-fade. The instruction is the
   one of the last step for a timed hold; for a rep stretch it follows
   the shown photo (the last step that names it).
5. **Views.** View 1 is `side`, view 2 is `front`. The button and its
   TalkBack text ("Change camera angle. View 1 of 2") stay. The view
   stays when the photo changes.
6. **Right side.** The photo mirrors as the sprite does today, and the
   texts swap sides as today.
7. **The photo area.** A black panel with the present corner radius. The
   photo is never cropped and never stretched: it fits inside the panel.
   In the player the panel is as wide as today and has the photo's
   aspect (720:820) when the screen has the height; on a short screen it
   gets lower, never under 240 dp, and no control or text of the player
   leaves the screen or overlaps (the present `compact` rule of the
   player decides when; keep its meaning). The editor preview keeps its
   present height. The list thumbnail keeps its present size.
8. **Memory and speed.** Decode off the main thread, as today. Hold at
   most the photos of the shown stretch (2 poses x the shown view, plus
   the next photo of a cross-fade). The previous photo stays until the
   new one is ready, so the panel never flashes empty.
9. **Clean-up.** Remove the frame rules, the sheet cache, and the clock
   code that nothing uses after the change, with their tests. Name each
   removed or changed test in the report with its reason.
10. **Documents.** Update `SPEC.md` and `DESIGN.md` where they describe
    the demonstrations (load the `unslop` skill first).

## Not in scope

The stretch data in Room, the `figure` keys, the routine player's timer,
the step texts, a network call, a dependency or version change. No
comment that a name, a type, or a test can say; no task or review IDs in
code.

A known fault of this machine: a test worker with native graphics
sometimes hangs at 100 % CPU. Run each Gradle test command under
`timeout` (240 s for one class, 420 s for the suite). On a hang,
`kill -9` the "Gradle Test Executor" process of this worktree only and
run again, up to 3 times. Do not spend time on its cause. Export
`ANDROID_HOME="$HOME/Library/Android/sdk"`. Other agents run Gradle on
this Mac: do not touch their processes.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| P1 | Assets | The asset folder | 38 photos and `guides.json`; no sprite or poster file. A test reads `guides.json` and checks that every photo a step or a rep names exists for both views, for all nine keys. |
| P2 | Texts | Names, step titles, instructions | Equal to the base, letter for letter (a test or a diff of the extracted texts proves it). |
| P3 | Preview | Each key: play; pause; a tap on each step | The photo of the table for each step; the instruction of that step; the timeline as today. |
| P4 | Exercise, hold | A timed hold of each of the six stretches that do not move | The hold photo and the last instruction. |
| P5 | Exercise, reps | wgs, elephant, catcow with `motion` 0, 0.49, 0.5, 1 | start, start, hold, hold; the instruction follows the photo. |
| P6 | Views | The view button in preview and in the exercise | side, front, side; the pose does not change; the TalkBack text names the view. |
| P7 | Right side | A phase with side "Right" | The mirrored photo and the swapped texts, as on the base. |
| P8 | Photo area | 360 x 780 dp and 360 x 640 dp, font scale 1.0 and 1.3, preview and exercise, the longest instruction | The whole photo shows, not cropped, not stretched. No overlap, nothing off the screen. The panel is at least 240 dp high. |
| P9 | Other users | The stretch editor preview; the stretch list thumbnails; a stretch with no demonstration | Photos in the present sizes; the present "No demonstration for this stretch" text. |
| P10 | Loading | A change of step, of view, and of stretch | No empty frame between two photos (a test on the loading state, or a statement of how the code holds the previous photo with its file:line). |
| P11 | All | The whole suite; the pictures; the APK | The `Tests:` command passes on the final commit. A screenshot test (as `LoadShotsTest`; create its folder `android/app/build/shots-stretch` first) makes pictures of: the player in preview at the first and the last step, the front view, a right side, the exercise, at both sizes and font scales of P8; the editor preview; the stretch list. You opened each picture. The report gives the size of the asset folder before and after. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- Each new test fails when its result breaks: give a break proof for P1, P3, P5, and P7.
- The report lists the pictures with their paths.
- Commit on branch `fig05` (no push, no merge). Messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| P1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.

## After the deep review (2026-11-27)

Rule 8, corrected: a set holds the photos of one stretch in one view. In
the exercise that is the two rep poses (start and hold), or the one hold
pose, plus the photo that fades out. The findings F1 to F5 of the deep
review are fixed by the task `FIG05-fix1.md` in this folder.
