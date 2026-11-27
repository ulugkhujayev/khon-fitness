# HR01-P2 band import, R6 verification

**Merge.** The design closes F1, the timer admission ordering class, under the accepted D7c rule. Reviewed snapshot `dfe707b13d7f`.

- **Read.** The pending result can clear during draft preparation. The first statement in `importDraft` reads pending before it builds either row type. That value survives preparation and feeds the write check even if the timer commit clears the pending entry. An unclaimed, uncovered draft returns `TIMER_PENDING` before any insert. See `Repository.kt:418–436`, `Dao.kt:342–352`, and the W2 assertions in `ImportAdmissionTest.kt:253–270`.
- **Read.** In the R5 early Stop schedule, the import passes the write check after the service sets `done` and before it saves the pending entry. D7c puts the import first at this point. With the same draft and Stop time, both schedules save the band session, its source claim and its rounds, then commit the original app session's duration, timer bounds, completed run number and round times. The two schedules produce the same stored values. The lead's refusal stands. See `HR01-P2-import.md:73–74,96,126`, `TimerService.kt:348–359`, `TimerSnapshot.kt:101–104,141–144`, `Repository.kt:343–357,423–434`, and `Dao.kt:242–256,346–352`.

- **Ran.** `cmp` matched the supplied fix diff to the snapshot source diff. `shasum -a 256` returned `a26970e3abb5f0f756330e0d8a04c9aad718691fc3c001a594113f73f177666f`. This matches receipt `r-20261126234552-6844be`.
- **Read.** Step 6 uses the lead's witness run because this sandbox cannot write receipts. Witness record `F1-4.json` records `fixed` at `dfe707b13d7f`. Its receipt names the patched test commit `8c391f9eed7b` and logs `BUILD SUCCESSFUL in 12s`. The lead reported `BUILD SUCCESSFUL in 1m 31s`, 839 tests and 0 failures for the full suite. This review ran source and receipt checks only.

F1=closed
