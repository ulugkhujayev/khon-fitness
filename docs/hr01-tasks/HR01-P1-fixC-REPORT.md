# HR01-P1-fixC report

Commits on `hr01-fixc` (base `d9b444e`, task file commit `b553c23`):

- `ee80569` Read the zone inside each range write, select heart rate records by overlap, and check file offsets without overflow
- `23db637` Split the range overflow test into one test per guard

Focused command (F): `JAVA_HOME=... ANDROID_HOME=... android/gradlew -p android :app:testDebugUnitTest --tests '*.data.HeartZoneTransactionTest' --tests '*.data.RangeOverflowTest' --tests '*.data.HeartReadTest' --tests '*.data.HeartFillTest' --tests '*.data.BackupHrTest' --tests '*.data.GymRangeTest'`
Real output of F on `ee80569`: `BUILD SUCCESSFUL in 18s`; XML: HeartZoneTransactionTest 5/0 failures, RangeOverflowTest 3/0, HeartReadTest 8/0, HeartFillTest 16/0, BackupHrTest 12/0, GymRangeTest 10/0.

Tests command on `23db637`: exit 0, `BUILD SUCCESSFUL in 2m 10s`, `71 actionable tasks: 29 executed, 20 from cache, 22 up-to-date`; the JUnit XML holds 779 tests, 0 failures, 0 errors.

## Contract

| # | Status | Check ran | Real output |
|---|---|---|---|
| Z1 | done: `commitSessionHr`, `deleteStaleSessionHr`, `removeSessionHr`, `confirmRange` take `() -> ZoneId` and call it inside the transaction; `HeartFill` and `KhonRepository.confirmRange` pass the provider; the pass's delete decides staleness with the zone read in the delete | F; witness C-F1; break run A | F green; `WITNESS C-F1 fixed at=ee80569ef621 receipt=r-20261126215816-2de5c3`, receipt log `> Task :app:testDebugUnitTest` ... `BUILD SUCCESSFUL in 41s`; break A: 5 HeartZoneTransactionTest cases and AT7 fail |
| Z2 | done: `askRange` asks from 24 h (`RECORD_LOOKBACK_MS = DAY_MS`) before the range start; `touching` keeps records with `endMs > from` and `startMs < to`; the adapter maps each record to `BoundedHeartRecord` and filters each page with `touching`; `readHeart` and its counts unchanged | F (HeartReadTest AT5 and selection test); witness C-F3 | AT5 through `platform`+`touching`: raw 26, accepted 24, avg/max 130, filled; bounds records dropped, 20 h record kept; `WITNESS C-F3 fixed at=ee80569ef621 receipt=r-20261126215920-4fec2f`, log `BUILD SUCCESSFUL in 16s` |
| Z3 | done: `validFileHr` refuses `fromMs < 0`; offset valid when `0 <= offsetSec < (toMs - fromMs - 1) / 1000 + 1` (no overflow since `0 <= fromMs < toMs`) | F (BackupHrTest two new tests); witness C-F2 | `Long.MAX_VALUE` row with offset 2405, `fromMs` negative, offset `Int.MAX_VALUE`: dropped in Replace and Merge, 4 sessions kept with `avgHr` 150, valid row kept; `WITNESS C-F2 fixed at=ee80569ef621 receipt=r-20261126215901-0a2678`, log `BUILD SUCCESSFUL in 17s` |
| Z4 | done: every instance of both searches is in the Coverage table | `grep -rn "ZoneId\|zone" data health`; `grep -rn "1000\|lengthMs\|+ CARDIO\|+ STAMP\|now - \|fetchedAt + 1"` over `data`, `health` | 4 zone writes found (all fixed); 11 overflow sites found (see Coverage) |
| Z5 | done: AT7 now changes the zone in Room's transaction executor after the provider returned; new C29 test does a held reread of an `off` row, then a second Remove (state stays `off`, only `fetchedAt` changes) | break run A: zone read before each transaction (4 call sites) and `|| now?.fetchedAt != from?.fetchedAt` removed | `HeartFillTest > AT7 ... FAILED`, `HeartFillTest > C29 a second Remove beats a held reread of an off row by its stamp alone FAILED`; `41 tests completed, 10 failed` |
| Z6 | done: old Part C rows still pass in F (C21–C34b, AT4–AT7, AT17–AT29 tests). Changed assertions: AT5 (records now carry bounds and go through `askRange`+platform filter+`touching`; same 26/24/130 numbers); AT7 (zone switch moved from "during the provider hold" to "as the commit transaction starts", holds removed; same DROPPED and no-row result). No other old assertion changed | F | all old tests in HeartReadTest, HeartFillTest, BackupHrTest, GymRangeTest pass |
| Z7 | done: no change in `timer/`, `data/TimerCommit.kt`, `ui/`, schemas, `res/`, Gradle files, manifest, version; no new dependency; no task or review ID in code | `git diff --name-only d9b444e..HEAD \| grep -E "timer/\|TimerCommit\|/ui/\|schemas\|res/\|gradle\|Manifest"` | empty output |

## Break proofs

- Run A (zone captured before the 4 transactions; stamp check removed; `touching` keeps all; old `validFileHr` line): `41 tests completed, 10 failed`: BackupHrTest 2, HeartFillTest C29 + AT7, HeartReadTest selection test, HeartZoneTransactionTest 5.
- Run B (`askRange` returns the range; `plusMs` plain sum; `laterThan` old line): `25 tests completed, 5 failed`: AT5, selection test, sum test, RangeOverflowTest cardio tail and gym tail. The `laterThan` break fails no test: the old wrap `MAX + 1 = MIN` already gave `now` (no wrong result before).
- Run C (`spanMs` plain difference; `minusMs` plain difference): `25 tests completed, 5 failed`: length test, sum test, age test, selection test (lookback at `Long.MIN_VALUE`), completeness test.

## Coverage

| ID | Instance | Status | Test | Found by |
|---|---|---|---|---|
| Z1.1 | `KhonDao.commitSessionHr` zone (fill commit) | fixed: zone read in the transaction | HeartZoneTransactionTest fill commit; HeartFillTest AT7; witness C-F1 `fixed` | review C-F1 |
| Z1.2 | `KhonDao.deleteStaleSessionHr` zone (cleanup delete), pass picks rows with the delete's zone | fixed: zone read in the delete transaction | HeartZoneTransactionTest cleanup; witness C-F1 `fixed` | review C-F1 |
| Z1.3 | `KhonDao.removeSessionHr` zone (Remove) | fixed: zone read in the transaction | HeartZoneTransactionTest two Remove cases; witness C-F1 `fixed` | review C-F1 |
| Z1.4 | `KhonDao.confirmRange` zone via `KhonRepository.confirmRange` | fixed: zone read in the transaction | HeartZoneTransactionTest confirmRange | search `ZoneId` in `data` |
| Z1.5 | set stamp (`checkSetStamped` SQL), backup merge clears (`importBackup`, `boundsChanged`, `mergedSet`) | kept: they take no zone and check no range | GymRangeTest, BackupHrTest (pass) | search `ZoneId` in `data` |
| Z1.6 | `HeartFill.read` range choice and pass fill selection before the read | kept: a choice, not a write; the commit checks again with its own zone | HeartFillTest AT7 | search `zone()` in HeartFill |
| Z2.1 | `HealthHeartSource.records` filter by start time | fixed: `askRange` + `touching` per page | HeartReadTest AT5 and selection test; witness C-F3 `fixed` | review C-F3 |
| Z3.1 | `validFileHr` offset sum `fromMs + offsetSec * 1000L` | fixed: whole-second limit, `fromMs >= 0` | BackupHrTest two new tests; witness C-F2 `fixed` | review C-F2 |
| Z3.2 | `HrRange.lengthMs` (`toMs - fromMs`) used by `hrWindow` and `readHeart` | fixed: `spanMs` saturates (old wrap gave no false range: a wrapped length is negative) | HeartReadTest length test | search `lengthMs` |
| Z3.3 | `hrWindow` app cardio tail `to + CARDIO_TAIL_MS` (file `timedTo`) | fixed: `plusMs` | RangeOverflowTest cardio tail | search `CARDIO_TAIL` |
| Z3.4 | `gymRange` tail `last + STAMP_TAIL_MS` (`checkedAt`) | fixed: `plusMs` | RangeOverflowTest gym tail | search `STAMP_TAIL` |
| Z3.5 | `confirmableRange` `finishedAt - startedAt` | fixed: `spanMs` (old wrap gave the same LENGTH refusal) | RangeOverflowTest length case | search `- session.startedAt` |
| Z3.6 | `fillDue` `now - toMs` (file `timedTo` far in the past made a pass read every time) | fixed: `spanMs` | HeartReadTest age test | search `now -` |
| Z3.7 | `hrRow` `now - range.toMs` (waiting instead of none) | fixed: `spanMs` | HeartReadTest age test | search `now -` |
| Z3.8 | `readHeart` `2 * (last - first)` | fixed: `spanMs(first, last) >= ceil(length / 2)` | HeartReadTest completeness test | search `2 *` |
| Z3.9 | `laterThan` `fetchedAt + 1` (file `fetchedAt`) | fixed: explicit `Long.MAX_VALUE` case; no behavior change (old wrap gave `now`) | HeartReadTest largest stamp test | search `fetchedAt` |
| Z3.10 | `askRange` `fromMs - RECORD_LOOKBACK_MS` | fixed: `minusMs` | HeartReadTest selection test | new code |
| Z3.11 | `roundAverage` `row.fromMs + offsetSec * 1000L`; file `checkedAt`, round times, `timedFrom`, `timedTo`, `firstMs`, `lastMs` value checks | kept: a shown row is a valid file row or a read row, so the sum is below `toMs`; the file has no value check of the other columns (design: only HR rows are checked by value) and `firstMs`, `lastMs` are only stored | BackupHrTest, HeartFillTest C23 (pass) | search `1000`, `firstMs`, `checkedAt` |
| Z4.1 | the class searches | kept: the list above is the result | rows Z1.1–Z3.11 | search commands in Z4 |
| Z5.1 | AT7 zone test | fixed: zone flips in the transaction executor | HeartFillTest AT7; break run A | review G/J tests |
| Z5.2 | Remove stamp test | fixed: new held-reread-of-off test | HeartFillTest C29 second Remove; break run A | review G/J tests |
| Z6.1 | changed old assertions AT5 and AT7 | fixed: listed in Z6 | HeartReadTest AT5; HeartFillTest AT7 | own diff |
| Z7.1 | forbidden paths and kinds of change | kept: none touched | `git diff --name-only` empty | own diff |

Witness results on `ee80569`: C-F1 `fixed`, C-F2 `fixed`, C-F3 `fixed`. On `23db637`: `WITNESS C-F1 fixed at=23db637e8695 receipt=r-20261126220250-9ce061`, `WITNESS C-F2 fixed at=23db637e8695 receipt=r-20261126220304-7ff1dd`, `WITNESS C-F3 fixed at=23db637e8695 receipt=r-20261126220317-4b8e12`; each receipt log runs `:app:testDebugUnitTest` and ends `BUILD SUCCESSFUL`.

Kept resources: none. The witness clones are removed by the tool.

RESULT {"status":"done","head":"23db637"}
