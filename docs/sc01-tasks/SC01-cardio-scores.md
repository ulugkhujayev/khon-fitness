# SC01-cardio-scores: heart rate figures of a cardio session (time at or above 85 %, zone shares, peak and drop of each round)

Task-ID: SC01-cardio-scores
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/sc01
Base: 6139309
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/sc01/docs/sc01-tasks/SC01-cardio-scores-REPORT.md

## Why

The heart rate work on this branch stores the band's heart rate of a
finished session (`session_hr`, shown only through `hrFacts`) and the
clock times of each timer round (`interval_logs.startedAt`, `endedAt`).
The owner does Norwegian 4x4 and Zone 2 sessions and approved, on
2026-11-26, figures that say how hard a session really was: the time at
or above 85 % of the maximum, the peak of each round, the drop in each
rest, and for an easy session the share of time in the zone.

Read `docs/hr01-tasks/HR01-COMMON.md`, sections "The range of a session"
and "What the app shows", and `ui/screens/PictureRules.kt` (the chart of
the Review screen; reuse its bucket and round helpers).

This branch holds three merged lines of work: the data layer, the chart
of the Review screen (`PictureRules.kt`, `ReviewPicture.kt`), and the
heart rate screens (`HeartRules.kt`, `HeartDataScreen.kt`, the Settings
row "Heart rate data" in `PlanScreen.kt`, measured values in
`ExerciseDetail.kt` and `CardioScreen.kt`). Fixers still work on the
chart and on the heart rate screens on their own branches, and a
builder works on the band import: put your code in new files
(`ScoreRules.kt`, `ReviewScores.kt`, tests) and keep each change to an
existing file to a few lines, so that the lead can merge. Do not change
the `data`, `timer`, or `health` packages, `HeartRules.kt`,
`HeartDataScreen.kt`, `PictureRules.kt`, or `ReviewPicture.kt`; when you
need a helper of the chart rules, call it.

Hard rules of the app: everything stays on the phone; the app never
suggests exercises, sets, or loads; it shows facts only: no "good", no
"too low", no arrows, no green or red for a value, no target line. Read
`DESIGN.md` before any layout: no cards with per-row buttons, no pills,
no captions, no helper sentences; the existing row, group, sheet, and
chart components only.

## Scope

Read-only for workout data: no Room entity, DAO write, repository write
of a session, or backup change of sessions. One new setting in the
existing settings store. No new dependency, no version change.

### The maximum

- A setting "Maximum heart rate" (whole bpm, 100 to 230, empty by
  default), stored where the app's other settings are stored. When the
  backup file carries the settings, it carries this one the same way;
  when it does not, it does not.
- The measured maximum: the highest `maxHr` of the filled facts of
  sessions that finished in the last 365 days. None: no measured value.
- The maximum in use: the typed value; else the measured one; else none.
  With none, nothing of this task is shown anywhere.
- Settings: one row "Maximum heart rate" with the value on the right
  ("186 bpm"); with no typed value, the measured one in the muted color
  ("183 bpm"); with neither, "Not set". A tap opens the existing
  number-entry sheet; clearing the field removes the typed value.

### Rules (pure functions with unit tests, in a new `ScoreRules.kt`)

Input: the session, its exercise, its filled fact (buckets of 5 s), its
interval logs, the maximum in use. Every figure counts measured buckets
only; a bucket stands for 5 s. No value is made up for a gap.

- **Hard line**: `ceil(0.85 x maximum)` bpm.
- **Time at or above the hard line**: the count of buckets at or above
  it, times 5 s, as minutes and seconds ("11:35").
- **Zone**: from `ceil(0.60 x maximum)` to `floor(0.70 x maximum)` bpm,
  both inside. **Shares**: the percent of buckets below, inside, and
  above the zone, rounded half up, then the largest remainder corrected
  so that the three sum to 100.
- **Round peak**: the highest bucket whose time is inside
  `[startedAt, endedAt)` of the round; needs 2 or more buckets inside.
- **Round drop**: the round's last bucket minus the lowest bucket of the
  rest after it. The rest runs from the round's `endedAt` to the next
  round's `startedAt`, or to the range end for the last round. Needs a
  round peak and 2 or more buckets in the rest; a negative result shows
  as it is.
- A round without both times, or outside the range, has no figures.

### Review screen (finished cardio sessions with a filled fact and a maximum in use)

A group titled "Heart rate" after the existing round group (or after
the figures when there are no rounds):

- Exercise intensity high: a row "At or above 159 bpm" with the time on
  the right.
- Exercise intensity low: three rows "Below 112 bpm", "112 to 130 bpm",
  "Above 130 bpm" with the percent on the right.
- Each round that has a peak: a row "Round 1" with "peak 168" and, when
  it exists, " · drop 41" on the right, tabular figures.
- Rows are not tappable. A group with no row is absent.
- Spoken texts with full words: "At or above 159 beats per minute, 11
  minutes 35 seconds"; "Round 1, peak 168, drop 41 beats per minute".

### Cardio type screen (the screen that lists the sessions of one cardio exercise)

One chart with the existing `LineChart`, titled as the existing charts
of that screen are titled:

- intensity high: "At or above 85 %" in minutes per session;
- intensity low: "In zone" in percent per session.

A point for each session of that exercise with a filled fact, computed
with the maximum in use today. Fewer than 2 points: no chart.

### Documents and pictures

- `SPEC.md` and `DESIGN.md`: the setting, the rules with their numbers
  (85 %, 60 to 70 %, 5 s buckets, 365 days), the group, the chart, in
  each file's own style. Load the `unslop` skill first.
- A screenshot test in the way `PictureShotsTest.kt` does it: a 4x4
  session with 4 rounds, a Zone 2 session, the Settings row, the type
  screen chart, at 360 and 411 dp and font scale 1.0 and 1.3. Look at
  each picture yourself before you report, and name the files.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| S1 | Maximum | Typed 186 and measured 190; typed empty and measured 183; neither; the only filled fact is 366 days old; typed 99, 231, text | 186; 183; none; none; the three bad values are refused by the sheet and nothing is stored. |
| S2 | Hard line | Maximum 186; 187; buckets at 157, 158, 159 with maximum 186 | 159 (ceil of 158.1); 159 (ceil of 158.95); only the 159 bucket counts. Time for 139 counted buckets: "11:35". |
| S3 | Zone | Maximum 186; buckets 111, 112, 130, 131 | Zone 112 to 130; shares 25, 50, 25. |
| S4 | Shares sum | 3 buckets, one in each part; 0 buckets | 34, 33, 33 in a fixed documented order that sums to 100; no rows and no division by zero. |
| S5 | Round peak | A round with 48 buckets; with 1 bucket; without times; that starts before the range | Its highest bucket; nothing; nothing; nothing. |
| S6 | Round drop | Last work bucket 166, rest buckets down to 124; a rest with 1 bucket; the last round with the range end 40 s after it; a rest whose lowest bucket is above the last work bucket | 42; no drop; a drop from the buckets up to the range end when 2 or more exist; the negative number. |
| S7 | Facts | A waiting fact; none; removed; a stale row; an unfinished session; a gym session | No group and no chart point in each. |
| S8 | Review | High intensity with 4 rounds; low intensity steady; high intensity without rounds; no maximum | The hard row and 4 round rows; 3 zone rows; the hard row only; nothing. The existing Review content is unchanged (its tests stay green). |
| S9 | Live | The typed maximum changes while Review is open; the fill commits while Review is open | The rows follow without leaving the screen. |
| S10 | Type screen | 3 sessions with filled facts and 2 without; 1 with | A chart with 3 points in date order; no chart. Pixel test: the line is drawn; with the line removed the test fails. |
| S11 | Layout | 360 and 411 dp, font scale 1.0 and 1.3, 10 rounds | No clipped or overlapping text; the existing tap-target and contrast tests pass. |
| S12 | Speech | The hard row, a zone row, a round row, the Settings row | The texts of Scope with full unit words. |
| S13 | Rules | Whole change | No verdict words or colors, no suggestion or target text. No Room, DAO write, permission, dependency, or version change. `SPEC.md` and `DESIGN.md` updated. No comment that a name, a type, or a test can say; no task or review IDs in code. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- A test for each new rule fails on the base (or does not compile there) and passes on the branch.
- The `Tests:` command passes on the final commit. Commit on branch `sc01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| S1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
