# FIG05-fix1 report

Branch `fig05`. Base `ef05d83e`. Commits:

- `ee0dffa2` Show a stretch photo and its texts as one frame and cache only its poses
- `9b7230a8` Find the demonstration panel by its spoken text in the pixel tests
- `7d6e99fd` Give the pixel tests of the demonstration panel room below the measured values
- `7aa4761d` Hold the photo load past the fade in the cross-fade pixel test
- `448acb20` Test the poses a demonstration panel may show

## What changed

- `StretchDemonstration.kt`: the panel and the instruction under it read one `DemoFrame` (photo, instruction, spoken text). A new photo and its texts show together, after the decode. Until then the old frame stays, also on a new panel (`DemoPhotos.last` is now a frame). A missing photo gives a frame with no photo and the new texts (a black panel).
- `DemoPhotos.load` takes the set of photos the panel may show next. A load with a new set drops every held photo outside it. The prefetch loads only that set.
- `StretchDemo.shownPoses`: in the preview, the step poses; during reps, the two rep poses; in a timed hold, the hold pose.
- `readDemoGuides`: a missing `guides.json` reads as an empty map, so every stretch shows "No demonstration".

## Contract

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: `readDemoGuides` returns an empty map for a missing file. `decodePhoto` (lead, `ef05d83e`) returns null. A null photo gives a black panel with the right texts. | `gradlew :app:testDebugUnitTest --tests '*StretchPhotoPlayerTest'`; witness F1 | 9 tests, 0 failures. `WITNESS F1 fixed at=7aa4761d690b receipt=r-20261127075022-c50b20`. Break (catch the wrong exception): `java.io.FileNotFoundException: stretch_demos/missing.json` |
| X2 | done: one frame for the photo and its texts. Tested for a rep change, a preview step tap, and a view change, each while the load waits. | test `the instruction and the spoken text change with the photo, not before it`; witness F2 | passes. `WITNESS F2 fixed at=7aa4761d690b receipt=r-20261127075044-ad5448`. Break (texts follow the target): `ComparisonFailure: the photo of 'World's greatest stretch. Rotate. ...' expected:<...wgs-[hold]-side.webp> but was:<...wgs-[start]-side.webp>` |
| X3 | done: the cache holds only `shownPoses` of the shown view; a new set drops the rest. | test `the cache holds only the poses the panel can show next`; `StretchDemoTest` pose-set test; witness F3 | passes; StretchDemoTest 10 tests, 0 failures. `WITNESS F3 fixed at=7aa4761d690b receipt=r-20261127075107-58d3ef`. Break (exercise set = all poses): `expected the setup photo to go; the panel shows stretch_demos/catcow-start-side.webp`. Break (timed hold set = all poses): `expected:<[hold]> but was:<[setup, start, hold]>` |
| X4 | done: new class `StretchPhotoPixelsTest` (native graphics, `nodeImage`). Flip: needle hold photo, left vs right panel pixels. Cross-fade: the load waits 30 frames (longer than the 300 ms fade), then 30 frames after the hold photo shows; each frame must keep at least a quarter of the start photo's brightness. | `gradlew :app:testDebugUnitTest --tests '*StretchPhotoPixelsTest'` | 2 tests, 0 failures. Measured: flipped difference 0.059, unflipped 9.56, brightness 8.97; lowest mid-fade brightness 6.15 against start 10.47. Break (no mirror): `the right side is the left side flipped: mean difference 9.564547061306321`. Break (empty panel while loading): `frame 12 while the hold photo loads is empty` |
| X5 | done: all three witnesses print `fixed`. The suite failed only on G11 and `ScoreScreenTest.tenRoundsFitAt411Scale1`; both pass alone. `assembleDebug` and `lintVitalRelease` pass. | `timeout 900 gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease` at `448acb20`; then the two classes alone; then `:app:assembleDebug :app:lintVitalRelease` | `1229 tests completed, 2 failed, 69 skipped` (G11; ScoreScreenTest: `could not find any node that satisfies: (TestTag = 'score-group')`). Alone: ScoreScreenTest 14 tests, 0 failures; TargetGroupTest 16 tests, 0 failures. Build and lint: `BUILD SUCCESSFUL in 25s` |

## Coverage

| Row | Instance | Status | Test | Found by |
|---|---|---|---|---|
| X1.1 | missing asset: `decodePhoto` `StretchDemonstration.kt:79` (panel, prefetch, thumbnail) | fixed by lead `ef05d83e` | `a photo file that is missing decodes to nothing`; witness F1 `fixed` | review |
| X1.2 | `DemoCatalog` / `readDemoGuides` `StretchDemonstration.kt:52` (player, editor preview, thumbnail) | fixed | `a guide file that is missing reads as no demonstrations` | search for `assets.open` (2 hits in main) |
| X1.3 | panel with a null photo `StretchDemonstration.kt:130-135` | fixed: black panel and new texts; before, the old photo stayed with the new text | read only | caller trace |
| X1.4 | thumbnail `DemoPoster` `StretchDemonstration.kt:173` | kept: a null image gives a black box | read only | caller trace |
| X2.1 | texts ahead of photo: exercise rep change `StretchDemonstration.kt:135,144,249` | fixed | pair test, part 1; witness F2 `fixed` | review |
| X2.2 | preview step tap | fixed | pair test, part 2 | task |
| X2.3 | preview step while playing | fixed (same path) | `preview plays each step's photo ...` | caller trace |
| X2.4 | view change | fixed | pair test, part 3 | task |
| X2.5 | new panel (phase or stretch change) starts on `DemoPhotos.last` with its texts `:101,114,130` | fixed | read only | caller trace |
| X2.6 | right side (new panel, same photo) | kept: the photo is held, so it shows at once with the swapped text | `a right side mirrors the photo and swaps the instruction` | caller trace |
| X3.1 | cache budget: exercise reps `StretchDemo.kt:39`, `StretchDemonstration.kt:107,134,219` | fixed | cache test; witness F3 `fixed` | review |
| X3.2 | timed hold | fixed | `a panel may show the step poses ...` | search |
| X3.3 | preview to exercise in one panel (Cat cow set-up photo) | fixed | cache test | search |
| X3.4 | view change, both ways | fixed | cache test | task |
| X3.5 | stretch change | fixed: a new set drops the old photos | read only | caller trace |
| X4.1 | tests without pixels: right-side test | fixed: new pixel test | `the right side draws the left side's photo flipped` | review |
| X4.2 | loading test | fixed: new frame-by-frame pixel test | `a change of pose draws a photo in every frame of the cross-fade` | review |
| X5.1 | witnesses and suite: `witness run` F1, F2, F3 at `7aa4761d`; the `Tests:` command at `448acb20` | fixed | the three witnesses print `fixed`; suite fails only on two load flakes that pass alone | task |

## Pictures

`android/app/build/shots-stretch/` (42 files, made by the suite run at 12:53). Opened: `preview-last-360x780-1_0.png`, `right-360x640-1_3.png`, `exercise-360x780-1_0.png`, `list-360x780-1_0.png`, `editor-360x780-1_0.png`. Photo and text agree, the right side is mirrored, and the thumbnails and the editor preview draw.

## Notes

- deviation: the step timeline and the view button label change at once on a tap. They show the control's state, not the photo. The rule covers the instruction and the spoken text only.
- deviation: the suite ran under `timeout 900`, not 600, because it also builds and lints. No hang occurred.
- deviation: the witnesses ran at `7aa4761d`. The final commit `448acb20` adds only a test file.
- The fixer protocol asks only for the new tests and the touched classes. The task asked for the full suite as well, so it ran too.
- `ScoreScreenTest.tenRoundsFitAt411Scale1` failed in the suite and passed alone. It is outside this change. Guess: load flake, like G11.
