# UI05 P3-FIX2: findings of the Sol verify of P3-FIX

Class: Normal (protocol step 6, fixer). Item 1 touches stored data: treat
it with Risky care. Read `COMMON.md` in this folder first; its rules and
checks apply. Review follows `~/.config/agents/review-protocol.md`. Base:
`9474acf` on `ui05-port`. Do not start P4 work. Do not change the number
field rules in `Stepper.kt`.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`.

## 1 (P1): the cardio form's whole-row save undoes the timer's time

Missed instance of the P3-FIX class "session fields lost by whole-row
writes". `ui/screens/CardioScreen.kt:37` keeps the draft it opened with;
`:45` saves every column. `ui/screens/HistoryScreen.kt:204` opens an
unfinished cardio session while its timer runs. Open the form, edit Note,
let the timer finish (it writes `timeSec`), then Save: the old `timeSec`
comes back.

Decision: the form saves only what the user changed, merged onto a fresh
row.
- New repository method `saveCardioEdits(opened: Session, edited: Session,
  intervals)`: in one `@Transaction`, read the current row; for each column
  the form can edit, take `edited`'s value when it differs from `opened`'s,
  else keep the current row's value; set `finishedAt` only when the current
  row has none (as `:45` does now); write the result; save the intervals.
- `CardioScreen.kt:45` calls it with the draft it opened with and the
  edited draft. `updateSession` then has no caller left: remove it.
- Room tests (in-memory, as `SessionWritesTest`): timer writes `timeSec`,
  then the form saves a note edit: both kept; the form edits Time, then
  saves: the form's time wins; the form saves, then the timer writes: the
  timer's time wins; an untouched form save changes nothing but
  `finishedAt`. The first test fails with the old whole-row save.

## 2 (P2): the open block resets after Back and Resume

`ui/screens/SessionScreen.kt:201` keeps the chosen block in the navigation
entry's saved state; Back (`:163`) removes the entry and Resume
(`TodayScreen.kt:48`) makes a new one, so the choice resets.
`OpenPickRestoreTest` only restores saved state, never this sequence.

Decision: keep the open-block choice in `AppViewModel` in a map keyed by
session id (process lifetime is enough; drop the entry when the session
finishes or is deleted). The screen reads and writes it there. Replace the
restore test with a test of the real sequence: choose a block, leave the
screen (dispose its composition), open the same session again: the choice
is kept; another session starts from its own default.

## 3 (P2): deferred focus has no regression test

`SessionScreen.kt:313,342` (pending focus target) has no test.

Decision: a Robolectric Compose test (`ui-test-junit4` is already a test
dependency) with a lazy list long enough that the target row starts off
screen. Cover: wrap to an off-screen row, focus lands once it is placed,
the target clears, a header tap cancels the target, a later recomposition
does not take focus again. If the screen's pieces are too coupled to test,
extract the pending-focus logic into a small composable or state holder and
test that, used by the screen unchanged.

## Done

`COMMON.md` checks pass. Report under 25 lines: commits, the command and
the tail of its output, the test count, each item with the file:line of
its fix and test, and which new tests fail on the base commit.
