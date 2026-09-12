# Khon Fitness 0.4: stretching and eating window

Decisions (2026-09-12): stretching is a daily routine on Today, not a plan item. Animations are drawn in the app with Compose. The eating window has fixed daily hours plus a tap to open early or close late for today. No app icon change: a small home-screen widget shows the state instead. One release with both.

Order of work: spec, data, screens, animations, icon, polish, release. Each group ends with a check on the emulator at 411 dp and 360 dp, and the Android 9 image for anything that touches the launcher or notifications.

## 0. Spec and design

- [x] SPEC.md: add phase 3 with the two features, the data model below, and the Today layout change.
- [x] DESIGN.md: rules for the stretch player (full screen, phase color like the timer, figure centered, name and countdown, next stretch line) and for the window row on Today.
- [x] HTML mockup (mockup/v04.html, captures in docs/shots/) of three screens: Today with the two new rows, the stretch player, the window editor. Headless captures at 390 and 360 wide for a look before Kotlin.

## 1. Data

- [ ] Room v3 with AutoMigration 2 to 3.
- [ ] `Stretch`: id, name, side (none, left/right), mode (hold or reps), holdSec or reps default, figure key, muscles, cue text, link (reference video URL), builtin.
- [ ] `StretchRoutine`: id, name, active flag, order. `RoutineStretch`: routineId, stretchId, order, holdSec override.
- [ ] `StretchSession`: id, routineId snapshot name, date, startedAt, totalSec, completed count, skipped count.
- [ ] `EatingWindow` settings: startMinute, endMinute, enabled, remindBeforeMin. `WindowDay`: date, openedAt, closedAt, note. One row per day, written when the day's state changes.
- [ ] Seed the owner's routine "Daily mobility" (2026-09-12 list) with these defaults, all editable:

  | # | Stretch | Sides | Default | Reference |
  |---|---|---|---|---|
  | 1 | Elephant walks | no | 10 reps each leg, about 60 s | youtube.com/watch?v=bIS8e2ZI-u0 |
  | 2 | Hamstring stretch, palms on ground | no | 45 s hold | youtube.com/watch?v=2nfsR9PC7hQ |
  | 3 | World's greatest stretch | left, right | 5 reps per side | youtube.com/watch?v=7XheaZERvBQ |
  | 4 | Thread the needle | left, right | 30 s per side | youtube.com/watch?v=gyew25Vaqj8 |
  | 5 | Hip flexor stretch | left, right | 45 s per side | youtube.com/watch?v=ktgtEWGhFd8 |
  | 6 | 90/90 stretch | left, right | 45 s per side | youtube.com/watch?v=FM7-7-a0FLg |
  | 7 | Shoulder internal rotation, no aid | left, right | 30 s per side | youtube.com/watch?v=3Av8-RLNvDk |
  | 8 | Cat cow | no | 10 reps, about 40 s | youtube.com/watch?v=2of247Kt0tU |
  | 9 | Plow pose | no | 45 s hold | youtube.com/watch?v=IoURFlXOuqg |

  About 9 minutes total. Reps-mode stretches run as a timer of the given length with a rep cue every few seconds.
- [ ] Stretch library rows show a small play icon when a link exists; tap opens YouTube. The player itself stays offline and drawn.
- [ ] Backup: add the four tables to export, import, describeBackup, merge, replace.
- [ ] Widget: show the stretch state (done today or not) under the workout line.

## 2. Today screen

- [ ] Stretch row under the workout block: name of the active routine, "N min · M stretches", a dot: green when done today, ring when not. Tap starts the player. Long press or chevron opens the routine list.
- [ ] Window row: "Eating window · 12:00–20:00" with the state and the time left ("closes in 2 h 10 m", "opens in 40 m"). Tap opens the window sheet: Open now, Close now, Edit hours, history link.
- [ ] Keep the block order: week strip, workout, update row, band row, stretch row, window row, bodyweight. No cards inside cards.

## 3. Stretch player

- [ ] Full-screen player like the timer: figure, stretch name, side label, big countdown, "Next: ..." line, Elapsed of total.
- [ ] Controls: Pause, Skip, Back, Stop. Volume keys skip? No. Keep it to taps.
- [ ] Side switch: a stretch with sides runs left then right with a 3 s change cue.
- [ ] Audio: same engine as the timer (alarm stream tone + TTS). Cues: stretch name at start, "switch sides", three beeps before the end, one long beep at the end of the routine.
- [ ] Runs in the TimerService so the screen can go off. Lock-screen notification with the countdown, Pause and Stop actions.
- [ ] End: write StretchSession, mark done on Today, go back to Today. The service persists first, the screen only navigates (same rule as the 0.3.4 fix).
- [ ] History: a dot per day on the History calendar in a mobility color; a line "Stretching · 12 days this month · avg 9 min" above bodyweight.

## 4. Routine editor

- [ ] Programs tab: a "Stretching" row next to Exercises. Routine list, one active.
- [ ] Routine editor: rows of stretches with hold seconds, reorder by drag handle, remove, add from the stretch library.
- [ ] Stretch library: list with figure thumbnails, search, filter by muscle. Builtin stretches cannot be deleted, custom ones can.
- [ ] Stretch editor for custom stretches: name, sides, default hold, muscles, cue, pick a figure from the drawn set.

## 5. Drawn figures

- [ ] A `Figure` composable: a line figure drawn on Canvas from a pose (joint angles), animated between pose A and pose B with a 2 s ease, looping. One stroke width, one color, phase-colored background.
- [ ] Poses for the nine seed stretches first (elephant walk, palms-to-floor fold, world's greatest stretch in its three parts, thread the needle, hip flexor lunge, 90/90, shoulder internal rotation, cat cow, plow), then the rest of the 30:  hamstring standing, hamstring seated, calf wall, quad standing, pigeon, figure four, butterfly, glute bridge hold, child pose, cat cow, thoracic rotation, cobra, downward dog, chest doorway, cross-body shoulder, triceps overhead, lat side reach, neck side, neck forward, wrist flexor, wrist extensor, ankle circles, deep squat hold, 90/90 hips, couch stretch, side lunge, seated forward fold, spinal twist seated, shin stretch.
- [ ] Mirror the figure for the right side.
- [ ] A debug screen that lists every figure so poses can be checked quickly on the emulator.

## 6. Eating window

- [ ] Settings: hours editor (two time pickers), reminder minutes before close and before open, enable switch.
- [ ] State engine: open when now is inside the hours, or opened early today, and not closed early today. Closed otherwise. State changes at boundaries and on manual taps.
- [ ] Widget: a small 1x1 or 2x1 Glance widget that shows the state and the time left ("Open · 2 h 10 m", "Closed · opens 12:00"). Tap opens the window sheet. Refreshed at the boundaries and after each tap. The existing Today widget gets the same line under the workout.
- [ ] Scheduling: AlarmManager exact alarms at open and close times (setExactAndAllowWhileIdle), re-armed at boot and after each change. They refresh the widgets and post the reminders on a low channel.
- [ ] Test on the Android 9 image: widget refresh at a boundary, alarm after a reboot.
- [ ] History: a strip on History, one cell per day, filled when the window was kept (no early open, no late close), outline when not.

## 7. Leftovers from the audits

- [ ] Settings: "Restore last pre-import copy" row that lists the files in files/backups and imports one with Replace.
- [ ] Today: when today's item is done, show "Done · 6 sets · 1.2 t" on the block and a Start again link, instead of the plain Start.
- [ ] Accessibility names on set checks and steppers: "Log set A1 Bench press", "Bench press A1 weight, kilograms". Cheap now that rows have the exercise name in scope.

## 8. Release

- [ ] Version 0.4.0, versionCode 16.
- [ ] Emulator pass: 411 dp and 360 dp, font scale 1.3 on the two new screens.
- [ ] Android 9 image: player with screen off, window widget at a boundary, alarms after a reboot.
- [ ] Release build, commit, tag, PUSH, gh release with notes.

## Not in 0.4

Stretch photos or videos, a stretching plan item in the week template, food logging, calorie targets, iOS.
