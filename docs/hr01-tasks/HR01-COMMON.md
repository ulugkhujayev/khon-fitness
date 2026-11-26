# HR01 common: heart rate for every session, by time range (design 3)

Read this before a phase file. It holds the decided design; the phase files
hold the contract rows. Designs 1 and 2 were attacked
(`HR01-ATTACK-R1.md`, `HR01-ATTACK-R2.md`, both open-ended). Design 3
removes the parts that kept failing instead of patching them. Attack R3
(`HR01-ATTACK-R3.md`) found one more instance of a known class (a wall
clock that moves), and this text now applies one clock rule to gym and
timer alike. "What
changed after the attacks" at the end lists each decision.

## Why

The owner controls every workout in the app (gym sessions, the 4x4 timer)
and wears a Mi Band 9 that sends data to Health Connect through Mi Fitness.
Two defects today:

1. A 4x4 done with the app timer and then imported from the band becomes
   two sessions (`Repository.importDraft` always inserts).
2. Band strength workouts are hidden from the import on purpose, so a gym
   session never gets a heart rate.

The fix: **the app session owns its time range, and the app reads the
band's heart rate for that range.** A band workout becomes a session only
when no app session of the same kind ran at that time.

Owner decisions (2026-11-26): merge the old double sessions once (design 3:
by one owner action on a shown list, see "Old pairs"); store the
clock time of each 4x4 work round and of each set check; all data stays on
the phone; the app never suggests exercises, sets, or loads.

Three principles decide every case:

1. **No data is better than wrong data.** An uncertain time gives no
   range, no pair is merged without the owner, a thin read shows nothing.
2. **The reader writes only its own table.** It never writes a column the
   owner can type into. A typed value and a measured value never share a
   place, so no rule about who owns a value exists.
3. **A range is stored once, by the code that saw the workout, and only
   when its clocks are verified.** Nothing infers a range later from dates
   or durations.

## Data model (Room 4 to 5)

New nullable columns, epoch ms unless said:

- `set_logs.checkedAt`: when the set became done in a live session.
- `interval_logs.startedAt`, `interval_logs.endedAt`: wall-clock start and
  end of that work round. A round has both or none.
- `sessions.timedFrom`, `sessions.timedTo`: the stored range of the
  workout. Exactly these writers set it:
  - **gym Finish** (below);
  - **the timer commit** of a cardio session (below);
  - **the import of a band workout**: the band's start and end;
  - **the old-pair merge** (phase 2): the band copy's start and end, when
    the app session has none;
  - **the migration**, once: `startedAt` and `finishedAt` (the band's own
    bounds) for every finished imported cardio session (`sourceId` set).
    Nothing else: an older gym session gets no range from the migration;
  - **the owner**, by hand, for a finished gym session without a range
    ("Use this time range", below).
- `sessions.startedElapsed`: the elapsed-realtime value at the start of a
  gym session (the anchor of "Verified clocks"). Null for older sessions.
- `sessions.timerRun INTEGER`, `sessions.timerDone INTEGER`: run numbers
  of the session, no clock in them. `timerRun` is the number of the
  latest run that started; `timerDone` is the number of the latest run
  whose commit was accepted.
- `sessions.linkedIds TEXT`: Health Connect record ids, joined by `,`,
  merged into this app session. `sourceId` keeps its meaning ("this
  session was imported"); a session with `linkedIds` is an app session.

New table `session_hr`, at most one row per session, no foreign key (an
upsert of the session must never delete it; every delete path removes it
by hand). Only the reader, "Remove", session delete, and backup import
write it:

| Column | Meaning |
|---|---|
| `sessionId TEXT PRIMARY KEY` | |
| `fromMs`, `toMs` | the range that was read, half-open `[from, to)` |
| `state TEXT` | `filled`, `waiting`, `none`, `off` |
| `samples TEXT` | `offsetSec:bpm` joined by `,`; one pair per 5-second bucket with data (the bucket mean, rounded half up); may be empty |
| `rawCount`, `sampleCount` | samples read, and samples accepted |
| `origin TEXT`, `origins TEXT` | the package whose samples were accepted; every package seen, joined by `,` |
| `firstMs`, `lastMs`, `maxGapSec` | nullable; of the accepted samples, before bucketing |
| `avgHr`, `maxHr` | nullable; mean (rounded half up) and maximum of the accepted samples; set only in `filled` |
| `fetchedAt` | |

There is no `sessions.maxHr`, no measured distance, and no stored round
average.

## The range of a session (pure function `hrWindow(session, zone)`)

`[from, to)` or nothing. All must hold:

- The session is finished; `timedFrom` and `timedTo` are set;
  `timedFrom < timedTo`.
- The session's `date` equals the local date of `timedFrom` or the local
  date of `startedAt` (a session made at 23:59 whose timer starts at
  00:01 keeps its range). (A session moved to another date has no range. After a time zone change a session
  near midnight can lose its range; accepted.)
- Gym: `[timedFrom, timedTo)`, length from 10 minutes to 4 hours.
- App cardio (`sourceId` null): `[timedFrom, min(timedTo + 60 s,
  finishedAt))`, length from 2 minutes to 6 hours.
- Imported cardio: `[timedFrom, timedTo)`, length from 2 minutes to
  6 hours.

`runRange(session)` (phase 2, "covered"): `[timedFrom, timedTo)` of an app
cardio session with both set, finished or not, with the same date and
length rules.

## What the app shows (pure function `hrFacts(session, row, zone)`)

One function decides what every screen may show:

- No range: **no range**. A row that exists is ignored.
- A row in `off`: **removed**.
- A row whose `fromMs`, `toMs` equal the range: **filled** (with `avgHr`,
  `maxHr`, samples, and the facts of the read), **waiting**, or **none**
  by its state.
- No row, or a row for another range: **not read**. A stale row is never
  shown.

The average of a work round is computed when shown: the mean of the
buckets inside `[startedAt, endedAt)` of that round, from a **filled** row,
when 2 or more buckets are inside; else nothing.

A typed value is shown where it exists. A measured value is shown only
where the typed one is null. Saving a form never copies a measured value
into a typed column.

## Reading (`HeartSource` interface, real one in `health/`, fake in tests)

- Read every page of heart rate records that touch the range, also
  records that begin before it or end after it. `rawCount` is every
  sample seen in them.
- Accept a sample when its time is in `[from, to)` and its bpm is from 30
  to 230. Drop exact duplicates (same time, bpm, package).
- `origin` is the package with the most accepted samples (a tie: the
  first by name). Only its samples count. Two of its samples at one
  instant count once (the first).
- A read is **complete** when it has 10 or more accepted instants and the
  time from the first to the last is at least half of the range.
- A missing provider, a missing heart rate permission, or any exception
  is a **failed read**: nothing is written and nothing changes.

## Fill (`fillHeartRate`)

- One runner for the whole app. A request while it runs makes it run once
  more after it ends. Finish and Save never wait for it.
- Triggers: gym Finish, cardio Save, app foreground (at most one start per
  10 minutes of elapsed real time), Import screen open, a successful
  import of a band workout, and `rereadHeartRate`.
- A pass, over finished sessions:
  - Delete each row that is not `off` and whose session has no range or
    another range (cleanup; `hrFacts` already hides it). The delete
    checks the session and the row again inside its transaction.
  - Read each session whose range ended in the last 30 days (and not in
    the future) and that has no row or a `waiting` row.
  - Never read `off` or `none`.
- Result: complete gives `filled` with `avgHr` and `maxHr`. Not complete
  gives `waiting` while the range ended 7 days ago or less, else `none`;
  samples and counts are stored, `avgHr` and `maxHr` are null. A failed
  read changes nothing and is not a try.
- Commit, in one Room transaction, deny by default. Write the row only
  when all hold: the session exists; the repository generation is the one
  the read started with (replace import and reset raise it); `hrWindow`
  gives the range that was read; and the session's row is the row the
  read started from (same `state` and `fetchedAt`, or still absent). Else
  drop the result.
- `rereadHeartRate(sessionId)`: reads that session now, at any age, also
  from `off` and `none`. The old row stays until a read commits. It
  returns filled, waiting, none, failed, or no range.
- `removeHeartRate(sessionId)`: the row becomes `off` with empty samples,
  null values, and a new `fetchedAt`, so a read that was already running
  cannot commit.

## Verified clocks

One helper, used by gym and timer. An **anchor** is a pair taken at one
instant: the wall time and the elapsed-realtime value. Later, the clocks
are **in step** when the wall clock and elapsed-realtime moved by the same
amount since the anchor, within 2 s. A changed phone clock, a reboot, or a
missing anchor is not in step. A wall time is stored as a clock fact only
when the clocks are in step at that instant.

## Clock times

- A gym session stores its anchor at its start: `startedAt` and
  `startedElapsed`.
- `checkSet` stamps `checkedAt = now` in the same statement that turns a
  pending set to done, and only when the session is unfinished and its
  clocks are in step at that instant (else the set is done with a null
  stamp). A done set
  that is checked again keeps its first stamp. A check in a finished
  session leaves `checkedAt` as it is. Every path that makes a set pending
  or skipped sets `checkedAt` to null. A weight or reps edit keeps it.
- **Gym Finish**, in the finish transaction: the stamps are the
  `checkedAt` of done sets inside `[startedAt, finishedAt]`. With at
  least one stamp, clocks in step at Finish, and `timedFrom` null: `timedFrom = startedAt`,
  `timedTo = min(finishedAt, last stamp + 5 min)`. With no stamp or clocks not in step, nothing
  is written, and the session has no range. Later edits of sets never
  change the stored range.
- **"Use this time range"** (`confirmRange(sessionId)`): for a finished
  gym session with no `timedFrom`, whose `date` is the local date of
  `startedAt`, and whose `startedAt` to `finishedAt` lasts from 10
  minutes to 4 hours. The screen shows that range as clock times and the
  owner confirms it; then `timedFrom = startedAt`, `timedTo =
  finishedAt`, and a fill is asked. Any other session: refused, nothing
  written. This is the only way a session from before this version, or a
  session whose clocks were not in step, gets a range.
- **The timer run.** A run carries its session id, its run number, and
  its anchor. The Start action (on the screen or in the notification)
  takes the anchor and, in one transaction, gives the run its number:
  `timerRun + 1` (1 for the first), written to `sessions.timerRun`, and
  notes the session's `timeSec` at that instant. A snapshot of the old
  app version, restored after the upgrade, gets its number in the same
  way at the restore and is untrusted. The
  timer takes no anchor and no number while it waits on Ready; a Stop
  before Start stores nothing. Every time of the run is `anchor wall +
  (elapsed-realtime now - anchor elapsed)`.
  - A work round gets its start when work begins and its end when work
    ends (its last second, Skip, or Stop). A pause inside a round stays
    inside its range. A round that never started has no times. When one
    tick crosses several boundaries (restore after the process died),
    each boundary gets its own time from the phase lengths. A paused
    snapshot consumes no phase time.
  - The run is **trusted** while its clocks are in step at every restore
    and at its end. A reboot, a wall clock change, or a snapshot without
    an anchor makes the run untrusted for good.
  - On finish and on Stop, one transaction (`commitTimerRun`): the
    duration, only when the session's `timeSec` is still the value noted
    at Start (the owner typed nothing since); and for a trusted run `timedFrom`, `timedTo`, and
    the times of every round by index (rounds that did not run get null).
    An untrusted run writes the duration (under the same condition) and
    sets all its clock fields to null.
  - The commit of run n is accepted only when the session exists,
    `timerRun = n` (no newer run started), and `timerDone` is not n (not
    committed yet). It then sets `timerDone = n`. So a replay and a late
    older run change nothing, whatever the clocks did, also after an
    untrusted run cleared the clock fields or the owner typed a duration.
  - The snapshot of an ended run is cleared only after its commit
    returns, and only when the stored snapshot is still that run. A
    failed commit keeps it; the app commits it at its next start, under
    the same rules.
- Replace import and reset stop the timer and clear its snapshot first.
- Stretch runs are not affected.

## Backup

- `Backup` gains `sessionHr: List<SessionHr> = emptyList()`. The new
  columns travel with their entities. A 0.6.1 file imports as before; its
  sessions have no stored range and so no heart rate.
- An HR row from a file is accepted when its session exists after the
  import, `fromMs < toMs`, the state is one of the four, and `samples`
  parses. Other HR rows are dropped; they never fail the import or its preview,
  also when one row does not decode (each row is decoded by itself). Of two
  rows with one id the last wins.
- Replace: only the file's rows remain. Merge: a file HR row replaces the
  local row of the same session; a local row whose session survives
  stays (and `hrFacts` hides it when the range no longer matches).
- Merge only: a null `checkedAt`, `startedAt`, `endedAt`, `timedFrom`,
  `timedTo`, or `linkedIds` in the file never erases the local value of
  the same row; for `timedFrom` and `timedTo` only when the file row has
  the local row's `startedAt` and `finishedAt`. `timerRun` and
  `timerDone` keep the greater of the local and the file value, so a file
  never reopens a committed run.

## Band workouts (phase 2)

- **Covered**: a band cardio record is covered when an app cardio session
  with a `runRange` has an exercise of the same modality as the record,
  and the overlap is at least half of the record's range and at least half
  of the run range. The check runs on each original record, before the
  existing grouping of overlapping records. Import and Today's count use
  the same function. A gym session never covers a cardio record. Band
  strength records stay hidden as today.
- Ids in `linkedIds` count as imported, like `sourceId`.
- `importDraft` checks again inside its write, on each original record of
  the draft: no id is imported or linked, and no record is covered now. A
  refused import writes nothing; the screen reads the list again. A saved
  import stores the band's bounds in `timedFrom`, `timedTo` and asks for
  a fill. What the import writes into `avgHr` and the rounds stays as
  today.
- **Old pairs, by the owner's hand.** No session is ever deleted by the
  app alone. The app only finds candidate pairs and shows them; the owner
  merges a pair after he sees both sessions.
  - Candidate rule (`pairCandidates`, a pure function on fresh rows):
    each of A and B has its `date` on the local date of its `startedAt`.
    A = a finished app cardio session (`sourceId` and `linkedIds` null)
    with the range `[timedFrom, timedTo)` when set, else `[startedAt,
    startedAt + timeSec)`, at most 6 hours; an A with neither is no
    candidate. B = a finished imported cardio session, range `[startedAt,
    finishedAt)`. They have the same exercise or exercises of the same
    modality, and the overlap is at least half of both ranges. The rule
    only fills the list; it proves nothing and deletes nothing.
  - A candidate is **blocked** when an owner-only field (`effort`,
    `watts`, `stroke`, a round's `watts`) is set on both and different,
    or B has a round index that A does not have. A blocked pair is shown
    with its reason and cannot merge.
  - The owner can answer "Keep both" for a pair; it is not listed again
    (a DataStore set of `A id|B id`).
  - `mergePairs(pairs, unique)`, only from an owner action. Each asked
    pair carries the rows the owner saw (A, B, their logs, and their HR
    rows). First write
    `backups/before-merge-<time>.json` in the folder of the existing
    "before-import" copies with the full export, read it back, and parse
    it; a failure stops with nothing changed. Then one transaction: for
    each asked pair, check on fresh rows that it is still a candidate and
    not blocked, and that A, B, their logs, and their HR rows equal both
    the rows the owner saw and the rows in the parsed copy; with `unique` ("Merge
    all") also that neither A nor B has another candidate now; then merge
    it; else skip it and report why. It never merges a pair that was not
    asked.
  - The merge of one pair:
    - Band-derived fields (`avgHr`, `distanceM`, `laps`, `poolLength`,
      `timeSec`, a round's `avgHr`): A's value stays; a null takes B's.
    - Owner-only fields: a null on A takes B's value.
    - `note`: B's note goes on a new line after A's, unless it is empty
      or equal to A's whole note or to one whole line of it.
    - `timedFrom`, `timedTo`: when A has none, B's `startedAt` and
      `finishedAt`.
    - `linkedIds` = B's `sourceId`. Then B, its logs, and its HR row go.
  - "Merge all" merges only pairs that are not blocked and where A has
    no other candidate and B has no other candidate.

## What does not change

- No suggestion of any kind: times and heart rates are shown as facts.
- No network use is added. Nothing is written to Health Connect.
- The band-only cardio import works as today: its exercise choice,
  "Import all", and what it writes into `avgHr` and the rounds.
- `android:allowBackup` and the export to a file the owner picks stay as
  they are; the lead reports both to the owner as an open question.
- Layout rules in `DESIGN.md` bind every new row and screen.
- No comment that a name, a type, or a test can say; no task or review
  IDs in code. No AI attribution in commits.

## What changed after the attacks

| Attack point | Decision |
|---|---|
| R1: overlap alone hides or deletes | Hiding needs the same modality and half of **both** ranges. Deleting needs the owner's own action on a pair he sees (R5), no lost owner value, and a verified safety copy that holds the exact rows. |
| R1: "Add to" keeps its range in a cache row | "Add to" is cut from this task. |
| R1, R2: manual entry passes as live; a cleared stamp falls back to the finish time; wall clock jumps | Design 3 infers no range. Finish, the timer, and the band import store it. An untrusted timer run stores none. |
| R1: ten samples in a burst count | Complete needs 10 instants over half of the range, from one package. |
| R1, R2: typed values against reader values; restore puts a cleared value back; a round's bounds change during a read | The reader writes only `session_hr`. Round averages are computed when shown. Distance from the reader is cut. No ownership marks, no reconcile. |
| R2: a stale row shown as current; invalidation needs a successful read | `hrFacts` shows a row only for the current range. |
| R2: Remove against a read in flight; superseded timer run; retry after a typed duration | The commit needs the same row as at the read's start; a run writes only over no run or an older run; a retry writes the duration only into null. |
| R2 D21: the owner imports the band record while the app timer session is not saved | An ended timer run covers the record before Save. An import while the app timer still runs gives two sessions, as today; accepted. |
| R2 D22: what the band-only import writes into `avgHr` and rounds | Existing behavior, not changed here. |
| R2 D23: another band workout of the same modality inside a paused timer run | Not taken. Same modality, same time, half of both ranges is the same workout for one person. Hiding is undone by deleting the app session; the merge has the safety copy. |
| R2 D24: B's edited average loses to A's | Band-derived fields: the app session's own value wins and B's stays in the safety copy. Owner-only fields block. |
| R3: a moved phone clock during a gym session; an older timer result after an untrusted newer run; time on Ready | "Verified clocks" for gym and timer; `timerRun`; the anchor at Start. |
| R4: the migration trusts old gym clocks; an old pair matched by the time of Save | The migration gives a range only to band imports. An older gym session gets one only when the owner confirms the shown range. An old app session is placed by its measured duration, never by its Save time. |
| R4 C38: the phone clock is wrong by the same amount from Start to Finish | Not taken. No clock on the phone can show it, and the band takes its time from the same phone, so both timelines carry the same error. |
| R5: time on Ready or a typed duration makes a false old pair | The app deletes nothing by itself. The owner decision of 2026-11-26 ("merge them once") becomes: the app lists the candidate pairs once found, and the owner merges them with one action. The lead reports this change to the owner. |
| R6: a late older timer run after a backward clock correction; a merge confirmed on facts that changed; a new candidate after "Merge all" | Run numbers without a clock (`timerRun`, `timerDone`); `mergePairs` checks the rows the owner saw; "Merge all" checks one-to-one again on fresh rows. |
| R7: a timer commit lands after the owner typed a duration; an old snapshot after the upgrade; a file lowers run numbers; a timer that starts after midnight; facts for "Merge all" | The duration is written only over the value noted at Start; old snapshots get a number at restore; merge keeps the greater run numbers; the date rule also accepts the creation date; "Merge all" shows each pair's facts before the owner accepts. The lead ended the combined attack here: each later phase gets its own attack round before its build. |
| R1: a Room journal for the merge, generations on disk, platform backup rules, a recovery screen | Not taken. The merge has no automatic pass any more; replace stops the timer; the existing Restore sheet is the recovery route; platform backup is an owner question. |
| R1: the import's default exercise choice and "Import all" against the no-suggestion rule | Not changed: it maps a band record to one of the owner's own exercises and exists today. Reported to the owner. |
