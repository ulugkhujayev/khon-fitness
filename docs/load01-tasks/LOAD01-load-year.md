# LOAD01-load-year: a load number per session, a week chart, and a year of days on one screen

Task-ID: LOAD01-load-year
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/load01
Base: 4171ab0
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/load01/docs/load01-tasks/LOAD01-load-year-REPORT.md

## Why

The app now stores the band's heart rate of each finished session
(`session_hr`, shown only through `hrFacts`) and has a maximum heart
rate in use (`ui/screens/ScoreRules.kt` and `data/MaxHeartRate.kt`: the
typed value, else the measured one, else none). The owner approved, on
2026-11-26, one load number per session from the heart rate, a chart of
the weeks, and a year on one screen, to see how hard the weeks and the
year were.

Read `SPEC.md` (the heart rate sections) and `DESIGN.md` before any
layout. Hard rules of the app: everything stays on the phone; the app
never suggests exercises, sets, or loads; it shows facts only: no
"good", no "too much", no arrows, no green or red for a value, no target
line. No cards with per-row buttons, no pills, no captions, no helper
sentences; the existing row, group, and chart components.

Fixers still work on `ScoreRules.kt`, `ReviewScores.kt`, the heart rate
screens, and the band import on other branches: put your code in new
files and keep each change to an existing file to a few lines. Call the
existing function for the maximum in use; do not copy or change it.

## Scope

Read-only: no Room entity, DAO write, repository write, backup,
setting, permission, dependency, or version change.

### Rules (pure functions with unit tests, in a new `LoadRules.kt`)

- **Session load.** From a filled fact and the maximum in use. Each
  bucket stands for 5 s and gets a weight by its bpm against the
  maximum, in integer math (`bpm * 100` against `maximum * percent`):
  below 50 %: 0; 50 % to under 60 %: 1; 60 % to under 70 %: 2; 70 % to
  under 80 %: 3; 80 % to under 90 %: 4; 90 % and over: 5. The load is
  the sum of weight times 5 s, divided by 60, rounded half up to a whole
  number (weighted minutes). No value is made up for a gap. A session
  without a filled fact, or with no maximum in use, has no load (not 0).
- **Day.** The loads of the sessions of one local date (the session's
  `date`) added; a day whose sessions all have no load is "trained, no
  load"; a day without a finished session is empty.
- **Week.** The app's existing week start. A week's load is the sum of
  its days that have a load; a week with no such day has no value.
- **Year.** The 52 weeks that end with the current week, newest first.
- **Steps.** A day's color step 1 to 4 against the highest day load of
  the 52 weeks: up to 25 %, up to 50 %, up to 75 %, above. With one
  loaded day only, it is step 4.

### Screen "Load"

- Entry: one plain row "Load" with a chevron in History's Progress,
  next to the existing "Compare" row (same style). Absent while no
  session of the 52 weeks has a load.
- The nav bar "Load" with Back.
- A chart with the existing `LineChart`, titled "Load per week" as the
  other charts are titled: one point for each of the last 12 weeks that
  has a value. Fewer than 2 points: no chart.
- Under it a grid of the 52 weeks, newest at the top, one row per week:
  on the left the month name ("Nov") only on the row that holds that
  month's first day, in muted 12; then 7 day cells in the app's weekday
  order, equal width, about 14 dp high, 2 dp apart, 3 dp corners; on the
  right the week's load in muted tabular figures (nothing without a
  value). A one-line header over the grid holds the weekday letters.
  Cell fill: empty day: `Surface`; trained, no load: `Surface` with a
  1 dp muted border; a loaded day: the accent color at 4 fixed alpha
  steps (about 30, 50, 75, 100 %). Days after today in the current week
  are not drawn. The cells are not tappable. The whole page scrolls.
- Speech: each week row speaks as one line: "Week of November 23, load
  412, 4 days" (the count of days with a session); the chart as the
  other charts speak.
- `SPEC.md` and `DESIGN.md`: the rule with its numbers, the screen, in
  each file's own style. Load the `unslop` skill first.
- A screenshot test in the way `ScoreShotsTest.kt` does it: a year with
  about 150 sessions (gym and cardio, some without heart rate), and a
  year with 3 sessions, at 360 and 411 dp and font scale 1.0 and 1.3.
  Look at each picture yourself before you report, and name the files.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| L1 | Weights | Maximum 200; buckets at 99, 100, 119, 120, 139, 140, 159, 160, 179, 180, 200, 230 | Weights 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 5. Maximum 187 (bounds that are not whole numbers): each bound by the integer rule. |
| L2 | Load | 12 buckets of weight 5; 6 buckets of weight 1; 0 buckets above weight 0; a fact with gaps | 5; 1 (0.5 rounds up); 0 (a load of zero is a load); only the buckets that exist count. |
| L3 | No load | A waiting fact; none; removed; a stale row; no maximum in use; an unfinished session | No load in each (not 0). |
| L4 | Day and week | Two sessions on one date, one with a load of 40 and one without; a day with only sessions without load; a week across a month and a year end; the first day of the week | 40 and a loaded day; "trained, no load"; one week with the app's week start; sums as designed. |
| L5 | Steps | Day loads 10, 25, 26, 50, 75, 76, 100 with 100 the highest; one loaded day; all loads 0 | Steps 1, 1, 2, 2, 3, 4, 4; step 4; no division by zero and one fixed documented step. |
| L6 | Year | Today in the middle of a week; a session 52 weeks ago and one 53 weeks ago | 52 rows, newest first; days after today not drawn; the older session is not on the screen. |
| L7 | Entry | No loaded session in the 52 weeks; one | The row "Load" is absent; present, and opens the screen; Back returns. |
| L8 | Chart | 12 weeks with 5 values; with 1 value | A chart with 5 points in week order; no chart. Pixel test: the line is drawn; with the line removed the test fails. |
| L9 | Grid | The 150-session fixture | Pixel test: a loaded day's cell has the accent color with the alpha of its step; a "trained, no load" cell has a border and no fill; an empty cell has neither; the week sums stand on their rows. With the fills removed the test fails. |
| L10 | Live | The maximum in use changes while the screen is open; a fill commits | The grid and the chart follow without leaving the screen. |
| L11 | Layout | 360 and 411 dp, font scale 1.0 and 1.3, a week load of 4 digits | No clipped or overlapping text; the 7 cells keep equal width; the existing tap-target and contrast tests pass. |
| L12 | Speech | A week row with a load; one without; the chart | The texts of Scope with full words; a week without a value says "no load". |
| L13 | Rules | Whole change | No verdict words or colors, no target or suggestion text. No Room, DAO write, backup, setting, permission, dependency, or version change. `SPEC.md` and `DESIGN.md` updated. No comment that a name, a type, or a test can say; no task or review IDs in code. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- A test for each new rule fails on the base (or does not compile there) and passes on the branch.
- The `Tests:` command passes on the final commit. Commit on branch `load01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| L1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
