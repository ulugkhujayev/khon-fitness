# 0.4.5 stretch figures

The stretch guides now use a continuous human mesh, with a modeled face,
hands, feet, and defined chest, shoulder, and leg shapes. Dark shorts and
coral highlights replace the separate shirt, sleeve, and limb shapes.
The source is the official MakeHuman CC0 mesh and native skin weights.
`tools/stretch-demo/model/README.md` records the pinned source and the exact
19-target body preset.

All nine guides retain two camera angles and offline playback. The preview
updates every 40 ms instead of 80 ms. Tighter cameras make the figure larger
inside the existing panel. The surrounding background matches the decoded image.

The rig uses continuous limb rotations. Hands and feet use their modeled
surfaces for floor contact. Planted feet pivot across the floor, and the head
keeps its distance from the shoulders during a forward fold. The kneeling
hip-flexor pose also corrects a rear thigh that previously stretched to
0.66 m.

## Verification

- The release build and release vital lint pass. All 16 guide and timer
  unit tests pass.
- All 1,152 frames were rendered and packed into 36 WebP files, totaling
  7.42 MB. The package check passes for both camera views, sprite dimensions,
  numbered steps, and thumbnails. Camera output stays identical when a guide
  renders alone or last in the batch.
- All 37 packaged guide files, including the manifest, match the source
  assets byte for byte in the final APK.
- The final contact-aware motion check passes all 6,804 bone transitions.
  Before correction, the new rig had 50 failures, including surface jumps
  of 10 to 19 cm between adjacent frames.
- The final maximum skin movement is 12.2 cm during the plow transition,
  where the authored landmarks move 10.4 cm. This measurement includes the
  hand and foot contact corrections.
- All 58 images of the numbered poses were visually inspected across both
  camera angles.
- The model rebuild is deterministic. Its SHA-256 is
  `799bf8d1bef7c229a19fb4ddd5b0ccbebbf4cceae0574fb761b0ba84140da1f4`.

The final signed APK upgraded 0.4.4 on the Android 9 emulator. The installed
package reports version 0.4.5 and version code 22. Native checks confirm
visible routine progress and Start, numbered pose selection, camera changes,
the preparation countdown, and a stable paused timer. Animation previews
leave the exercise timer at 0:40. The right side waits for its own Start and
uses right-side instructions.

The left and right native figure crops match after mirroring, with mean
channel errors below 0.00002 on the 0 to 255 scale. Paused figure crops taken
two seconds apart match exactly. A taller panel was rejected because it hid
routine progress. The final compact panel retains its original 208 dp height.

Android 9's system media encoder failed during screen recording. No native
video was recorded, and Android 16 was not verified in this run. The MP4
preview below uses the packaged sprite pixels and the app's preview timing.

Full release lint reports an existing missing
`ENFORCE_UPDATE_OWNERSHIP` permission in `Updater.kt`. The same call exists
in the starting commit. This change does not modify the updater.

Local evidence is in `dist/qa-anatomy/`. `motion-report.json` records the
input hashes and measurements. `decisions.tsv` records the visual review
and corrections. `tools/stretch-demo/preview.py` builds a comparison page
from the actual packaged sprite sheets.

## Local build and previews

- APK: `dist/khon-fitness-0.4.5-anatomy.apk`, 12,916,313 bytes.
- APK SHA-256:
  `3f94723c1008e3844c02dde76d326370affb1118eb710f595a87ec0f975a9d62`.
- Before and after: `dist/qa-anatomy/before-after.png`.
- Animation preview: `dist/qa-anatomy/animation-preview.mp4`.
- Interactive comparison for all nine guides: `dist/qa-anatomy/index.html`.
- Native screenshots and check notes: `dist/qa-anatomy/native-*.png` and
  `dist/qa-anatomy/native-check.txt`.

This build is local. No commit, push, tag, or release was made.
