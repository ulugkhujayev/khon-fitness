# Khon Fitness

Android app that replaces the Notion "TP v3" page. One person, one phone, no
account. Decisions below were settled in an interview on 2026-09-08.

## Why

The Notion setup duplicates a template per session, needs a manual date edit,
and a swap touches four places. It records nothing about weight or reps, so
there is no progress to look at.

## Stack

- Kotlin, Jetpack Compose, Material 3, dark theme only, English only.
- Room (SQLite) on the phone. No server. minSdk 28 (Health Connect floor).
- Signed APK on GitHub releases. Same keystore every release.
- Repo: `personal/khon-fitness`. Package id `dev.mirzohidkhon.khonfitness`.

## Phases

1. Gym: programs, live session, exercise progress, history, bodyweight,
   backup, update check.
2. Cardio: cardio exercises, modalities, interval timer, manual entry,
   Health Connect import.
3. Stretching and eating window (0.4): a daily stretch routine on Today
   with a full-screen player and figures drawn in the app; an eating window
   with fixed daily hours, open-early and close-late taps, reminders, and a
   small widget that shows open or closed. Task list: docs/TASKS-0.4.md.

Each phase ships as an installable APK.

## Navigation

Three tabs.

- Today. Top: today's item on a card with a one-line preview (6
  exercises · 18 sets, or Bike · 4 × 4 min, 3 min rest), "Change" and one
  Start button. Change opens a sheet for today only: a pick writes a
  one-day change (the week plan stays), picking the week plan's item or
  "Back to plan" clears it. Change hides while a session runs and after
  any session is saved today. While a session runs: its sets done or the
  interval timer's state, and Resume. Once saved: the last session saved
  today under its own name, whatever the plan held, "Done" with sets (d of
  n when some were skipped), tonnes and minutes (cardio: km, minutes and
  pace), one Review button that opens the session read-only (no Edit;
  History keeps Edit), and "+1 more today" to History when other
  sessions were saved today. A week of rest days is a rest day with
  Change. With no program and no cardio workout yet: "No plan for today"
  and "Set up your week". Under it one group of rows: a newer
  release to install (Today checks on open; tap downloads and installs,
  the row shows the download percent), the stretch routine, the eating
  window, and band sessions waiting for import. Below: a bodyweight card
  with the last entry or today's save and its change since the day
  before, − and + that step 0.1 kg (hold repeats, 20 to 250 kg), the value
  to type (comma or dot), and one button: Save for a new day, Update when
  today has another value (the save replaces it), Saved (off) when the
  value is today's entry. Text that is not 20 to 250 kg stays, shows
  "Enter 20-250 kg", and turns the button off. Today follows the date: a
  new day shows at the next minute or on resume.
- History. Two pages, Log (the default) and Progress; each keeps its own
  scroll position and state (the week shown, the bodyweight range), also
  across page and tab switches. Today's date is read again on resume and
  at midnight; every range ends at today, so a later-dated session or
  weight counts nowhere. A session belongs to the day it started.
  - Log: one week at a time, "‹ Nov 2 – 8, 2026 ›". ‹ stops at the week
    of the first session (this week when none), › at this week. A week
    card: "<done> of <planned> planned done" and "<n> to go" (this week,
    open planned days from today on), else "<n> missed" or "Full week";
    a 7-part bar (done day in its item color, open planned day gray, rest
    near black); "<sets> sets · <t> lifted · <min> min cardio · <km> km ·
    <n> stretches" over the week's finished sessions and stretch runs.
    Minutes are summed in seconds, then rounded to the nearest minute. Planned = the item each day had:
    past days keep theirs, a one-day change counts. Then one row per day,
    Monday to Sunday: a finished session (gym "d of n sets · t · min",
    cardio "km · min · pace", "Health Connect" when imported, "<k> PR"
    in green when the session set records) opens the session with Edit;
    two sessions on a day give two rows; "Rest" with the bodyweight
    ("83.0 kg") when present; under a day's last row one line per stretch
    run, "Stretch: Daily · 10 min" ("· 2 skipped" when some were skipped,
    "Stretch: Daily · skipped" when none was done); "Skipped · <item>" for a past planned day; "Today:
    <item>" opens Today; "In progress · d of n sets" opens the running
    session; "Planned: <item>" for a later day.
  - Progress: personal records, sets per muscle this week / last week,
    bodyweight (1M / 3M / All, the change since the range's first entry),
    cardio of the last 30 days per type (sessions, km, time, pace, a
    sparkline "Distance per session" with the first and last value and
    date). Every type with a session ever has a row; one without sessions
    in the 30 days says "No sessions in 30 days". A type opens its detail: pace, average heart rate, heart rate
    in work intervals (else time), and its sessions. Each section without
    data shows one line.
- Plan. A "+" adds a gym program, a cardio workout, a stretch routine or
  an exercise. This week: Monday to Sunday in seven cells with each day's
  item by name, today tinted, and a dot on a day
  that a one-day change set; Edit opens the week plan. Then the gym
  programs (the active one filled; blocks, sets and "Every Tue, Sat" from
  the week plan, or "Not in the week"), the cardio workouts (type pill,
  name, plan; archived ones last under Archived), the stretch routines,
  and the Library: Exercises (strength only, searchable, grouped by
  muscle, archived ones under one closed "Archived (n)" row) and Stretches. Settings (export, import, check
  for update) opens from the gear on Today.
- A cardio workout's type (bike, run, swim, row, or one the user adds
  with "New type…" in the workout's editor: name and color) colors it on
  Today and in History. The type is stored on the workout, so a change
  also retypes and recolors its past sessions; the editor says so.

## Exercise progress

A record is the heaviest weight lifted. The best set of a session is the
heaviest weight, then the most reps at that weight. A session sets a
record when its best set beats every earlier session's; a tie on weight
and reps keeps the record with the earlier session. Only done sets with
reps count (a 0-rep set never does). No estimated 1RM is shown.

History's Progress lists every exercise's record, newest first: five
rows, then "All <n> exercises", a searchable list of every strength
exercise (its record, or "No sets yet") that opens each one's detail. "New" marks a record set this week. The
Log's "<k> PR" counts the session's exercises that set a record, not an
exercise's first session.

An exercise's detail (from a record row) shows the Best card ("62.5 kg × 6
· Oct 29"), a chart "Heaviest set per session" with record points green,
and every session newest first with every saved set ("60×8 · 62.5×6 ·
60×5 skipped", the record set green, skipped sets dim), each opening the
session. A session whose sets were all skipped lists with no chart point. An exercise without sets
shows its muscles and "No sets yet".

## Copy and chrome

Layout rules live in DESIGN.md. They bind the mockup and the app.

No section labels, no helper sentences, no pill badges. A screen has a
title, the content, and the buttons. Status shows as color or an icon,
not a word in a capsule. Every string on screen must earn its place.
Number inputs show the value and a unit suffix only. No caption above,
no native spinner arrows. The accessible name lives in the aria-label.

## Data model

- Exercise: the one unit for gym and cardio. Fields: name, kind
  (strength | cardio), notes (short text, shown in the set row hint),
  archived flag, builtin flag.
  - Strength: primary muscle group, secondary muscle groups (zero or
    more), weight step in kg (default 2.5). Muscle groups are a fixed
    list: chest, upper back, lats, shoulders, biceps, triceps, forearms,
    core, lower back, glutes, quads, hamstrings, calves. Hyperextension is
    lower back primary with glutes and hamstrings secondary.
  - Cardio: modality (bike, run, swim, row, or any the user adds),
    intensity (low | moderate | high), structure (steady, or intervals
    with warmup sec, work sec, rest sec, rounds), and the fields it logs
    (time, distance, average heart rate, watts, laps and pool length for
    swim). Seeded: Norwegian 4x4 = bike, high, intervals 4 x 240 s work
    / 180 s rest. Zone 2 run = run, low, steady. Zone 2 swim = swim, low,
    steady. Tabata bike = bike, high, 8 x 20 / 10.
  Library seeded from a public-domain dataset plus the 14 exercises in
  TP v3 plus the cardio seeds. The user can add, edit, archive, delete
  (only when no session uses it), and merge two exercises into one
  (history moves to the survivor). Merge asks first: "Merge A into B?
  12 sets and 4 sessions move to B. A is deleted." After it, a bar says
  "Merged into B" with Undo for 10 seconds. Undo puts A back and moves
  back every row the merge moved (set logs, cardio sessions, program rows,
  plan days, dated changes). Rows logged on B in those 10 seconds stay on
  B. The undo data lives in memory only. Once a session, a program or the
  plan uses an exercise, its kind is fixed.
- Modality (shown as "Type"): name, color. Seeded bike, run, swim, row. User can add one in a cardio workout's editor.
- Program: name, active flag, ordered list of blocks. One program is
  always active. Turning another program on moves the mark; deleting the
  active program gives it to the first one left. The last program cannot
  be deleted.
- Block: one or two exercises, superset flag (only meaningful with two).
  Per exercise in the block: set count, min reps, max reps.
- Session: date-time (editable, can be backdated), what was done: a
  program snapshot (gym) or a cardio exercise snapshot, note, effort
  1-10, finished flag.
- SetLog: session, exercise, set index, weight kg, reps, status
  (done | skipped | pending). Every tap saves. Reopening resumes an
  unfinished session.
- CardioLog (phase 2): per type fields, see below.
- Bodyweight: date, kg.
- WeekPlan: seven rows, each one item: a program (gym day), a cardio
  exercise, or rest. This is the template. It fills future dates.
- DayOverride: date, item. A per-date change that wins over the template.

Sessions store a snapshot of the program, so editing the program later never
changes what history shows.

Editors save drafts late. A new program is written on Done, or when Add
block needs it as the block's home; Add block saves it as Done would, with
its name and Active choice, and from then on it stays when left without
Done. A new program left before Done or Add block writes nothing. A new
block is written together with its first exercise; leaving before that
writes nothing.

A new routine is not a draft. "Stretch routine" under "+" on Plan writes it at once, named
"New routine", with no stretches, after the other routines, and active
only when no routine existed before it; a second tap while that write
runs makes nothing. The editor then opens on it. Each edit writes at once,
leaving keeps the routine, and Delete removes an unwanted one.

Every delete or remove asks first: an exercise, a program, a block, an
exercise taken out of a block. Deleting a program deletes its blocks and
their exercises, and turns plan days and dated changes that held it into
rest days from today on. Days before today keep it, so History still
counts them as planned and names them "Deleted program" ("Deleted
workout" for a cardio exercise that is gone). Deleting a block deletes
its exercises.

## Live session

- One scrolling list. One block is open at a time: at first the first block
  with a pending set, others collapsed with a check when done. Tap any
  header to open it (the open one closes); tap the open header to close it.
- Superset blocks show sets in order A1 B1 A2 B2 A3 B3. Non-superset blocks
  show all sets of A, then all sets of B.
- Each set row shows last time's weight and reps, prefilled. Tap the check to
  log as shown. A check needs reps above 0 and, unless the exercise steps by
  0 kg (bodyweight, such as Push-up), a weight above 0. Otherwise the set
  stays pending and the row says "Enter weight" or "Enter reps" under the
  field. The stored set decides, so a value typed just before the tap counts. Tap weight or reps to edit: the field takes keyboard input
  and has +/- steppers on both sides. A tap steps once; a held stepper
  repeats faster and saves once on release. The rep range shows as a hint.
- When you beat last time, the row shows the delta in green next to the
  value, for example "+2.5" or "+1 rep".
- After a check, focus moves to the next pending set's kg field in the same
  block (else one left behind), so you can type or step at once. After a
  block's last set the next block with a pending set opens, wrapping to an
  earlier one; with nothing pending every block closes.
- No progression suggestions. No rest countdown on gym days.
- Swap: tap the exercise name, pick a replacement from the library or add one,
  then choose scope: this session only, or update the program. A swap changes
  the sets at that block and slot. "Update the program" changes the program
  row at the same block and slot, so a second swap after a session-only one
  still reaches the program. If the program has no row there any more, the
  program stays as it is.
- Discard workout asks first: "Discard this workout? 3 checked sets are
  lost." Cancel keeps everything.
- Finish sits in the header only. It shows a summary: time since start,
  sets done, total volume, new bests, then effort and note (the same values
  as the page's fields), then Save. Save finishes the session: unfinished
  sets become skipped and stay visible as empty rows in history. A set
  checked with 0 reps (older versions allowed it) counts as not done and
  never as a new best. Save and Discard leave once: a double tap never
  finishes twice or closes Today.
- History sessions are editable after the fact. There a check changes the
  set and nothing else: no block opens or closes and no field takes focus.
- Each edit of a session's date, effort or note, and the timer's duration,
  saves only that field, so quick edits in a row, in the page or the
  summary, never undo each other.

## Loads

Kilograms everywhere. One step per exercise, default 2.5.

## Cardio (phase 2)

Starting a cardio item opens its session. Steady exercises show the
fields of the exercise. Interval exercises open the interval timer, or
show one row per work interval for manual entry.

- Swim fields: pool length (default 25 m), lap count, total time, stroke.
  Distance and pace per 100 m are computed.
- Run fields: distance, time, average heart rate. Pace is computed.
- Bike fields: time, distance (optional), average heart rate, average
  watts.
- Interval structure adds one row per work interval with average heart
  rate and watts or pace. Rest length comes from the exercise.
- Every cardio session has a note and effort 1-10.
- A new entry saves only with a time or a distance (swim laps count). Until
  then Save is off and the form says "Enter time or distance". A saved
  session edited from History keeps Save on.

## Interval timer

- Open from Today when the item is an interval cardio exercise. Tap Start
  timer for a three-second preparation countdown. Work/rest phases then
  advance automatically.
- Full-screen countdown. Phase color: warmup gray, work green, rest blue.
  Big remaining seconds, round counter, total elapsed.
- Three short beeps before each phase switch, one long beep at the end.
  Screen stays on. A foreground notification keeps it running when the
  phone is locked, with pause and stop.
- On finish, the app creates the session prefilled with the exercise and
  one row per work interval. You add HR or watts and save.
- After Stop, or after the last round, the timer stores its time. If you
  leave the form without saving, Today shows "Timer stopped" with Save
  (the form with the time and rounds) and Discard, not Resume. The stored
  time decides, so this holds after an app restart too.
- Import: a manual Import button in History reads new exercise sessions from
  Health Connect and shows them as drafts. Confirmed sources: Samsung Health
  and Mi Fitness both write session, heart rate, and distance. Neither is
  confirmed to write swim laps or interval splits, so those fields stay manual.
  Nothing is saved until the user confirms a draft.
  A draft that is already imported saves nothing, so a second tap adds no copy.

## Day plan

The weekly template fills future weeks. Today's Change writes a one-day
change for today only. History shows the plan but does not change it.
Template edits apply from today on and never touch past days or dates with
an override, so a past week keeps its planned count. The week plan screen
says "Each weekday repeats every week. To change one day only, use Change on
Today."

## Backup

Settings has Export and Import. Export writes one JSON file through the share
sheet. Import offers replace or merge, and writes in one transaction. After
an import exactly one program is active when any exists: merge keeps this
phone's active program; replace takes the backup's first active program in
list order, or the first program when none is active.

Routines keep at most one active, chosen before the import writes anything.
A merge onto a phone that has routines keeps this phone's active routine and
clears the mark on imported ones; when no routine is active on the phone, none
is active afterwards. A replace, or a merge onto a phone with no routine,
keeps the backup's first active routine by sortOrder, or none. A built-in
routine that the import seeds is never active. Every import also repairs three things in the
same transaction. It deletes routine rows whose routine or stretch is missing.
It creates the eating-window settings row with defaults when neither the
backup nor the phone has one. It writes the backup's stretch sessions over the
phone's, the one case where a recorded session changes. After an import, a
restore or a reset, the app re-arms the window alarms and redraws the widgets.

## Update

Settings has Check for update, and Today checks once on open and shows an
"Update to <tag>" row when a newer release is out. It reads the latest GitHub
release, compares the version, downloads the APK, and hands it to the package
installer. Install over the old build with no uninstall. Needs
REQUEST_INSTALL_PACKAGES.

## Stretching (phase 3)

Stretching is a daily routine, not a plan item. Today shows one row: the
active routine and its length, the line green with a check when done today. Tap opens a ready screen with an automatically playing 3D demonstration, stretch name, side, and
Start button. Start gives a three-second preparation countdown before work.
Numbered steps let the user inspect setup and movement, and Change view
switches camera angles. Demo playback is independent of the exercise clock.
Each stretch and side change waits for the user to start the next one.
Held poses stay still; repeated movements follow the timer and freeze on pause. Same audio engine and lock-screen
notification as the interval timer. The end writes a session and returns to
Today. Plan lists the routines (one active) and has a Stretches row in its Library: a stretch
library with 3D thumbnails and step guides. Custom stretches pick a guide from the set.

Routines and their rows sort by sortOrder, then id. A new routine goes after
the highest sortOrder. Turning a routine on makes it the only active one;
turning it off leaves none active, and then Today falls back to the first
routine and the widget shows no routine. A stretch can be deleted only when it
is not built in and no routine row uses it at the moment Delete runs. Deleting
a routine deletes its rows and keeps its stretch sessions, which hold the
routine name and totals.

A run's session is written once per session id, and a second write of the same
id changes nothing. A run that finishes in the live service after its routine
was deleted still records one session with the name it started with. Two
limits are accepted. First, a session deleted from History, by a replace or by
a reset comes back if a stale timer snapshot of the same run is recorded
again; the timer clears its snapshot after it records, so only such a replay
can do this. Second, a snapshot restored after process death is dropped when
its routine was deleted meanwhile, and that run records no session.

## Eating window (phase 3)

Fixed daily hours, for example 12:00 to 20:00. The state is open inside the
hours, or after an open-early tap, and closed after a close-late tap. Today
shows the state and the time left. A small home-screen widget shows the same.
Exact alarms at the boundaries refresh the widget and post the reminders.
Each setting writes only its own column. An opening or closing time that
would leave the hours shorter than one hour, or cross midnight, is refused.
History shows one cell per day, filled when the window was kept.
History counts from the day the window was first turned on. The app keeps
that date in DataStore (key `window_since`), not in the database and not in
the backup. Turning the window off and on keeps the first date. Without a
stored date (an existing user, or after an import or a reset) the start is
the earliest window day, else today, and the app stores it once. Days
before the start are grey and not counted. Until one day has settled the
summary reads "Tracking since Nov 3." instead of "0 of 0 days".

## Out of scope

Gym rest countdown, progression schemes, plate math, iOS, sync, accounts,
light theme, other languages, structured cardio import of laps.
