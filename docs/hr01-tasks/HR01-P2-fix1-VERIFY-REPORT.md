# HR01-P2 band import, R5 verification

**Verdict: merge after fixes.** Reviewed snapshot `e6f0f7872133`.

- **F1 · P2 · Timer admission reads pending state at the wrong time.** **Read:** `Repository.kt:418–434` builds the session and rounds before the submit read. Reuse the W2 case in `ImportAdmissionTest.kt:253–270`. Submit band record `b1` while the result for app session `c` is pending. Let the timer commit and clear its entry during draft preparation. The commit leaves `c` with `timerDone=1`, a duration and null timer bounds. Both pending reads now return false. The first submit returns `SAVED` and adds a second session with `sourceId=b1` and its rounds. W2 requires `TIMER_PENDING` and a fresh submit. Read pending as the first operation in `importDraft`. Test a commit that settles during draft preparation.
- **F1 · Same class, early Stop.** **Read:** `TimerService.kt:343–359` sets the state to done before it saves the pending entry. Submit while the timer runs. Stop a trusted 20-minute ride early while the import waits for Room. Pause the service after it sets done and before it saves. Let the import pass `Dao.kt:346–350`. The old snapshot still has time left, so `TimerSnapshot.kt:141–144` returns false. Resume the service before the insert. The import stores the 20-minute band copy and its claim. The timer then stores the same 20-minute range and round times on the original app session. Publish the pending result as part of the end transition, before the service exposes done. Add a test for Stop before the admission check.

## Coverage trace

- **Read, W1–W4:** The in-write pending read follows the local rows. The order is claim, coverage, then pending (`Dao.kt:342–351`). Both claim and coverage use fresh Room rows. The coverage zone is read inside the transaction. Import reads no generation. Replace and reset share the timer lock (`Repository.kt:433–435,455–491`). The lead's decision permits the chosen exercise and band records to stay as submitted. IDs and round values use those inputs. The draft date uses the current zone during row construction (`HealthImport.kt:39`, `Repository.kt:423`).
- **Read, F2 closed:** The lock test signals arrival and checks order without a sleep (`ImportAdmissionTest.kt:208–231`). Both screen actions check reload and retained drafts (`ImportScreenTest.kt:122–183)). The restart test closes and reopens Room, calls the foreground fill and replays the draft (`BandImportTest.kt:381–414`). The screen test counts fill calls through the permission result and recomposition (`ImportScreenTest.kt:185–218`).
- **Read, deviations:** The sole production screen caller uses the default Health Connect source (`KhonNav.kt:143`, `ImportScreen.kt:40–54`). The test counter feeds no saved or displayed value (`HeartFill.kt:52–67`). The semantics action invokes the same exercise callback as the row tap, with touch delivery outside that assertion (`ImportScreenTest.kt:122–126`, `ImportScreen.kt:138`, `Common.kt:145–148`). The W1/W2 compile gap affects the test run on the base. The witness supplies the base failure and the fixer reports B1 and B2.
- **Read, F3 closed:** The fix diff removes the cited comments or restores the previous wording. `TimerSnapshot.kt:137–140` documents how the pending result check treats snapshots before restore. The task restricts changes in that package to Scope 1. The fixer reports `comment-guard --base 79531bd` exit 0.

## Evidence

- **Ran:** `cmp` matched the supplied fix diff to the source portion of the round diff. `shasum -a 256` matched the witness log hash in receipt `r-20261126231943-289e19`.
- **Read:** Step 6 uses the lead's witness run because this sandbox cannot write receipts. `witness run F1.json --sha e6f0f7872133` recorded `fixed` and `BUILD SUCCESSFUL in 5s`. The base witness log reports `2 tests completed, 2 failed`. The fixer reports `BUILD SUCCESSFUL`, 839 tests and 0 failures at `79cb97b`. This review ran source checks only.

F1=fix-defect
F2=closed
F3=closed
