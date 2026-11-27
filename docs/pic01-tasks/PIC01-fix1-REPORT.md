# PIC01-fix1 report

Branch `pic01`, fix commit `11356e4` on base `2100daf` (task file commit `a086bbe`). No push, no merge.

Final `Tests:` run on `11356e4`: `BUILD SUCCESSFUL in 1m 41s`, JUnit XML totals `tests=783 failures=0 errors=0 skipped=21`
(781 before + 2 new rule tests; the skipped ones are other shot tests whose folders do not exist).

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: a bucket belongs to the picture when `range.fromMs + offsetSec * 1000L` is in `[fromMs, toMs)` (`bucketAtMs`, `HrRange.holds`, Long math); set marks and round bands use the same `holds`; the test fixtures make buckets with `bucketStartsSec` (ms compare) instead of `lengthMs / 1000` | `--tests '*PictureRulesTest*'` on the fix, and again with the old filter `offsetSec in 0 until range.lengthMs / 1000` put back (file restored after); `witness run F1.json --sha 11356e4` | fix: `tests="9" failures="0"`; old filter: `a bucket and a set in the last partial second of the range count, a bucket at the range length does not FAILED java.lang.AssertionError at PictureRulesTest.kt:114`; witness: `WITNESS F1 fixed at=11356e4ef2b1 receipt=r-20261126222108-8c6064` (receipt log: `> Task :app:testDebugUnitTest` executed, `BUILD SUCCESSFUL in 10s`) |
| X2 | done: the two pixel tests take every expected position from the fixture (bucket times and bpm, set `checkedAt`, round times) and the plot found from the grid lines: each bucket point within 2 px, the lone dot within 1 px, no accent pixel in a gap or outside the curve's ends, each tick at 1 dp and 7 dp above the plot bottom and not at 9.5 dp or under the bottom, tick columns and no other, each band inside and outside both edges and in its middle at three heights (top + 2 dp, between the middle grid lines, bottom - 2 dp; curve hidden), and the band count along the bottom | 6 mutations of `ReviewPicture.kt`, one per run of the two pixel tests, file restored after (script `mut.py` in the scratchpad); `witness run F2.json --sha 11356e4` | `curve y`: gym FAIL `[no curve at 0 s, 92 bpm, ...`, cardio FAIL; `tick y`: gym FAIL `[no tick foot on the plot bottom at 240000 ms, tick higher than 8.0 dp ...`; `band vertical span`: cardio FAIL `[no band at (272.2, 496.0) for 480000..720000 ms ...`; `lone dot`: gym FAIL `[no curve at 1890 s, 137 bpm, no lone dot at 1890 s]`; `curve x +4dp`: gym FAIL, cardio FAIL `[no curve at 0 s, 98 bpm ...`; `band x +3dp`: cardio FAIL `[no band at (272.2, 496.0) ..., band beyond its edge at (364.3, 496.0) ...`. Unchanged: both pass. Witness: `WITNESS F2 fixed at=11356e4ef2b1 receipt=r-20261126222121-67dd52`, log ends `Missed drawing changes: none` |
| X3 | done: every comment in the 7 Kotlin files of the change is gone; rules moved into names (`CURVE_MAX_JOINED_GAP_SEC`, `AXIS_MIN_LINES`, `AXIS_MAX_LINES`, `AXIS_STEPS`, `AXIS_BASE_MULTIPLE`, `growAboveThenBelowToMinLines`, `MIN_MARKS_FOR_ROWS`, `MIN_BUCKETS_FOR_ROW_BPM`, `WorkoutPicture.markOffsetsMs`, `GYM_CHECK_SEC_FROM_START`, `GYM_GAP_SEC`, `GYM_LONE_BUCKET_SEC`, `CARDIO_WARM_UP_SEC`, `CARDIO_ROUND_SEC`, `CARDIO_REST_SEC`, `CARDIO_COOL_DOWN_SEC`, `TICK_HEIGHT_DP`, `PICTURE_PAD_RIGHT_DP`). Kept comments: none | `git diff 2dbca2f HEAD -- <each .kt>` added lines grepped for `//`, `/*`, ` *`; `comment-guard --base 2dbca2f`; `witness run F3.json --sha 11356e4` | grep: no line (`scan done` only); `guard exit 0`; `WITNESS F3 fixed at=11356e4ef2b1 receipt=r-20261126222108-1e241b` |
| X4 | done: H1-H12 still hold; changed assertions: `p.marks` became `p.markOffsetsMs` (rename for F3, PictureRulesTest lines 69 and 105); the H7 pixel assertions were replaced by the X2 checks (F2); teardown of `PictureScreenTest` and `PictureShotsTest` now removes the screen, drains Room with `withTransaction {}`, then closes the database, as `TapTargetTest` and `SystemBackGuardTest` do | `--rerun --tests '*PictureScreenTest*'` 4 times on base `2100daf` (temp worktree, removed) and 4 times on the fix; full `Tests:`; 12 shots made on base and on the fix, compared with `cmp`, looked at as two sheets | base: 2 of 4 runs `a session without a range and an unfinished one show nothing new` FAIL `IllegalStateException: Cannot perform this operation because the connection pool has been closed` (a Room flow of the previous test after `db.close()`); fix: 4 of 4 runs `tests="10" failures="0"` and `tests="9" failures="0"`; all 12 shots `same` byte for byte as the base shots; the sheets show the chart, ticks, bands and "Sets in order" with no clipped text |
| X5 | done: the fix commit changes only files of the first change and this report | `git diff --stat 2100daf 11356e4` | 6 files: `PictureRules.kt`, `ReviewPicture.kt`, `PictureFixtures.kt`, `PictureRulesTest.kt`, `PictureScreenTest.kt`, `PictureShotsTest.kt`; no `SPEC.md`, `DESIGN.md` (no line there names whole seconds), `timer`, `data`, Room, dependency or version change |

## Coverage

| ID | Instance | Status | Test | Found by |
|---|---|---|---|---|
| X1.1 | `pictureBuckets` cut the range to whole seconds (`lengthMs / 1000`) and dropped a bucket in the last partial second | fixed: ms compare with `HrRange.holds(bucketAtMs(...))` | `a bucket and a set in the last partial second of the range count, a bucket at the range length does not` | review F1 |
| X1.2 | bpm axis built from those buckets | fixed: through X1.1 (`BpmAxis(120, 180, 20)` with the 600 s bucket) | same test | review F1 |
| X1.3 | spoken lowest and highest | fixed: through X1.1 (`highest 180 beats per minute`) | same test | review F1 |
| X1.4 | set row bpm span `[mark before, check)` | fixed: through X1.1; the span compare was already in ms, now uses `bucketAtMs` | same test (`rows[1].maxBpm == 180`), witness F1 | review F1 |
| X1.5 | set marks `[fromMs, toMs)` | kept: already ms, now `range::holds`; a check at `fromMs + 600_001` marks, one at `toMs` does not | same test | fixer search |
| X1.6 | round band start inside the range, end cut at `toMs` | kept: already ms, now `range.holds(start)` | `a round that starts in the last partial second of the range gets a band cut at the range end` | fixer search |
| X1.7 | drawing x of a bucket `x(offsetSec * 1000L)`, marks and bands in ms | kept: already ms over `lengthMs` | gym and cardio pixel tests | fixer search |
| X1.8 | `sinceSec = (at - prev) / 1000` | kept: floors a duration for "2:05", not a range end | `set rows give the clock, the time since the mark before, and the highest bpm of the span` | fixer search |
| X1.9 | end clock `clockAt(range.toMs)` | kept: HH:mm text, no bucket is dropped | `the chart and a set row speak in full unit words` | fixer search |
| X1.10 | test fixtures made buckets over `0 until lengthMs / 1000` | fixed: `bucketStartsSec` keeps a start while `it * 1000L < lengthMs` | all picture tests; 12 shots unchanged | fixer search |
| X2.1 | curve y position | fixed: bucket points within 2 px | gym and cardio pixel tests (mutation `curve y` fails both) | review F2 |
| X2.2 | tick y position | fixed: foot at 1 dp, top at 7 dp, none at 9.5 dp or under the bottom | gym pixel test (mutation `tick y` fails) | review F2 |
| X2.3 | band vertical span | fixed: three heights | cardio pixel test (mutation `band vertical span` fails) | review F2 |
| X2.4 | lone dot | fixed: dot within 1 px of its bucket | gym pixel test (mutation `lone dot` fails) | review F2 |
| X2.5 | curve shifted in x | fixed: points plus empty gap and end columns | gym and cardio pixel tests (mutation `curve x +4dp` fails both) | task scope |
| X2.6 | band shifted in x | fixed: inside and outside both edges | cardio pixel test (mutation `band x +3dp` fails) | task scope |
| X3.1 | `PictureRules.kt` comment blocks (18, 21, 26, 29, 32-35, 54, 61, 71, 75, 83-86, 105, 110, 122-125, 145-148) | fixed: removed, rules in constant and function names | witness F3, comment scan | review F3 |
| X3.2 | `ReviewPicture.kt` comment blocks (49, 57-61, 99-102) | fixed: removed | witness F3, comment scan | review F3 |
| X3.3 | `PictureFixtures.kt` comment blocks (9, 24, 27-30, 67) | fixed: removed, times in `GYM_*` and `CARDIO_*` names | witness F3, comment scan | review F3 |
| X3.4 | `PictureScreenTest.kt` comments (62, 107, 121, 133, 147, 155, 192, 261) | fixed: removed | witness F3, comment scan | review F3 |
| X3.5 | `PictureShotsTest.kt` comment block (45-49) | fixed: removed | witness F3, comment scan | review F3 |
| X3.6 | other comments in the change (`PictureRulesTest.kt`, `ReviewScreen.kt` lines) | kept: there were none; none added | comment scan of added lines | fixer search |
| X4.1 | teardown race: a Room flow of Review queried after `db.close()` and failed the next test (2 of 4 base runs) | fixed: screen removed, Room drained, then closed | `PictureScreenTest` 4 of 4 runs green | fixer (seen while testing) |
| X5.1 | files the fix commit touches | kept: only the 6 Kotlin files of the first change, plus this report | `git diff --stat 2100daf 11356e4` | task scope |

## Witnesses

- F1: `WITNESS F1 fixed at=11356e4ef2b1 receipt=r-20261126222108-8c6064`
- F2: `WITNESS F2 fixed at=11356e4ef2b1 receipt=r-20261126222121-67dd52` (`Missed drawing changes: none`)
- F3: `WITNESS F3 fixed at=11356e4ef2b1 receipt=r-20261126222108-1e241b`

## Screenshots

`android/app/build/shots-picture/review-{gym,cardio,plain}-{360,411}dp-x{1_0,1_3}.png`, made by `PictureShotsTest` in the final
`Tests:` run; all 12 equal the base shots byte for byte. Looked at as two sheets (top 4200 px of each shot).
