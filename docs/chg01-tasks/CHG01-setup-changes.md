# CHG01-setup-changes: the rate of progress before and after each change of an exercise's setup

Task-ID: CHG01-setup-changes
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/chg01
Base: aa7faf0
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/chg01/docs/chg01-tasks/CHG01-setup-changes-REPORT.md

## Why

The owner changes a program (another program, more sets, another rep
range) and cannot see what the change did. Every session already stores
the setup it ran with, so the app can mark each change and show the rate
of progress before and after it (owner approved this on 2026-11-26).

Hard rules of the app, all binding here: everything stays on the phone;
the app never suggests exercises, sets, or loads; it shows facts only.
This feature gives rates and no verdict: no "better", no "worse", no
arrows, no green or red. Read `DESIGN.md` before any layout: no cards
with per-row buttons, no pills, no captions, no helper sentences;
existing row, list, and chart components only.

This branch starts from the rep records branch. Use its decimal rounding
and sign helper for the new numbers (see `weekRate` and `trendText` in
`HistoryRules.kt`); do not write a second one.

## Scope

Read-only feature. Change no Room entity, DAO, repository function,
backup format, or version number. Add no dependency. The existing
Exercise detail content keeps its results (its tests stay green).

### Rules (pure functions with unit tests, in a new `SetupRules.kt`)

- Input: `liftHistory` of one exercise (finished gym sessions that hold a
  row of the exercise, in date order).
- `Setup` of a session for the exercise: the session's `programId`, the
  count of the exercise's set rows in that session (every status), and
  `minReps` and `maxReps` of its first row in execution order.
- **Periods.** A period is a run of consecutive sessions with the same
  `Setup`. A run of one session that is not the latest run joins the
  period before it (a one-day deviation is not a change); when there is
  no period before it, it joins the next one. The latest run is always a
  period, also with one session.
- **Change.** Each period after the first starts with a change. Its
  facts, in this order, each only when it differs from the setup of the
  period before: the program ("TP v3 → TP v4", from the sessions'
  `programName`; "No program" for null), the sets ("3 → 4 sets"), the
  reps ("8–12 → 5–8 reps"). A program whose name changed while its id
  stayed is no change.
- **Value of a session.** With weight: the greatest estimated one-rep
  maximum of its counted sets (`countsAsDone`), by the Epley formula
  `weight × (1 + reps / 30)`. An exercise is "with weight" when any
  counted set of its whole history is above 0 kg; then a session whose
  counted sets are all 0 kg has no value. An exercise with no weight in
  its whole history: the most reps of a counted set. A session with no
  counted set has no value.
- **Rate of a period.** The least-squares slope of the values against the
  session dates, per 7 days. It exists only with at least 3 valued
  sessions and at least 14 days from the first valued session to the
  last. Two sessions on one date are two points. Units: "kg/week", or
  "reps/week" for an exercise with no weight. One decimal, rounded and
  signed as the bodyweight trend rate does it.

### Screen (Exercise detail)

- A new group between the chart card and "Most reps by weight", only
  when the exercise has two periods or more. Section title: "Est. 1RM
  per week" (with weight) or "Reps per week" (no weight).
- One row per period, newest first. Left, two lines: the dates over the
  setup. Dates: "Since Nov 3" for the latest period, else "Oct 2 – Nov
  2" (the first session's date to the day before the next period
  starts); the year is added when it is not this year. Setup: "TP v4 ·
  4 sets · 5–8 reps" (one number when `minReps` equals `maxReps`). Right:
  the rate ("+0.8", "−0.3", "±0.0") with tabular figures, or "–" when the
  period has none. Both signs have the normal text color. A long program
  name ends with an ellipsis and never pushes the rate. The rows are not
  tappable and have no chevron.
- Spoken text of a row: the dates, the setup, and the rate with the full
  unit words ("plus 0.8 kilograms per week"); "no rate" for "–".
- The chart "Heaviest set per session": one thin vertical line in the
  muted color at the first session of each period after the first.
  `LineChart` gets an optional list of dates for it. A chart called
  without that list draws exactly as before.
- `SPEC.md` and `DESIGN.md`: the new group and the chart lines, in each
  file's own style. Load the `unslop` skill first.
- A screenshot test of the screen in the way `RepRecordShotsTest.kt`
  does it. Look at each picture yourself before you report, and name the
  picture files in the report.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| G1 | Periods | Three sessions with 3 sets of 8–12, then three with 4 sets of 8–12 | Two periods; the second starts at the fourth session; its fact is "3 → 4 sets". |
| G2 | Periods | The program id changes; the program name changes while the id stays; sessions with a null program | The first is a change with both names; the second is none; null is its own setup, named "No program". |
| G3 | Periods | 8–12 becomes 5–8; sets, reps, and program change in one session | "8–12 → 5–8 reps"; one change with the three facts in the order of Scope. |
| G4 | One-day deviation | Setups A A B A A; A A B B; A A B; B A A | One period; two; two (the latest has one session); one. |
| G5 | Sessions | An unfinished session; a session whose sets are all skipped; two sessions on one date | The unfinished one is not in the history. The skipped one counts for the setup and has no value. Two on one date are two sessions in their stored order. |
| G6 | Value | Sets 100 × 5 and 90 × 10 in one session; a 0 kg set in an exercise with weight; an exercise that never had weight with 12 and 15 reps | 120.0 (the 90 × 10 set); no value; 15. |
| G7 | Rate | Values 100, 101, 102, 103 on days 0, 7, 14, 21; the same falling; all equal; values 100, 104, 102 on days 0, 7, 14 | "+1.0"; "−1.0"; "±0.0"; "+1.0". |
| G8 | Rate | Two valued sessions over 30 days; three over 13 days; three over 14 days; five sessions of which two have a value | "–"; "–"; a rate; "–". |
| G9 | Rounding | A slope of exactly +0.25, −0.25, and 0.04 per week | "+0.3", "−0.3", "±0.0": the same rule as the bodyweight trend. |
| G10 | Group | An exercise with one period; with three periods; with no weight in its history | No group; three rows, newest first, the first one "Since …"; the title "Reps per week". |
| G11 | Row | Dates in this year and in the last year; `minReps` equal to `maxReps`; a 40-character program name | The year only on the old dates; "5 reps"; the name ends with an ellipsis, the rate stays whole. |
| G12 | Chart | No change; two changes | Without the list the chart's pixels equal the chart of the base. With it, a muted vertical line stands at each change date and at no other place. Break proof: with the line removed the test fails. |
| G13 | Layout | 360 and 411 dp, font scale 1.0 and 1.3 | No clipped or overlapping text; the existing tap-target and contrast tests pass. |
| G14 | Rules | Whole change | No verdict words or colors, no suggestion text. No Room, DAO, repository, backup, permission, dependency, or version change. `SPEC.md` and `DESIGN.md` updated. No comment that a name, a type, or a test can say; no task or review IDs in code. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- A test for each new rule fails on the base (or does not compile there) and passes on the branch.
- The `Tests:` command passes on the final commit. Commit on branch `chg01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| G1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
