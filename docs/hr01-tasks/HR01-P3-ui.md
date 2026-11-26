# HR01-P3-ui: heart rate on Review and the "Heart rate data" screen

Task-ID: HR01-P3-ui
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01
Base: 9d1ba17
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01/docs/hr01-tasks/HR01-P3-ui-REPORT.md

## Why

See `docs/hr01-tasks/HR01-COMMON.md`. Phases 1 and 2 are on this branch;
read their reports in `docs/hr01-tasks/`. This phase shows what they
store. It changes no stored data by itself: "Read again" and "Remove" call
`rereadHeartRate` and `removeHeartRate` of phase 1.

Read `DESIGN.md` before any layout: no cards with per-row buttons, no
pills, no captions, no helper sentences, the existing row and sheet
components only. The app never suggests anything: every text here is a
fact or an action name.

## Scope

- **Review screen** (a finished session, gym and cardio): one line with
  the heart rate facts next to the existing figures: "avg 128 · max 164
  bpm"; with one value only "avg 128 bpm" or "max 164 bpm"; with none, no
  line. Spoken text: "average 128, maximum 164 beats per minute". It
  follows the session's flow, so a fill that lands while the screen is
  open shows up.
- **Settings**: one row "Heart rate data" that opens a new screen.
- **"Heart rate data" screen**: the finished sessions of the last 30
  days, newest first, one row each: the name over the date, and on the
  right the state as a fact:
  - `filled`: "avg 128 · max 164"
  - `waiting`: "Waiting for data"
  - `none`: "No data"
  - `off`: "Removed"
  - a range and no row: "Not read"
  - no range: "No time range"
  A tap opens a sheet for that session with rows of facts: the range as
  clock times ("18:02 to 19:14"), readings ("412 read · 398 used"), first
  and last reading as clock times, the longest gap ("45 s"), the source
  (the package's app label when the system knows it, else the package
  name; other packages seen on a second row), average, maximum. Rows
  without a value are absent. Two actions under the facts: "Read again"
  (absent with no range) and "Remove" (absent with no row or `off`).
  "Read again" shows its result in the sheet as a fact line: "No data
  yet", "Health Connect did not answer", or the new values. A second tap
  while one runs does nothing. A session deleted meanwhile closes the
  sheet.
  With no Health Connect or no heart rate permission, the screen shows the
  same state and action the Import screen shows for that case (reuse its
  component; ask for heart rate only).
- **Restore sheet** (Settings): also lists `before-merge-*.json` copies,
  named "Before merge" plus the time, in the same list.
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
| E2 | Review | A fill commits while Review is open | The line appears or changes without leaving the screen. |
| E3 | Settings | The row | "Heart rate data" opens the screen; back returns to Settings. |
| E4 | List | Sessions in each of the six states; a session 31 days old; an unfinished session | Each state text as in Scope; the old and the unfinished session are absent; newest first. |
| E5 | Sheet | A filled row; a sparse row (3 readings, one gap); a row with two packages; a row with no reading | Facts as in Scope, absent when null; one reading shows no gap; clock times in the phone's zone. |
| E6 | Read again | Tap once; tap twice fast; the read fails; the read is sparse; the session is deleted during the read | One request; the old facts stay on a failure; the result line as in Scope; no crash, the sheet closes on delete. |
| E7 | Remove | Tap on a filled row | The row shows "Removed"; the session's reader values are gone as phase 1 defines; "Remove" is absent now; "Read again" brings the data back. |
| E8 | Provider | No Health Connect; no heart rate permission; heart rate without distance | The Import screen's state and action for the first two; the third works with no prompt. |
| E9 | Restore | A `before-merge` copy and a `before-import` copy exist | Both listed, each named by kind and time; restoring the merge copy works as the existing restore. |
| E10 | Layout | The new screen and sheet at font scale 1.0 and 1.3, widths 360 and 411 dp | No clipped or overlapping text; touch targets at least 48 dp; contrast as `ContrastTest` requires; the existing `TapTargetTest` rules pass for the new screen. |
| E11 | Rules | Whole change | No suggestion text, no caption, no helper sentence, no card with per-row buttons, no pill. No data-layer, manifest, permission, or version change. `SPEC.md` and `DESIGN.md` updated. |

## Attack cases

<!-- ATTACK-CASES -->

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- Every attack case above is a test that passes.
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
