# HR01-P3-ui: heart rate on Review and the "Heart rate data" screen

Task-ID: HR01-P3-ui
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-p3
Base: 662846b
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01-p3/docs/hr01-tasks/HR01-P3-ui-REPORT.md

## Why

See `docs/hr01-tasks/HR01-COMMON.md`. Phase 1 is on this branch; read
its report in `docs/hr01-tasks/` (phase 2, the band import, comes later
and this phase does not use it). This phase shows what phase 1 stores.
Names: `repo.sessionHr(id)` and `repo.allSessionHr` are the flows of the
rows; `KhonApp.heartFill` (`HeartFill`) has `rereadHeartRate(sessionId):
HrReread`, `removeHeartRate(sessionId): Boolean`, and `foreground()`;
`FakeHeart` in the test folder of the `data` package fakes the band. The
timer code of this branch is under a separate fix, and another builder
adds a chart to the Review screen on another branch: do not change the
`timer` package, and keep the change to the Review screen to the one
line. It changes no stored data by itself: "Read again" and "Remove" call
`rereadHeartRate` and `removeHeartRate` of phase 1.

Read `DESIGN.md` before any layout: no cards with per-row buttons, no
pills, no captions, no helper sentences, the existing row and sheet
components only. The app never suggests anything: every text here is a
fact or an action name.

## Scope

- **One rule for every screen**: what is shown comes from `hrFacts` of
  phase 1 and nothing else. A typed average is shown where it exists; the
  measured one only where the typed one is null. The maximum is always
  the measured one. No screen reads a `session_hr` row directly.
- **Review screen** (a finished session, gym and cardio): one line with
  the heart rate facts next to the existing figures: "avg 128 · max 164
  bpm"; with one value only "avg 128 bpm" or "max 164 bpm"; with none, no
  line. Spoken text: "average 128, maximum 164 beats per minute". It
  follows the flows of the session and of its HR row, so a fill that
  lands while the screen is open shows up.
- **Every other place that shows a session's or a round's average heart
  rate today** (History rows, the cardio type screens, the rounds of a
  cardio session on Review): a null typed value shows the measured one
  (for a round: the round average of phase 1). List each place in the
  report.
- **Cardio form**: an empty average field, and an empty round field,
  shows the measured value as its placeholder in the muted color. Save
  never writes a placeholder.
- **Settings**: one row "Heart rate data" that opens a new screen.
- **"Heart rate data" screen**: the finished sessions of the last 30
  days, newest first, one row each: the name over the date, and on the
  right the state as a fact:
  - `filled`: "avg 128 · max 164"
  - `waiting`: "Waiting for data"
  - `none`: "No data"
  - `off`: "Removed"
  - not read (also a row of another range): "Not read"
  - no range: "No time range"
  A tap opens a sheet for that session with rows of facts: the range as
  clock times ("18:02 to 19:14"), readings ("412 read · 398 used"), first
  and last reading as clock times, the longest gap ("45 s"), the average of each work round that has one, the source
  (the package's app label when the system knows it, else the package
  name; other packages seen on a second row), average, maximum. Rows
  without a value are absent. Two actions under the facts: "Read again"
  (absent with no range) and "Remove" (absent with no row or `off`).
  For a finished gym session with no range that `confirmRange` of phase 1
  accepts, the sheet shows the session's start and finish as clock times
  and one action "Use this time range"; after it the sheet shows the
  state of the new range. No other session has this action.
  "Read again" shows its result in the sheet as a fact line: "No data
  yet", "Health Connect did not answer", or the new values. A second tap
  while one runs does nothing. A session deleted meanwhile closes the
  sheet.
  With no Health Connect or no heart rate permission, the screen shows the
  same state and action the Import screen shows for that case (reuse its
  component; ask for heart rate only).
- `SPEC.md` and `DESIGN.md`: the new line, row, and screen, in each
  file's own style. Load the `unslop` skill first.
- A screenshot test of the new screen and the sheet in the way
  `HistoryShotsTest.kt` does it, when that harness can render them.

Do not change: the data layer (report a missing function as blocked), the
Import screen, the version number, the manifest. No new dependency.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| E1 | Review | Sessions with both values, avg only, max only, none; gym and cardio | The line as in Scope, or absent. The spoken text holds the full words. |
| E2 | Review | A fill commits while Review is open; "Remove" runs; the range changes so that the row is stale | The line appears, changes, or goes without leaving the screen. A stale row shows nothing. |
| E2a | Other places | A session with typed average; with measured only; rounds with typed, measured, and no value | Typed wins; measured fills the gaps; nothing where neither exists. Each place listed in the report has a test or is covered by a shared function with a test. |
| E2b | Cardio form | Empty fields with measured values; the owner saves without typing; the owner types a value | Placeholders show the measured values; after Save the typed columns are still null; a typed value is saved and shown instead. |
| E3 | Settings | The row | "Heart rate data" opens the screen; back returns to Settings. |
| E4 | List | Sessions in each of the six states, and one with a filled row of another range ("Not read"); a session 31 days old; an unfinished session | Each state text as in Scope; the old and the unfinished session are absent; newest first. |
| E5 | Sheet | A filled row; a sparse row (3 readings, one gap); a row with two packages; a row with no reading | Facts as in Scope, absent when null; one reading shows no gap; clock times in the phone's zone. |
| E6 | Read again | Tap once; tap twice fast; the read fails; the read is sparse; the session is deleted during the read; the read commits and its result reaches the sheet after "Remove" or after the range changed | One request; the old facts stay on a failure; the result line as in Scope; no crash, the sheet closes on delete. A late result shows nothing by itself: the sheet and the list show what `hrFacts` gives now. |
| E7 | Remove | Tap on a filled row | The row shows "Removed"; Review and the other places show no measured value for that session; typed values still show; "Remove" is absent now; "Read again" brings the data back. |
| E7a | Use this time range | A gym session of 40 min without a range; one of 9 min; a cardio session without a range | The first shows "18:02 to 18:42" and the action; a tap stores the range, the action goes, and the state follows the read. The others show "No time range" and no action. |
| E8 | Provider | No Health Connect; no heart rate permission; heart rate without distance | The Import screen's state and action for the first two; the third works with no prompt. |
| E10 | Layout | The new screen and sheet at font scale 1.0 and 1.3, widths 360 and 411 dp | No clipped or overlapping text; touch targets at least 48 dp; contrast as `ContrastTest` requires; the existing `TapTargetTest` rules pass for the new screen. |
| E11 | Rules | Whole change | No suggestion text, no caption, no helper sentence, no card with per-row buttons, no pill. No data-layer, manifest, permission, or version change. `SPEC.md` and `DESIGN.md` updated. |

## Attack cases

None: this phase is Normal class and has no attack step.

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The `Tests:` command passes on the final commit. Commit on branch `hr01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row
and per attack case:

| # | Status | Check ran | Real output |
|---|---|---|---|
| E1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
