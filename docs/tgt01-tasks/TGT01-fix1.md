# TGT01-fix1: fix the review findings of the targets

Task-ID: TGT01-fix1
Class: Normal
Protocol: ~/.config/agents/review-protocol.md
Workdir: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/tgt01
Base: 87061ca
Capabilities: static
Tests: JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease
Report: /Users/mirzohidkhon/deepvision/personal/khon-fitness-wt/tgt01/docs/tgt01-tasks/TGT01-fix1-REPORT.md
Coverage: required

## Why

The deep review of the targets is in
`docs/tgt01-tasks/TGT01-targets-DEEP-REPORT.md`. Read it, then
`docs/tgt01-tasks/TGT01-targets.md` and its report. Six findings are
open. Each is a class: fix every instance, not only the lines the review
names.

Witness files, in
`/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks/TGT01-targets-W/`:
`F1.json` to `F5.json`. All five print `reproduces` on the base, each at
an assertion. Run
`~/.config/agents/tools/witness run <file> --sha <your commit>` for each
with `AGENTS_TASKS=/Users/mirzohidkhon/deepvision/personal/khon-fitness/tasks`
and `ANDROID_HOME="$HOME/Library/Android/sdk"` exported, and read each
receipt log. All five must print `fixed`. Do not change the witness
files; when one cannot pass for a reason outside the finding, report it
as blocked with the exact error.

The branch now holds a merge of the integration branch (after the
review). The shared number field (`NumberField` in
`ui/components/Components.kt`) has an `onTextChange` parameter there that
gives the text as typed; `ui/screens/MaxHeartSheet.kt` and `maxEntry` in
`ui/screens/ScoreRules.kt` show its use. Use it; do not change
`Components.kt`, `Stepper.kt`, or `LineChart.kt`.

## Scope and lead decisions

1. **F1, the sheet judges the text as typed.** One pure function per
   kind takes the raw text and gives Clear, Store(value), or Refused.
   - Empty text: Clear.
   - Accepted: ASCII digits with at most one separator, a point or a
     comma (a comma is the decimal sign). Leading zeros keep the value
     ("0080" is 80). The value must be inside the limits of the kind and
     on its step: bodyweight one decimal at most ("80.0", "80,5";
     "80.25" and "80.001" are refused), lift kg a multiple of 0.5
     ("100.5", "100.50", "100,0"; "100.3" and "100.501" are refused),
     reps whole ("12", "12.0" is refused).
   - Everything else is Refused: letters, a sign, a space, a second
     separator, other digit scripts, a value outside the limits.
   - Refused: Save is off and nothing is stored or cleared. The typed
     text stays in the field.
   - The step buttons of the field keep working and give a valid text.
2. **F2, exact direction and exact "Reached" for bodyweight.** The
   stored entry average must be exact: store the sum of the entries and
   their count (or another exact form), not a rounded average. The
   direction is the sign of the exact difference between the target and
   the entry average. "Reached" and the distance to the target use the
   exact present average, not its text with one decimal. A stored record
   in the old form (a rounded average) still reads; treat its value as
   exact.
3. **F3, stored values have limits.** A stored record reads as no target
   unless each of its numbers is a plain decimal (no exponent) of at
   most 12 characters, the target is inside the limits of its kind and
   on its step, the stored sum and count are positive, the count is at
   most 1000, and the entry average is from 20 to 300 kg. No stored text
   can throw or run long arithmetic. The date must parse; a date after
   today reads as today's entry (the rules must not fail on it).
4. **F4, the unit of a lift target.** The draft of an open sheet belongs
   to its unit. When the unit of the exercise changes while the sheet is
   open (a first session with weight arrives), the sheet closes and the
   draft is dropped. A stored target of the other unit shows "Not set",
   and the next Save (a value or an empty field) replaces or removes the
   stored record of the exercise, whatever its unit.
5. **F5, speech.** A "Not at this rate" row speaks "Not at this rate"
   only. A "More than 2 years" row speaks its rate, then "more than 2
   years". The texts on screen do not change.
6. **F6, tests.** Add:
   - a layout test that does not skip (not in the screenshot class): the
     result row with "More than 2 years" and with "Mar 14, 2027" next to
     the label "At +12.5 kg/week", at 360 and 411 dp and font scale 1.0
     and 1.3, with no clipped or overlapping text; and the same two
     states in the screenshots;
   - the stretch exercise route: no target group;
   - lift windows: a session exactly 55 days old counts and one 56 days
     old does not; a span of exactly 14 days gives a date and 13 days
     does not;
   - a bodyweight rate that does not print exactly: entries that give
     −0.26 kg/week with a 2.0 kg distance give 54 days (the printed
     "−0.3" would give 47);
   - a lift whose exact slope and printed rate give different days;
   - a lift whose line is at or above the target today with no session
     at the target: the date is today.
7. Keep the lead's change: the group title of a weighted lift is "Target
   est. 1RM".

No Room, DAO, backup, permission, dependency, or version change. No
comment that a name, a type, or a test can say; no task or review IDs in
code. Update `SPEC.md` and `DESIGN.md` where a rule changes (load the
`unslop` skill first). Look at each new screenshot yourself.

## Contract

| # | Finding | Case | Expected result |
|---|---|---|---|
| X1 | F1 | "abc", "8x0", "-80", "+80", " 80", "80.001", "80.25", "100.501", "100.3", "7.05", "12.0" (reps), "1,000.5", "٨٠" with a stored target of 100 | Save is off; a tap on Save stores nothing; the stored target stays 100. |
| X2 | F1 | "0080", "80,5" (bodyweight); "100,0", "100.50", "0000015" (lift kg); "0000015" (reps); an empty field | 80.0; 80.5; 100; 100.5; 15; 15 reps; the target is removed. |
| X3 | F2 | Entries 80.0, 80.0, 80.1 on the entry day, target 80.0; a week later three entries of 79.9 | "Reached". Witness F2 prints `fixed`. |
| X4 | F2 | A present average of 80.04 against a target of 80.0 going down; a record in the old form | Not reached (a date or "Not at this rate"); the old record reads with its value. |
| X5 | F3 | Stored texts with exponents, 13 or more characters, a zero or negative count, an average of 19 or 301, a broken date, a date after today | No target (or today's entry for the late date); no exception; the group renders. Witness F3 prints `fixed`. |
| X6 | F4 | An open reps sheet with "15" when a weighted session arrives; a stored reps target on a weighted exercise and a Save of an empty field; and a Save of 100 | The sheet closes and nothing is stored; the stored record is removed; the record is 100 kg. Witness F4 prints `fixed`. |
| X7 | F5 | The spoken text of each result state for lift kg, reps, and bodyweight | The texts of decision 5. Witness F5 prints `fixed`. |
| X8 | F6 | The tests of decision 6 | Each exists, passes, and fails when its rule is broken (break proof in the report). |
| X9 | All | Witness F1; the whole suite; the hard rules | F1 prints `fixed`. The `Tests:` command passes. No suggested or prefilled target other than the owner's own. |

## Definition of done

- Every contract row holds. Each check ran, and the report holds its real output.
- The Coverage table lists every instance of each class (instance, status, test, found by) and the five witness results.
- The `Tests:` command passes on the final commit. Commit on branch `tgt01` (no push, no merge), messages hold only the change, no AI attribution.

## Report

Write the report to the `Report:` path. Use one table row per contract row:

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: <what you did> | <command> | <real output, tail> |

The status is the first word of the second cell: `done`, `deviation`, or
`blocked`. Give the reason after the colon for `deviation` and `blocked`.

Run `~/.config/agents/tools/task-result check <task file>` before your final
message.
