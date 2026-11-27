# NGT01-nights-data: read nights from Health Connect and compute the night facts

Task-ID: NGT01-nights-data
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/ngt01
Base: 5567185
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/ngt01/docs/ngt01-tasks/NGT01-nights-data-REPORT.md

## Why

The owner approved "nights next to training". His band (Mi Band 9 through
Mi Fitness) writes sleep sessions to Health Connect. The app will show the
night before each training day in three places: a line per day in the
History Log, a "Nights" screen, and rows on Review and Compare.

This task builds everything under the screens: the read, the rules, the
texts, the in-memory store, and the Compare rows. A second task (a UI
builder) draws the screens on top of it. Build no screen here.

The owner's hard rules apply to every text and number:

- All data stays on the phone. Health Connect is on the phone. Add no
  network call.
- Facts only. No score, no verdict word ("good", "short", "poor"), no
  color that judges, no advice, no target, no notification.
- The app only reads sleep. It never writes it and never asks the band
  for anything.

Class note: the change adds one Health Connect read permission and no
stored data, no migration, and no access rule. That is why it is Normal.

## Lead decisions

1. **No Room table, no backup field.** Nights live in memory and come
   from Health Connect again after a process start. Do not change the
   Room schema, the DAO, or the backup format.
2. **A separate permission.** Add
   `android.permission.health.READ_SLEEP` to the manifest and a constant
   for `HealthPermission.getReadPermission(SleepSessionRecord::class)`.
   Do not add it to `HealthImport.readPermissions`: the Import screen
   and `pendingCount` must work as before without the sleep permission.
   The read code never requests a permission; the screen task does.
3. **A source interface**, as `HeartSource` in `data/HeartRead.kt`: a
   `fun interface` that returns plain sleep spans for a time range, a
   Health Connect implementation in `health/` (all pages, reuse
   `readAllPages` when it fits), and a pure mapping from the record
   fields to a span so that tests need no Health Connect.
4. **The rules are pure functions** in a new file under `ui/screens/` or
   `data/` (follow where `HeartRules.kt` and `HeartRead.kt` draw that
   line). The zone and "today" are parameters.

### The night rules

A *span* is one sleep session record: origin package, start, end, and its
awake stages (stage types AWAKE, OUT_OF_BED, AWAKE_IN_BED). Every other
stage type, and time with no stage, counts as asleep.

- N1. Drop a span whose end is not after its start, or that is longer
  than 24 hours.
- N2. The night of day D has the window from 18:00 of D-1 to 12:00 of D,
  local time in the given zone, end not included. A span belongs to the
  night whose window holds its midpoint. A span with its midpoint from
  12:00 to 18:00 is a nap and belongs to no night.
- N3. Inside one night, per origin: the asleep time is the union of the
  spans minus the union of their awake stages (clip each stage to its
  span). Time that two spans share counts once. The range of the night
  is the first start to the last end.
- N4. When two origins have spans in one night, keep the origin with the
  most asleep time (a tie goes to the origin whose package name sorts
  first). Never add origins together.
- N5. Drop a night with less than one minute asleep.
- N6. A day with daylight-saving change gets its window from the local
  wall times; lengths come from instants.

The result is a map from day to `Night(date, asleepSec, fromMs, toMs)`.

### The texts

- A night length: minutes rounded as `roundMinutes` does, then
  "7 h 05 m" (minutes with two digits) and "45 m" under an hour. Spoken:
  "7 hours 5 minutes", "45 minutes", "1 hour 1 minute".
- A night line: "Night 7 h 05 m · 23:10 – 06:15" (an en dash with
  spaces, the clock format of `clockTime`). Spoken: "Night 7 hours 5
  minutes, 23:10 to 06:15".
- The Log line of a day: the night line when the day has a night; "No
  night data" when the nights are loaded, the day is not after today,
  and the day is on or after the first day that has a night; else no
  line (`null`).
- Review rows: "Night before" with the length, "Asleep" with
  "00:35 – 06:15".

### The figures

- **Last 14 nights**: the 14 days that end today, each with its night or
  none. The average asleep time of those that have one (none: no
  average), how many have one, and the shortest (a tie goes to the
  newest) with its date.
- **Night before a session, last 8 weeks**: finished sessions with a date
  in the 56 days that end today. Group by the title the app shows
  (`programName ?: exerciseName ?: "Session"`). Per title: the distinct
  days that have a night, their count, and the average asleep time. A
  title with no night is left out. Order: most nights first, then the
  title.
- **A period (Compare)**: the nights of the days inside the period: their
  count and their average asleep time. `PeriodStats` gets the two
  values; `periodStats` takes the nights as a new parameter with an
  empty default, so every present caller and test compiles unchanged.
  `compareGroups` gets a group titled "Nights" with the rows "Sleep per
  night" (the length, or `NO_VALUE`) and "Nights with data" (the count).
  The group is left out when both periods have no night. Its place:
  after "Bodyweight".

Averages are in whole seconds (integer division of the sum); the text
rounds to minutes.

### The store

An in-memory holder, created in `KhonApp` next to `heartFill` and reached
through `AppViewModel`:

- a `StateFlow` with the states: not read yet; no access (Health Connect
  not installed, needs an update, or the sleep permission is not
  granted); loaded (the map of nights); failed (a read error and nothing
  loaded before);
- `refresh(force = false)`: reads the last 1,100 days up to now and maps
  them with the rules. It does nothing when the last good read is
  younger than 5 minutes and `force` is false. Two calls at one time
  make one read. A failed read keeps the nights that were loaded before.
  A read that finds the permission gone moves to "no access" and clears
  the nights;
- the clock, the zone, and the source are constructor parameters so that
  tests use fakes.

## Not in scope

Screens, navigation, the permission request, SPEC.md, DESIGN.md,
screenshots. No dependency or version change. No comment that a name, a
type, or a test can say; no task or review IDs in code.

A known fault of this machine: a test worker with native graphics
sometimes hangs at 100 % CPU. Run each Gradle test command under
`timeout` (240 s for one class, 420 s for the suite). On a hang,
`kill -9` the "Gradle Test Executor" process of this worktree only and
run again, up to 3 times. Do not spend time on its cause. Export
`ANDROID_HOME="$HOME/Library/Android/sdk"`.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| D1 | Permission | The manifest and the Import permission set | The manifest has `android.permission.health.READ_SLEEP`. `HealthImport.readPermissions` still holds exactly the three old permissions (a test checks it). |
| D2 | N1, N2 | Zone Asia/Tashkent. Spans: 23:10 Nov 2 to 06:15 Nov 3; 14:00 to 15:00 Nov 3; 04:00 to 12:30 Nov 4; end before start; 25 hours long | Nights for Nov 3 (7 h 05 m) and Nov 4 (8 h 30 m). The nap, the reversed span, and the long span give nothing. |
| D3 | N2 edges | Midpoint exactly 18:00 of D-1; exactly 12:00 of D; one second before each | 18:00 belongs to the night of D; 12:00 belongs to no night; one second before 12:00 belongs to D; one second before 18:00 belongs to no night. |
| D4 | N3 | One origin: 23:00 to 02:00 and 01:00 to 06:00 with awake stages 01:30 to 01:45 (in both spans) and 05:50 to 06:10 (past the end) | Asleep 6 h 35 m; range 23:00 to 06:00. |
| D5 | N4, N5 | Two origins in one night: 7 h and 6 h 30 m; two origins with equal time; a night of 40 s | The 7 h origin; the origin that sorts first; no night. |
| D6 | N6 | Zone America/New_York, the nights of 2026-03-08 and 2026-11-01, a span 22:00 to 07:00 local | 8 h and 10 h asleep; each on its day. |
| D7 | Texts | 25,500 s; 2,700 s; 3,660 s; 25,530 s; a night 23:10 to 06:15 | "7 h 05 m"; "45 m"; "1 h 01 m"; rounded as `roundMinutes`; "Night 7 h 05 m · 23:10 – 06:15" and its spoken text. |
| D8 | Log line | Loaded nights with the first on Nov 3; days Nov 2, Nov 3, Nov 5 (no night), tomorrow; the state "not read yet" and "no access" | No line; the night line; "No night data"; no line; no line; no line. |
| D9 | Last 14 nights | 14 days with 11 nights; no night at all; two shortest nights of equal length | Average of the 11, count 11, the newest of the two shortest with its date; no average and count 0. |
| D10 | Night before a session | Sessions of three titles in 56 days, one title twice on one day, one unfinished session, one session 57 days old, one title with no night | One day counts once per title; the unfinished and the old session do not count; the title with no night is left out; the order is most nights first, then title. |
| D11 | Compare | Two periods with 26 and 0 nights; both with 0 | Rows "Sleep per night" (length and `NO_VALUE`) and "Nights with data" (26 and 0) in a group "Nights" after "Bodyweight"; no group. Every present Compare test passes unchanged. |
| D12 | Record mapping | A record with stages AWAKE, LIGHT, DEEP, REM, OUT_OF_BED, AWAKE_IN_BED, UNKNOWN, SLEEPING; a record with no stage | Only the three awake types become awake time; the record with no stage is all asleep. |
| D13 | Store | A fake source: first read; a second `refresh()` after 4 minutes and after 6 minutes; `refresh(force = true)`; two calls at one time; a read that throws after a good read and with none before; the permission gone | One read; no read, then a read; a read; one read; the old nights stay, or the state "failed"; "no access" with no nights. |
| D14 | All | The whole suite | The `Tests:` command passes on the final commit. No Room schema file, DAO, or backup code changed (`git diff --stat 5567185` shows it). |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- Each new test fails when its rule breaks: give a break proof for D2 to D5 and D13 (the mutation, the failing output, the passing output after the revert).
- The report lists the public names the screen task must use: the state type, the store and its view model entry, the permission constant, and each rule and text function with its signature.
- Commit on branch `ngt01` (no push, no merge). Messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| D1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Under the table, add the list of public names.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
