# UI05 P4-FIX: fixes from the Sol deep review of P4

Class: Normal (protocol step 6, fixer). Items 1 to 4 touch stored data:
treat them with Risky care. Read `COMMON.md` in this folder first; its
rules and checks apply. Review follows `~/.config/agents/review-protocol.md`.
Base: `41dbb9e` on `ui05-port`. Fix every instance of each class, search
for siblings, and add tests. Do not start P5 work. Do not change the number
field rules in `Stepper.kt`. No Room schema change, no migration.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`.

## 1 (P2): a new program saved by Add block can be left behind

`ui/screens/EditorRules.kt:88,92,94`, `ui/screens/Editors.kt:78`. The
program saved by Add block is removed again only in `onCleared`; process
death leaves an empty program, and the restored editor loses the saved id,
so Done can create a second program.

Decision (lead): drop the "deleted again" rule. Add block on a new program
saves it as a real program, exactly as Done would (an empty name becomes
the default name Done uses; the Active choice is applied then too). From
that moment the editor is an existing program's editor: it shows Delete,
and leaving without Done keeps it. The editor keeps the program's id in
`rememberSaveable`, so a restored editor edits the same row. A new program
left before Add block or Done still writes nothing. Remove the cleanup
code that this makes dead. Update `SPEC.md` and `DESIGN.md` ("Editors":
"left without Done and without a block it is deleted again" goes).

Tests: Add block then leave keeps one program with that block; Add block,
then a restored editor (saved state) and Done updates the same row (no
second program); a new program left with no Add block and no Done writes
nothing.

## 2 (P2): editors write whole rows from an old snapshot

`ui/screens/Editors.kt:206,246` (exercise: Done, Archive) and `:293,301`
(modality: Done, Archive). An import that lands while the editor is open
is undone by Done or Archive.

Decision: as `saveCardioEdits` does: a repository method per editor that,
in one `@Transaction`, reads the current row, applies only the fields the
user changed (edited value differs from the opened value), and writes it;
Archive and Unarchive are targeted updates of that one column. The Kind
change rechecks "is this exercise used?" inside the same transaction and
keeps the stored Kind if it is used now. A row deleted meanwhile is not
re-created. Search every other editor and list screen for whole-row writes
from a held copy (stretch editors are P5, but list any you see).

Tests (in-memory Room): import changes a field, then Done of an unrelated
edit keeps both; Archive keeps a concurrent rename; Kind change refused
when a session started using the exercise after the editor opened; Done on
a deleted row writes nothing. Each fails with the old whole-row write.

## 3 (P2): backup merge can leave two active programs

`data/Repository.kt:276`, `ui/screens/EditorRules.kt:52`. Export with A
active, then make B active, then merge the old export: both are active and
both switches lock.

Decision: after every import, exactly one program is active when any
program exists. Merge keeps this device's active program and clears the
flag on imported ones. Replace takes the backup's active program; if the
backup has several, the first by the program list order; if it has none,
the first program. Do this inside the import transaction.

Tests: the merge sequence above; replace with zero, one and two active in
the backup.

## 4 (P2): backup errors escape the status message

`ui/screens/ProgramsScreen.kt:208,225,284`: a failed read during import or
restore of a saved copy throws past the error handling; a null output
stream reports "Backup exported."

Decision: every file operation in export, import and restore runs inside
one error path that shows the error footnote (red). A null stream is an
error. Put the result mapping in a pure function and test it.

## 5 (P2): test gaps

Besides the tests above: deleting an exercise, a block and a program keeps
the sessions and set logs that point at them, and History still shows
them (seed sessions and logs); full backup round trips (export, then merge
into an empty database, then replace) keep every table equal.

## 6 (P3): false archive text

`ui/screens/Editors.kt:265,298` says an archived modality leaves the
exercise Modality picker, but it stays selectable. Make the text true:
read what the picker does and state that, or say nothing about the picker.

## Done

`COMMON.md` checks pass. Report under 30 lines: commits, the command and
the tail of its output, the test count, each item with every instance and
the file:line of its fix and test, which new tests fail on the base
commit, and the sibling list for item 2.
