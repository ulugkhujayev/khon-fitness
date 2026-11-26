# HR01 Part C deep review, round R11

**Verdict: merge after fixes.** Reviewed snapshot `662846bca375` against Part C base `407a906649b0`. The findings below have source evidence and test patches. The lead must run the witnesses before recording the findings.

## Coverage

| Entries | Status and evidence |
|---|---|
| Part C C26–C29, AT4, AT7, AT23, AT29; M4, R8 C43 | **Read, traced.** `HeartFill.kt:79–149` to `Dao.kt:410–454`. The transaction checks the generation, range, and start row. The time zone used in the check can be stale. See C-F1. |
| C21–C25, C31, AT5; E1–E3, E6, E23–E24, M5 | **Read, traced.** `HealthHeartSource.kt:17–36` to `HeartRead.kt:21–108`. The adapter checks the heart rate grant and reads pages. The pure reader checks times and bpm, drops duplicates, picks the origin, and computes counts, gaps, and coverage. The provider drops the left-crossing record first. See C-F3. |
| C23, C29–C32, C35, AT30; affected A/B/C writers and I1–I2 | **Read, traced.** `Repository.kt:320–385`, `KhonApp.kt:29–31`, `MainActivity.kt:26–29`, and `Dao.kt:410–454, 724–728`. Finish and Save queue the runner. Foreground uses elapsed time. The new fill writes only `session_hr`. The Part C diff adds no manifest or dependency change. |
| C33–C34b, AT6, AT17–AT18, AT24–AT25, AT31; F1–F20, L2, M8, R8 C38/C44/C45 | **Read, traced.** `Repository.kt:34–44, 426–450`, `BackupMerge.kt:11–53`, and `Dao.kt:688–728`. Replace clears the old tables. Merge keeps the local columns and HR row, clears moved bounds and round times, and clears stamps on merged pending or skipped sets. Old files use defaults. Offset validation can wrap. See C-F2. |
| C17a, AT27; backup side of R8 C39 | **Read, traced.** `Repository.kt:441–465` passes the generation change to `Dao.kt:688–689`. The Part C tests exercise a held commit against the import. The timer implementation is outside this round. |
| G/J tests and the Part C report | **Read, traced.** `HeartReadTest.kt`, `HeartFillTest.kt`, `BackupHrTest.kt`, and the Part C cases in `TimerCommitTest.kt`. `HeartFillTest.kt:168–179` changes the zone before the provider returns, so it misses the transaction wait. Its Remove fixtures change both state and stamp. A held reread of an off row followed by a second Remove would isolate the stamp check. `HeartFillTest.kt:338–353` replaces the provider with a throwing fake for the missing-grant and recovery paths. A real grant test still needs proof. |
| Other inventory entries | Outside Part C, including existing band import UI, timer internals, and future measured HR screens. The Part C stale display check uses `HeartRate.kt:56–65`. |

## Findings

- **P2 C-F1. Check then act outside one transaction.** **Read.** `HeartFill.kt:106–113` gives cleanup the zone from pass start, so cleanup can delete a row that became current near midnight. `HeartFill.kt:139` and `HeartFill.kt:98` capture the zone before Room starts the commit or Remove. `Dao.kt:425–430, 436–440, 449–453` checks each range with that prior zone. A queued commit can store an invalidated result. A queued Remove can create an off row after the range disappears. Resolve the zone inside each write transaction. Witness `C-F1.json`.
- **P2 C-F3. Provider filter excludes overlapping records.** **Read.** `HealthHeartSource.kt:22–25` requests the exact sample window. Android's raw record query compares both filter bounds with the record start time. In AT5 it drops the first record and returns 13 raw and 12 accepted samples, so the read cannot fill. `HeartReadTest.kt:59–68` constructs both records after selection and passes even if the provider drops one. Request early starts within the provider's existing access limits, then select overlapping records by their original bounds before counting samples. Witness `C-F3.json` invokes the adapter with a shadow that models the platform filter. No device run. [Android raw read query](https://android.googlesource.com/platform/packages/modules/HealthFitness/%2B/539a50dd0/service/java/com/android/server/healthconnect/storage/datatypehelpers/RecordHelper.java#838)
- **P3 C-F2. Backup bounds check overflows.** **Read.** `HeartRead.kt:123`, through `BackupMerge.kt:52–53` and `Dao.kt:703`. A 40-minute range ending at `Long.MAX_VALUE` accepts offset 2405. Adding it wraps the sample time below the start, and the check accepts it. Both restore modes can store the bad row. Reject overflow and check the full half-open interval. Witness `C-F2.json`.

## Checks

- **Ran.** `git apply --check` on the witness patches from snapshot `src/`. All patches applied in check mode.
- **Ran.** `witness run <file> --sha 662846bca3750a1d5d153a59d997d8d99bfc5ab4` for C-F1, C-F2, and C-F3. Each printed `undecided` and `receipt=none`. The sandbox returned `Operation not permitted` under `~/.config/agents/state/witness/HR01-P1-data/`. No Gradle test result from this review.
- Use the step 5 fallback. Run the saved JSON files from `khon-fitness/tasks/HR01-P1-data-W/` at the snapshot head and verify failures match the assertions before recording the findings. The report, three JSON files, and three test patches are the retained files.

C-F1=found
C-F2=found
C-F3=found
