# CMP01-compare-periods: two periods side by side on one screen

Task-ID: CMP01-compare-periods
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/cmp01
Base: 9d1ba17
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/cmp01/docs/cmp01-tasks/CMP01-compare-periods-REPORT.md

## Why

Progress shows this week against last week, and 30 days of cardio. The
owner cannot ask "what did the last 12 weeks give against the 12 before"
or "program X against program Y". The data for the answer is all in the
app (owner approved this on 2026-11-26).

Hard rules of the app, all binding here: everything stays on the phone;
the app never suggests exercises, sets, or loads; it shows facts only. The
screen gives two columns of numbers and no verdict: no "better", no
arrows, no green or red. Read `DESIGN.md` before any layout: no cards with
per-row buttons, no pills, no captions, no helper sentences; existing row,
list, and sheet components only.

## Scope

Read-only feature. Change no Room entity, DAO, repository function, backup
format, or version number. Add no dependency. Existing Progress sections
and their functions keep their results (their tests stay green); where a
function is fixed to "this week", add a range-based function and let the
old one call it.

### Rules (pure functions with unit tests, in a new `CompareRules.kt`)

- `Period(from, to)`: both dates inclusive, `from <= to`. `days` = the
  count of dates. `weeks` = `days / 7.0`.
- A session counts in a period when it is finished and its `date` is in
  the period.
- `periodStats(period, sessions, logs, exercises, stretches,
  bodyweights)` gives:
  - gym sessions; counted sets (`countsAsDone`); sets per week (sets /
    weeks, one decimal); lifted kg (weight x reps of counted sets);
  - cardio sessions; cardio seconds (`timeSec`); meters (the existing
    `meters(session)`);
  - stretch sessions;
  - bodyweight: the mean of the entries in the period and their count;
  - sets per muscle per week: the existing rule (primary 1, each secondary
    0.5) over the period, divided by weeks;
  - best set per exercise: `bestSet` over the period's sets of that
    exercise;
  - per cardio type (the existing type grouping): sessions, meters,
    seconds, and the existing pace text.
- Presets, from `today`: "Last 4 weeks" (the 28 days that end today),
  "The 4 weeks before" (the 28 days before those), "Last 12 weeks" and
  "The 12 weeks before" (84 days each), "This year" (January 1 to today),
  "Last year" (the whole year).
- `programPeriod(programId, sessions)`: from the date of the first
  finished session of that program to the date of its last; null with
  none.
- Custom dates: a "From" after the "To" moves the "To" to the same day.

### Screen

- Entry: at the end of the Progress content, one list group with one row
  "Compare two periods" and a chevron. New route `compare`.
- Top group, two rows: the left column's period and the right column's
  period, each shown as its date range ("Oct 2 – Oct 29"; with the year
  when it is not this year). Defaults: left "The 4 weeks before", right
  "Last 4 weeks". A tap opens a sheet with one choice list: the six
  presets, then one row per program that has a finished session (its name
  over its date range), then "Custom dates", which shows two date rows
  (the existing date row with the Material picker) in the same sheet. The
  choice survives rotation and process death (`rememberSaveable`).
- Under it, the facts as list groups. Every row: the label on the left,
  then two right-aligned columns of equal width with tabular figures, the
  left period first. A value that does not exist is "–". Both columns
  have the same color. Group titles use the existing section title.
  1. No title: "Days", "Gym sessions", "Sets", "Sets per week", "Lifted"
     (the existing tons text), "Cardio sessions", "Cardio time" (the
     existing duration text), "Distance" (km, one decimal), "Stretching".
  2. "Bodyweight": "Average" (kg, one decimal), "Entries".
  3. "Sets per muscle · per week": one row per muscle with a value in
     either period, one decimal, the existing muscle order.
  4. "Best set": one row per exercise with a counted set in either
     period, by name; the existing set text ("80 kg × 6").
  5. "Cardio": per type with a session in either period, three rows:
     "<Type> · sessions", "<Type> · distance", "<Type> · pace"; a row
     whose both values are "–" is absent.
- A group with no rows is absent. With no data in both periods, the screen
  shows the first group with zeros and nothing else.
- Spoken text of a row: the label, then each value with its period's
  range.
- `SPEC.md` and `DESIGN.md`: the new screen, in each file's own style.
  Load the `unslop` skill first.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| C1 | Period | A session on `from`, on `to`, one day before, one day after; an unfinished session in the period | The first two count; the others do not. |
| C2 | Presets | `today` = 2026-11-27 | Last 4 weeks = Oct 31 to Nov 27; the 4 before = Oct 3 to Oct 30; 12 weeks = Sep 5 to Nov 27 and Jun 13 to Sep 4; this year = Jan 1 to Nov 27; last year = Jan 1 to Dec 31, 2025. No preset pair overlaps or leaves a gap. |
| C3 | Totals | Two gym sessions with counted, skipped, and 0-rep sets; one cardio with time and distance; one swim with laps; one stretch session | Sessions, sets, lifted kg, cardio seconds, meters (laps x pool length for the swim), and stretch count as the rules say; uncounted sets add nothing. |
| C4 | Per week | 14 sets in 28 days; 14 sets in 10 days | 3.5 and 9.8. |
| C5 | Bodyweight | Three entries in the period, one outside; none | The mean of three and "3"; "–" and "0". |
| C6 | Muscles | The fixture of the existing sets-per-muscle test over one week | The same numbers the existing function gives for that week; over two weeks, half of the two-week sum. |
| C7 | Best set | An exercise with sets in both periods; in one only; only uncounted sets | Both best sets; one and "–"; absent. |
| C8 | Cardio | Run in both periods, bike in one, a type with sessions but no distance | Rows as in Scope; the pace is the existing pace text; a row of two "–" is absent. |
| C9 | Program | A program with sessions on Sep 2, Oct 1, Nov 20; a program with none | Sep 2 to Nov 20; not in the sheet. |
| C10 | Custom | From after To | To moves to From's day; the columns update. |
| C11 | Screen | The entry row, the sheet, each group, an empty database | As in Scope; no crash and no empty titled group with no data. A screen or semantics test where the repo tests screens this way. |
| C12 | Layout | 360 and 411 dp, font scale 1.0 and 1.3, long exercise names | No clipped value; a long label wraps or ends with an ellipsis and never pushes a column; touch targets at least 48 dp; the existing tap-target and contrast tests pass with the new screen. |
| C13 | Rules | Whole change | No verdict words or colors, no suggestion text. No Room, DAO, repository, backup, permission, dependency, or version change. The existing Progress numbers do not change. `SPEC.md` and `DESIGN.md` updated. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- A test for each new rule fails on the base (or does not compile there) and passes on the branch.
- The `Tests:` command passes on the final commit. Commit on branch `cmp01` (no push, no merge), messages hold only the change, no AI attribution.
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
