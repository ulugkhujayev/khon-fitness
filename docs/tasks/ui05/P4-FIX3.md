# UI05 P4-FIX3: redesign after the second failed verify (design alarm)

Class: Normal (protocol step 6, fixer), Risky care: item 1 writes stored
data. Read `COMMON.md` in this folder first; its rules and checks apply.
Base: `cabd65f` on `ui05-port`. Do not start P5 work. Do not change the
number field rules in `Stepper.kt`. No Room schema change, no migration.

Paths are under `android/app/src/` unless they start with `/`.

Sol's probes for both items are in `/private/tmp/ui05-p4-verify4-probes/`
(`P4Verify4CardioProbe.kt`, `P4Verify4RaceProbe.kt`, `init.gradle` adds
them to the test source set). Read them; turn their cases into real tests
in the repo (do not keep the reflection); then delete that folder.

## 1. Cardio interval edits: per-field edits, targeted writes

Why the old approach fails twice: the form keeps whole interval rows
(first a snapshot, then "the current row plus the user's change") and
diffs rows on Save. Any imported value that reaches the held row looks
like a user edit. Sol: import watts 220; edit heart rate; import watts
240; Save gives 220. Reverse: edit watts after an import changed heart
rate gives the old heart rate.

Decision: the form never holds interval rows for saving. It keeps only
the fields the user typed, per interval id:
`edits: Map<String, IntervalEdit>` where `IntervalEdit(avgHr: Edit<Int?>,
watts: Edit<Int?>)` and `Edit` tells "not touched" apart from "set to a
value, null included" (a small sealed type or a pair of flag and value;
your choice, one shape for both fields). The shown value of a field is
the edit when touched, else the current row's value.

Save: `saveCardioEdits(opened, edited, intervalEdits)` in its existing
`@Transaction`: for each touched field, one targeted
`UPDATE interval_logs SET <col> = :v WHERE id = :id` (no row: nothing).
Untouched fields are never written. Remove `mergeIntervalEdits` and every
row snapshot (`openedById`) it needed. Keep the edits in `remember(s.id)`
as now.

Tests (Robolectric screen tests as `CardioIntervalsKeptTest`, plus Room
tests for the DAO): Sol's two sequences; round added and removed (keep
the two current tests); a field typed then typed back to its first value
is still written (it was touched); clearing a field writes null; a row
deleted before Save is not re-created. Each new sequence test fails on
`cabd65f`.

## 2. Race helper: track transaction boundaries, select points by writes

Why the old approach fails: `db.inTransaction()` and "BEGIN excepted"
cannot see that a transaction ended and another began; the selector
`firstBeginAfterRead` needs a nested BEGIN, so a correct step with plain
statements never matches.

Decision: move `Race` out of `ConcurrentWritesTest` into its own test
file (`test/.../data/Race.kt`, internal) and test it directly. It no
longer calls `inTransaction()`. It reads only the statements the query
callback reports:
- a depth counter: `BEGIN...` adds 1, `END TRANSACTION` subtracts 1 (read
  Room 2.6's `QueryInterceptorDatabase` for the exact strings and whether
  each is reported before or after it takes effect; handle both
  `beginTransaction` and `beginTransactionNonExclusive`);
- after `arm`, the step is "one transaction" while its first statement is
  a BEGIN at depth 0 and the depth has not returned to 0 since;
- at the matched statement: depth 0 (counting a BEGIN being reported as
  not yet open) -> the competing write runs now, to the end, on another
  thread (raw SQL writers as now; join without a time limit that decides
  the result, fail only on a hang guard); depth >= 1 and one transaction
  -> it runs in `finish()`; depth >= 1 and not one transaction -> throw
  "the step is not one transaction" (a clear failure, never a pass).

Selectors: replace `firstBeginAfterRead` and `betweenWindowReadAndWrite`
with one `firstWriteAfterRead(table?)`: the first BEGIN, INSERT, UPDATE
or DELETE after a SELECT (of that table when given). It matches with or
without nested BEGINs.

Tests of the helper (`RaceTest`, raw SQL through
`db.openHelper.writableDatabase` as Sol's probe): two separate
transactions -> fails (throws or the write lands between them, never
deferred to `finish`); one transaction with nested BEGINs -> deferred;
one transaction without nested BEGINs (plain `execSQL`) -> matched and
deferred; no transaction -> runs at the match. Then: the 5 race tests in
`ConcurrentWritesTest` still fail on `19305e4` code with an assertion or
the "not one transaction" failure (copy them into a scratch worktree at
`../khon-fitness-wt/ui05-p4fix3-base`, run, remove the worktree) and pass
on the fix.

## Done

`COMMON.md` checks pass (full suite, assemble, lint). Report under 25
lines: commit, the command and the tail of its output, the test count,
each item with the file:line of its fix and tests, and which new tests
fail on `cabd65f` (item 1) and on `19305e4` (item 2), with their failure
lines. Delete `/private/tmp/ui05-p4-verify4-probes` at the end.
