# Stretch figure revision 0.4.6

The 0.4.5 man's chest had a low, rounded pectoral contour. The extra
pectoral target was removed, and a smooth correction now flattens the lower
chest. The correction affects only the front chest. The model keeps its
MakeHuman skeleton, skin weights, UVs, and vertex IDs. The pinned MakeHuman
source and exact conversion formula live in `tools/stretch-demo/`.

I reviewed `docs/research/stretch-anatomy/REPORT.md` and kept the supported
changes. Highlights now follow each step. Elephant walks switches color to
the straight leg. Cat cow switches between the back and front of the trunk.
The hip flexor guide marks the front of the rear hip rather than the
contracting glute. 90/90, Plow, Thread the Needle, and World's Greatest
Stretch use narrower targets. Color under the shorts remains visible.

The poses and cues also changed. The hip flexor demonstrates a pelvic tuck
and a small forward shift. Elephant walks lifts each bent heel. Both 90/90
knees bend to 90 degrees, and the alternate camera shows the back leg.
Shoulder rotation keeps the hand low. The Plow cue permits foot support and
asks for weight on the upper back. Cues add breathing without overriding the
routine timer. Elephant walks briefly holds each end of a repetition.

`docs/research/stretch-anatomy/IMPLEMENTATION.md` records why some detailed
muscle claims and fixed hold times from the report were not copied. The
colored areas are visual guides. They do not measure muscle strain.

## Verification

- The converter rebuilt the approved chest sculpt exactly from pinned
  MakeHuman inputs. The positions, normals, UVs, triangles, vertex IDs,
  skeleton, skin weights, and landmarks match the reviewed candidate. Model
  SHA-256: `161c056763005f6ae070d00d280f487ff9c229351b27511cf890b345a40d0e49`.
- The renderer produced 1,152 frames, 64 per guide and camera view. I
  inspected all 58 numbered poses in both views. The packed WebP files total
  7.14 MB.
- The contact-aware motion check passed 6,804 joint comparisons. It found
  zero abrupt joint turns and zero landmark steps over 15 cm. Its largest
  actual body movement was 12.2 cm during the Plow lift, where the authored
  landmark moved 10.4 cm. An earlier Plow pose jumped 34.6 cm between frames;
  its final knee now stays on the same bend path.
- The highlight check passed 29 phase and side cases. This check measures
  color masks on the rest model. The numbered-pose images provide the visual
  check for the final rendered colors.
- Camera output matches when WGS renders alone or after every other guide.
  All sprite dimensions, thumbnails, and step frame ranges pass the package
  check.
- The release build and vital lint passed. The 19 guide and timer unit tests
  passed.
- All 37 guide assets inside the final APK match the source files byte for
  byte. The APK reports version 0.4.6, code 23.
- The signed APK upgraded successfully on an Android 9 emulator. The compact
  routine shows progress and Start. WGS left and right use matching mirrored
  figures and side-specific cues. The hip flexor guide shows both views and
  the new chest in the installed app. Start runs its countdown, and Pause
  freezes the timer and figure. Two paused figure screenshots taken two
  seconds apart match pixel for pixel. Mirrored WGS figure crops have mean
  channel differences below 0.00001 on the 0 to 255 scale.

The signed local APK is `dist/khon-fitness-0.4.6-anatomy.apk`, 12,637,169
bytes. SHA-256:
`febc6cf60bd586721908503e2e8f995eb96bc33f7875572ca73cdb346646a588`.

Visual evidence is `dist/qa-anatomy-revision/chest-before-after.png`,
`catalog-final.png`, `animation-preview.mp4`, and the interactive gallery
`index.html`. The gallery draws the actual packaged sprites and supports both
cameras, frame scrubbing, and left/right mirroring.
Native screenshots and the check output are `native-*.png` and
`native-check.txt`. `native-hip-front.png` and `native-hip-redraw.png` show
the figure inside the installed app.

The earlier full lint check for 0.4.5 reported an existing updater permission
issue in `Updater.kt`. This revision does not touch the updater. No full lint
run was made for 0.4.6.

This build is local. No commit, push, tag, or release was made.
