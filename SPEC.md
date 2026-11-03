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

- Today. Top: a week strip, Monday to Sunday, one cell per day with the
  planned item drawn as a colored dot (modality color; strength is the
  accent color; a high-intensity item has a filled dot, low intensity a
  ring). Today is highlighted. Tap a day to see or change its item.
  Middle: today's item name, a one-line preview (7 blocks, 42 sets, or
  4 x 4 min on bike), one Start button; while a session runs, the time
  since its start, its sets done, and Resume. Under it one group of rows:
  a newer release to install (Today checks on open; tap downloads and
  installs, the row shows the download percent), the stretch routine, the
  eating window, and band sessions waiting for import. Below: bodyweight as a chart of the last 30 days with the latest
  value, and an entry card: the value as a big figure (tap to type) over
  a ruler to drag, its change since the last day weighed, and one button:
  Add for a new day, Update when today has another value (the save
  replaces it), Saved (off) when the value is today's entry. While the
  typed value is blank or not above 0 the button is off.
- History. Month calendar, one colored dot per session. Tap a day to open
  the session. Below: bodyweight chart, and an exercise picker that shows
  a progress card (see Exercise progress).
- Programs. Program list (many, one active), exercise library (strength
  and cardio, filter by kind, muscle, modality), modalities, week plan,
  settings (export, import, check for update).

## Exercise progress

Per exercise, the History tab shows:

- A line chart of estimated 1RM per session over all sessions, Epley
  formula from the best set: weight x (1 + reps / 30).
- Top set per session (highest weight, then highest reps at that weight).
- The last sessions as a table, sets as weight x reps, newest first.

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
  12 sets and 4 sessions move to B. A is deleted." Merge then moves every
  row (set logs, cardio sessions, program rows, plan days, dated changes)
  and deletes A in one transaction. There is no Undo. If A or B is gone
  by then, or they are the same exercise, nothing is written and the
  editor stays open with "Not merged: one of the two exercises is gone."
  Once a session, a program or the plan uses an exercise, its kind is
  fixed.
- Every closing action of an editor (Done, Back, Archive, Unarchive,
  Delete, Merge, and Save, Discard and Back in a session or cardio form)
  runs once: a second tap, or a second exit in the same frame, does
  nothing. A delete or merge that is refused when it runs keeps the editor
  open, and the next action works.
- Modality: name, color. Seeded bike, run, swim, row. User can add.
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

A new routine is not a draft. "+" on Stretching writes it at once, named
"New routine", with no stretches, after the other routines, and active
only when no routine existed before it; a second tap while that write
runs makes nothing. The editor then opens on it. Each edit writes at once,
leaving keeps the routine, and Delete removes an unwanted one.

Every delete or remove asks first: an exercise, a program, a block, an
exercise taken out of a block. Deleting a program deletes its blocks and
their exercises, and turns plan days and dated changes that held it into
rest days. Deleting a block deletes its exercises.

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
  row at the same position: blocks in (order, id), rows in (slot, id), rows
  whose exercise is gone left out, as the session numbered them. A second
  swap after a session-only one still reaches the program.
- Session start stores the session's origin outside the database (program,
  start time, and a fingerprint of every block and row of the program).
  "Update the program" changes the program only while the session is
  unfinished, its origin is there, and the program is exactly as it was at
  the start or after the last update from this session. Any change to the
  program's blocks or rows since (order, exercise, sets, reps, a block or
  row removed or added) refuses: only the workout changes, and a sheet
  says "The program changed after this workout started. Only this workout
  changed." Finish and Discard remove the origin; an import (both modes)
  and Reset remove every origin. Origins are not in the backup.
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
- A new entry saves only with a time, a distance or laps above 0. Until
  then Save is off and the form says "Enter time or distance". The form
  always shows Time, also for an exercise that lists only heart rate or
  watts. Save checks the stored row with the form's edits, so a time the
  timer stores after the form opened counts. The data layer checks the
  same rule again when Save runs; a refusal writes nothing, keeps the form
  open and shows the line. A saved session edited from History keeps Save
  on and is not checked.

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

The weekly template fills future weeks. On the History calendar, long-press a
day to change its activity, or drag one day onto another to swap the two.
An overridden date shows a small mark. Template edits apply from today on and
never touch past days or dates with an override.

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
Today. Programs has a Stretching row: routines (one active), a stretch
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
