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
  pace; minutes rounded to the nearest, as in History and the finish
  summary, so 3 min 40 s is 4 min), one Review button that opens the session read-only (no Edit;
  History keeps Edit), and "+1 more today" to History when other
  sessions were saved today. A week of rest days is a rest day with
  Change. With no program and no cardio workout yet: "No plan for today"
  and "Set up your week". Under it one group of rows: a newer
  release to install (Today checks on open; tap downloads and installs,
  the row shows the download percent), the stretch routine, the eating
  window, and band sessions waiting for import. Below: a bodyweight card
  with the last entry, or after today's save the 7-day average and its
  change per week (else the change since the day before), − and + that step 0.1 kg (hold repeats, 20 to 250 kg), the value
  to type (comma or dot), and one button: Save for a new day, Update when
  today has another value (the save replaces it), Saved (off) when the
  value is today's entry. Text that is not 20 to 250 kg stays, shows
  "Enter 20-250 kg", and turns the button off. Today follows the date: a
  new day shows at the next minute or on resume. The home-screen widget's
  Start resumes a running session, else starts today's item; once a
  session is saved today it only opens Today, never a second session.
- History. Two pages, Log (the default) and Progress, named side by side
  in the large title as tabs (the open one marked). The title row stays
  at full size while the page scrolls, so both tabs work at any scroll
  position. Each page keeps its own
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
    in green when the session set records, "<k> rep PR" when it set rep
    records) opens the session with Edit;
    two sessions on a day give two rows; "Rest" with the bodyweight
    ("83.0 kg") when present; under a day's last row one line per stretch
    run, "Stretch: Daily · 10 min" ("· 2 skipped" when some were skipped,
    "Stretch: Daily · skipped" when none was done); "Skipped · <item>" for a past planned day; "Today:
    <item>" opens Today; "In progress · d of n sets" opens the running
    session; "Planned: <item>" for a later day.
  - Progress: personal records, sets per muscle this week / last week,
    bodyweight (1M / 3M / All, the change since the range's first entry,
    the 7-day average and its change per week, the average as a second
    line in the chart),
    cardio of the last 30 days per type (sessions, km, time, pace, a
    sparkline "Distance per session" with the first and last value and
    date). Every type with a session ever has a row; one without sessions
    in the 30 days says "No sessions in 30 days". A type opens its detail: pace, average heart rate, heart rate
    in work intervals (else time), time at or above 85 % and time in zone (see
    "Heart rate"), and its sessions. Each section without
    data shows one line.
  - Compare two periods: the last row of Progress. Two rows name the
    left and the right column's period as a date range ("Oct 3 – Oct 30",
    with the year for a date outside this year); the defaults are the 4
    weeks before and the last 4 weeks. A tap opens one choice list: Last 4
    weeks, The 4 weeks before (the 28 days before those), Last 12 weeks,
    The 12 weeks before, This year, Last year, then each program with a
    finished session (from its first to its last session), then Custom
    dates with From and To (a From after the To moves the To to that day).
    The choices survive rotation and a process restart. Under them, both
    periods side by side, the left one first: days, gym sessions, done
    sets with reps, sets per week, lifted, cardio sessions, cardio time,
    distance and stretch runs; bodyweight average and entries; sets per
    muscle per week (the same rule as above); the best set of each
    exercise; and per cardio type its sessions, distance and pace. A value
    that does not exist is "–"; a group or cardio row with nothing to show
    is left out. The screen states numbers only: no verdict, arrow or color
    tells which period is better.
- Plan. A "+" adds a gym program, a cardio workout, a stretch routine or
  an exercise. This week: Monday to Sunday as seven day symbols (gym,
  run, bike, swim, other cardio, rest) under the day's letter, today's
  letter in accent, and a dot on a day that a one-day change set. A tap
  on a day selects it, rings it, and names its item in full under the
  strip; today is selected first. On a screen too narrow for seven 48 dp
  columns the strip scrolls sideways. Edit opens the week plan. Then the gym
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
reps count (a 0-rep set never does). No estimated 1RM is shown as a value.
The rate per setup below is the only place that uses one.

A rep record is more reps at a weight already lifted. A done set above
0 kg is one when an earlier session has a set at its weight or heavier
and every such set has fewer reps. A set of the same session that is as
heavy or heavier with as many reps or more takes its place (of two equal
sets the first one counts), and so does the session's weight record set.
Most weeks hold no weight record, so this is where most progress shows.
A 0 kg exercise has no rep records: more reps there is a weight record.

History's Progress lists every exercise's record, newest first: five
rows, then "All <n> exercises", a searchable list of every strength
exercise (its record, or "No sets yet") that opens each one's detail. "New" marks a record set this week. The
Log's "<k> PR" counts the session's exercises that set a record, not an
exercise's first session. The finish sheet's "New bests" uses the same rule
and the same count. "<k> rep PR" and the finish sheet's "Rep records"
count the exercises with a rep record and no weight record. The finish
sheet compares with every other session, also later ones when an old
session is edited.

An exercise's detail (from a record row) shows the Best card ("62.5 kg × 6
· Oct 29"), a chart "Heaviest set per session" with record points green,
and every session newest first with every saved set ("60×8 · 62.5×6 ·
60×5 skipped", the record set green, rep record sets bold, skipped sets
dim, a "PR" chip for a weight record, else a "Rep PR" chip), each opening
the session. Between the Best card and the sessions, "Most reps by
weight" lists the sets that no other set matches in both weight and reps
("80 kg" and its date, "× 8"), heaviest first, at most six, when there
are two or more. A session whose sets were all skipped lists with no chart point. An exercise without sets
shows its muscles and "No sets yet".

A program change shows on the exercise as a change of setup. The setup of
a session is its program id, the count of the exercise's set rows (every
status) and the rep range of its first row. A period is a run of sessions
with one setup. A single session inside a run is a one-day deviation and
stays in the period before it (or the next one when there is none before).
The latest run is always a period. After the single sessions join, two
neighbor periods with one setup are one period, so A A B A is one period
and A A B is two. A renamed program keeps its id, so it is
no change. When an exercise has two periods or more, its detail shows
"Est. 1RM per week" between the Best card and "Most reps by weight", or
"Reps per week" for an exercise that never had a counted set above 0 kg.
One row per period, newest first: "Since Nov 3" or "Oct 2 – Nov 2" (the
year only when it is not this year) over "TP v4 · 4 sets · 5–8 reps", and
the rate on the right. The rate is the least-squares slope of the session
values per week: the best Epley estimate (weight × (1 + reps / 30)) of the
counted sets above 0 kg, or the most reps for an exercise without weight.
It needs 3 valued sessions over 14 days or more, else the row shows "–".
It rounds like the bodyweight trend ("+0.8", "−0.3", "±0.0"). The app
says nothing about which setup was better. The "Heaviest set per session"
chart draws a thin muted vertical line at the first charted session of
each period after the first. A period with no charted session gets no
line.

One bodyweight entry can move a kilogram from water alone, so the trend
uses a 7-day average: the mean of the entries from 6 days before a date to
that date, shown only with 3 or more entries. The change per week is
today's average minus the average that ends 7 days earlier ("7-day avg
83.2 kg · −0.3 kg/week", "±0.0" inside 0.05 kg). History's chart draws the
average for each entry as a thin muted line; its first days use entries
from before the range.

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
- Modality (shown as "Type"): name, color. Seeded bike, run, swim, row. User can add one in a cardio workout's editor.
- Program: name, active flag, ordered list of blocks. One program is
  always active. Turning another program on moves the mark; deleting the
  active program gives it to the first one left. The last program cannot
  be deleted.
- Block: one or two exercises, superset flag (only meaningful with two).
  Per exercise in the block: set count, min reps, max reps.
- Session: date-time (editable, can be backdated), what was done: a
  program snapshot (gym) or a cardio exercise snapshot, note, effort
  1-10, finished flag. Clock facts, all nullable: `timedFrom` and
  `timedTo` (the workout's range, see "Heart rate"), `startedElapsed`
  (elapsed realtime at the start of a gym session), `timerRun` and
  `timerDone` (the numbers of the latest timer run that started and of the
  latest one that committed), and `linkedIds` (Health Connect record ids
  merged into this app session).
- SetLog: session, exercise, set index, weight kg, reps, status
  (done | skipped | pending), `checkedAt`. Every tap saves. Reopening
  resumes an unfinished session.
- IntervalLog: one per work round, with typed average heart rate and
  watts, and `startedAt`, `endedAt`: the clock times of that round. A
  round has both or neither.
- SessionHr: at most one per session, written only by the heart rate
  reader, "Remove", session delete, and backup import. It holds the range
  that was read, a state (filled, waiting, none, off), the 5-second
  buckets as `offsetSec:bpm`, the sample counts, the packages, and the
  average and maximum (filled only). It has no foreign key, so every path
  that deletes a session deletes this row too.
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
- Swap: tap the exercise name or the swap symbol beside it, pick a replacement from the library or add one,
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
  sets done, total volume, new bests, rep records, then effort and note (the same values
  as the page's fields), then Save. Save finishes the session: unfinished
  sets become skipped and stay visible as empty rows in history. A set
  checked with 0 reps (older versions allowed it) counts as not done and
  never as a new best. Save and Discard leave once: a double tap never
  finishes twice or closes Today. Save lands on Today, also for a session
  opened from History's "In progress" row.
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
  one row per work interval. You add HR or watts and save. Save of an
  unfinished session lands on Today, wherever the form was opened; Save of
  an edit to a saved session goes back to History.
- Each run has a number. The Start action, on the screen or in the
  notification, writes `timerRun + 1` to the session and takes the anchor
  (wall time and elapsed realtime at that instant). Ready takes no anchor,
  and Stop on Ready stores nothing. Every time of the run is the anchor's
  wall time plus the elapsed realtime since it. A work round gets its start
  when work begins and its end at its last second, Skip, or Stop. A pause
  stays inside its round.
- A run is trusted while its clocks stay in step at every restore and at
  its end. A reboot, a changed phone clock, or a snapshot of the older app
  version makes it untrusted for good. On finish and on Stop one
  transaction writes the duration (only when the owner typed nothing since
  Start), and for a trusted run the range and every round's times. An
  untrusted run writes null clock fields. The commit of run n is accepted
  only while `timerRun` is n, `timerDone` is not n, and no import or reset
  ran since the commit began. A replay or an older run writes nothing. The
  snapshot of an ended run goes only after its commit returns; a failed
  commit runs again at the next app start.
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

## Heart rate

The app reads the band's heart rate from Health Connect for the time range
of each workout. It reads only, writes only `session_hr`, and never writes a
column the owner can type into. A typed value shows where it exists; a
measured value shows only where the typed one is empty. No data is better
than wrong data: an uncertain time gives no range, and a thin read shows
nothing.

Clocks. A wall time counts as a clock fact only when the wall clock and
elapsed realtime moved by the same amount since the anchor, within 2 s. A
gym session takes its anchor at Start. A set check stamps `checkedAt` only
in an unfinished session with clocks in step; else the set is done without
a stamp. A second check keeps the first stamp. Every path that makes a set
pending or skipped clears it.

The range is stored once, by the code that saw the workout:

- Gym Finish, with at least one stamp and clocks in step: from the start
  to the last stamp plus 5 minutes, at most the finish time.
- The timer commit of a trusted cardio run.
- The band's own start and end for an imported cardio session.
- "Use this time range": the owner confirms the start and finish of a
  finished gym session without a range, 10 minutes to 4 hours long, on
  the date of its start. The write checks that the shown times are still
  the session's.

The range a read uses needs a finished session whose date is the local
date of `timedFrom` or of its start. Gym: 10 minutes to 4 hours. App cardio
adds 60 s after `timedTo`, but never past the finish; cardio ranges are 2
minutes to 6 hours. A session moved to another date has no range.

A read takes every record that touches the range from every page. It
accepts samples inside the range with 30 to 230 bpm, and drops exact
duplicates. The package with the most accepted samples is the origin, and
only its samples count, one per instant. The read is complete with 10 or
more instants that span at least half of the range. Complete gives
`filled`. Else the row is `waiting` while the range ended 7 days ago or
less, then `none`. A missing Health Connect, a missing heart rate
permission, or any error writes nothing and does not count as a try.

One runner reads, one read at a time. Gym Finish, cardio Save, and the app
coming to the foreground (at most once per 10 minutes of elapsed time) ask
it to run; none of them waits. A pass first deletes each row that is not
off and no longer matches its session's range. Then it reads each session
whose range ended in the last 30 days and that has no row or a waiting row.
It never reads off or none. A result is written only when, in its
transaction, the session still exists, no import or reset ran since the
read began, the range is unchanged, and the row is the one the read started
from.

Reread reads one session now, at any age and from any state. Remove turns
the row off with no samples and no values, and no pass reads it again.

The average of a work round is computed when shown, from the buckets of a
filled row inside the round's times, when 2 or more buckets fall inside.

What the screens show comes from one rule, `hrFacts`, and no screen reads a
row directly. A row for another range than the session's current one shows
nothing. The maximum is always the measured one.

- Review shows "avg 128 · max 164 bpm" under the stats line, or only the
  value that exists, or no line. A screen reader hears "average 128,
  maximum 164 beats per minute". The line follows the session and its row,
  so a read that lands while Review is open shows up.
- Review's average heart rate row and its rounds, and the cardio type
  screen's two heart rate charts, show the measured value where the typed
  one is empty.
- The cardio form shows the measured average, and each round's measured
  average, as the placeholder of an empty field. Save writes only what the
  owner typed.
- Settings has a "Heart rate data" row. Its screen lists the finished
  sessions of the last 30 days, newest first: the name over the date, and
  the state on the right ("avg 128 · max 164", "Waiting for data", "No
  data", "Removed", "Not read", "No time range"). A tap opens a sheet with
  the facts of the read: the range as clock times, readings read and used,
  first and last reading, the longest gap, each work round's average, the
  source app and other apps seen, average and maximum. A fact without a
  value is left out.
- The sheet has "Read again" (not without a range) and "Remove" (only
  while a row that is not off exists; it asks first). "Read again" shows
  its result as one line: the new values, "No data yet", or "Health
  Connect did not answer". A result that no longer matches the row shows
  nothing. A second tap while a read runs does nothing. A session deleted
  meanwhile closes the sheet.
- A finished gym session without a range that "Use this time range"
  accepts shows its start and finish as clock times and that action.
- Without Health Connect or without the heart rate permission, the screen
  shows the Import screen's card for that case. It asks only for heart
  rate.

Review of a finished session with a range shows its heart rate as one
picture. What it shows comes from `hrFacts` alone, so a stale, removed, or
waiting row shows nothing. With a filled row, a chart sits under the header
figures: the buckets as a curve, cut where two neighbors are more than 60 s
apart (a lone bucket is a dot), a tick at the bottom at each set check
inside the range (gym), and a band behind each timed round (cardio). Nothing
is drawn between buckets. A gym session with 2 or more checks inside the
range also gets "Sets in order" after its exercises: each checked set in
time order, with the clock time of its check, the time since the check
before, and the highest bucket from that check to its own when 2 or more
buckets fall in between. The group shows without heart rate too, then with
no bpm. An unfinished session or one without a range shows neither.

The maximum heart rate is a setting, a whole number from 100 to 230 bpm,
empty by default. It sits in the Health group of Settings as "Maximum heart
rate" with the typed value on the right ("186 bpm"). Without a typed value
the row shows the measured one in muted ("183 bpm"). That is the highest
maximum of the filled reads of sessions that finished in the last 365 days.
With neither, the row says "Not set". A tap opens a sheet with one number
field and Save. Save is off for a value outside 100 to 230, and an empty
field removes the typed value. The setting lives in DataStore next to the
other app settings, so a backup does not carry it. Reset removes it.

The app uses the typed maximum, else the measured one. With no maximum,
none of the figures below show. Each figure counts the 5-second buckets of
a filled read and nothing else. A bucket stands for 5 s, and a gap adds no
time.

- Hard line: 85 % of the maximum, rounded up. A maximum of 186 gives 159
  bpm. The time at or above it is the bucket count times 5 s.
- Zone: from 60 % of the maximum rounded up to 70 % rounded down, both
  ends inside. A maximum of 186 gives 112 to 130 bpm. The shares below, in
  and above the zone are rounded half up. When the three do not sum to 100,
  the part with the largest remainder takes the step. On a tie the order is
  below, in, above, so 1, 1 and 1 buckets give 34, 33 and 33.
- Round peak: the highest bucket inside the round's `[startedAt,
  endedAt)`. It needs 2 or more buckets there.
- Round drop: the round's last bucket minus the lowest bucket of the rest
  after it. The rest runs from `endedAt` to the next round's `startedAt`,
  or to the range end after the last round. The drop needs a peak and 2 or
  more buckets in the rest. A negative drop shows as it is.
- A round without both times, or one that starts outside the range, has no
  figures.

Review of a finished cardio session with a filled read adds a "Heart rate"
group after its rounds, or after its figures when it has no rounds. A
high-intensity exercise gets "At or above 159 bpm" with the time ("11:35").
A low-intensity one gets "Below 112 bpm", "112 to 130 bpm" and "Above 130
bpm" with their percents. Moderate intensity gets neither. Each round with
a peak adds "Round 1" with "peak 168 · drop 41". The rows are not
tappable. They speak in full words: "At or above 159 beats per minute, 11
minutes 35 seconds", "Round 1, peak 168, drop 41 beats per minute". They
follow the setting and the read while Review is open.

The cardio type screen adds "At or above 85 %", minutes per session of its
high-intensity exercises, and "In zone", the percent per session of its
low-intensity ones. Each point uses today's maximum, and a chart needs 2
points.

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

The clock columns and the heart rate rows travel in the file. A file from
before them imports as before, and its sessions have no range. No import
takes `startedElapsed` from a file: an anchor holds only on the phone and
the boot that took it. Each heart rate row is decoded and checked by
itself, and a bad one never fails the preview or the import. A row is
taken only when its session exists after the import, its range is not
empty, its state is one of the four, every sample lies inside the range
with 30 to 230 bpm, a filled row has samples and values in that range, and
an off row has neither. Of two rows for one session the last wins.

Replace takes the file's rows. Merge keeps the local unit: a session, set,
or round that exists on the phone keeps its own range, anchor, run numbers,
linked ids, stamp, round times, and heart rate row. A new one takes the
file's values with its row. Then, in the same transaction, a session whose
start, finish, kind, or exercise changed loses its range, anchor, and round
times, and a set that is not done loses its stamp.

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
Skip moves to the next stretch or side; Back returns to the one before
with its timer reset, waiting for Start, and takes back its done or
skipped count (off on the first). The player leaves once, and only while
it is on top: its end, its delayed close, Stop and system Back never pop
the screen under it. The routine's length reads "9 stretches · 10 min"
("1 stretch") on Today and Plan.
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
