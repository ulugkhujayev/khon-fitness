# TGT01-targets: a target that the owner types, and the date on which the present trend reaches it

Task-ID: TGT01-targets
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/tgt01
Base: 4171ab0
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/tgt01/docs/tgt01-tasks/TGT01-targets-REPORT.md

## Why

The owner approved, on 2026-11-26, "Targets with an honest date: you set
the target, the app shows when your trend reaches it". The app already
computes two trends: the bodyweight rate (`trendText` in
`ui/screens/HistoryRules.kt`: the 7-day average and its change against
the 7 days before) and the rate of a lift (`periodRate` and
`sessionValue` in `ui/screens/SetupRules.kt`: a least-squares line
through the best estimated 1RM, or the best reps, of each session).

Hard rules of the app: everything stays on the phone; the app never
suggests exercises, sets, loads, or targets; the owner types each
target. The app shows facts only: no "good", no "behind", no arrows, no
green or red for a value, no progress bar. Read `DESIGN.md` before any
layout: no cards with per-row buttons, no pills, no captions, no helper
sentences; the existing row, group, and sheet components only.

"Honest" means: the date comes from measured values only, the row names
the rate that it used, and the app shows no date when the trend does not
support one.

## Scope

No Room entity, DAO, migration, repository write, backup change,
permission, dependency, or version change. Do not change
`ui/components/Components.kt` or `ui/components/LineChart.kt`; other
open branches change them. Put the rules and the store in new files; in
`ExerciseDetail.kt` and `HistoryProgress.kt` add only the call of the
new group.

### The store (new `data/Targets.kt`, in the existing settings DataStore, as `data/MaxHeartRate.kt` does it)

- One bodyweight target: the value in kg (20.0 to 300.0, one decimal),
  the 7-day average at the moment of the entry (or the latest entry when
  there is no average; none when there is no bodyweight entry), and the
  date of the entry.
- One target for each strength exercise, keyed by the exercise id: the
  value (estimated 1RM in kg, 1.0 to 500.0, steps of 0.5, for an
  exercise with weight; reps, 1 to 500, whole, for one without), and the
  date of the entry.
- A missing key, a broken value, or a value outside its limits reads as
  no target. Clearing the field removes the target. The app's existing
  "Reset" of the settings removes all targets, if such an action clears
  the settings store today; if not, leave it.
- Targets are not in the backup.

### Rules (pure functions with unit tests, new `ui/screens/TargetRules.kt`)

Every rule takes `today` as a parameter.

**Lift.** The value of a session is `sessionValue`. The trend uses the
sessions of the last 56 days that have a value.

- Reached: the first session on or after the entry date whose value is
  at or above the target. Result: "Reached" with that session's date.
- Else, no line: fewer than 3 such sessions in the 56 days, or fewer
  than 14 days from the first to the last of them. Result: no date.
- Else fit the least-squares line of `periodRate` (exact decimal
  arithmetic, the same sums). Slope at or below 0, or a rate that
  `signedTenths` prints as "±0.0": result "Not at this rate".
- Else the date is the first day, not before today, on which the line is
  at or above the target. More than 730 days after today: result "More
  than 2 years".
- The rate text is the one of `periodRate` for the same sessions.

**Bodyweight.** The present value is the 7-day average of `trendText`;
the rate is its kg per week.

- The direction is the sign of target minus the stored average of the
  entry. No stored average, or equal values: the direction is the sign
  of target minus the present average; equal again: "Reached" with the
  entry date.
- Reached: the present average is at or beyond the target in that
  direction. Result: "Reached" (no date; an average has no single day).
- Else, no rate (no average for today or for the 7 days before): no date.
- Else a rate of "±0.0" or a rate against the direction: "Not at this
  rate".
- Else the date is today plus the days that the distance needs at that
  rate, rounded up to a whole day. More than 730 days: "More than 2
  years".

Each date uses the exact rate, not its rounded text. All arithmetic is
decimal or checked: no overflow and no division by
zero for any stored value.

### Exercise screen (strength exercises only, `ExerciseDetail.kt`)

A group titled "Target" after the group of setup periods (or after the
chart when there are none):

- Row "Target" with the value on the right: "100 kg" (no decimal when it
  is whole, else one), or "12 reps", or "Not set" in the muted color. A
  tap opens a number-entry sheet titled "Target est. 1RM" or "Target
  reps", with the present target in the field; an empty field removes
  the target. A value outside the limits is refused by the sheet and
  nothing is stored.
- With a target and a result, a second row. Label: "At +0.4 kg/week"
  (the rate text and unit, "reps/week" for reps) when the result is a
  date, "More than 2 years", or "Not at this rate"; label "Reached" when
  it is reached. Value on the right: the date ("Mar 14", with the year
  when it is not this year: "Mar 14, 2027"), "More than 2 years", "Not
  at this rate", or the date of the session that reached it.
- With a target and no date (no line): the first row only.
- The second row is not tappable.

### History, Progress, section "Bodyweight" (`HistoryProgress.kt`)

After the existing bodyweight content, the same group "Target" with the
same two rows: "Target" with "80.0 kg" (always one decimal, as the
bodyweight texts) or "Not set"; then "At −0.3 kg/week" with the date, or
"Reached" with no value on the right. The sheet is titled "Target
bodyweight". The group shows also when there is no bodyweight entry yet
only if the section itself shows then; else follow the section.

### Speech

Full words: "Target, 100 kilograms, button"; "Target, not set, button";
"At plus 0.4 kilograms per week, March 14, 2027"; "Reached, November
20"; "Not at this rate". Use `spokenRate` where it fits.

### Documents and pictures

- `SPEC.md` and `DESIGN.md`: the store, the rules with their numbers (56
  days, 3 sessions, 14 days, 730 days, the limits), and the two groups,
  in each file's own style. Load the `unslop` skill first.
- A screenshot test in the way `RecordShotsTest` or the other
  `*ShotsTest` files do it: the exercise screen with a date, with
  "Reached", with "Not at this rate", with "Not set"; the bodyweight
  section with a date and with "Not set"; the sheet; at 360 and 411 dp
  and font scale 1.0 and 1.3. Look at each picture yourself before you
  report, and name the files.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| G1 | Store | Set, read, clear a lift target and the bodyweight target; a broken stored text; a value outside the limits; two exercises | The value comes back with its entry date; none; none and nothing stored; each exercise has its own target. |
| G2 | Lift date | Sessions at days 0, 14, 28, 42 with values 90, 92, 94, 96 (today = day 42), target 100 | Slope 1.0 kg/week; the date is day 70; the label says "+1.0 kg/week". |
| G3 | Lift no line | 2 sessions; 3 sessions inside 13 days; all sessions older than 56 days | No date, the Target row only. |
| G4 | Lift flat or down | Values 96, 94, 92, 90; values that give a rate printed "±0.0" | "Not at this rate". |
| G5 | Lift reached | A session on the entry date at the target; a session before the entry date above the target and none after; a later session above it | "Reached" with that session's date; not reached (the forecast rules apply); "Reached" with the first such date. |
| G6 | Lift far | A slope that needs 731 days; one that needs 730 | "More than 2 years"; a date. |
| G7 | Reps | An exercise without weight, best reps 8, 9, 10, 11 over 42 days, target 15 | A date from the reps line; the label says "reps/week"; the Target row says "15 reps". |
| G8 | Bodyweight date | Stored average 83.2, present average 82.0, rate −0.3 kg/week, target 80.0 | The date is today plus 47 days (2.0 / 0.3 × 7 = 46.67, rounded up). |
| G9 | Bodyweight direction | Target above the stored average with a falling rate; present average beyond the target; no stored average; equal values | "Not at this rate"; "Reached"; the direction comes from the present average; "Reached". |
| G10 | Bodyweight no rate | Entries in the last 7 days only; no entry in the last 7 days | The Target row only, in both. |
| G11 | Limits | Lift target 0.5, 500.5, 100.3, text; reps 0, 501, 7.5; bodyweight 19.9, 300.1, 80.25 | Each is refused by the sheet and nothing is stored. |
| G12 | Live | The target changes while the screen is open; a new session or bodyweight entry arrives; the date changes at midnight | The rows follow on the next composition with the new `today` (no timer is needed while the screen stays open). |
| G13 | Other screens | Cardio and stretch exercise screens; an exercise with no session | No group on cardio and stretch; the Target row only. |
| G14 | Layout | 360 and 411 dp, font scale 1.0 and 1.3, the longest texts ("At +12.5 kg/week", "More than 2 years", "Mar 14, 2027") | No clipped or overlapping text; the existing tap-target and contrast tests pass. |
| G15 | Speech | Each row state | The texts of Scope with full unit words. |
| G16 | Rules | Whole change | No verdict words or colors, no suggested target, no prefilled value other than the owner's own target. No Room, DAO, backup, permission, dependency, or version change. `SPEC.md` and `DESIGN.md` updated. No comment that a name, a type, or a test can say; no task or review IDs in code. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- A test for each new rule fails on the base (or does not compile there) and passes on the branch.
- The `Tests:` command passes on the final commit. Commit on branch `tgt01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| G1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
