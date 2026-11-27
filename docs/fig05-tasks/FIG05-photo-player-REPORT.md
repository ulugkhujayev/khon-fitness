# FIG05-photo-player: report

The builder ended before it wrote this report. The lead wrote it from
the builder's messages (marked "builder") and from the lead's own runs
(marked "lead"). Head: `ce74626e`.

| # | Status | Check ran | Real output |
|---|---|---|---|
| P1 | done: 38 photos and `guides.json` in the asset folder, no sprite or poster file | lead: `ls android/app/src/main/assets/stretch_demos \| wc -l`; `du -sk` | `39`; `732` (KB; builder: 8,432,073 bytes before) |
| P2 | done: names, step titles, and instructions equal the base | builder: diff of the texts taken from both versions of `guides.json` | builder: "the 9 names and 29 step texts are the same as on the base, letter for letter" |
| P3 | done: the preview shows the photo of each step | builder: break proof, the last step's photo for every step | builder: 3 player tests fail, pass after the revert |
| P4 | done: a hold shows the hold photo and the last instruction | lead: full suite, try 3 | `1222 tests completed, 1 failed, 69 skipped` (the one failure is P11's) |
| P5 | done: reps change the photo at motion 0.5 | builder: break proof, `>= 0.5` to `> 0.5` | builder: 1 model test and 1 player test fail |
| P6 | done: the view button changes side and front | lead: pictures `front-*` and `preview-*` | opened by the lead: same pose, other camera |
| P7 | done: the right side shows the mirrored photo | builder: break proof, never mirror | builder: 1 player test fails |
| P8 | deviation: the whole photo shows and is never cropped, but at 360 x 640 dp, and at 360 x 780 dp with font scale 1.3 in the exercise, the column scrolls under the fixed button with the panel at its 240 dp minimum | lead: all 41 pictures in `android/app/build/shots-stretch` | at 360 x 780 dp and font scale 1.0 everything fits; in `exercise-360x780-1_3` the Pause button covers part of the timer until a scroll |
| P9 | done: editor preview, list thumbnails, and "No demonstration for this stretch" | lead: pictures `editor-*`, `list-*` | opened by the lead |
| P10 | done: the previous photo stays until the next one is ready | builder's statement; not checked by the lead | see the cross-fade code in `StretchDemonstration.kt` |
| P11 | deviation: the suite ran to its end with one failure outside this change | lead: `suite.sh` (the `Tests:` command), 3 tries: two hung on the known test-worker fault, the third ended | `1222 tests completed, 1 failed`: `TargetGroupTest > G11 the est 1RM sheet refuses values off its limits and steps` (a 5 s wait timed out under load). Alone: `tests="14" skipped="0" failures="0"` |

P8 is the lead's decision: accepted. The owner's phone is 360 x 780 dp.
The same test failed in the suite of branch `ngt01` in the same hour and
passes alone there too.
