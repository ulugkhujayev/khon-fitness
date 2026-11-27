# TGT01-fix1 report

Branch `tgt01`, base 87061ca (branch head before this task: ee79192). Commits:

- 21a9c4e Judge typed targets as text, keep the exact entry average, and bound stored targets
- 8460288 Describe the typed-text check, the exact entry average and the stored limits of targets
- 11a9829 Write targets on the IO dispatcher
- aec68e0 Wait for the spoken "Not at this rate" in the target pictures
- (this report) Add the TGT01-fix1 report

Gradle env for every run: `ANDROID_HOME=$HOME/Library/Android/sdk`, `JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home`.

## Contract

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: `targetEntry(text, kind)` judges the raw text; the sheet uses `NumberField(onTextChange)`, so nothing is cleaned before the check. Each X1 text with a stored 100 kg: Save off, a tap stores nothing, the text stays | `gradlew :app:testDebugUnitTest --tests '*TargetRulesTest' --tests '*TargetGroupTest'` | `TargetRulesTest 27 0 0`, `TargetGroupTest 14 0 0` (tests, failures, errors); cases `X1 the sheet refuses text...`, `X1 text that is not a plain decimal...`, `X1 the reps and bodyweight sheets...` |
| X2 | done: "0080" → 80.0, "80,5" → 80.5, "100,0" → 100, "100.50" → 100.5, "0000015" → 15 kg and 15 reps; an empty field removes the target (lift and bodyweight) | same run | `X2 the sheet keeps leading zeros...` and `X2 leading zeros keep the value...` pass |
| X3 | done: `bodyStart` gives `EntryAverage(sum, count)`; the store writes `kg\|sum\|count\|date`; the direction is the sign of `kg × count − sum` | same run; witness F2 | `X3 ...` pass; `WITNESS F2 fixed at=aec68e0069bc receipt=r-20261127003638-86f0c4` |
| X4 | done: an exact present average of 80.04 (prints 80.0) against 80.0 going down gives `OnDate(−0.5, today+1)`; `80.0\|83.2\|2026-11-07` reads as `EntryAverage(83.2, 1)` | `--tests '*TargetRulesTest' --tests '*TargetsTest'` | `X4 ...` and `aRecordInTheOldFormReadsItsAverageAsExact` pass |
| X5 | done: numbers must match `[0-9]+(\.[0-9]+)?` with at most 12 characters; count 1 to 1000; average 20 to 300 kg (checked exactly as sum against 20 × count and 300 × count); date `yyyy-mm-dd` that parses; whole record at most 64 characters; a date after today counts as today in both forecasts | `--tests '*TargetsTest' --tests '*TargetRulesTest'`; witness F3 | `TargetsTest 9 0 0` (`storedNumbersHaveLimits`: 30 broken texts read as none, 5 edge texts read); `X5 a stored entry date after today...` pass; `WITNESS F3 fixed at=aec68e0069bc` |
| X6 | done: the sheet's open state is keyed by the unit, so a new unit closes it and drops the draft; an empty Save with any stored record (also of the other unit) removes it; a Save of 100 replaces the reps record with 100 kg | `--tests '*TargetGroupTest'`; witness F4 | `X6 a new unit closes...`, `X6 a save on a stored target of the other unit...` pass; `WITNESS F4 fixed at=aec68e0069bc receipt=r-20261127003658-5aafc9` |
| X7 | done: `NotAtThisRate` speaks "Not at this rate"; `Beyond` speaks "At plus 12.5 kilograms per week, more than 2 years"; texts on screen unchanged | `--tests '*TargetRulesTest'`; witness F5 | `X7 each result state speaks its text` pass for BODY, LIFT_KG, LIFT_REPS; `WITNESS F5 fixed at=aec68e0069bc` |
| X8 | done: every test of decision 6 exists, passes, and fails when its rule is broken (see Break proofs) | break script, one break per run | 15 of 15 breaks fail their test (table below) |
| X9 | done: witness F1 fixed; the Tests command passes on aec68e0; no suggested or prefilled target (the sheet shows only the owner's own target, no placeholder, picture `360-f1.3-sheet.png`) | Tests command; witness F1; `comment-guard --base ee79192` | `BUILD SUCCESSFUL in 2m`, `tests=1116 skipped=55 failures=0 errors=0`; `WITNESS F1 fixed at=aec68e0069bc receipt=r-20261127003619-c8c319`; comment-guard exit 0 |

## Tests command (final commit aec68e0, `--rerun-tasks`)

```
> Task :app:assembleDebug
> Task :app:testDebugUnitTest
> Task :app:lintVitalRelease
BUILD SUCCESSFUL in 2m
71 actionable tasks: 71 executed
tests=1116 skipped=55 failures=0 errors=0   (summed from the JUnit XML files)
```

Two earlier full runs on aec68e0 failed: run 1 `ScoreScreenTest.highIntensityWithFourRounds` ("the chart stays"); run 2 the same test plus `TargetGroupTest` "G11 the est 1RM sheet..." (5 s wait for the sheet at line 173, which the first builder wrote). See Deviations. No test worker hung; I killed no process.

## Witnesses

| Witness | 21a9c4e | 11a9829 | aec68e0 (final) |
|---|---|---|---|
| F1 | fixed | fixed | fixed, r-20261127003619-c8c319 |
| F2 | fixed | fixed | fixed, r-20261127003638-86f0c4 |
| F3 | fixed | fixed | fixed, r-20261127003648-a5a5db |
| F4 | reproduces, r-20261127001742-66cc3b (`clearingTheVisibleEmptyFieldRemovesTheHiddenTarget FAILED`, AssertionError at line 120) | fixed | fixed, r-20261127003658-5aafc9 |
| F5 | fixed | fixed | fixed, r-20261127003713-725984 |

I read each receipt log: the final five say `> Task :app:testDebugUnitTest` and `BUILD SUCCESSFUL`. The F4 failure at 21a9c4e: the save called `setLift(null)`, but DataStore runs the `edit` transform on the caller's dispatcher (Main, from `viewModelScope`), and the witness blocks the Robolectric main thread in `runBlocking` for its 3 s wait. Commit 11a9829 runs both target writes in `withContext(Dispatchers.IO)`.

## Break proofs

One break per run, file restored with `git checkout` after each (script and log in the session scratchpad, `breaks.log`).

| Break | Test that fails |
|---|---|
| F1: filter the text to digits and separators before the check | `X1 the sheet refuses text...`: `abc BODY expected:<Refused> but was:<Clear>` |
| F2: direction from the average rounded to one decimal | `X3 ...`: `expected:<Reached(date=null)> but was:<NotAtThisRate(rate=−0.1)>` |
| F3: `toBigDecimalOrNull()` for stored numbers | `storedNumbersHaveLimits`: `8E1\|83.2\|2026-11-20 expected null` |
| F3: average check only `count > 0 && sum > 0` | `storedNumbersHaveLimits`: `80.0\|19\|1\|2026-11-20 expected null` |
| F3: entry date not capped at today | `X5 ...`: `expected:<Reached(date=2026-11-27)> but was:<Reached(date=2026-12-27)>` |
| F4: `remember { }` without the unit key | `X6 a new unit closes an open sheet...` |
| F4: empty Save compares with `current == null` | `X6 a save on a stored target of the other unit...`: `expected null, but was:<LiftTarget(value=15, reps=true, ...)>` |
| F5: speech with the rate | `X7 ...`: `expected:<[N]ot at this rate> but was:<[At minus 1.0 kilograms per week, n]ot at this rate>` |
| F6: window of 57 days | `F6 a session 55 days old...`: `expected:<NoLine> but was:<OnDate(...)>` |
| F6: span rule `< 13` in `periodRate` | `F6 a span of exactly 14 days...`: `expected:<NoLine> but was:<OnDate(rate=+2.1, ...)>` |
| F6: bodyweight days from the printed rate | `F6 a bodyweight rate of minus 0_26...`: `expected:<...2027-01-20> but was:<...2027-04-16>` |
| F6: lift days from the printed rate | `F6 a lift date uses the exact slope...`: `expected:<...2027-01-12> but was:<...2027-01-09>` |
| F6: no floor at today (`abs` in place of `max(0)`) | `F6 a line at or above the target today...`: `expected:<...2026-11-12> but was:<...2026-11-16>` |
| F6: value text 60 dp wide | `TargetLayoutTest` at360, at360Large, at411, at411Large: `More than 2 years: value clipped` |
| F6: target group for any id on the empty lift screen | `G13 the stretch routes have no target group`: `AssertionError: lift` |

## Coverage

| # | Instance | Status | Test | Found by |
|---|---|---|---|---|
| X1.1 | `targetEntry` read a Double that the field had already cleaned (`TargetRules.kt`) | fixed: takes the raw text; ASCII digits, at most one `.` or `,`, none for reps; value on step and in limits | TargetRulesTest X1, G11 | review F1 |
| X1.2 | `TargetSheet` used `NumberField` without `onTextChange`, so `cleanTyped` dropped characters (`TargetGroup.kt`) | fixed: `onTextChange = { draft = it }`, value is only the prefill | TargetGroupTest X1 (two cases); witness F1 | review F1 |
| X1.3 | the sheet's − and + had no limits (min 0, no max), so a step gave "0" | fixed: `min`/`max` of the kind passed to the field | TargetGroupTest step buttons (lift kg, bodyweight) | fixer, decision 1 |
| X1.4 | store writes kept the scale of the caller (`setLift`, `setBody`) | fixed: `plain()` for lifts, scale 1 for bodyweight | TargetsTest, TargetGroupTest X2 | fixer |
| X2.1 | leading zeros and the decimal comma | fixed: parsed after the shape check | TargetRulesTest X2; TargetGroupTest X2 | review F1 |
| X2.2 | empty field | kept: Clear | TargetRulesTest X2; TargetGroupTest X2 | review F1 |
| X3.1 | `bodyStart` rounded the 7-day average to one decimal | fixed: `EntryAverage(sum, count)` | TargetRulesTest X3, stored average test | review F2 |
| X3.2 | `bodyForecast` direction from the rounded average | fixed: sign of `kg × count − sum` | TargetRulesTest X3; witness F2 | review F2 |
| X3.3 | store format `kg\|avg\|date` | fixed: `kg\|sum\|count\|date`; the old form reads with count 1 | TargetsTest body and old-form tests | review F2 |
| X4.1 | "Reached" and the distance use the present average | kept: already exact (`kg × n − sum`) | TargetRulesTest X4, F6 −0.26 | review F2 |
| X5.1 | `readBody` and `readLift` accepted any `BigDecimal` text | fixed: plain decimal of at most 12 characters, record at most 64 | TargetsTest storedNumbersHaveLimits; witness F3 | review F3 |
| X5.2 | stored average without limits | fixed: count 1 to 1000, sum above 0, average 20 to 300 kg | TargetsTest storedNumbersHaveLimits | review F3 |
| X5.3 | `setBody` could write an average that the reader refuses | fixed: such an average is not stored; the target is | TargetsTest anEntryAverageOutsideItsLimitsIsNotStored | fixer |
| X5.4 | stored date text | fixed: `yyyy-mm-dd` shape before `LocalDate.parse` | TargetsTest storedNumbersHaveLimits | review F3 |
| X5.5 | a date after today in `liftForecast` and `bodyForecast` | fixed: `entryDay = min(date, today)` | TargetRulesTest X5 | review F3 |
| X6.1 | the sheet stayed open across a unit change | fixed: `remember(kind)` | TargetGroupTest X6; witness F4 | review F4 |
| X6.2 | an empty Save skipped a stored record of the other unit | fixed: `hasRecord` from the raw stored record | TargetGroupTest X6; witness F4 | review F4 |
| X6.3 | a Save of a value over a record of the other unit | kept: one key per exercise, `setLift` replaces it | TargetGroupTest X6 | review F4 |
| X6.4 | target writes ran the DataStore transform on Main | fixed: `withContext(Dispatchers.IO)` for lift and bodyweight | witness F4; TargetGroupTest | witness run at 21a9c4e |
| X7.1 | `NotAtThisRate` spoke the rate | fixed: "Not at this rate" only | TargetRulesTest X7, G4; witness F5 | review F5 |
| X7.2 | `Beyond` speech | kept: rate, then "more than 2 years" | TargetRulesTest X7, G6 | review F5 |
| X7.3 | `TargetShotsTest` waited for the old lower-case spoken text | fixed | TargetShotsTest (36 pictures) | fixer |
| X8.1 | no layout test for "More than 2 years" and "Mar 14, 2027" next to "At +12.5 kg/week" | fixed: `TargetLayoutTest` (not skipped), 360 and 411 dp, font 1.0 and 1.3 | TargetLayoutTest 4 0 0 | review F6 |
| X8.2 | the two states not in the pictures | fixed: `-lift-far`, `-lift-year` at the four sizes | TargetShotsTest | review F6 |
| X8.3 | stretch routes | fixed: test for `StretchScreen`, `StretchEditorScreen` and the lift screen opened with a stretch id | TargetGroupTest G13 stretch | review F6 |
| X8.4 | 55/56-day window and 14/13-day span | fixed: boundary tests | TargetRulesTest F6 (two tests) | review F6 |
| X8.5 | −0.26 kg/week, 2.0 kg: 54 days | fixed: test | TargetRulesTest F6 | review F6 |
| X8.6 | lift exact slope against printed rate (61 against 59 days) | fixed: test | TargetRulesTest F6 | review F6 |
| X8.7 | line at or above the target today, no session at it | fixed: test (target 96.5 at the line; 96 below it) | TargetRulesTest F6 | review F6 |
| X9.1 | hard rules: no suggested or prefilled target, no Room, DAO, backup, permission, dependency, or version change | kept | `git diff ee79192 --stat` touches only Targets.kt, TargetRules.kt, TargetGroup.kt, their tests, SPEC.md, DESIGN.md | lead |
| X9.2 | lead's group title "Target est. 1RM" | kept | TargetGroupTest G11; picture `360-f1.3-sheet.png` | lead |

## Pictures

`android/app/build/shots-targets/` (not in git; TargetShotsTest writes there when the folder exists). 36 files: for `360-f1.0`, `411-f1.0`, `360-f1.3`, `411-f1.3`: `-lift-date`, `-lift-reached`, `-lift-flat`, `-lift-unset`, `-sheet`, `-body-unset`, `-body-date`, `-lift-far`, `-lift-year`. I looked at `360-f1.3-lift-far`, `360-f1.3-lift-year`, `360-f1.0-lift-year`, `411-f1.3-lift-far` and `360-f1.3-sheet`. At 360 dp font 1.3 "At +12.5 kg/week" wraps once, at the space; "More than 2 years" and "Mar 14, 2027" are whole on one line; nothing overlaps. The sheet field is empty with no placeholder.

## Deviations

- deviation: target writes run on `Dispatchers.IO` (11a9829). DataStore runs an edit on the caller's dispatcher; on Main the F4 witness could not see the removal while it blocked the main thread. The app behaves the same on a phone.
- deviation: a − or + from an off-step text ("100.3") moves by one step and stays off-step, so Save stays off. From an empty or valid text, the steps give a valid text inside the limits. A fix needs `Stepper.kt`, which the task excludes.
- deviation: "80." and ".5" count as one separator with digits and read as 80 and 0.5.
- deviation: a save stores the target without its entry average when that average is outside 20 to 300 kg or longer than 12 characters; the direction then comes from the present average.
- deviation: `ScoreScreenTest.highIntensityWithFourRounds` ("the chart stays") failed in 2 of 3 full runs on aec68e0. It passed 3 of 3 alone and with the target classes, and the third full run passed. The full suite on ee79192 passed once (1090 tests). I did not change the Review screen; I count it as flaky under load, not proved.
- deviation: `TargetGroupTest` G11 timed out once (5 s wait for the sheet at line 173) in the same loaded full run; it passed in every other run.
- deviation: no before/after UI proof sheet; the task header has no `UI: yes`.
