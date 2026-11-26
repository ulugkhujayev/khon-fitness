# PIC01-workout-picture: the heart rate curve of a session with its sets and rounds

Task-ID: PIC01-workout-picture
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/pic01
Base: 662846b
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/pic01/docs/pic01-tasks/PIC01-workout-picture-REPORT.md

## Why

The heart rate work (branch `hr01`, this branch starts from it) stores,
for a finished session, the band's heart rate over the session's time
range (`session_hr`), the clock time of each set check
(`set_logs.checkedAt`), and the clock times of each timer round
(`interval_logs.startedAt`, `endedAt`). This task shows them as one
picture on the Review screen: where the owner worked, where the rests
were long, which set cost the most (owner approved this on 2026-11-26).

Read `docs/hr01-tasks/HR01-COMMON.md`, sections "The range of a session"
and "What the app shows", and the report of the data task in the same
folder for the function names. One rule from there binds every screen:
what is shown comes from `hrFacts` and nothing else; a stale row is never
shown; no screen reads a `session_hr` row past that function.

Names from the data task: the screen reads `repo.sessionHr(id)` (a flow
of the row or null) and passes it to `hrFacts(session, row, zone)`;
`parseSamples` gives the buckets. In tests, `FakeHeart` (test folder of
the `data` package) fakes the band's records and `KhonApp.heartFill`
(`HeartFill`) runs a fill or `removeHeartRate(sessionId)`. The timer
code of this branch is under a separate fix: do not change the `timer`
package or `data/TimerCommit.kt`.

Hard rules of the app: everything stays on the phone; the app never
suggests exercises, sets, or loads; it shows facts only: no "too long",
no "good", no arrows, no green or red for a value. Read `DESIGN.md`
before any layout: no cards with per-row buttons, no pills, no captions,
no helper sentences; the existing row, list, and chart styles.

## Scope

Read-only feature. Change no Room entity, DAO write, repository write,
backup format, or version number. Add no dependency. You may add a read
(a flow) to the repository when the screen needs one that is missing;
name it in the report.

### Rules (pure functions with unit tests, in a new `PictureRules.kt`)

- Input: the session, its `hrFacts`, its set logs (gym) or interval logs
  (cardio).
- **Curve.** Only from a filled fact: its buckets as (seconds from the
  range start, bpm). Two neighbor buckets more than 60 s apart are not
  joined: the curve is a list of segments. A segment of one bucket is a
  dot. No value is made up between buckets.
- **Set marks (gym).** Each done set with a `checkedAt` inside the range,
  in time order. A set without a stamp has no mark.
- **Set rows (gym).** For each marked set, in time order: its exercise
  name and set text (the existing `setText`), the clock time of its check
  ("18:12"), the time since the mark before it as minutes and seconds
  ("2:05"; nothing for the first), and the highest bpm of the buckets in
  the time from the mark before it (the range start for the first) up to
  its own check, when 2 or more buckets are inside; else nothing.
  The rows exist without a filled fact too (then without bpm), as soon
  as the session has a range and 2 or more marked sets.
- **Round bands (cardio).** Each round with `startedAt < endedAt`, both
  inside the range, cut to the range.
- **Axis.** bpm from a tick at or under the lowest bucket to a tick at or
  over the highest, in steps of 10 or 20 so that there are 3 to 5 lines.
  Time: the clock time of the range start and of the range end.

### Screen (Review, finished sessions)

- With a filled fact: a chart block at the top of the Review content,
  under the existing header figures. The curve in the accent color; the
  axis lines and labels as the existing `LineChart` draws them; for gym,
  a short muted tick at the bottom of the plot at each set mark; for
  cardio, each round band as a faint filled area behind the curve. No
  legend and no caption. Height about 180 dp. Spoken text of the block:
  "Heart rate from 18:02 to 19:14, lowest 92, highest 164, 18 sets
  marked" (or "4 rounds marked").
- Without a filled fact: no chart, and no placeholder for it.
- Gym, 2 or more marked sets: a group titled "Sets in order" after the
  exercise groups and before "Notes". One row per set: the name over the
  set text on the left; on the right the clock time, the time since the
  set before, and the bpm when it exists, with tabular figures, in that
  order. A long name ends with an ellipsis and never pushes the values.
  The rows are not tappable.
- Cardio gets the chart only.
- An unfinished session, a session without a range, a removed or stale
  row: nothing new on the screen.
- `SPEC.md` and `DESIGN.md`: the picture and the group, in each file's
  own style. Load the `unslop` skill first.
- A screenshot test in the way `HistoryShotsTest.kt` does it: a gym
  session with 18 sets over 70 minutes, a 4x4 cardio session with 4
  rounds, a gym session with stamps and no heart rate, at 360 and 411 dp
  and font scale 1.0 and 1.3. Look at each picture yourself before you
  report, and name the picture files in the report.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| H1 | Curve | Buckets every 5 s; a gap of 60 s; a gap of 65 s; one bucket alone between two gaps | One segment; one segment; two segments; a dot. |
| H2 | Facts | A waiting fact; none; removed; no range; a filled row of another range | No curve and no chart in each case. |
| H3 | Set marks | Done sets with stamps inside the range, one before `timedFrom`, one after `timedTo`, one done without a stamp, one skipped | Only the stamped done sets inside the range, in time order (not in set order). |
| H4 | Set rows | Marks at 18:10:00, 18:12:05, 18:15:00 with buckets; the same without a filled fact; a single mark | Times since: nothing, "2:05", "2:55". The bpm is the highest bucket in each span, nothing with fewer than 2 buckets. Without a fact: the same rows with no bpm. One mark: no group. |
| H5 | Round bands | 4 rounds inside the range; a round that ends after the range end; a round without times; a round with `startedAt` equal to `endedAt` | 4 bands; the long one cut at the range end; the last two give no band. |
| H6 | Axis | Buckets from 92 to 164; all buckets at 120; one bucket | 3 to 5 lines that cover every bucket; a flat line still has a visible span; no crash and no division by zero. |
| H7 | Chart | The gym fixture and the cardio fixture | Pixel test: the curve is drawn in the plot area; a tick stands at each set mark's column and at no other; a band covers each round's columns; with the curve removed the test fails; with the marks removed the test fails. |
| H8 | Screen | Review of: a gym session with a filled fact; with stamps and no fact; cardio with rounds; a session without a range; an unfinished session | Chart and group; group only; chart only; nothing new; nothing new. The existing Review content is unchanged in each (its tests stay green). |
| H9 | Live | The fill commits while Review is open; "Remove" runs | The chart appears and goes without leaving the screen. |
| H10 | Layout | 360 and 411 dp, font scale 1.0 and 1.3, a 40-character exercise name, 30 sets | No clipped or overlapping text; the group scrolls with the page; the existing tap-target and contrast tests pass. |
| H11 | Speech | The chart block and a set row | The texts of Scope, with full unit words ("beats per minute", "2 minutes 5 seconds since the set before"). |
| H12 | Rules | Whole change | No verdict words or colors, no suggestion text. No Room, DAO write, backup, permission, dependency, or version change. `SPEC.md` and `DESIGN.md` updated. No comment that a name, a type, or a test can say; no task or review IDs in code. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- A test for each new rule fails on the base (or does not compile there) and passes on the branch.
- The `Tests:` command passes on the final commit. Commit on branch `pic01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| H1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
