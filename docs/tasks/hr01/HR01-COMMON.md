# HR01 common: heart rate for every session, by time range

Read this before `HR01-P1-data.md` or `HR01-P2-import-ui.md`. It holds the
decided design. The phase files hold the contract rows.

## Why

The owner controls every workout in the app (gym sessions, the 4x4 timer)
and wears a Mi Band 9 that sends data to Health Connect through Mi Fitness.
Two defects today:

1. A 4x4 done with the app timer and then imported from the band becomes
   two sessions (`Repository.importDraft` always inserts; nothing looks for
   an app session at the same time).
2. Band strength workouts are hidden from the import on purpose
   (`ImportScreen.kt`, `drafts?.filter { !it.strength }`), so a gym session
   never gets a heart rate.

Both have one cause: the app treats a band workout as a session. The fix:
**the app session owns its time range, and the app reads the band's heart
rate for that range.** A band workout becomes a session only when no app
session covers its time.

Owner decisions (2026-11-26): merge the old double sessions once; store the
clock time of each 4x4 work round; store the clock time of each set check;
all data stays on the phone; the app never suggests exercises, sets, or
loads (show facts only).

## Data model (Room 4 to 5)

- `set_logs.checkedAt INTEGER` (nullable, epoch ms): the moment the set
  became done in a live session.
- `interval_logs.startedAt INTEGER`, `interval_logs.endedAt INTEGER`
  (nullable, epoch ms): wall-clock start and end of that work round.
- `sessions.maxHr INTEGER` (nullable). `sessions.avgHr` exists.
- New table `session_hr`, one row per session that was looked up:
  - `sessionId TEXT PRIMARY KEY`
  - `fromMs INTEGER NOT NULL`, `toMs INTEGER NOT NULL`: the range read
  - `samples TEXT NOT NULL`: `offsetSec:bpm` pairs joined by `,`, offset
    from `fromMs`, one pair per 5-second bucket that has data (the bucket's
    mean, rounded). Empty string when no sample was found.
  - `sampleCount INTEGER NOT NULL`: raw samples read, before bucketing
  - `origins TEXT NOT NULL`: package names joined by `,`
  - `fetchedAt INTEGER NOT NULL`
  - `state TEXT NOT NULL`: `filled`, `waiting` (no sample yet, try again),
    or `none` (gave up)
- Migration 4 to 5 only adds columns and the table. No row changes.
- Backup: `Backup` gains `sessionHr: List<SessionHr> = emptyList()`. An old
  backup file imports as before. Replace clears and writes it; merge
  upserts by `sessionId` and drops rows whose session is missing.
- Every delete of a session also deletes its `session_hr` row.

## The time range of a session (pure function `hrWindow`)

A session has a **live window** only when its clock times are real:

- The session's `date` equals the local date of `startedAt`. A backdated
  session has no window.
- Gym: from `startedAt` to the earlier of `finishedAt` and (last
  `checkedAt` + 5 min). With no `checkedAt` at all: `startedAt` to
  `finishedAt`, and no window when that is longer than 4 hours.
- Cardio with interval logs that hold times: from `startedAt` to (last
  `endedAt` + 60 s), never past `finishedAt` when that is set.
- Other cardio: from `startedAt` to `finishedAt`, but no window when
  `timeSec` is set and (`finishedAt` - `startedAt`) is shorter than 60 % of
  `timeSec` (the user typed the session in afterwards), or when the window
  is longer than 6 hours.
- An unfinished session has a window only when it is cardio with a stored
  `timeSec` above 0 (the timer stopped, the form is not saved yet):
  `startedAt` to `startedAt + timeSec + 15 min`, cut at now.
- A window shorter than 60 s is no window.

A session that took a band workout by hand ("Add to", phase 2) uses that
workout's start and end as its window instead.

## Fill

`fillHeartRate(now)` in the repository layer, with the Health Connect read
behind an interface so tests give it fake samples:

- Candidates: sessions with a live window that ended in the last 30 days
  and have no `session_hr` row, or a row in state `waiting`.
- For each: read heart rate samples in the window. With at least 10 raw
  samples: write the `session_hr` row as `filled`, and set
  `sessions.avgHr` and `sessions.maxHr` **only where they are null** (a
  typed value stays). For a cardio session with interval logs that hold
  times, set each interval's `avgHr` only where null, from the samples
  inside `[startedAt, endedAt)`. For a cardio session whose exercise logs
  distance, set `distanceM` only where null, from the Health Connect
  distance total of the window, when it is above 0.
- With fewer than 10 samples: write or keep `waiting` until the window's
  end is older than 7 days, then `none`.
- All writes for one session happen in one transaction, and they re-read
  the session inside it: a session deleted meanwhile writes nothing.
- It runs when a session finishes (gym Finish, cardio Save), when the app
  comes to the foreground (at most once per 10 minutes), and when the
  Import screen opens. Without Health Connect or without the heart rate
  permission it does nothing and never throws.
- No background job in this task.

## Clock times

- `checkSet` stamps `checkedAt = now` only when the set's session is
  unfinished. A check in a finished session (an edit from History) leaves
  `checkedAt` as it is. A set that goes back to pending or becomes skipped
  gets `checkedAt = null`.
- The interval timer records, for each work round, the wall-clock time when
  the round started and when it ended (its last second, a Skip, or Stop).
  A pause inside a round stays inside that round's range. When one tick
  crosses several phases (restore after process death), each boundary gets
  its own computed time, not the time of the tick. On finish and on Stop
  the service writes the ranges to `interval_logs` by session and index, in
  the same place where it writes the duration. Rounds that never started
  keep null.

## Band workouts (phase 2)

- A band workout is **covered** when its range overlaps the live window of
  an app session (gym or cardio) by at least half of the shorter of the
  two. Covered workouts are not drafts: the Import list and Today's count
  leave them out. Strength workouts from the band stay hidden as today.
- A band cardio workout that is not covered stays a draft. Its sheet gets a
  first group "Add to" with the app's cardio sessions of the same day that
  have no `session_hr` row in state `filled`. A pick sets that session's
  window to the workout's range, fills it at once, and the draft is gone
  (its record id goes into the session's `sourceId`, so it never returns).
- Old pairs, once: an app cardio session (no `sourceId`) and an imported
  cardio session (`sourceId` set) whose ranges overlap by at least half of
  the shorter one, with exercises of the same type. The app session takes
  the copy's `avgHr`, `distanceM`, `laps`, `poolLength`, `effort`, `note`
  and each interval `avgHr` and `watts`, only where its own value is null
  or empty; then the copy and its logs are deleted. Before the first
  delete, the app writes one backup file (`exportJson`) into its private
  files folder. The pass runs once (a DataStore flag set after it ends) at
  app start, off the main thread.

## What does not change

- No suggestion of any kind. Heart rate, rest, and times are shown as
  facts.
- No network use is added. Health Connect is on the phone.
- The watch-only cardio import (draft, pick exercise, save) works as today.
- Layout rules in `DESIGN.md` bind every new row and screen: no cards with
  per-row buttons, no pills, no captions, no helper sentences.
- No comment that a name, a type, or a test can say. No task IDs or review
  IDs in code (`comment-guard` refuses them).
- No AI attribution in commits.
