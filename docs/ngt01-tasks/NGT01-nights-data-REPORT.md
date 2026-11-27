# NGT01-nights-data report

Commit: f00d51d on branch `ngt01` (base 5567185; trunk `release-0.7.0` tip is still 5567185, so no merge was needed).
Focused command: `timeout 420 android/gradlew -p android :app:testDebugUnitTest --tests '*NightsTest' --tests '*NightStoreTest' --tests '*NightRulesTest' --tests '*SleepRecordTest' --tests '*CompareRulesTest' --tests '*HeartReadTest'`

| # | Status | Check ran | Real output |
|---|---|---|---|
| D1 | done: `READ_SLEEP` in the manifest; `HealthSleepSource.SLEEP_READ`; `HealthImport.readPermissions` unchanged | SleepRecordTest `D1 the manifest asks for sleep and the Import set keeps the three old permissions` | SleepRecordTest tests="2" failures="0" errors="0" |
| D2 | done: N1 and N2 in `nights()` | NightsTest `D2 ...` (2 tests). Break: drop the 24 h check | break: `expected:<[2026-11-03, 2026-11-04]> but was:<[2026-11-03, 2026-11-04, 2026-11-09]>`; revert: NightsTest tests="10" failures="0" |
| D3 | done: window `[18:00 D-1, 12:00 D)` from wall times | NightsTest `D3 ...` (2 tests). Break: `>=`/`<` to `>`/`<=` | break: 2 failed, `expected:<[2026-11-03]> but was:<[]>`, `expected:<2026-11-03> but was:<null>`; revert: 10 passed |
| D4 | done: union of spans minus union of clipped awake stages | NightsTest `D4 ...`. Break: no clip | break: `expected:<23700> but was:<23100>`; revert: 10 passed |
| D5 | done: most asleep origin, tie by package name, under 60 s dropped | NightsTest `D5 ...` (3 tests). Breaks: max→min; no sort; 60 s→1 s | break: `expected:<25200> but was:<23400>`; `expected:<1793642400000> but was:<1793644200000>`; `AssertionError` (40 s night kept); revert: 10 passed |
| D6 | done: New York 2026-03-08 8 h, 2026-11-01 10 h | NightsTest `D6 ...` | 10 passed |
| D7 | deviation: the spec gives no whole-hour spoken text; I chose "7 hours" (written "7 h 00 m") | NightRulesTest `D7 ...` (3 tests) | NightRulesTest tests="8" failures="0" |
| D8 | done: `nightLogLine` / `nightLogSpoken` | NightRulesTest `D8 the Log line of a day` | 8 passed |
| D9 | done: `lastNights`, days oldest first | NightRulesTest `D9 the last 14 nights` | 8 passed |
| D10 | done: `nightBeforeSessions`, title by `sessionName` | NightRulesTest `D10 ...` (2 tests) | 8 passed |
| D11 | done: `PeriodStats.nights`/`nightAvgSec`, `periodStats(..., nights = emptyMap())`, group "Nights" after "Bodyweight" | NightRulesTest `D11 ...`; CompareRulesTest unchanged | CompareRulesTest tests="14" failures="0"; NightRulesTest 8 passed |
| D12 | done: `sleepSpan(...)` + `SleepSessionRecord.span()` | NightsTest `D12 ...`; SleepRecordTest `D12 ...` with a real `SleepSessionRecord` | NightsTest 10 passed; SleepRecordTest 2 passed |
| D13 | done: `NightStore` | NightStoreTest (7 tests). Breaks: fresh 0 ms; no in-flight join; failed read overwrites; NoAccess not set | break: `expected:<1> but was:<2>`; `expected:<1> but was:<3>`; `ClassCastException ... NightsState$Failed`; `expected:<NoAccess> but was:<Loaded(...)>`; revert: NightStoreTest tests="7" failures="0" |
| D14 | done: full `Tests:` command on f00d51d | `android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug :app:lintVitalRelease`; `git diff --stat 5567185` | `BUILD SUCCESSFUL in 2m 27s`; 127 result files, tests=1231 skipped=69 failures=0 errors=0. The first run lost its Gradle daemon ("daemon disappeared unexpectedly") and was run again. Diff: no Room schema, DAO, or backup file |

Other local decisions: `readAllPages` now calls a new generic `readEveryPage`, which the sleep source uses too. A missing provider, a missing permission, or a `SecurityException` during the read throws `NoSleepAccess`. Only a loaded read counts as a good read for the 5-minute rule.

## Public names for the screen task

- Permission: `HealthSleepSource.SLEEP_READ` (`health/HealthSleepSource.kt`). It is not in `HealthImport.readPermissions`; request it with the screen's own launcher.
- State: `sealed interface NightsState { NotRead; NoAccess; Loaded(nights: Map<LocalDate, Night>); Failed }` (`data/NightStore.kt`).
- Night: `data class Night(date: LocalDate, asleepSec: Int, fromMs: Long, toMs: Long)` (`data/Nights.kt`).
- Store: `class NightStore(source: SleepSource, scope: CoroutineScope, clocks: Clocks = SystemClocks, zone: () -> ZoneId = ZoneId::systemDefault)` with `val state: StateFlow<NightsState>` and `suspend fun refresh(force: Boolean = false)`. App: `KhonApp.nightStore`. View model: `AppViewModel.nightStore`, `AppViewModel.nights: StateFlow<NightsState>`.
- Source: `fun interface SleepSource { suspend fun spans(range: HrRange): List<SleepSpan> }`, `class NoSleepAccess`, `HealthSleepSource(context)`.
- Rules (`data/Nights.kt`): `nights(spans: List<SleepSpan>, zone: ZoneId): Map<LocalDate, Night>`, `nightWindow(date, zone): TimeSpan`, `nightOf(ms, zone): LocalDate?`, `sleepSpan(origin, startMs, endMs, stages: List<SleepStage>, awakeTypes: Set<Int>): SleepSpan`, `SleepSessionRecord.span()`, `AWAKE_STAGES`.
- Texts (`ui/screens/NightRules.kt`): `nightLength(sec: Int): String`, `nightLengthSpoken(sec: Int): String`, `nightClock(n: Night, zone: ZoneId): String`, `nightLine(n: Night, zone: ZoneId): String`, `nightLineSpoken(n: Night, zone: ZoneId): String`, `nightLogLine(day: LocalDate, state: NightsState, today: LocalDate, zone: ZoneId): String?`, `nightLogSpoken(...same...): String?`, `NO_NIGHT_DATA`, `nightReviewRows(night: Night?, zone: ZoneId): List<NightFact>` with `NightFact(label, value, spoken)`.
- Figures: `lastNights(nights: Map<LocalDate, Night>, today: LocalDate): LastNights(days: List<Pair<LocalDate, Night?>>, averageSec: Int?, count: Int, shortest: Night?)`; `nightBeforeSessions(sessions: List<Session>, nights: Map<LocalDate, Night>, today: LocalDate): List<TitleNights(title, days: List<LocalDate>, averageSec: Int)>` with `count`; `averageSec(sec: List<Int>): Int?`.
- Compare: `periodStats(..., nights: Map<LocalDate, Night> = emptyMap())`, `PeriodStats.nights`, `PeriodStats.nightAvgSec`; `compareGroups` adds the "Nights" group by itself.
