# PR01-fix2: fix the two defects the verify round found in the first fixes

Task-ID: PR01-fix2
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/pr01
Base: c1a3cd2
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/pr01/docs/pr01-tasks/PR01-fix2-REPORT.md
Coverage: required

## Why

The verify round of `PR01-fix1` is in
`docs/pr01-tasks/PR01-fix1-VERIFY-REPORT.md`. Read it, then
`docs/pr01-tasks/PR01-fix1.md` and its report. Two fixes have a defect
inside the fix diff. This task repairs them and nothing else. A second
failed verify round is a design alarm, so close every instance.

Witness file for F2:
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/PR01-rep-records-weight-trend-W/F2.json`.
Run `~/.config/agents/tools/witness run <file> --sha <your commit>` with
`ANDROID_HOME="$HOME/Library/Android/sdk"` exported (the witness clone has
no `local.properties`; without the variable Gradle stops with "SDK
location not found" and the tool prints a false `reproduces`). Read the
receipt log to confirm that the test ran. It must stay `fixed`.

## Scope

- **F2, the Finish request (`SessionScreen.kt`, `openSummary`).**
  - One pending request at most: a Finish tap while a read is pending
    starts no second read.
  - A result is shown only when its request is still wanted. A request is
    dropped when the sheet was dismissed after it started, when the
    screen leaves, and when the session is deleted or finished meanwhile.
    A dropped result never opens the sheet.
  - One read gives both set lists: the session's own sets and the other
    sessions' sets come from the same `allSetLogs` result, and the time
    is taken once, at the tap. The figures of one sheet belong to one
    instant.
  - Search the branch diff for every other place that starts a read and
    publishes its result into UI state later; list each one in the
    Coverage table, also when it needs no change.
  - Tests in `FinishSaveOnceTest.kt` (or next to it) with a query that
    the test holds and releases: two taps give one read and one sheet; a
    dismissal before the release leaves no sheet; a deletion before the
    release leaves no sheet and no crash; a set checked between the tap
    and the release is in both lists or in neither.
- **F4, the chart test (`LineChartTrendTest.kt`).**
  - Check the main line itself: accent pixels in the columns between two
    markers, with a trend and without one. The markers alone must not
    satisfy the check.
  - The last assertion compares a chart called with no trend argument and
    a chart called with an empty trend; make it check accent and muted
    pixels of both.
  - Break proof: remove the main stroke in a temp copy of `LineChart.kt`
    and show that this test fails; then show the same for the trend
    stroke.
  - Look at the other new tests of the first fix (`HistoryScreenTest.kt`
    243 and 267, `RepRecordsTest.kt`) for the same class, a check that a
    partial drawing or a partial text still passes, and fix each
    instance.

Do not change: the rep record rule, the trend numbers, any layout or
text, Room, the version number. No new dependency. No comment that a
name, a type, or a test can say; no task or review IDs in code.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| Y1 | Finish | Two fast taps on Finish while the read is held | One read; one sheet after the release. |
| Y2 | Finish | Tap, a second tap, the first sheet dismissed, then a late result | No sheet opens again. |
| Y3 | Finish | Tap, then the session is deleted before the release | No sheet, no crash. |
| Y4 | Finish | A set is checked between the tap and the release | The sheet's "Sets done", "Rep records" and "New bests" all come from one read; the time is the tap time. |
| Y5 | Finish | A fresh view model, one tap (the F2 witness) | Still `fixed`. |
| Y6 | Chart test | The main stroke removed; the trend stroke removed | The test fails in each case; it passes on the real code. |
| Y7 | Chart test | No trend argument against an empty trend | Equal accent and muted pixel counts, both checked. |
| Y8 | Rules | Whole change | Only `SessionScreen.kt`, tests, and the report change, unless a sibling instance needs more; each extra file is named in the report with its reason. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of both classes (instance, status, test, found by) and the break proof of each changed test.
- The `Tests:` command passes on the final commit. Commit on branch `pr01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| Y1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
