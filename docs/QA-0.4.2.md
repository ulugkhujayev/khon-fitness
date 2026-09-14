# Khon Fitness 0.4.2 verification

Signed APK, version code 19. These checks were completed before publication.

The player previously began counting as soon as it opened. The old service also advanced straight into the next stretch and classified both beeps and speech as alarms. The pose pairs put plow's feet on the wrong side of the head and did not show 90/90's knee bends clearly.

The player now waits for Start, counts 3, 2, 1, then counts exercise time. Each stretch and each side waits again. Cardio only waits at its initial start; its work/rest phases still advance automatically. Waiting and preparation do not count toward exercise duration. Stop records completed phases rather than the entire planned routine.

The four reported poses have new coordinates. Repeated movements follow exercise time; held poses stay still. WGS rotates the arm through joint angles to avoid shortening it in mid-motion. Near and far limbs have different brightness. 90/90 has an oblique floor reference.

## Checks

- 15 JVM regression tests pass, covering timer transitions and pose geometry.

  Re-run with `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest` from `android/`.
- Debug and signed release builds pass. The pose generator reproduces the checked-in Kotlin file exactly.
- Android 16, API 36, at 360 x 640 dp. Opening waits, Start counts down, Pause freezes, completion waits for the next tap, and Skip waits too. Screen-off completion produces a Ready notification and the next stretch still has its full duration after unlocking.
- Android 9, API 28. Installed signed 0.4.1, then upgraded to signed 0.4.2 without uninstalling. Checked Start, 3/2/1, Pause, and all 14 stretch/side steps. The final stretch finished with the screen locked and returned to Today. Screenshots are in `docs/shots/v042/`.
- Cardio on Android 9 waits at Start timer, then advances automatically. The live screen and notification reached Work 3/8 without another tap.
- A stopped Android 16 session stored 91 seconds, 2 completed phases and 3 skipped phases. It did not claim all 14 were completed.
- `apksigner verify` passes with the existing Khon Fitness release certificate.
- `git diff --check` passes.
- Full debug lint reports one existing error at `update/Updater.kt:73`, the missing `ENFORCE_UPDATE_OWNERSHIP` permission, plus 32 warnings and one hint. The same call exists in the starting commit. Release's required lint checks pass.

Bluetooth and wired-headphone playback remain unverified on physical hardware. Code now uses `STREAM_MUSIC` for tones and `USAGE_MEDIA` for speech, and stops audio on `AUDIO_BECOMING_NOISY`. Android refused a shell-generated version of that protected broadcast. Check beeps, speech, and disconnect behavior with the owner's actual earphones.

## References

The layout uses one clear primary action, consistent alignment, and generous tap targets, following [Apple's button guidance](https://developer.apple.com/design/human-interface-guidelines/buttons). The media routing choice follows [Android AudioAttributes](https://developer.android.com/reference/android/media/AudioAttributes).

Pose checks used [HSS's 90/90 instructions](https://www.hss.edu/health-library/move-better/hip-flexor-stretch), [Yoga Journal's thread-the-needle demonstration](https://www.yogajournal.com/practice/yin-yoga-poses-to-unwind-after-work/), and [its plow demonstration](https://www.yogajournal.com/poses/plow-pose). The diagrams are original line drawings.

APK SHA-256: `ae41e57c9819d3e1cf876acbe25251e86c8d1c1cb309f0b65892712403b4496c`.
