# TGT02-stale-date: report

| # | Status | Check ran | Real output |
|---|---|---|---|
| S1 | done: `Forecast.Stale` after "Reached" and before the line | `gradlew :app:testDebugUnitTest --tests "*TargetRulesTest"` | `BUILD SUCCESSFUL`; test "a newest session more than 28 days old gives no rate and no date" |
| S2 | done: the old G3 assertion (`NoLine`, no second row at day 84) now expects `Stale(day 28)` and the row "No session since Oct 29" | same | passes |
| S3 | done: `Forecast.Passed` when the days to the line are zero or less | same | test "a line that is at the target today or was there before gives no date"; the old test F6 now expects `Passed` in place of today's date |
| S4 | done: no change to the "Reached" branch | same | tests G4 and F2 pass unchanged |
| S5 | done: no change | `--tests "*TargetRulesTest" --tests "*TargetGroupTest" --tests "*TargetsTest"` | 62 tests started, `BUILD SUCCESSFUL in 12s` |
| S6 | done: manifest flag, rules file, `BackupOffTest` | `--tests "*BackupOffTest"` | 2 tests pass |
| S7 | done: one paragraph in `SPEC.md`, the row list in `DESIGN.md` | read | both name "No session since" and "No date" |

## Break proofs

- With the stale check and the passed check removed from `liftForecast`, `TargetRulesTest` fails 4 tests of 29: G3, "a line that is at the target today or was there before gives no date", "a newest session more than 28 days old gives no rate and no date", "a line at or above the target today with no session at it gives no date".
- With `android:allowBackup="true"`, `BackupOffTest` fails "Android makes no backup copy of the app data".

## Changed assertions of old tests

- G3: a target whose sessions are all older than 28 days had no second row; it now has the row "No session since".
- F6 (renamed): a line at or above the target today gave today's date; it now gives no date.

## Not verified

- The device transfer rule on a real phone. The test reads the rules file; it does not run a transfer.
- The full suite. The lead runs it on the integration branch.

## Fixes after the deep review R1

Both findings are test gaps. No production code changed.

- **F1.** `BackupOffTest` now reads the `dataExtractionRules` reference of the `application` tag from the merged manifest, opens that resource, and checks that its root tag is `data-extraction-rules` before it reads the exclusions.
  - Break 1 (ran): the manifest without `android:dataExtractionRules`. `BackupOffTest > the cloud backup and the device transfer leave out every domain FAILED`, `java.lang.AssertionError at BackupOffTest.kt:44`.
  - Break 2 (ran): the root tag renamed to `wrong-root`. The same test `FAILED`, `org.junit.ComparisonFailure at BackupOffTest.kt:26`.
- **F2.** New test `a reached target stays reached when the newest session is older than 28 days` in `TargetRulesTest`: the history of G5 with today on day 71 gives `Reached(day 14)` and the row "Reached · Oct 15".
  - Break (ran): the 28-day check moved before the Reached check. The new test `FAILED`, `java.lang.AssertionError at TargetRulesTest.kt:87`.
- After the reverts (ran): `:app:testDebugUnitTest --tests "*BackupOffTest" --tests "*TargetRulesTest"`, 32 tests started, `BUILD SUCCESSFUL`.

The two witness runs of the lead at `4f5a2f6` did not run their tests: each stopped at its own guard "less than 30 GiB free" (30.4 GB were free, which is 28.3 GiB). Their receipts say `reproduces` for that reason only. The lead confirmed both findings by reading and with the breaks above.

Sol's protocol notes: the task keeps class Normal (two display rules and one manifest flag, no stored data). The rows S4 and S5 are kept behavior; their tests pass on both heads, and the task's break rule applies to S1 to S3 and S6 only.
