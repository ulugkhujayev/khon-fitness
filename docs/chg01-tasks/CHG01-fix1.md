# CHG01-fix1: fix the deep review findings of the setup changes group

Task-ID: CHG01-fix1
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/chg01
Base: 698198a
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/chg01/docs/chg01-tasks/CHG01-fix1-REPORT.md
Coverage: required

## Why

The deep review of branch `chg01` is in
`docs/chg01-tasks/CHG01-setup-changes-DEEP-REPORT.md`. Read it and the
original task file `docs/chg01-tasks/CHG01-setup-changes.md` first. This
task is step 6 of the protocol: fix each class, every instance.

Witness files (each with a test patch next to it), in
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/CHG01-setup-changes-W/`:
`F3.json`, `F5.json`, `F6.json`. Run
`~/.config/agents/tools/witness run <file> --sha <your commit>` for each
with `ANDROID_HOME="$HOME/Library/Android/sdk"` exported (the witness
clone has no `local.properties`; without the variable Gradle stops with
"SDK location not found" and the tool prints a false `reproduces`). Read
each receipt log to confirm that the test ran. Show `fixed` for the
three in the Coverage table. `F1.json` and `F2.json` are not used: see
the decisions below.

## Scope

Lead decisions per finding:

- **F1, the latest one-session run (no code change, wording only).** The
  builder's rule stays: after the one-session runs have joined their
  neighbors, two neighbor periods with the same setup are one period. So
  A A B A is one period, as A A B A A is. A A B is two. Add a unit test
  for A A B A and A B A (one period each) if none exists, and say the
  rule in `SPEC.md` in one sentence.
- **F2, chart markers (fix, decided design).** A marker belongs to a
  charted point, not to a date. `ChartPoint` gets a flag for it; the
  list of dates goes away. A marker stands at the first charted point of
  each period, except when that point is the first point of the chart. A
  period with no charted point (every session skipped) has no marker.
  The point is chosen by its session, never by its date, so two sessions
  on one date cannot take each other's marker. A chart whose points
  carry no flag draws exactly as before. Tests: a boundary session
  without a best set (the marker stands at the period's next charted
  session); two periods of which the middle one has no charted point
  (one marker, at the first point of the third period); a change whose
  period has no charted point at the end (no marker); two sessions on
  one date in different periods (the marker is at the second); one
  point (no marker); the pixel test checks the exact column of each
  marker against the point's own column, not a step derived from the
  markers.
- **F3, spoken text (fix).** The spoken row is built from its parts: the
  dates in words, the saved program name unchanged (no replacement of
  dashes or dots inside it), the sets, the rep range ("5 to 8
  repetitions"), and the rate with "kilograms per week" or "repetitions
  per week". Search the diff for every other place that rewrites visible
  text into spoken text by string replacement, and fix each.
- **F4, tests that a partial result passes (fix).** Add: values 10, 40,
  12 on days 0, 1, 14 (least squares, not the end points); a period where
  skipped sessions make the session span longer than the valued span
  (the 14-day rule uses the valued sessions); the two same-day sessions
  in reversed input order; the long name checked by its real layout (the
  text is cut with an ellipsis and the rate is whole), not only by
  bounds. For each changed or new test, break the code in a temp copy
  and show that the test fails.
- **F5, large rep counts (fix).** Convert to `Double` before the
  arithmetic in both branches of the session value. Search `SetupRules.kt`
  and the code this branch added to `HistoryRules.kt` for other `Int`
  arithmetic on saved values.
- **F6, numbered comments (fix).** Remove the nine section comments in
  `SetupRulesTest.kt` and every other comment the branch added that a
  name, a type, or a test can say, or that holds a contract row number.
  List the added comment lines you kept, each with its reason.

Do not change: the rules of the original task beyond the decisions
above, the texts on the screen, Room, the version number. No new
dependency.

## Contract

| # | Part | Case | Expected result |
|---|---|---|---|
| X1 | Periods | A A B A; A B A; A A B | One period; one period; two periods. `SPEC.md` states the rule. |
| X2 | Markers | The six cases of F2 in Scope | As in Scope. A chart with no flagged point is pixel-equal to the base chart. |
| X3 | Spoken text | A program named "Push – Pull · PM"; an exercise without weight | The name is spoken as saved; "repetitions per week". Witness F3 `fixed`. |
| X4 | Tests | The four additions of F4 | Each is a test that fails when the code is broken in the named way, with the break shown. |
| X5 | Value | A set with `Int.MAX_VALUE` reps, with and without weight | A positive value, no overflow. Witness F5 `fixed`. |
| X6 | Comments | The branch diff | No numbered or redundant comment added. Witness F6 `fixed`. `comment-guard --base aa7faf0` exits 0. |
| X7 | Rules | Whole change | The screenshots of the original task are made again and you looked at each; the report names the files. No Room, DAO, backup, dependency, or version change. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of each class (instance, status, test, found by), the witness results, and the break proofs.
- The `Tests:` command passes on the final commit. Commit on branch `chg01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
