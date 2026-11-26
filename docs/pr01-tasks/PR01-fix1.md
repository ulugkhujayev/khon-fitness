# PR01-fix1: fix the deep review findings of rep records and the bodyweight trend

Task-ID: PR01-fix1
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/pr01
Base: c0a6ace
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/pr01/docs/pr01-tasks/PR01-fix1-REPORT.md
Coverage: required

## Why

The deep review of branch `pr01` (step 5 of the protocol) is in
`docs/pr01-tasks/PR01-rep-records-weight-trend-DEEP-REPORT.md`. Read it
and the original task file `docs/pr01-tasks/PR01-rep-records-weight-trend.md`
first. This task is step 6: fix each class, every instance.

Witness files (each has a test patch next to it):
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/PR01-rep-records-weight-trend-W/F1.json`
and `F2.json`. Run
`~/.config/agents/tools/witness run <file> --sha <your commit>` for each
and show `fixed` in the Coverage table.

## Scope

Lead decisions per finding:

- **F1, decimal rounding (fix).** Class: a displayed decimal is rounded
  from a binary double. Round the magnitude half up on the decimal value,
  then apply the sign: −0.25 shows "−0.3", +0.25 shows "+0.3"; the "±"
  sign is only for a magnitude below 0.05; a mean of 79.45 shows "79.5".
  Search the branch diff for every other place that formats a new number
  (`weekAverage`, `weekRate`, `trendText`, `trendPoints`, the trend line
  on the card, the Today status) and fix each instance. Do not change the
  rounding of text that existed on the base.
- **F2, the finish sheet reads a list nobody collects (fix).** The
  session screen reads `vm.allSetLogs.value`; the flow is
  `WhileSubscribed`, so it is empty when no screen collects it. Then "New
  bests" and "Rep records" are both wrong. Make the session screen get the
  other sessions' logs in a way that is loaded when the sheet opens
  (collect the flow on the screen, or read once from the repository before
  the sheet shows). Search the branch for the same pattern
  (`.value` of a `stream(...)` flow read outside a collector) in code this
  branch added or touched, and fix each instance.
- **F3, a count after a History edit (not a defect, no change).** The
  lead wrote contract row C7 badly. No summary exists in the History edit
  flow and none is added. C7 means only: `summarize` compares against
  every other session, also later ones; the rule test holds that. Do not
  add UI for it.
- **F4, test gaps (fix).** Add assertions for: "Most reps by weight" with
  seven eligible sets shows six rows, the heaviest six, and no row has a
  click action; one Log row that holds both "1 PR" and "1 rep PR" with
  their styles; the chart with and without a trend (the scale and the
  drawn series, in the way the repo can assert a chart).
- **Lead finding L1, a hanging separator (fix).** In the Log row, a long
  line wraps as "… 58 min ·" and then "2 rep PR" on the next line
  (`ui/screens/HistoryLog.kt`, the `DayKind.DONE` line). The separator
  must stay with the text after it: use a no-break space between "·" and
  the count for both the "N PR" and the "N rep PR" parts, and a normal
  space before the "·". Add a test of the built string.

Change nothing else. No Room, DAO, repository-function, backup, version,
or manifest change (reading the repository from the screen for F2 is
allowed; adding a repository read function is allowed only when none
fits).

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| X1 | F1 | Rates −0.25, +0.25, −0.05, +0.05, −0.04, +0.04; a mean of 79.45 | "−0.3", "+0.3", "−0.1", "+0.1", "±0.0", "±0.0" kg/week; "79.5 kg". Witness F1 says `fixed`. |
| X2 | F2 | A fresh view model; an earlier finished session with 85x3 and 80x6; a running session with 80x8; open the session screen directly and tap Finish | The sheet shows "Rep records 1" and the right "New bests". Witness F2 says `fixed`. |
| X3 | F4 | The three test gaps | Each new assertion fails when its rule is broken in a temp copy (show it), and passes on the branch. |
| X4 | L1 | A Log row with a long line and both counts | The built string keeps "·" and the count together; the screenshot of the Log shows no line that ends with "·". |
| X5 | Rules | Whole change | Only the fixes above. The `Tests:` command passes on the final commit. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The report has a `## Coverage` table: one row per instance of each class (instance, status, test, found by).
- Commit on branch `pr01` (no push, no merge), messages hold only the change, no AI attribution, no task or review IDs in code.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
