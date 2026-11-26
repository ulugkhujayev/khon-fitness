# CMP01-fix1: fix the deep review findings of the compare screen

Task-ID: CMP01-fix1
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/cmp01
Base: d9fb265
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/cmp01/docs/cmp01-tasks/CMP01-fix1-REPORT.md
Coverage: required

## Why

The deep review of branch `cmp01` (step 5 of the protocol) is in
`docs/cmp01-tasks/CMP01-compare-periods-DEEP-REPORT.md`. Read it and the
original task file `docs/cmp01-tasks/CMP01-compare-periods.md` first. This
task is step 6: fix each class, every instance. The lead accepts all six
findings.

Witness files (each has a test patch next to it):
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/CMP01-compare-periods-W/F1.json`
to `F4.json`. Run
`~/.config/agents/tools/witness run <file> --sha <your commit>` for each
and show `fixed` in the Coverage table. Export
`ANDROID_HOME="$HOME/Library/Android/sdk"` first: without it the witness
clone cannot find the SDK, Gradle stops, and the witness can never say
`fixed`. After each run, read the receipt log it names and confirm that
the test itself ran.

## Scope

- **F1, records inside the selected period go missing.** Compare reads
  History's data, which drops every record dated after today. Compare must
  filter by its own periods only: a program period reaches its last
  finished session, and a custom period counts every record dated inside
  it, also after today. History's own callers keep their filter. Search
  `CompareScreen.kt` and `CompareRules.kt` for every input list (sessions,
  set logs, stretch sessions, bodyweights, exercises) and fix each one
  that comes through the History filter.
- **F2, unequal space for values.** In the two period rows the date range
  is measured first and a long program name ends with an ellipsis. In the
  stacked layout the gap sits between two cells of equal width.
- **F3, the weekly muscle numbers changed.** The old weekly function must
  give exactly the base result for every input: the same session choice
  for a repeated session id and the same row order for equal totals. Keep
  the shared counting rule, and keep the base behavior in the wrapper.
- **F4, the old cardio range throws.** The old 30-day function with any
  `days` value, also a negative one, behaves as on the base (every
  historical type listed with empty sessions and zero totals). The new
  `Period` keeps its `from <= to` rule.
- **F5, comments.** Remove each comment that repeats a name, a type, or a
  test (the review lists them); keep the text-wrapping explanation. Search
  the whole branch diff for more of the class.
- **F6, test gap.** C8's test asserts per-type seconds, meters, and
  sessions for a run with time and distance plus a run with time only, and
  that the pace uses only the run that has both.

Change nothing else. No Room, DAO, repository, backup, version, or
manifest change.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| X1 | F1 | A program with finished sessions on Nov 20 and Nov 28, today Nov 27; a custom period up to Nov 28 with gym, cardio, stretch, and bodyweight records on Nov 28 | The program period is Nov 20 to Nov 28 with two gym sessions; the custom period counts each Nov 28 record. History's Log and Progress show what they showed before. Witness F1 says `fixed`. |
| X2 | F2 | A very long program name in a period row at 360 dp; the stacked layout at font 1.3 | The whole date range shows and the name is cut; both value cells have the same width. Witness F2 says `fixed`. |
| X3 | F3 | Equal totals for two muscles; one session id in two finished snapshots | The old weekly function returns exactly what the base returns. A test compares against values taken from the base code. Witness F3 says `fixed`. |
| X4 | F4 | The old cardio function with `days = -1` and with a type that has only older sessions | No throw; the base result. Witness F4 says `fixed`. |
| X5 | F5 | The branch diff | No comment that repeats code or a test; `comment-guard --base 9d1ba17` is clean. |
| X6 | F6 | The C8 test | The new assertions fail when per-type seconds return zero in a temp copy (show it), and pass on the branch. |
| X7 | Rules | Whole change | Only the fixes above. The `Tests:` command passes on the final commit. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The report has a `## Coverage` table: one row per instance of each class (instance, status, test, found by).
- Commit on branch `cmp01` (no push, no merge), messages hold only the change, no AI attribution, no task or review IDs in code.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Add a `## Coverage` section with one row per instance of each class. Every
contract row ID needs at least one row:

| # | Instance | Status | Test | Found by |
|---|---|---|---|---|
| X1.1 | <file:line or name> | fixed: <what changed> | <test name> | <grep pattern, caller trace, or coverage list entry> |

The row ID is `<contract ID>.<n>`. The status is `fixed`, `kept` (the
instance stays, and the test proves that it is safe), or `blocked:
<reason>`. The test cell is `none` only for `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
