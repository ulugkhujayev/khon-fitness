# PIC01 deep review R1

**Merge after fixes.** Reviewed snapshot `2100daf02d6f` against `2dbca2f8a4998`. The lead needs to run the witnesses before recording the round.

**Coverage, read.** Traced H1–H6 through `PictureRules.kt` and `PictureRulesTest.kt`. Traced H7–H11 through `ReviewPicture.kt`, `ReviewScreen.kt`, `PictureScreenTest.kt`, and `PictureShotsTest.kt`. Traced H12 through the complete diff, including `SPEC.md` and `DESIGN.md`.
- The chart and row bpm use `hrFacts` at `ReviewPicture.kt:51–54` and `PictureRules.kt:149–170`. The test opens Review before it fills and removes the row at `PictureScreenTest.kt:242–258`. No navigation occurs between those checks.
- `HeartRate.kt:26–37` admits positive ranges up to six hours. `ReviewPicture.kt:79–95` uses that range for the curve, set ticks, and round bands. The diff adds no database writes, permissions, dependencies, or version changes to the app.

## P2

- **F1. Loss of buckets in a partial final second. Read.** `PictureRules.kt:57–58` rounds the range length down to whole seconds before filtering. A range of 600,002 ms permits the 600-second bucket. The filter drops it, so the curve, axis, spoken extrema, and set peaks can lose data at `PictureRules.kt:132–133, 158–170`. `HeartRead.kt:57–68, 120–126` can produce or accept that bucket. Compare each offset in milliseconds with the half-open range length. The witness checks both the curve and the second set's peak.
- **F2. Pixel tests accept incorrect drawing positions. Read.** `PictureScreenTest.kt:145, 168, 184` counts accent pixels across the canvas. It can accept a shifted curve or a missing singleton dot. Lines 148–152 and 169 sample one row for ticks, so ticks can detach from the bottom and still pass. Lines 156–160 and 185 sample one row for bands, so a thin strip can pass as a band. Check expected curve points, gap pixels, the singleton dot, both tick endpoints, and the band at multiple heights. Derive expected positions from fixture times and bpm.

## P3

- **F3. Redundant comments. Read and ran.** Remove the following comments. Use names for units and tests for behavior.
  - `PictureRules.kt:18, 21, 26, 29, 32–35, 54, 61, 71, 75, 83–86, 105, 110, 122–125, 145–148`.
  - `ReviewPicture.kt:49, 57–61, 99–102`.
  - `PictureFixtures.kt:9, 24, 27–30, 67`.
  - `PictureScreenTest.kt:62, 107, 121, 133, 147, 155, 192, 261`.
  - `PictureShotsTest.kt:45–49`.

**Ran.** `witness check F1.json F2.json F3.json` exited 0. Each `witness run <file> --sha 2100daf02d6f4a667991eee11d5433ec28cc4d78` returned `undecided`, exit 3, `receipt=none`. The sandbox denied writes to the shared witness state. No Gradle result was verified. I checked the F1 Kotlin calls against snapshot signatures and confirmed that each F2 mutation matches one source fragment. Running the F3 Python check directly on the snapshot printed `30 redundant comment blocks` and exited 1.

Witness JSON and test patches are in [PIC01-workout-picture-W](/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/PIC01-workout-picture-W). Use each witness with the full snapshot SHA. For F2, check for the `Missed drawing changes` line to confirm that the selected pixel tests ran.

F1=found
F2=found
F3=found
