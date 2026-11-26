# HR01 phase 2 band import, R4 deep review

**Verdict: do not merge.** Snapshot head `79531bd21d339e761e19bc8285efa689eb18ea23`. Kotlin filenames refer to the snapshot's main or test package under `src/android/app/src`.

## Coverage trace

- **Read, traced:** A1, A8, A12, A18–A19, A22, A25, A36–A37, A43–A44, A52, A54, A86–A89, A104, C20–C21, E4–E11, E20–E21, E23–E24, I19, K421/K423–424/K426, L6 and Q D22. `BandCover.kt:20,30,41`, `HeartRate.kt:62`, `Dao.kt:18,310,337,341,352`, `HealthImport.kt:126,130,146,166,172,190` and `Repository.kt:417,429`. Both readers take the same Room snapshot of claims, ranges and exercise modalities. Both filter original records before grouping. The write repeats the same rule and checks whole ID tokens. Room rolls back the session and rounds together. The new tie order changes the source of values. Each group's member claims and original ranges are still carried through to the write.
- **Read, traced:** A27, A46–A47, A99, D17–D26, D32–D33, D35, F5–F6, I2, I15, K427, L4–L5 and L7 at the import boundary. `MainActivity.kt:21`, `TimerService.kt:155,187,295,343,533,547`, `TimerSnapshot.kt:141`, `Repository.kt:346,443,481`. Both cold-start fixtures reach the new predicate. The timer and import writes share a lock. F1 covers the pending state between those writes.
- **Read, traced:** E1–E3, E12–E19, E22, H32, I1, K425/K431–432/K434/K436 at the import boundary. `KhonApp.kt:28`, `HeartFill.kt:32,59`, `ImportScreen.kt:57,59,69,108,126`, `TodayScreen.kt:100,143` and `KhonNav.kt:143`. The open effect asks for a fill without waiting. The list effect handles permission and batch refresh. The diff adds no screen text, permission, manifest change or dependency. `SPEC.md:358` records the import rules.
- **Read, traced:** AT01, AT02, AT03/03a, AT04, AT08/08a, AT11 and AT12/12a through the assertions in `BandImportTest.kt` and `ImportAdmissionTest.kt`. AT05, AT07/07a, AT09, AT10 and AT24 have gaps in F1 and F2. The remaining inventory entries were not traced. They are outside this phase. K numbers refer to lines in the supplied inventory. Provider pagination remains the existing single-page read and is outside this phase.

## Findings

**F1 · P2 · Timer admission reads pending state at the wrong time**
- **Read:** `Repository.kt:442–445` reads a Boolean under the timer lock, before Room can start the write transaction. `Dao.kt:352–361` uses that Boolean after it reads the local rows. An import can queue behind another Room transaction, then a covering run can publish its result before the import checks any Room rows. The import uses the old Boolean and saves. The timer commits next, leaving two sessions. `TimerService.kt:351–359` publishes before it takes the repository lock.
- **Read:** `Repository.kt:346–360,442–445` also lets a pending-first submit wait behind a timer commit that already holds the lock. If that commit has no covering range, the submit sees the cleared pending entry and saves. D7b requires a pending refusal and a fresh choice on retry.
- Fix: make the pending check part of the write's admission and preserve pending-first refusal while a timer commit holds the lock. Keep the claim and coverage result precedence. The existing reverse test ends the timer after the import passed its Room checks. It explicitly observes both writes (`ImportAdmissionTest.kt:206–225)). F1's duplicate occurs before those checks.
- Witness: `F1.json` and `F1.patch`. The patch holds Room before either write enters and uses the real pending store. Expected for each first submit: `TIMER_PENDING`, zero import rows and zero fill calls. The source predicts `SAVED`, one row and one fill for each. The lead must run it to confirm.

**F2 · P3 · Attack tests bypass the step they claim to prove**
- **Read:** AT07a, `ImportAdmissionTest.kt:210–216`. A 500 ms wait checks only that a worker did not finish. Room alone can queue its write even if the timer lock is removed. Signal the timer's arrival at the admission boundary and prove it cannot pass the lock before the import releases it.
- **Read:** AT05 and AT09, `BandImportTest.kt:331,336,369–373`. These tests call the batch helper and query the list directly. They cannot fail if the screen never reloads or leaves after a failed write. Exercise both screen submit actions and assert the reload and retained draft.
- **Read:** AT10, `BandImportTest.kt:387–395`. A new repository uses the same open in-memory database and calls the runner directly. Close and reopen a file-backed Room database, then request fill through a restart entry point and replay the old draft.
- **Read:** AT24, `ImportOpenFillTest.kt:56,69,75–79`. The click loop can run zero times and never returns a permission result. The test counts passes, while `HeartFill.kt:32,59–61` can combine requests. Require a permission callback, drive recomposition of the screen itself, and count requests at the call so duplicate requests cannot disappear.
- No runtime witness for F2. These are missing test steps and assertions.

**F3 · P3 · Comments repeat what the code or tests say**
- **Read:** Remove or express through names the added comment lines in `BandCover.kt:8,11–14,16`, `Repository.kt:152–155,409,411,413`, `TimerCommit.kt:38`, `HealthImport.kt:102,109,124,142,165` and `ImportScreen.kt:68`. Keep the contract text where a listed line also states a real rule. The old comment at `Dao.kt:333–336` also became detached from the method it described and now promises a Boolean for a query that returns rows.
- **Read:** Test instances are `BandImportTest.kt:33–36,76,269–272`, `ImportAdmissionTest.kt:46–49,63,114,122,132` and `ImportOpenFillTest.kt:30,57`. Move the behavior into fixture names and assertions. No runtime witness for F3. This is a source wording rule.

## Checks run

- **Ran:** `witness check F1.json` exited 0. Checked the Room, timer and coroutine calls against snapshot declarations and existing fixtures. **Ran:** The repository's `comment-guard` scanner against added snapshot lines reported `0 tagged lines`.
- **Ran:** `witness run F1.json --sha 79531bd21d339e761e19bc8285efa689eb18ea23` printed `undecided`. The sandbox denied the receipt directory. A retry with temporary receipt storage stopped at Gradle's `gradle-8.14-all.zip.lck (Operation not permitted)` before any tests. The retry's `reproduces` reflects the wrapper failure. Removed the temporary receipt storage. The witness JSON and patch remain for the lead.

F1=found
F2=found
F3=found
