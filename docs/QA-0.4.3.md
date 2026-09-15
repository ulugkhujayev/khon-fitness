# 0.4.3 stretch demonstrations

The ready screen now automatically plays an offline 3D human demonstration.
Nine guides have numbered setup and movement steps, plain instructions, and
two camera angles. Selecting a step pauses the demo. Play demo restarts it.
The exercise clock still waits for Start and the three-second countdown.
Each subsequent phase opens ready, with its demonstration playing.

## Validation

- Debug and signed release builds pass. Release vital lint passes.
- Six guide playback and mirroring tests and ten timer state tests pass.
- Asset inspection verifies nine guides, eighteen 64-frame sprite sheets,
  eighteen thumbnails, and valid step frame ranges.
- Viewed a sequence contact sheet for all nine guides, including intermediate
  poses. Viewed the native Android player on Android 16 and Android 9.
- Android 16 interaction checks cover autoplay without timer advancement,
  step selection, camera changes, Start countdown, Pause, and Skip returning
  to a ready screen with autoplay. A completed first exercise also waited
  for Start on the next exercise.
- Android 9 signed upgrade from 0.4.2 succeeds. Traversed the routine through
  all guide types, selected final steps and changed cameras. No application
  crash appeared in the Android 9 crash buffer.
- Fixed caption-driven layout movement and shortened the ready timer line
  after inspecting the smaller Android 9 screen. Final player screenshots
  show the guide, instructions, routine progress, and Start button together.
- Screenshots are local at `dist/qa-0.4.3/android9-wgs.png` and
  `dist/qa-0.4.3/android16-wgs.png`.

The app decodes one 3072 by 2432 sheet per camera using RGB565, about 15 MB.
A sampled Android 16 debug player process used about 100 MB total PSS. This
is a point measurement, not a long-run memory or performance benchmark.

The renderer and poses are original source under `tools/stretch-demo`.
The layout experiment is archived in local branch
`prototype/stretch-demonstrations`, commit `db7ed92`. The implementation
uses numbered steps with a single large view and a camera switch.

## Limits

The demonstrations have not had a first-time-user comprehension study or a
qualified movement review. Physical headphone routing was not retested in
this revision; the media routing changes from 0.4.2 remain unchanged. The
Android 16 emulator's Google speech service crashed during one timer check;
the app continued and the timer checks were completed. Full lint retains
the unrelated updater permission finding documented in QA-0.4.2.

The signed APK is prepared locally. This revision has not been pushed or
published.

APK size: 10,545,817 bytes. SHA-256:
`0496821293073bc4b45262c0f0a3f923bb2d39d23d53a33207df3d7722568573`
