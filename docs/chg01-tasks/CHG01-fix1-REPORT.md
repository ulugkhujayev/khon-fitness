# CHG01-fix1 report

Commits on `chg01`: `d8c6946` (fixes and tests), `afd85c8` (A A B A test on its own). Break proofs ran in the temp worktree `khon-fitness-wt/CHG01-fix1-break`, now removed.

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: rule unchanged; new test `two neighbor periods with one setup after a deviation joins are one period` (A A B A, A B A one period each; A A B two periods stays in the G4 test). SPEC.md: "After the single sessions join, two neighbor periods with one setup are one period, so A A B A is one period and A A B is two." | `gradlew :app:testDebugUnitTest --tests '*SetupRulesTest'`; break B1 (merge branch removed) | `SetupRulesTest tests=26 failures=0`; B1: `expected:<[[s0, s1, s2, s3]]> but was:<[[s0, s1, s2], [s3]]>` |
| X2 | done: `ChartPoint.change` flag; `changes` list, `changeAt` and `changeDates` removed; `bestPoints(history)` flags the first charted session of each period by session id, never the chart's first point. `LineChart` draws a line at each flagged point. Six F2 tests in `SetupRulesTest` and `LineChartChangesTest` | focused tests; base render at `aa7faf0` vs branch; breaks B2a (by date), B2b (first point), B2c (line one point right) | `cmp chart-plain.png chart-base.png` → IDENTICAL, both sha1 `fa2a6f7611be91a35a22989664ef4827a6e7bdca`. B2a: `two sessions on one date ... expected:<[c]> but was:<[b, c]>`; B2b: `first point ... expected:<[]> but was:<[a]>`; B2c: `the line of point 3 stands at its marker expected:<398.35074> but was:<507.5>` |
| X3 | deviation: lead wording followed, so witness F3 does not show `fixed`. The row is built from parts (`periodDates(..., " to ")`, `spokenSetup`, `spokenRate`), the saved name is not changed, the range reads "8 to 12 repetitions", the rate "repetitions per week". The witness test asserts "8 to 12 reps"; the task says "5 to 8 repetitions". The witness needs that word changed to close | `witness run F3.json --sha d8c6946`; witness patch run in the temp worktree; breaks B3a, B3b | `WITNESS F3 reproduces at=d8c6946d4ca4 receipt=r-20261126204443-741dce`; only difference: `expected:<... 2 sets, 8 to 12 rep[]s, no rate> but was:<... 2 sets, 8 to 12 rep[etition]s, no rate>`; `saysTheFullUnitForAnUnweightedRate` passed. B3a: `expected:<Since Oct 5, Push [– Pull ·] PM ...> but was:<... Push [to Pull,] PM ...>`; B3b: `expected:<minus 0.3 rep[etition]s per week> but was:<minus 0.3 rep[]s per week>` |
| X4 | done: four tests added: 10, 40, 12 on days 0, 1, 14 → "−5.9"; skipped sessions on days 0 and 20 around valued days 3–16 → no rate, 3–17 → "+2.1"; same-day sessions given in reverse → stored order; the long name image equals a reference Text with an ellipsis at the row's width (and that reference is ellipsized), the rate image equals an unclipped reference | breaks B4a–B4d | B4a: `expected:<[−5.9]> but was:<[+1.0]>`; B4b: `expected null, but was:<±0.0>`; B4c: `expected:<[a, c, d]> but was:<[a, d, c]>`; B4d (Ellipsis → Clip): `the long name is drawn with an ellipsis` at 360/411 dp, font 1.0/1.3 |
| X5 | done: both branches convert reps to Double before the arithmetic. Test with `Int.MAX_VALUE` with and without weight | `witness run F5.json --sha d8c6946`; breaks B5, B5b | `WITNESS F5 fixed at=d8c6946d4ca4 receipt=r-20261126204424-c6bd02` (log: `Task :app:testDebugUnitTest` ran with the witness filter, BUILD SUCCESSFUL). B5: `expected:<2.147483677E9> but was:<-2.147483619E9>`; B5b: `expected:<2.147483647E9> but was:<-1.0>` |
| X6 | done: nine `// ---- G` comments removed; 63 other comment lines removed or rewritten (count of removed comment lines in `git diff 698198a afd85c8 -U0 -- android` → 72, 9 of them G). Kept 5 added lines, listed below | `witness run F6.json --sha d8c6946`; `comment-guard --base aa7faf0` | `WITNESS F6 fixed at=d8c6946d4ca4 receipt=r-20261126204424-302c25`; comment-guard exit 0 |
| X7 | done: screenshots made again with `SetupShotsTest` and looked at: markers at the first point of each period, long name with ellipsis, rates whole. No Room, DAO, backup, dependency or version change | `git diff aa7faf0 --stat`; screenshots | `android/app/build/shots-setup/1-bench-360.png, 2-bench-411.png, 3-bench-360-font-1.3.png, 4-pullup-reps.png, 5-row-long-program-font-1.3.png, chart-plain.png`. Diff touches only DESIGN.md, SPEC.md, LineChart.kt, ExerciseDetail.kt, HistoryRules.kt, SetupRules.kt and tests |

Tests (`Tests:` command on `afd85c8`): `> Task :app:testDebugUnitTest` / `> Task :app:assembleDebug` / `> Task :app:lintVitalRelease` / `BUILD SUCCESSFUL in 1m 40s`; results: suites=78 tests=736 skipped=25 failures=0 errors=0. The last commit adds only this report.

## Coverage

| ID | Instance | Status | Test | Found by |
|---|---|---|---|---|
| X1.1 | F1: A A B A and A B A in `setupPeriods` | kept: rule unchanged, SPEC sentence added | `two neighbor periods with one setup after a deviation joins are one period` | deep review |
| X2.1 | F2: skipped boundary session | fixed: marker at the period's next charted session | `a change whose first session has no best set marks the next charted session of its period` | deep review |
| X2.2 | F2: middle period with no charted point | fixed: one marker, at the third period | `a middle period without a charted session has no marker, ...` | deep review |
| X2.3 | F2: last period with no charted point | fixed: no marker | `a last period without a charted session has no marker` | deep review |
| X2.4 | F2: two sessions on one date in two periods | fixed: marker chosen by session id | `two sessions on one date in different periods put the marker on the second` | deep review |
| X2.5 | F2: one point; first period without points | fixed: never on the chart's first point | `the first point of the chart never carries a marker, ...` | deep review + fixer |
| X2.6 | F2: pixel test step derived from the markers | fixed: column from the point's own green marker | `LineChartChangesTest` | deep review |
| X3.1 | F3: `periodRows` spoken row by `.replace` | fixed: built from parts | `a row speaks the saved program name as saved and the rep range in words`; witness F3 (reproduces on "reps" only) | deep review |
| X3.2 | F3: `spokenRate` "reps per week" | fixed: "repetitions per week" | `the spoken rate has its sign and unit in words` | deep review |
| X3.3 | F3: other `.replace` on visible text in the branch diff | kept: none found (no added line with `.replace(` in `git diff aa7faf0 afd85c8 -- android`) | `a row speaks the saved program name as saved and the rep range in words` | fixer search |
| X4.1 | F4: least squares vs end points | fixed: days 0, 1, 14 test | `the rate is the least squares slope per week` | deep review |
| X4.2 | F4: valued span vs session span | fixed: skipped ends test | `the 14 days run from the first valued session to the last, not over skipped ones` | deep review |
| X4.3 | F4: same-day input order | fixed: reversed input test | `two sessions on one date keep their stored order when given in reverse` | deep review |
| X4.4 | F4: long name and rate by real layout | fixed: images equal ellipsized and unclipped references | `SetupGroupTest` `the rows fit at ...` (4 tests) | deep review |
| X5.1 | F5: `value30` weighted `30 + reps` | fixed: Double before the sum | `a set with the largest rep count has a positive value with and without weight`; witness F5 fixed | deep review |
| X5.2 | F5: `value30` unweighted `reps * 30` | fixed: Double before the product | same test; witness F5 fixed | deep review |
| X5.3 | F5: `trendText` `now.size * before.size` (entry counts, not saved values) | fixed: BigDecimal product, same result | existing bodyweight trend tests in the full suite | fixer search |
| X6.1 | F6: nine `// ---- G` comments in `SetupRulesTest.kt` | fixed: removed | witness F6 fixed | deep review |
| X6.2 | F6: 63 other added comment lines (SetupRules, ExerciseDetail, HistoryRules, LineChart, tests, seed) | fixed: removed or cut to the 5 kept lines | `comment-guard --base aa7faf0` exit 0 | fixer search |
| X7.1 | Screenshots and no Room, DAO, backup, dependency or version change | kept: screenshots made again and looked at | `SetupShotsTest`, full `Tests:` command | fixer |

Witnesses: F3 reproduces (wording, see X3), F5 fixed, F6 fixed. Run again at `0c079e5` (report commit):
`WITNESS F3 reproduces at=0c079e5548b0 receipt=r-20261126205039-8de0ed`,
`WITNESS F5 fixed at=0c079e5548b0 receipt=r-20261126205050-380b13`,
`WITNESS F6 fixed at=0c079e5548b0 receipt=r-20261126205106-fe8ffc`.

Kept comment lines:
- `LineChart.kt` KDoc, one sentence on `ChartPoint.change`: the shared component's contract for its callers.
- `HistoryRules.kt` `/** [denominator] must be above 0: the sign comes from [numerator] alone. */`: a precondition the type cannot say.
- `SetupRules.kt` `/** Times 30, so weight × (30 + reps) stays exact ... */`: why the scale exists.
- `SetupShotsTest.kt` `/** Runs only when app/build/shots-setup exists; the PNGs land there. */`: how to run it.

ASK code: the F3 witness asserts "reps", the task asks for "repetitions". I followed the task; Sol must change one word in `F3.patch` line 37 for the witness to close.
deviation: X3, witness F3 shows reproduces on the rep word only.
