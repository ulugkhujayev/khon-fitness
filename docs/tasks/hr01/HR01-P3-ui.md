# HR01-P3-ui: the visible parts: Add to, heart rate on Review, the raw data screen

Task-ID: HR01-P3-ui
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01
Base: 9d1ba17
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/hr01/docs/tasks/hr01/HR01-P3-ui-REPORT.md

## Why

See `docs/tasks/hr01/HR01-COMMON.md`. Phases 1 and 2 are on this branch.
This phase shows their results. `DESIGN.md` binds every row and screen:
read it before you draw anything. The owner rejects cards with per-row
buttons, pills, captions, helper sentences, horizontal scroll rows, and big
gaps. He was away when this was designed and has not seen it, so keep to
patterns that already exist in the app (`GroupedList`, `ListRow`, `Sheet`,
`ChoiceList`, `SectionTitle`, `GroupFooter`, `EditorTopBar`).

## Scope

Phase 3 of 3.

- Import sheet of a draft: a first group "Add to" (`SheetGroupTitle`,
  `ChoiceList`), one row per candidate from the phase 2 function, named
  "<exercise name> · <start time HH:mm>". The existing "Import as" group
  stays under it. No candidate, no group.
- Review screen (`ui/screens/ReviewScreen.kt`) and the read-only parts that
  already show a cardio session's average heart rate: a gym session and a
  cardio session show "avg 128 · max 164 bpm" when both exist, "avg 128
  bpm" when only the average exists, nothing when none.
- A new screen "Heart rate data", opened from a row in Settings. It is the
  owner's tool to see what the band really delivers. One row per session
  of the last 30 days that has a live window, newest first: the session
  name and date; then one line of facts: the state in words ("filled",
  "waiting for band data", "no data"), raw sample count, the median gap
  between samples in seconds, and the source app names. A tap on a row
  opens a sheet with the full facts: range start and end, first and last
  sample time, sample count, median and longest gap, sources, when it was
  read, and one action "Read again" that drops the row and fills again.
  With Health Connect missing or not allowed, the screen shows the one
  line the Import screen shows for that case and the same action.
- `versionName` 0.7.0, `versionCode` 29.
- `SPEC.md` (Navigation, Import) and `DESIGN.md` describe the new parts.

Do not change the data rules of phases 1 and 2.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| E1 | Add to | A draft with two candidates | The sheet shows "Add to" with two rows above "Import as"; a tap adds, closes the sheet, and the draft leaves the list. |
| E2 | Add to | A draft with no candidate | No "Add to" group; the sheet is as today. |
| E3 | Add to | The write is refused (phase 2, D8) | The sheet stays open and shows one line with the reason. |
| E4 | Review | Gym session with average and maximum; cardio with average only; a session with none | The three texts of Scope, in the place where the cardio average shows today. |
| E5 | Heart rate data | Three sessions: filled, waiting, none | Three rows with the facts of Scope; the median gap is computed from the stored samples. |
| E6 | Heart rate data | "Read again" | The row is read again and updates; a second tap while it runs does nothing. |
| E7 | Heart rate data | Health Connect missing or not allowed; no session with a window | One line each, no empty frame. |
| E8 | Access | Every new row and action | 48 dp touch target, a spoken name, and the existing tap test and contrast test cover the new screens (extend them). |
| E9 | Font scale | Font scale 1.6 at 360 x 780 dp | No text is cut on the new sheet group, the Review line, or the new screen. |
| E10 | Version and docs | Build | 0.7.0 (29); `SPEC.md` and `DESIGN.md` updated. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- New behavior has Robolectric tests: E1 to E7 and E9.
- You captured the new sheet group, the Review line, and the new screen (the repo has shot tests, for example `HistoryShotsTest`), and you looked at each capture. The report gives their paths.
- The `Tests:` command passes on the final commit. Commit on branch `hr01` (no push), messages hold only the change.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| E1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
