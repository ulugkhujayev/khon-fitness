# Is there an app that already does what Khon Fitness does?

Researched 2026-09-12. Two independent passes per topic (gemini-3.8-flash via agy, muse-spark-1.3 via opencode2) plus my own checks. About 60 apps looked at. Raw reports with source URLs are in `docs/research/`.

Short answer: no. Nothing covers items 1 to 7 in one app, and nothing at all does item 9. The closest you get is a stack of four apps, two of them subscriptions, and even then the Health Connect merge and the eating-window icon do not exist anywhere.

## The nine things Khon does or will do

1. Gym logging: programs, superset blocks, prefill from last time, one-tap set done, session-only swap, e1RM and sets-per-muscle charts.
2. Cardio: steady runs and swims with pace and laps, plus an interval timer (4x4, Tabata) with voice, lock-screen countdown, per-round HR and watts.
3. Week plan: template plus per-date overrides, move a workout to another day.
4. Bodyweight entry and chart.
5. Health Connect import of Mi Fitness and Samsung Health sessions, merging the band and the watch when they overlap.
6. Dense dark UI, widget with Start.
7. Offline, JSON export, free, self-updating.
8. Next: stretching routines with per-stretch timer, animation, history.
9. Next: eating window with the app icon showing open or closed.

## Closest apps per area

### Gym (item 1)

| App | Price | What it has | What it lacks |
|---|---|---|---|
| Hevy | free, Pro $24/yr | best logger: supersets, prefill, swap, e1RM, muscle volume, CSV export | free tier caps routines at 4, calendar is history only, Health Connect write-only, cloud account, card UI |
| Boostcamp | free, Pro | programs library, supersets, RPE, offline | no export found, no interval timer, no plan calendar |
| StrengthLog | free, Premium | e1RM and per-muscle reports | those are paid, Health Connect import explicitly absent |
| FitNotes | free | Android 9 ok, offline, no account, CSV, prefill, one-tap, dense list UI | no supersets, no per-muscle weekly, no Health Connect, no plan calendar |
| LiftLog | free, AGPL | local, JSON and CSV export, Android and iOS | weak charts, no calendar, no intervals |
| openGym | free, AGPL | supersets, prefill, routine per weekday with reschedule, bodyweight chart, JSON export, sideload APK, imports Hevy/Strong/FitNotes | cardio is time plus speed only, no interval timer, no Health Connect, no muscle charts; new project (Aug 2026) |

openGym (gitlab.com/DuarteSantos8/opengym) is the nearest open-source relative of Khon for items 1, 3, 4, 7.

### Cardio and intervals (item 2)

No phone app both runs a 4x4 or Tabata timer with voice cues and stores per-round HR and watts and logs swims with laps. The market is split:

- Timers only: Seconds Pro, Interval Timer by Deltaworks, Norwegian 4x4 app (norwegian4x4.com, records to Apple Health). None records sensor data per round.
- Recorders only: FitoTrack and RunnerUp (open source, intervals with voice, HR, no watts, no swim), OpenTracks (HR and power, no interval player).
- Planners only: Intervals.icu (best calendar with drag-drop, free), needs a watch or the Health Sync app to get data in on Android.

### Week plan calendar (item 3)

Only the endurance planners have it: Intervals.icu, Polar Flow, TrainingPeaks. Gym loggers show history calendars and cannot plan ahead. openGym has "a routine per weekday" and "reschedule any day", which is the closest.

### Health Connect import with merge (item 5)

No app imports finished sessions from Mi Fitness and Samsung Health and merges overlaps. Hevy, Strong, Garmin, Polar, Coros write to Health Connect and do not read sessions. Samsung Health reads Health Connect but is cloud-locked, and its newer builds need Android 10 or newer.

### Stretching (item 8)

| App | Price | Notes |
|---|---|---|
| Bend | free starter, about $30 to $80 per year | illustrations plus per-stretch timer, custom routines, streaks |
| Stretch Exercise by Leap Fitness | free with ads | animations, voice, auto history, Android 9 ok |
| Fitify Flexibility and Stretching | freemium | HD video, voice, per-stretch and rest timers, history, offline |
| Pliability | $200 per year | coached video routines, Garmin and Whoop sync |
| GOWOD | about $12 per month | mobility assessment, 300 free videos, Strava sync |

None logs gym work. No open-source stretching app with animations exists. Hevy and JEFIT include stretches as sets, not as timed routines.

### Eating window with icon change (item 9)

No app changes its home-screen icon by fasting state. The manifests of the open-source ones were checked: FastTrack, FastTimes, MyFastingApp, ONE, Solo Forge. Commercial apps (Zero, Fastic, Simple, Window, BodyFast) use widgets and notifications.

- Best open-source fasting app: MyFastingApp (GPL, offline, JSON and CSV export, widget).
- Apps that bundle fasting with training: MyFitnessPal Premium, Cronometer Gold, Solo Forge (open source, fasting plus timers plus weight plus Health Connect), OpenNutriTracker (open source, food plus fasting).

Technical note: on Android the icon change is an activity-alias toggle, which works on Android 9. Samsung's launcher redraws the grid when it happens. On iOS the switch needs the app in the foreground and shows a system alert, so it cannot happen in the background at the window boundary.

## If you stopped building Khon

The least-bad stack for the S8 today:

1. Hevy or FitNotes for gym.
2. Seconds Pro for the 4x4 timer.
3. Bend or the Leap stretching app.
4. MyFastingApp for the eating window.

Four apps, two accounts, no merged band-plus-watch history, no icon change, no single week plan across gym and cardio.

## Verdict

Keep building Khon. The parts that make it worth having are exactly the parts no one ships: one week plan across gym and cardio, an interval timer that stores per-round data, Health Connect import that merges two wearables, and the eating-window icon. openGym is worth watching as a reference for gym UX and import formats, not as a replacement.
