# CHG01-setup-changes report

Branch `chg01`, commits on base aa7faf0: b25ee4d (rules), 1fb0e94 (screen, chart, docs, tests),
7daa17d (history test sort key). Not pushed, not merged.

Tests command on 7daa17d (third run; the second stopped with "Gradle build daemon disappeared unexpectedly", no test failure):
`BUILD SUCCESSFUL in 2m 23s`, JUnit XML totals `tests=726 skipped=25 failures=0 errors=0`.

Focused runs use `android/gradlew -p android :app:testDebugUnitTest --tests '<class>'` (JAVA_HOME as in the task).

| # | Status | Check ran | Real output |
|---|---|---|---|
| G1 | done: `setupPeriods` and `changeFacts` in SetupRules.kt; test "three sessions of 3 sets then three of 4 sets…" | SetupRulesTest | `SetupRulesTest tests=15 skipped=0 failures=0 errors=0` |
| G2 | done: `Setup` holds the program id, not the name; `SetupPeriod.programName` gives "No program" for a null id; test "a new program id is a change with both names…" | SetupRulesTest | `tests=15 failures=0` |
| G3 | done: facts in the order program, sets, reps; test "a new rep range is a change, and three changes…" asserts `[TP v3 → TP v4, 3 → 4 sets, 8–12 → 5–8 reps]` | SetupRulesTest | `tests=15 failures=0` |
| G4 | done: AABAA one period, AABB two, AAB two (latest one session), BAA one; test "a single session that is not the latest is no change" | SetupRulesTest | `tests=15 failures=0` |
| G5 | done: `liftHistory` drops the unfinished session; the all-skipped one has a setup and no value; two on one date stay in stored order | SetupRulesTest | `tests=15 failures=0` |
| G6 | done: Epley kept exact as weight × (30 + reps); 100×5 and 90×10 give 120.0; 0 kg with weight gives null; 12 and 15 reps give 15 | SetupRulesTest | `tests=15 failures=0` |
| G7 | done: least-squares slope per 7 days, computed in BigDecimal; +1.0, −1.0, ±0.0, +1.0 | SetupRulesTest | `tests=15 failures=0` |
| G8 | done: fewer than 3 valued sessions or under 14 days gives null ("–"); 2 over 30 days, 3 over 13, 2 valued of 5 are null, 3 over 14 is a rate | SetupRulesTest | `tests=15 failures=0` |
| G9 | done: extracted `signedTenths` from `trendText` in HistoryRules.kt; the trend and the rate both use it; +0.25 gives "+0.3", −0.25 "−0.3", 0.04 "±0.0", also from real slopes | SetupRulesTest, RepRecordsTest | `SetupRulesTest tests=15 failures=0`, `RepRecordsTest tests=14 failures=0` |
| G10 | done: `PeriodGroup` sits between the chart card and Most reps; curl (one period) shows no group; bench gives three rows newest first, the first "Since …"; pull-ups give "Reps per week"; rows have no OnClick | SetupGroupTest | `SetupGroupTest tests=7 skipped=0 failures=0 errors=0` |
| G11 | done: year added only for dates outside this year ("Dec 5, 2025 – Oct 4"); "5 reps" when min = max; the 40-char name is narrower than its full width, the rate keeps its full measured width | SetupRulesTest, SetupGroupTest | `tests=15 failures=0`, `tests=7 failures=0` |
| G12 | done: `LineChart(changes = …)` draws a 1 dp K.Muted line at `changeAt`. Without the list: PNG identical to base aa7faf0 (`cmp` IDENTICAL, both sha1 80ff48b6e3b125a95bd009cfd7191ae911769757, base rendered in a temp worktree, now removed). Two changes: exactly 2 lines at points 3 and 7. Break proof: with the drawLine removed the test fails | LineChartChangesTest; break run with the line deleted | `LineChartChangesTest tests=2 failures=0`; break: `one line per change: [] expected:<2> but was:<0>` |
| G13 | done: rows at 360 and 411 dp, font 1.0 and 1.3: dates over setup, name before tail, setup before rate, rate inside the row and whole; TapTargetTest and ContrastTest pass | SetupGroupTest (4 layout cases), full suite | `SetupGroupTest tests=7 failures=0`, `TapTargetTest tests=10 failures=0`, `ContrastTest tests=11 failures=0` |
| G14 | done: no verdict words or colors in the diff; no Room, DAO, repository, backup, Gradle or manifest file changed; SPEC.md and DESIGN.md updated (unslop loaded); comment guard clean | `git diff aa7faf0 --name-only \| grep -iE "Dao\|Entities\|Repository\|Backup\|build.gradle\|Manifest"`; `comment-guard --base aa7faf0` | grep: no output; comment-guard: `exit 0` |

New tests on base: copied to an aa7faf0 worktree, `compileDebugUnitTestKotlin` gave 114 `e:` lines
(`No parameter with name 'changes' found`, `Unresolved reference 'changeAt'`, `Unresolved reference 'PERIOD_RATE'`).

Screenshots (looked at each): android/app/build/shots-setup/1-bench-360.png, 2-bench-411.png,
3-bench-360-font-1.3.png, 4-pullup-reps.png, 5-row-long-program-font-1.3.png (from SetupShotsTest).

Notes for the lead:
- HistoryScreenTest "most reps by weight…" failed after the change. Its data ends with a one-set session, so it now has two periods and the page grows. Rows below the fold have clipped `boundsInRoot`. Commit 7daa17d sorts by `positionInRoot.y`. The assertions did not change.
- A period's end date never goes before its own last session. This covers a period change between two sessions on one date. A period that starts and ends on one date shows that date alone.
- A one-session run of the same setup as the period before joins that period, also when it is the latest run.
- A small gap of about one character can stay between an ellipsized name and " · 3 sets" (Compose ellipsis in a weighted box).
- Pre-existing, not changed: the Heaviest set chart shows 0 kg for an exercise without weight, and its value labels can overlap.
- No trunk merge: the lead's brief says "do not merge", and this branch is based on the rep records branch.
