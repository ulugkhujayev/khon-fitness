# TGT01-targets report

Branch `tgt01`, base 4171ab0. Commits:

- da0f1c1 Add typed targets with the date the present trend reaches them
- b3b3bfd Test the target groups on screen and keep kg/week on one line
- 81cde5b Describe the targets in SPEC.md and DESIGN.md
- (this report) Add the TGT01 report

## Contract

| # | Status | Check ran | Real output |
|---|---|---|---|
| G1 | done: `data/Targets.kt` stores both targets as strings in the settings DataStore (`target_bodyweight`, `target_lift.<id>`); missing, broken, wrong-type, or out-of-limit values read as none | `gradlew :app:testDebugUnitTest --tests '*TargetsTest'` | `TargetsTest tests="6" failures="0" errors="0"` |
| G2 | done: `liftForecast` gives `OnDate("+1.0", day 70)`; label "At +1.0 kg/week" | `--tests '*TargetRulesTest'` | `TargetRulesTest tests="16" failures="0" errors="0"` |
| G3 | done: 2 sessions, 3 inside 13 days, and all older than 56 days give `NoLine`; rows have the Target row only | `--tests '*TargetRulesTest'` | same run, 16/16 pass |
| G4 | done: 96, 94, 92, 90 gives `NotAtThisRate("−1.0")`; 90, 90, 90, 90.1 gives `NotAtThisRate("±0.0")` | `--tests '*TargetRulesTest'` | same run, 16/16 pass |
| G5 | done: session on the entry date reaches it; a session before the entry date only does not (forecast `−0.7`); the first later session reaches it | `--tests '*TargetRulesTest'` | same run, 16/16 pass |
| G6 | done: 731 days gives `Beyond` ("More than 2 years"); 730 days gives the date | `--tests '*TargetRulesTest'` | same run, 16/16 pass |
| G7 | done: reps 8 to 11 over 42 days give `+0.5`, day 98, "At +0.5 reps/week", "15 reps" | `--tests '*TargetRulesTest'` | same run, 16/16 pass |
| G8 | done: today plus 47 days, "At −0.3 kg/week" | `--tests '*TargetRulesTest'` | same run, 16/16 pass |
| G9 | done: all four cases as specified | `--tests '*TargetRulesTest'` | same run, 16/16 pass |
| G10 | done: both cases give `NoLine`, the Target row only | `--tests '*TargetRulesTest'` | same run, 16/16 pass |
| G11 | done: `targetEntry` refuses each value; in the real sheet Save is off and nothing is stored (0.5, 500.5, 100.3, "abc" becomes empty; 0, 501, 7.5; 19.9, 300.1, 80.25) | `--tests '*TargetRulesTest' --tests '*TargetGroupTest' --tests '*TargetsTest'` | `TargetGroupTest tests="6" failures="0" errors="0"` |
| G12 | done: rows recompute from `remember(stored, history, today)`; test changes the target, adds a session, and changes `today` while the screen is open | `--tests '*TargetGroupTest'` | 6/6 pass |
| G13 | done: no group on a cardio exercise; an exercise with no session shows the Target row only (stretches have their own screen, no group) | `--tests '*TargetGroupTest'` | 6/6 pass |
| G14 | done: 28 pictures at 360 and 411 dp, font 1.0 and 1.3; I looked at them; no clipped or overlapping text. "At +12.5 kg/week" wraps once at 360 dp font 1.3, at the space | `--tests '*TargetShotsTest'` with `build/shots-targets`; full suite runs `TapTargetTest` | `TargetShotsTest tests="4" failures="0"`; `TapTargetTest tests="12" failures="0"` |
| G15 | done: "Target, 100 kilograms" + role Button, "Target, not set", "At plus 0.4 kilograms per week, March 14, 2027", "Reached, November 20", "At …, not at this rate" | `--tests '*TargetRulesTest' --tests '*TargetGroupTest'` | pass, as above |
| G16 | done: no verdict words or colors, no suggestion, no prefill other than the owner's own target; no Room, DAO, backup, permission, dependency, or version change; SPEC.md and DESIGN.md updated | `git diff 4171ab0 --stat`; `comment-guard --base 4171ab0` | 17 files changed; `exit=0` |

## Tests

Base proof: I copied the three new test files into a worktree at 4171ab0
and ran `gradlew :app:compileDebugUnitTestKotlin`. Output:
`e: .../TargetsTest.kt:28:27 Unresolved reference 'Targets'.` and more.

Tests command on 81cde5b (with `--continue`, see deviations):

```
> Task :app:testDebugUnitTest
> Task :app:assembleDebug UP-TO-DATE
> Task :app:lintVitalRelease
BUILD SUCCESSFUL in 1m 29s
71 actionable tasks: 3 executed, 68 up-to-date
tests=1026 skipped=49 failures=0 errors=0   (summed from the JUnit XML files)
```

## Pictures

`android/app/build/shots-targets/` (not in git; run TargetShotsTest with
that folder present). For each of `360-f1.0`, `411-f1.0`, `360-f1.3`,
`411-f1.3`: `-lift-date`, `-lift-reached`, `-lift-flat`, `-lift-unset`,
`-sheet`, `-body-unset`, `-body-date`.

## Deviations

- deviation: `HeartDataScreenTest` "Review's line appears, changes and goes…" is flaky ("connection pool has been closed"). It failed in my first two full runs. On base 4171ab0 alone it failed 1 of 5 runs; on this branch alone 1 of 3. The third full run passed. I did not change it.
- deviation: no merge of the trunk before the final tests. The lead told me not to merge. `int07` is now at ea109cf.
- deviation: `value30` (SetupRules.kt) and `weekEntries`, `decimalSum` (HistoryRules.kt) changed from private to internal, so the rules reuse the exact sums instead of copying them.
- deviation: Reset now also calls `vm.targets.clearAll()` (PlanScreen.kt). Reset already removes the typed maximum heart rate, the other owner-typed setting, and its sheet says "every setting goes back".
- deviation: the stored lift target keeps its unit. A target in the other unit (reps target, then the exercise gets a set with weight) reads as "Not set".
- deviation: an exercise with no session uses reps, because `withWeight` of no history is false. This is the same rule as "Reps per week".
- deviation: the 56-day window is today and the 55 days before, as the 7-day average uses today and the 6 days before.
- deviation: a bodyweight target typed at its average with no present average shows no second row.
- deviation: the stored bodyweight average is the one-decimal value that History prints.
- deviation: saving the same value again keeps the old entry date and stored average.
- deviation: "Not at this rate" rows speak the rate too ("At minus 1.0 kilograms per week, not at this rate"), because the row shows it.
- deviation: the result label puts word joiners around "/" so "kg/week" never breaks at the slash; the spoken text and the rules keep plain "/".
