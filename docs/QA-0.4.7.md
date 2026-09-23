# Longer shorts for stretch figures

Build 0.4.7 paints dark shorts from the navel to 6 cm below both knee joints.
The waist and hems have subtle lighter bands. The mask follows the model's
rest vertices through every pose, so it does not slide when the hips or knees
bend. Hands, calves, and feet keep their body color. Coral stretch targets
remain visible on the clothing.

The body shape, animation poses, guide text, and timer behavior are the same
as 0.4.6. This revision changes the figure colors and package version.

## Verification

All 58 numbered poses were inspected in both camera views. The waist and
both hems stay attached in standing, folded, kneeling, seated, and inverted
positions. Hands, feet, and calves remain bare. The highlight mask check
passed all 29 phase cases.

The review images and package checks are under `dist/qa-knee-shorts/`.
The final render has 1,152 frames across 18 views. The packer produced 36
WebP images and one guide manifest, 7.19 MB in the Android assets directory.
The render order and packaged sheet checks passed for all nine guides.
The Android unit suite passed 19 tests. The signed release build passed,
including release vital lint.

The 0.4.7 APK is `dist/khon-fitness-0.4.7-knee-shorts.apk`, version code 24.
Its 37 stretch assets match the source assets byte for byte. APK SHA-256:
`b3ed77524392af0cdeac81099046583049a7418f9695d844d2d5145d1d66dfbb`.
I installed it on the Android 9 API 28 emulator and inspected the standing
and hip flexor poses in the actual player. The shorts stay attached in both
camera views, and the routine still shows its explicit Start control.

`dist/qa-knee-shorts/before-after.png` compares packaged 0.4.7 frames with
0.4.6. `dist/qa-knee-shorts/animation-preview.mp4` samples the packaged
animation; `native-standing.png` and `native-hip-side.png` show the app.
