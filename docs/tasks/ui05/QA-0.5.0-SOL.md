# QA 0.5.0 on emulators (Sol, three parallel agents)

Step: check (release QA on a real device). Report only; change no repo file.
Your task message names your AREA (A, B, or C) and your DEVICE. Test only your area.

## Goal

Khon Fitness 0.5.0 (versionCode 26) is the first release with the UI ported
from the Expo app into the Kotlin app. Find every defect a user would hit:
crashes, wrong data, broken flows, layout faults, slow screens, wrong fonts
(the Inter font was cut to Latin and basic Cyrillic; look for boxes or
fallback glyphs). Test the signed release APK, not a debug build.

## Inputs

- APK: `/private/tmp/claude-501/-Users-mirzohidkhon-deepvision-personal-khon-fitness-tools/de079724-56e9-4d80-a89f-647336d0de5a/scratchpad/khon-0.5.0-sizecut.apk`
  (package `dev.mirzohidkhon.khonfitness`).
- Behavior: `SPEC.md`. Look: `DESIGN.md`. Source: `android/app/src/main/java/`.
  The SPEC wins over this file when they disagree (for example: no rest
  countdown on gym days, SPEC.md:156).
- Repo root: `/Users/mirzohidkhon/deepvision/personal/khon-fitness`.

## Device rules

1. Call the T3 tool `device_open` with hostId `local` and your deviceId.
   It returns an `agent-device` command bound to your device. Use only that command.
2. Do every launch, tap, text entry, alert, setting and screenshot with it.
   Use `device_screenshot` to look.
3. `@eN` refs expire after the screen changes. A "stale ref" or
   "not found" error is normal: run `snapshot -i` again and go on. It is not
   a failure.
4. Use `adb -s <your serial>` only for install/uninstall, logcat, `am start -W`,
   `dumpsys`, `pm`, `am kill`, and time changes. Never for taps, text, or
   screenshots. adb: `~/Library/Android/sdk/platform-tools/adb`.
5. Never touch another serial. Never close or boot a device.
6. A failed agent-device command is not a reason to stop: skip that item,
   note it, and go on. Stop only if `device_open` itself fails twice, or the
   device goes offline.

Start: uninstall any old copy, install the APK, `adb logcat -c`, launch.
Work fast: one pass per item, a screenshot only for a defect or a key state.

## Areas

### A: gym (Programs, live session, History)

1. Today tab: week strip, day selection, today's item, Start.
2. Programs and routines: create, rename, reorder, delete (confirm dialog);
   "+" saves an empty routine at once; Back from every editor.
3. Live gym session: log sets, hold-repeat steppers, focus moves to the next
   set, one open block at a time, swap an exercise (both scopes), Finish only
   from the header and its summary, leave and come back, `am kill` in the
   background and relaunch mid-session.
4. History and progress: the finished session shows the right numbers;
   exercise progress chart; bodyweight entry and chart.

### B: cardio, stretching, performance

1. Cardio: interval timer (start, pause, background, notification, finish),
   manual cardio entry, the values in History.
2. Stretching: daily routine on Today, full-screen player, demo animation,
   view switch, mirror side, fast skip forward and back, rotate if allowed.
3. Performance (release APK):
   - Cold start: `am force-stop`, then `am start -W -n <activity>` (find it with
     `cmd package resolve-activity --brief <pkg>`). 3 runs, TotalTime each.
   - Memory: `dumpsys meminfo <pkg>` total PSS and Graphics, on Today and
     while a stretch demo plays.
   - Jank: `dumpsys gfxinfo <pkg> reset`, scroll Today, History, Programs,
     then `dumpsys gfxinfo <pkg>`: janky frames %, 90th and 99th percentile.

### C: eating window, settings, robustness

1. Eating window: set hours, open early, close late, reminders, the home
   screen widget (add it if agent-device can; else say so).
2. Settings: backup export and import round trip (data must match),
   update check (show what happens offline and online).
3. Robustness: empty states on every tab; font scale 1.3
   (`settings text-size`); long names (40+ chars) in a program and an exercise;
   Cyrillic and Uzbek text (`Жим лёжа`, `Oʻtirib turish`) in names;
   `am kill` in the background on each tab, then relaunch.

## Logcat (every area)

At the end: `adb -s <serial> logcat -d | grep -iE "FATAL|ANR|khonfitness|AndroidRuntime"`.
Report each crash or ANR with the action that caused it.

## Output

Save files in `/tmp/khon-qa-050/<area>/` (create it). Report, under 80 lines:

- Defects: one line each: severity (crash / data / broken / visual / minor),
  screen, exact steps, expected vs actual, screenshot path, and the source
  `file:line` if you find it (READ or GUESS).
- Area B only: the perf table.
- Items you did not cover and why.

Do not edit, commit, or push anything in the repo. Leave your emulator
booted with the app installed.
