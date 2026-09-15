# 0.4.4 visual correction

The 0.4.3 visual review failed. It accepted a tilted camera, crude joints,
and a lunge that looked suspended above the mat. Passing timer tests and
checking that images loaded did not establish acceptable visual quality.

The renderer called SceneKit's `look(at:)` repeatedly on one camera. That
method uses the camera's current `worldUp`. Later camera angles accumulated
roll. It now passes world up explicitly and rejects a rolled horizon.
The regression check compares World's greatest stretch rendered alone with
its render after all other guides. Restoring the old camera call triggers
the new camera assertion. Apple's SDK documents the distinction in
`SCNNode.h`, and in the [camera orientation API](https://developer.apple.com/documentation/scenekit/scnnode/look(at:up:localfront:)).

Continuous limb surfaces replace overlapping cylinders and joint spheres.
The torso blends pelvis and shoulder orientation separately. The lunge has
a shorter front thigh and a tucked rear foot. Plow uses a torso length
consistent with the standing model. The renderer aligns planted soles with
the mat and uses a tighter shadow map. Frames increase from 384 by 304 to
512 by 400 with less WebP compression.

All nine guides were regenerated. Their setup, intermediate, and held frames
were inspected as a contact sheet. These remain stylized instructional
figures. A MakeHuman experiment was rejected because the retargeted shoulders
and feet deformed badly; no downloaded model or asset was added to the app.

The sprite sheets are 4096 by 3200. A decoded ARGB8888 sheet uses 52.4 MB,
compared with 14.9 MB before. The Android check exposed color banding with
RGB565; the player now preserves the full color range. The renderer keeps the current camera sheet;
changing view may briefly retain the old and new allocations until collection.

## Checks

- Debug and signed release builds and release vital lint.
- Six guide playback and mirroring tests, plus ten timer state tests.
- Render-order regression and packaged texture dimensions.
- Native Android 9 and Android 16 player inspection, including Start waiting,
  step selection, alternate cameras, and a signed upgrade over 0.4.3.

This is a local review build. No tag, push, or release publication is part
of this correction.

The signed review APK is 14,128,137 bytes. SHA-256:
`ef4282ddd67a0cc14c7d07467a91e55d0fdf46bf876ff4933860553050d0f52b`

Android 9 used about 154 MiB PSS after ten additional camera changes, with
an empty crash buffer. This is a point measurement, not a long-run memory
benchmark. Final device screenshots are in `dist/qa-0.4.4/`.

A native app recording of the autoplaying lunge guide is saved at
`dist/qa-0.4.4/stretch-preview.mp4`. It shows the setup and movement while
the exercise clock stays at 0:40.
