# UI05 P4-FIX2: findings of the Sol verify 2 of P4

Class: Normal (protocol step 6, fixer). Every item touches stored data:
treat it with Risky care. Read `COMMON.md` in this folder first; its rules
and checks apply. Review follows `~/.config/agents/review-protocol.md`.
Base: `19305e4` on `ui05-port`. Do not start P5 work: leave
`saveEatingWindow`, the stretch writes and `ui/screens/StretchEditors.kt`
and `WindowSheet.kt` alone. Do not change the number field rules in
`Stepper.kt`. No Room schema change, no migration.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`.

## Design (lead decision, applies to every item)

The branch pattern stays: each multi-step write is one `@Transaction`
method on `KhonDao` (an interface; default method bodies may call pure
rules such as `PlanRules`, `PlanData` and `Seed`, passing `this`). The
repository method only delegates. Do not add a second transaction
mechanism (no `withTransaction` in the repository). A row deleted
meanwhile is not re-created.

## Class 1 (P2): check then act outside one transaction

1. `data/Repository.kt:175` `saveWeekPlan`: the reads (`weekPlanOnce`,
   `overridesOnce`, the date lists for `freezeFrom`) and the write
   (`dao.saveWeekPlan`) move into one DAO `@Transaction`. A manual override
   saved between the read and the write must stay manual (not replaced by
   an auto override).
2. `data/Repository.kt:185` `swapDays`: one DAO `@Transaction` builds
   `PlanData` from the `*Once()` reads, resolves both dates with
   `itemFor`, and writes both overrides.
3. `data/Repository.kt:110,111` `openWindowNow`, `closeWindowNow`: one DAO
   `@Transaction` each: insert the day row if missing (`INSERT OR IGNORE`
   with defaults), then a targeted `UPDATE window_days SET openedAt = ...,
   closedAt = NULL` (open) or `SET closedAt = ...` (close). Other columns
   of the day row are never written from a held copy.
4. `ui/screens/Editors.kt:135` slot pick: one DAO `@Transaction`
   `setSlotExercise(blockId, slot, exerciseId, newRowId)`: if the block is
   gone, write nothing; if a row for (blockId, slot) exists, update only its
   `exerciseId`; else insert the new row. The screen calls it instead of
   choosing from its displayed list. `firstPick`/`createBlock` stay.
5. Session creation commits the session before its logs:
   `startProgramSession` (`Repository.kt:212`), `startCardioSession`
   (`:233`), `importDraft` (`:270`). Add one DAO `@Transaction`
   `insertSessionWithLogs(session, setLogs, intervalLogs)`; all three use
   it. The reads that build the logs may stay before it.
6. Seeds: `seedIfEmpty`, `seedStretchesIfEmpty` (`Repository.kt:296,298`)
   and the checks inside `Seed.applyStretches` (`:347,351`) run their
   checks and writes in one DAO `@Transaction` each. `resetToSeed`
   (`:299`): `clearAll` plus all seed writes in one DAO `@Transaction`.

Search the branch for other check-then-act or multi-write sequences outside
one transaction (repository, view model, timer service, widgets, health
import). Fix each the same way, or list it with a reason it is safe.

## Class 2 (P2): whole-row interval writes from the cardio form

`ui/screens/CardioScreen.kt:40` holds interval copies, and
`saveCardioEdits` (`data/Dao.kt:183`) upserts them whole. An import that
changed watts, then a note edit and Save: the old watts return.

Decision: as the session columns. `CardioScreen` keeps the intervals as
opened (`remember(s.id)`) beside the edited list. A pure
`mergeIntervalEdits(current, opened, edited)` next to `mergeCardioEdits`
applies, per interval id, only the fields that differ between opened and
edited onto the current row; an interval deleted meanwhile is skipped; an
unchanged interval is not written. `saveCardioEdits` reads the current
intervals and writes the merge inside its transaction.

## Tests

In-memory Room, as `test/.../data/EditorWritesTest.kt` and
`SessionWritesTest.kt`. For each item, a sequence test of the race Sol
named (for example: read the plan, add a manual override, then save the
week plan: the override stays manual; import changes watts, then a note
Save keeps the new watts; a slot row added after the screen's list was
read is updated, not duplicated; open the window on a day whose row an
import changed keeps the other fields). Each fails on the base commit's
code where a sequence can show it. For pure atomicity (process death
between two writes) a sequence test cannot fail on the base: make the
second write fail inside the transaction (for example a duplicate primary
key or a constraint in a test-only call) and show nothing stays, or say in
the report which items have only the generated-code proof
(`build/generated/ksp/.../KhonDao_Impl.kt` shows the transaction wrapper).

## Done

`COMMON.md` checks pass. Report under 30 lines: commits, the command and
the tail of its output, the test count, each item with the file:line of
its fix and test, which new tests fail on the base commit, and the sibling
search result for class 1.
