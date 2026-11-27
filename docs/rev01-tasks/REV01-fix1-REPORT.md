# REV01-fix1: report

Branch `rev01`, base d10034e. Commits: `0320131` (state at the tap, block scroll by key, bar before the logs, tests), `f075156` (block scroll starts in the tap's frame again), `1546aab` (history test waits for the cardio figures). Last code commit: `1546aab`.

## Contract

| # | Status | Check ran | Real output |
|---|---|---|---|
| X1 | done: the Review action reads the live state at the tap (`sessionState.first?.finished`, `editing`, `draft == opened && edits.isEmpty()`) and runs through the new `ExitGuard.visit`, which takes the screen's guard (no-op while Save, Back, Discard or Delete holds it), navigates only while the screen is on top, then frees the guard. Edit/Done runs through `leave.leave`, so Edit after a Review tap does nothing. | witness F1 at 1546aab; `ReviewRowStateTest` (4 tests: typed note, typed round value, Save then Review, Edit then Review and Review then Edit) | `WITNESS F1 fixed at=1546aab4cfa2 receipt=r-20261127012424-c665c8`; tests pass; note and "150" stay in the fields; Save stores the note and lands on `[today, history]` |
| X2 | done: the block scroll keeps the block key (`BlockReveal`). It starts in the tap's frame, as on the base, with the index from the key plus the live row state; `LaunchedEffect(reviewRow)` starts it again for the same block when the row comes or goes. A user drag clears it. | witness F2 at 1546aab; `ReviewRowStateTest` "a block opened in the frame of Edit/Done scrolls to that block" (360×480 dp); temp capture test at 2b10ad5 and at f075156 (gym Edit mode, Edit mode + Block 2 opened after 3 s, unfinished gym), `cmp` | `WITNESS F2 fixed at=1546aab4cfa2 receipt=r-20261127012444-d71b4f`; tests pass; `gym-edit equal`, `gym-edit-block2 equal`, `gym-unfinished equal` (byte-equal PNGs, 1080×2400) |
| X3 | done: no code change for F3 and F5; the row layout is unchanged. | witnesses F3, F5 at 1546aab | `WITNESS F3 fixed at=1546aab4cfa2 receipt=r-20261127012510-ce9b37`; `WITNESS F5 fixed at=1546aab4cfa2 receipt=r-20261127012550-8f06f3` |
| X4 | done: no early `return` on the sets or rounds. The top bar, Back, Finish/Save/Edit, both exit guards, `LeaveIfSessionMissing` and every hook after `s ?: return` run from the first frame with the session; only the `LazyColumn` (gym) and the scrolling `Column` (cardio) wait for `loadedLogs`/`loadedIntervals`. | witness F4 at 1546aab; `ReviewRowStateTest`: 6 late-log tests (gym and cardio, finished and unfinished: bar Back, system Back, delete while loading) and "the body shows once the logs load" | `WITNESS F4 fixed at=1546aab4cfa2 receipt=r-20261127012531-77b217`; tests pass: bar shows with "History"/"Today" and Edit/Finish/Save, no scroll node before the logs, each case ends on `[today, history]` |
| X5 | done: V4 waits for `HEART_PICTURE_TAG` on gym and cardio Review and for the cardio figures (Intervals, Round 1 and last round "<bpm> bpm"); V5 cardio test adds the bar Back (tapped twice) from Review to the form with the scroll place; V6 waits for History content (Log, cardio row, no deleted row); V7 new test: a fill that commits while the cardio form is open. | break proofs, `--tests '*ReviewFromHistoryTest'` with one temp change each, restored with `git checkout` | no `HeartPicture`: `never: Review draws the heart rate picture` (gym and cardio); `CardioReview` off: `never: Review shows the cardio figures`; Review bar Back a no-op: `never: the bar Back returns to the form; routes [today, history, cardio/{id}, review/{id}]`; HistoryScreen returns at once: `never: History draws its log with the cardio session and without the deleted one`; cardio `sessionHr(...).take(1)`: `never: the committed fill shows` |
| X6 | done: removed the four comments the review names (CardioScreen guard, history test class and helper, fit test class) and the other branch comments that code says: ReviewRowShotsTest class doc, CardioScreen and SessionScreen doc additions, "Null until ..." comments, `reviewBack`, `reviewEntrySpoken` and `ReviewEntry` docs. | `~/.config/agents/tools/comment-guard --base 2b10ad5` | `guard=0` |
| X7 | done: no existing assertion changed except in the four V4–V7 tests of decision 4 (assertions added, none removed). The 16 pictures were made again and looked at: no clipped or overlapping text; only gym, long and cardio at 360 dp ×1.3 stack the value. | `Tests:` command on 1546aab, with `android/app/build/shots-review-row` created | `BUILD SUCCESSFUL in 2m 1s`; 112 result files, 1088 tests, 0 failures, 0 errors, 51 skipped |

## Coverage

| # | Instance | Status | Test | Found by |
|---|---|---|---|---|
| X1.1 | Cardio row action with a typed draft field | fixed: live `draft == opened` at the tap | ReviewActionStateWitness (F1); ReviewRowStateTest typed note | review F1 |
| X1.2 | Cardio row action with a typed interval field | fixed: live `edits.isEmpty()` at the tap | ReviewRowStateTest typed round value | review F1 |
| X1.3 | Cardio Review tap while Save runs | fixed: `ExitGuard.visit` | ReviewActionStateWitness (F1); ReviewRowStateTest Save then Review | review F1 |
| X1.4 | Gym Review tap after Edit | fixed: live `editing` at the tap | ReviewActionStateWitness (F1); ReviewRowStateTest Edit then Review | review F1 |
| X1.5 | Gym Edit after a Review tap in the same frame | fixed: Edit through `leave.leave` | ReviewRowStateTest Review then Edit | builder (same class) |
| X2.1 | Block opened, then Edit removes the row | fixed: scroll by block key, restart on row change | ReviewBlockScrollWitness (F2); ReviewRowStateTest Edit | review F2 |
| X2.2 | Block opened in Edit, then Done adds the row | fixed: same | ReviewRowStateTest Done | review F2 |
| X2.3 | No row: gym screen as on the base | kept: byte-equal PNGs | temp capture at 2b10ad5 and f075156, `cmp` | task decision 2 |
| X3.1 | F3 cardio scroll offset after Review | kept: no change | ReviewCardioScrollWitness (F3) | review F3 |
| X3.2 | F5 fit boundary | kept: no change | ReviewWidthBoundaryWitness (F5) | review F5 |
| X4.1 | Gym unfinished, late sets | fixed: no early return | SessionHeaderLoadingWitness (F4); ReviewRowStateTest | review F4 |
| X4.2 | Cardio unfinished, late rounds | fixed: no early return | SessionHeaderLoadingWitness (F4); ReviewRowStateTest | review F4 |
| X4.3 | Gym finished, late sets | fixed: same | ReviewRowStateTest | task decision 3 |
| X4.4 | Cardio finished, late rounds | fixed: same | ReviewRowStateTest | task decision 3 |
| X4.5 | Cardio exit guard installed after the rounds check | fixed: guard runs from the first frame | ReviewRowStateTest system Back while loading | review coverage note |
| X4.6 | `LeaveIfSessionMissing` while the logs load | kept: already before the return; now tested | ReviewRowStateTest delete while loading | task decision 3 |
| X5.1 | V4 picture and cardio figures | fixed: asserted | ReviewFromHistoryTest | review F6 |
| X5.2 | V5 cardio bar Back | fixed: asserted | ReviewFromHistoryTest | review F6 |
| X5.3 | V6 visible History | fixed: asserted | ReviewFromHistoryTest | review F6 |
| X5.4 | V7 cardio fill | fixed: new test | ReviewFromHistoryTest | review F6 |
| X6.1 | Four named comments | fixed: removed | comment-guard | review F7 |
| X6.2 | Other branch comments that code says | fixed: removed | comment-guard | builder |
| X7.1 | Whole suite and 16 pictures | kept: pass, looked at | Tests command; ReviewRowShotsTest | task |

Witness results at 1546aab: F1 fixed, F2 fixed, F3 fixed, F4 fixed, F5 fixed. At bbd7536 (lead): F1, F2, F4 reproduce; F3, F5 fixed.

Base proof for the new tests: `ReviewRowStateTest` with `android/app/src/main` from bbd7536 gives 13 tests, 13 failed, for example `expected:<[cardio]/{id}> but was:<[review]/{id}>`, `never: the bar shows while the logs load`, and `the first block at the top of the list after Edit; tops {Block 3=192.0, ...} expected:<Block [2]>`.

## Tests

```
> Task :app:testDebugUnitTest
> Task :app:assembleDebug UP-TO-DATE
> Task :app:lintVitalRelease
BUILD SUCCESSFUL in 2m 1s
71 actionable tasks: 4 executed, 67 up-to-date
files 112 tests 1088 skipped 51 failures 0 errors 0
```

The first full run timed out (exit 124) on the known worker hang; I killed the worker (pid 26705, 100 % CPU, this worktree). The second run lost its Gradle daemon ("Gradle build daemon disappeared unexpectedly"). The third run passed (above). The first of these runs also showed a timing race in my new cardio figures assertion; it now waits (`1546aab`) and its break proof still fails.

## Screenshots

`android/app/build/shots-review-row/review-row-{gym,long,plain,cardio}-{360,411}dp-x{1_0,1_3}.png`, all 16 looked at.

## Deviations

- Edit/Done of the gym bar now runs only while the screen is on top (`leave.leave`), so a Review tap and Edit in one frame do not both run.
- SPEC.md and DESIGN.md are not changed: no documented behavior changed.
- Removed after use: base worktree `khon-fitness-wt/REV01-fix1-base` (`git worktree remove`) and the temporary capture test.
