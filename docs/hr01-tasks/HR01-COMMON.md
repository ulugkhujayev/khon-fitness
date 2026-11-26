# HR01 common: heart rate for every session, by time range (design 2)

Read this before a phase file. It holds the decided design; the phase files
hold the contract rows. Design 1 was attacked (`HR01-ATTACK.md`, verdict
open-ended) and is replaced by this file. "What changed after the attack"
at the end lists each decision.

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

Owner decisions (2026-11-26): merge the old double sessions once; store the
clock time of each 4x4 work round and of each set check; all data stays on
the phone; the app never suggests exercises, sets, or loads.

One principle decides every doubtful case: **no data is better than wrong
data.** An uncertain time gives no range, an uncertain pair is not merged,
a thin read fills nothing.

## Data model (Room 4 to 5)

New nullable columns, all epoch ms unless said:

- `set_logs.checkedAt`: when the set became done in a live session.
- `interval_logs.startedAt`, `interval_logs.endedAt`: wall-clock start and
  end of that work round. A round has both or none.
- `sessions.timedFrom`, `sessions.timedTo`: wall-clock start and end of
  the session's timer run. `timedFrom` is also the identity of the run.
- `sessions.maxHr INTEGER`.
- `sessions.linkedIds TEXT`: Health Connect record ids, joined by `,`,
  that were merged into this app session. `sourceId` keeps its meaning
  ("this session was imported"); a session with `linkedIds` is still an
  app session.

New table `session_hr`, at most one row per session, no foreign key (an
upsert of the session must never delete it; every delete path removes it
by hand):

| Column | Meaning |
|---|---|
| `sessionId TEXT PRIMARY KEY` | |
| `fromMs`, `toMs` | the range that was read, half-open `[from, to)` |
| `state TEXT` | `filled`, `waiting`, `none`, `off` |
| `samples TEXT` | `offsetSec:bpm` joined by `,`; one pair per 5-second bucket with data (the bucket mean, rounded); may be empty |
| `rawCount`, `sampleCount` | samples read, and samples accepted |
| `origin TEXT`, `origins TEXT` | the package whose samples were accepted; every package seen, joined by `,` |
| `firstMs`, `lastMs`, `maxGapSec` | nullable; of the accepted samples, before bucketing |
| `fetchedAt` | |
| `avgHr`, `maxHr`, `distanceM` | nullable; what the reader measured |
| `rounds TEXT` | `index:bpm` joined by `,`; the reader's average per work round |
| `applied TEXT` | ownership marks, see "Who owns a value" |

The migration only adds columns and the table. No row changes.

## The time range of a session (pure function `hrWindow`)

Input: the session, its set logs, its interval logs, the time zone.
Output: `[from, to)` or nothing. Rules, all must hold:

- The session is finished, and its `date` equals the local date of
  `startedAt`. (A session moved to another date has no range. After a
  time zone change a session near midnight can lose its range; that is
  accepted.)
- **Gym** (`itemType` program): `from = startedAt`. Stamps are the
  `checkedAt` values of counted sets inside `[startedAt, finishedAt]`.
  With a stamp: `to = min(finishedAt, last stamp + 5 min)`. A session with
  no `checkedAt` on any set (made before this version): `to = finishedAt`.
  The length must be from 10 minutes to 4 hours, else no range.
- **App cardio** (`sourceId` null): only with `timedFrom` and `timedTo`:
  `from = timedFrom`, `to = min(timedTo + 60 s, finishedAt)`. Length from
  2 minutes to 6 hours. A cardio session without a timer run has no range.
- **Imported cardio** (`sourceId` set): `from = startedAt`,
  `to = finishedAt` (the band's own bounds). Length from 2 minutes to
  6 hours.
- `from < to`, and `to` is not in the future at read time.

## Reading (`HeartSource` interface, real one in `health/`, fake in tests)

One function reads a range and is used by every path:

- Read every page of heart rate records that touch the range. `rawCount`
  is every sample seen.
- Accept a sample when its time is in `[from, to)` and its bpm is from 30
  to 230. Drop exact duplicates (same time, bpm, package).
- `origin` is the package with the most accepted samples (a tie: the
  first by name). Only its samples count. Two of its samples at one
  instant count once (the first).
- A read is **complete** when it has 10 or more accepted instants and the
  time from the first to the last is at least half of the range.
- Distance, only for a cardio session whose exercise has the distance
  field: the Health Connect distance total of the range from the same
  `origin`, when above 0. No distance permission, or a failed distance
  read: no distance, and the heart rate result still stands.
- A missing provider, a missing heart rate permission, or any exception
  is a **failed read**: nothing is written and nothing changes.

## Fill (`fillHeartRate`)

- One runner for the whole app. A request while it runs makes it run once
  more after it ends. Finish and Save never wait for it.
- Triggers: gym Finish, cardio Save, app foreground (at most one start per
  10 minutes of elapsed real time), Import screen open, and the two
  explicit calls below.
- A pass looks at every finished session:
  - It has a range that ended in the last 30 days, and no row, or a
    `waiting` row, or a `filled` or `none` row whose stored range differs
    from the current one: **read it**.
  - It has a row that is not `off`, and it has no range now: **withdraw**
    the row (see below) and delete it.
  - `off` rows are never read by a pass.
- Result of a read: complete gives `filled` with `avgHr` (mean of accepted
  samples, rounded half up), `maxHr`, `rounds` (each work round with both
  times and 2 or more accepted instants inside `[startedAt, endedAt)`),
  and `distanceM`. Not complete gives `waiting` while the range ended 7
  days ago or less, else `none`; the samples and counts are stored, the
  measured values are null. A failed read changes nothing and does not
  count as a try.
- Commit, in one Room transaction: read the session and its logs again;
  write only when the session still exists, the repository generation is
  the one the read started with (replace import and reset raise it), and
  `hrWindow` still gives the range that was read. Else drop the result.
- `rereadHeartRate(sessionId)`: reads that session now, at any age, also
  from `off`. The old row stays until a read commits. It returns what
  happened (filled, waiting, none, failed, no range).
- `removeHeartRate(sessionId)`: withdraws the row and sets it to `off`
  with empty samples.

### Who owns a value

The reader may put four kinds of value into the owner's rows:
`sessions.avgHr`, `sessions.maxHr`, `sessions.distanceM`, and
`interval_logs.avgHr` of a round. `applied` holds one mark per value
(`avg`, `max`, `dist`, `r0`, `r1`, ...), each either **reader** or
**user**.

On commit, for each value with the reader's new result `new` (may be
null) and the previous row's result `old`:

- Mark **user**: do nothing, ever.
- Mark **reader**: when the row still holds `old`, write `new` (a null
  `new` clears it and removes the mark). When it holds anything else, the
  owner changed or cleared it: set the mark to **user**.
- No mark: when the row holds null and `new` is not null, write `new` and
  set the mark to **reader**. A row that holds a value is the owner's:
  nothing is written and no mark is set.

**Withdraw** = for each **reader** mark whose row still holds the reader's
value, set the row to null; then clear the marks.

After a backup import, every surviving `session_hr` row is **reconciled**
with its session: the marks are computed again (a value equal to the
reader's result is **reader**, no **user** mark survives), and a null
value with a reader result in a `filled` row is filled.

## Clock times

- `checkSet` stamps `checkedAt = now` in the same statement that turns a
  pending set to done, and only when the session is unfinished. A done set
  that is checked again keeps its first stamp. A check in a finished
  session leaves `checkedAt` as it is. Every path that makes a set pending
  or skipped sets `checkedAt` to null. A weight or reps edit keeps it.
- The timer run carries its session id from its start. `timedFrom` is the
  wall time when the run starts to count.
- A work round gets its start when work begins and its end when work ends
  (its last second, Skip, or Stop). A pause inside a round stays inside
  its range. A round that never started has no times.
- A boundary seen by a normal tick gets the wall time of that boundary.
  When one tick crosses several boundaries (restore after the process
  died), each gets its own computed time from the snapshot's anchor, but
  only when the anchor is **trusted**: the snapshot holds a wall time and
  an elapsed-realtime value, and both clocks moved by the same amount
  (within 2 s) since then, and the speed did not change. Otherwise every
  round touched by that gap has no times. A paused snapshot does not
  consume time.
- On finish and on Stop, one transaction (`commitTimerRun`) writes the
  duration as today, `timedFrom`, `timedTo`, and the times of every round
  of that session (a new run replaces the round times of an older run).
  It writes nothing when the session is gone, and nothing when the row
  already holds this run (`timedFrom` equal and `timedTo` set): a replay
  cannot change a duration the owner typed later.
- The snapshot of an ended run is cleared only after the commit returns.
  A failed commit keeps it; the app tries again at its next start.
- Replace import and reset stop the timer and clear its snapshot first.
- Stretch runs are not affected.

## Backup

- `Backup` gains `sessionHr: List<SessionHr> = emptyList()`. The new
  columns travel with their entities. A 0.6.1 file imports as before.
- An HR row from a file is accepted when its session exists after the
  import, `fromMs < toMs`, the state is one of the four, and `samples` and
  `rounds` parse. Other HR rows are dropped; they never fail the import.
- Replace: only the file's rows remain. Merge: a file row replaces the
  local row of the same session; a local row whose session survives stays.
- After every import: reconcile (above). A stale range is found by the
  next fill pass.

## Band workouts (phase 2)

- **Covered**: a band cardio record is covered when a finished app session
  (`sourceId` null) with a range has an exercise of the same modality as
  the record, and the overlap is at least half of the record's range and
  at least half of the session's range. Covered records are removed before
  the existing grouping of overlapping records; Import and Today's count
  use the same function. A gym session never covers a cardio record. Band
  strength records stay hidden as today.
- Ids in `linkedIds` count as imported, like `sourceId`.
- `importDraft` checks again inside its write that no id of the draft is
  imported or linked and that the draft is not covered now. A refused
  import writes nothing; the screen reads the list again.
- **Old pairs, once.** A = a finished app cardio session (`sourceId` and
  `linkedIds` null) whose `date` is the local date of `startedAt`, range
  `[startedAt, finishedAt)` or `[timedFrom, timedTo)` when set, at most
  6 hours. B = a finished imported cardio session, range
  `[startedAt, finishedAt)`. They are a candidate pair when they have the
  same exercise or exercises of the same modality, and the overlap is at
  least half of both ranges. A pair merges only when A has no other
  candidate and B has no other candidate, and no value is lost:
  - `distanceM`, `laps`, `poolLength`, `stroke`, `effort`, `watts`, and a
    round's `watts`: both set and different blocks the merge. So does a
    round index that B has and A does not have.
  - `avgHr`, `timeSec`, and a round's `avgHr`: A's value stays; a null
    takes B's.
  - `note`: B's note, when not empty and not already in A's, goes on a new
    line after A's.
  - `timedFrom`, `timedTo`: when A has none, B's `startedAt` and
    `finishedAt` (the band's bounds), so the fill can read the range.
  - `linkedIds` = B's `sourceId`. Then B, its logs, and its HR row go.
  Order: build the plan; with no pair, set the flag and stop. Else write
  `backups/before-merge-<time>.json` in the folder of the existing
  "before-import" copies, with the full export, and read it back; a
  failure stops the pass with no flag. Then all pairs in one transaction,
  each checked again on fresh rows. Then set the DataStore flag. A second
  run finds no pair. A restore of that copy keeps the flag, so the pairs
  come back and stay.
- Blocked and uncertain pairs stay as two sessions.

## What does not change

- No suggestion of any kind: times and heart rates are shown as facts.
- No network use is added. Nothing is written to Health Connect.
- The band-only cardio import works as today, also its exercise choice
  and "Import all".
- `android:allowBackup` and the export to a file the owner picks stay as
  they are; the lead reports both to the owner as an open question.
- Layout rules in `DESIGN.md` bind every new row and screen.
- No comment that a name, a type, or a test can say; no task or review
  IDs in code. No AI attribution in commits.

## What changed after the attack

| Attack point | Decision |
|---|---|
| Overlap alone hides or deletes | Hiding needs the same modality and half of **both** ranges. Deleting also needs a one-to-one pair and no lost value, after a safety copy that the existing Restore sheet lists. |
| "Add to" keeps its range in a cache row | "Add to" is cut from this task. An uncovered band workout is imported as today. |
| Manual entry passes as live; a late check makes a day-long range | Cardio ranges come only from the timer or the band. Gym needs 10 minutes to 4 hours and uses check stamps. The estimate for unfinished sessions is cut. |
| Ten samples in a burst count as a result | Complete needs 10 instants over half of the range, from one package. A thin read stores facts and fills nothing. |
| A late read writes into a changed or restored session | Commit checks the generation and the range again in the write transaction. |
| Typed values against reader values | Ownership marks; "Read again" keeps the old row until a new read commits; "Remove" withdraws. |
| Timer replay, failed write, restore after death | One commit with run identity, snapshot kept until commit, computed boundaries only from a trusted anchor. |
| Journal in Room for the merge pass, database generations on disk, platform backup rules, a recovery screen | Not taken. One transaction plus a flag is idempotent; the generation lives in memory because replace stops the timer; the existing Restore sheet is the recovery route; platform backup is an owner question. |
| The import's default exercise choice and "Import all" against the no-suggestion rule | Not changed: it maps a band record to one of the owner's own exercises and exists today. Reported to the owner. |
