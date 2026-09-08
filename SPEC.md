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

Each phase ships as an installable APK.

## Navigation

Three tabs.

- Today. Top: a week strip, Monday to Sunday, one cell per day with the
  planned item drawn as a colored dot (modality color; strength is the
  accent color; a high-intensity item has a filled dot, low intensity a
  ring). Today is highlighted. Tap a day to see or change its item.
  Middle: today's item name, a one-line preview (7 blocks, 42 sets, or
  4 x 4 min on bike), one Start button. Below: bodyweight as a small
  sparkline of the last 30 days with the latest value, and a small add
  field: type a number, tap add. No card chrome, no Save button, no
  visible caption.
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
  (history moves to the survivor).
- Modality: name, color. Seeded bike, run, swim, row. User can add.
- Program: name, active flag, ordered list of blocks.
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

## Live session

- One scrolling list. The current block is expanded, others collapsed with a
  check when done. Tap any header to open it.
- Superset blocks show sets in order A1 B1 A2 B2 A3 B3. Non-superset blocks
  show all sets of A, then all sets of B.
- Each set row shows last time's weight and reps, prefilled. Tap the check to
  log as shown. Tap weight or reps to edit: the field takes keyboard input
  and has +/- steppers on both sides. The rep range shows as a hint.
- When you beat last time, the row shows the delta in green next to the
  value, for example "+2.5" or "+1 rep".
- After a check, focus moves to the next set's kg field so you can type or
  step at once.
- No progression suggestions. No rest countdown on gym days.
- Swap: tap the exercise name, pick a replacement from the library or add one,
  then choose scope: this session only, or update the program.
- Finish shows a summary: sets done, total volume, new bests. Unfinished sets
  become skipped and stay visible as empty rows in history.
- History sessions are editable after the fact.

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

## Interval timer

- Start from Today when the item is an interval cardio exercise.
- Full-screen countdown. Phase color: warmup gray, work green, rest blue.
  Big remaining seconds, round counter, total elapsed.
- Three short beeps before each phase switch, one long beep at the end.
  Screen stays on. A foreground notification keeps it running when the
  phone is locked, with pause and stop.
- On finish, the app creates the session prefilled with the exercise and
  one row per work interval. You add HR or watts and save.
- Import: a manual Import button in History reads new exercise sessions from
  Health Connect and shows them as drafts. Confirmed sources: Samsung Health
  and Mi Fitness both write session, heart rate, and distance. Neither is
  confirmed to write swim laps or interval splits, so those fields stay manual.
  Nothing is saved until the user confirms a draft.

## Day plan

The weekly template fills future weeks. On the History calendar, long-press a
day to change its activity, or drag one day onto another to swap the two.
An overridden date shows a small mark. Template edits never touch past days
or dates with an override.

## Backup

Settings has Export and Import. Export writes one JSON file through the share
sheet. Import offers replace or merge.

## Update

Settings has Check for update. It reads the latest GitHub release, compares the
version, downloads the APK, and hands it to the package installer. Install over
the old build with no uninstall. Needs REQUEST_INSTALL_PACKAGES.

## Out of scope

Gym rest countdown, progression schemes, plate math, iOS, sync, accounts,
light theme, other languages, structured cardio import of laps.
