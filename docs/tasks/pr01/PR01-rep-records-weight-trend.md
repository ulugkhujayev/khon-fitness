# PR01-rep-records-weight-trend: rep records next to weight records, and a 7-day bodyweight trend

Task-ID: PR01-rep-records-weight-trend
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/pr01
Base: 9d1ba17
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/pr01/docs/tasks/pr01/PR01-rep-records-weight-trend-REPORT.md

## Why

Two facts the app has the data for and does not show (owner approved both
on 2026-11-26):

1. The only record is the heaviest weight (`bestSet`, `beats` in
   `ui/screens/HistoryRules.kt`). A session where the owner does 80 kg x 8
   after a best of 80 kg x 6 under an 85 kg record shows nothing. Most
   training weeks hold no weight record, so most progress is invisible.
2. Bodyweight shows the latest entry against one older entry
   (`bodyweightChange`, `bodyweightStatus`). One entry moves up to a
   kilogram from water alone, so the number says little.

Hard rules of the app, all binding here: everything stays on the phone; the
app never suggests exercises, sets, or loads; it shows facts only. No
estimated 1RM (`SPEC.md` says so). Read `DESIGN.md` before any layout
change: no cards with per-row buttons, no pills, no captions, no helper
sentences.

## Scope

Rules go in the existing rule files as pure functions with unit tests;
screens only call them. Change no Room entity, DAO, repository function,
backup format, or version number. Add no dependency.

### Rep record (decided)

Words: a **counted set** is one `countsAsDone` accepts; its weight is
`weightKg ?: 0.0`. Sessions of an exercise are ordered as `liftHistory`
orders them. "Earlier" means a counted set of an earlier finished session
of the same exercise; sets of the same session are never "earlier".

A counted set `l` of a session is a **rep record** when all hold:

- at least one earlier set has weight >= `l`'s weight;
- `l.reps` is greater than the most reps of every earlier set with
  weight >= `l`'s weight;
- no other counted set of the same session has weight >= `l`'s weight and
  reps >= `l`'s reps (of two equal sets, the earlier one in set order
  stays);
- `l` is not the session's weight record set (`record && l === best`).

So a rep record is always at a weight at or below a weight lifted before,
and the weight record rule does not change. A bodyweight exercise (0 kg)
has no rep records: more reps at 0 kg is already the weight record.

- `LiftSession` gains `repRecords: List<SetLog>` (set order), filled by
  `liftHistory`.
- `repRecordCount(sessionId, histories)`: the exercises of that session
  with at least one rep record and no counted weight record in that
  session (`recordCount` already counts those).
- `Summary` (finish sheet, `SessionRules.summarize`) gains `repRecords:
  Int` by the same rule against every other session's sets. Like `bests`,
  it compares with all other sessions, not only earlier ones. An exercise
  that counts in `bests` does not count in `repRecords`.
- `repBests(history): List<Pair<SetLog, Session>>`: the all-time sets that
  no other set of the exercise matches or beats in both weight and reps
  (of equals, the earliest), heaviest first, each with its session.

Screens:

- `ExerciseDetail.kt`, session rows: a rep record set is `K.Text`,
  SemiBold (the weight record set stays green). The row chip is "PR" for a
  weight record as today; else "Rep PR" with `LabelChip(text, K.Text)` when
  the session has a rep record. One chip per row.
- `ExerciseDetail.kt`, new group between the record block and "Sessions ·
  newest first", only when `repBests` has two or more rows: title "Most
  reps by weight", one row per set, at most 6 (the heaviest 6): left
  "80 kg", right "× 8" bold tabular, the date muted as in the record rows.
  Rows are not tappable.
- `HistoryLog.kt`, day row line: after the green "N PR", append
  " · N rep PR" in `K.Text` SemiBold when the count is above 0 (also with
  no weight PR).
- `SessionScreen.kt`, finish sheet: one row "Rep records" with the count,
  directly under "New bests".
- `HistoryProgress.kt` record list: no change.

### Bodyweight trend (decided)

- `weekAverage(all, end): Double?`: the mean of the entries dated from
  `end - 6 days` to `end`; null with fewer than 3 entries.
- `weekRate(all, today): Double?`: `weekAverage(today) -
  weekAverage(today - 7 days)`; null when either is null.
- `trendText(all, today): String?`: "7-day avg 83.2 kg" and, when the rate
  exists, " · −0.3 kg/week". Signs as in `bodyweightChange` ("+", "−", and
  "±" inside 0.05 kg). Null without an average.
- `trendPoints(all, inRange): List<ChartPoint>`: for each entry in range,
  the mean of all entries in the 7 days that end on its date, where that
  window holds 3 or more entries (the window uses all entries, also those
  before the range).

Screens:

- `HistoryProgress.kt`, Bodyweight card: the trend text as a second line
  under the big number row, `bodySmall`, muted, tabular; absent when null.
  The existing "since" text stays.
- `LineChart`: an optional second series `trend: List<ChartPoint> =
  emptyList()`, drawn as a 1.5 dp line in `K.Muted` without points, on the
  same scale (the scale covers both series). The other callers do not
  change. The chart's content description stays as it is.
- `TodayRules.bodyweightStatus`: after today's entry is saved and the
  average exists, the line is the trend text alone ("7-day avg 83.2 kg ·
  −0.3 kg/week"). Every other case stays as today.

### Docs

Update `SPEC.md` (the record rule and bodyweight parts) and `DESIGN.md`
(the History and Today parts that describe these rows) in each file's own
style. Load the `unslop` skill before you write them.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| C1 | Rep record | Earlier 85x3 and 80x6; today 80x8 | 80x8 is a rep record; the session is no weight record; `repRecordCount` is 1 and `recordCount` is 0. |
| C2 | Rep record | Earlier 80x6; today 80x6, and another day 75x6 | No rep record in either (equal reps; 75x6 does not beat 6 reps at >= 75). |
| C3 | Rep record | Earlier 80x6 and 70x7; today 80x8 and 70x8 | Only 80x8 (70x8 is matched by a heavier set of the same session). Today 80x8 and 70x10: both. |
| C4 | Rep record | Earlier 80x6; today 85x3 and 80x8 | 85x3 is the weight record (green, chip "PR"); 80x8 is a rep record in `repRecords`; `repRecordCount` is 0 for this exercise; the row has one chip, "PR". |
| C5 | Rep record | First session of an exercise; a 0 kg exercise; skipped and 0-rep sets; an unfinished session; two sessions on one date | No rep record in the first session or at 0 kg; uncounted sets and unfinished sessions never count and never serve as "earlier"; order by date then `startedAt`. |
| C6 | Rep record | Two equal sets 80x8 in one session, both above the earlier best | One rep record: the earlier set in set order. |
| C7 | Finish sheet | A session with one weight record exercise and one rep record exercise | "New bests 1", "Rep records 1". Editing an old session from History gives the same count rule against all other sessions. |
| C8 | Rep bests | Sets 85x3, 80x8, 80x6, 70x8, 60x15 over several sessions | `repBests` = 85x3, 80x8, 60x15 (70x8 is matched by 80x8), each with the session that first set it. With one row the group is absent. |
| C9 | Screens | ExerciseDetail, History day row | Texts, colors, and chips as in Scope. A screen or semantics test where the repo already tests that screen; else a test of the function that builds the text. |
| C10 | Weight trend | 2 entries in the last 7 days; 3 entries; 3 now and 2 in the 7 days before; 3 and 3 | Null; average only; average only; average and rate. An entry dated after `today` is ignored. Window edges: `today - 6` is in, `today - 7` is in the older window. |
| C11 | Weight trend | Rates +0.30, −0.30, +0.04, −0.04 | "+0.3 kg/week", "−0.3 kg/week", "±0.0 kg/week" for both small ones. |
| C12 | Weight trend | Today card: no entry today; entry today with no average; entry today with average; with rate | As today; as today; "7-day avg 83.2 kg"; "7-day avg 83.2 kg · −0.3 kg/week". The existing `bodyweightStatus` tests still pass or change only for these two new cases. |
| C13 | Weight trend | Chart with range 1M and entries before the range | `trendPoints` uses the earlier entries for the first windows; a point exists only where its window holds 3 or more entries; the chart scale covers both series; a chart call without `trend` draws as before. |
| C14 | Rules | Whole change | No Room, DAO, repository, backup, permission, dependency, or version change. No suggestion text of any kind. `SPEC.md` and `DESIGN.md` updated. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- A test for each new rule fails on `9d1ba17` (or does not compile there) and passes on the branch.
- The `Tests:` command passes on the final commit. Commit on branch `pr01` (no push, no merge), messages hold only the change, no AI attribution.
- No comment that a name, a type, or a test can say; no task or review IDs in code.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| C1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
