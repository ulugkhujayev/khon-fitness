# UI05 P3-FIX: fixes from the Sol deep review of P3

Class: Normal (protocol step 6, fixer). Item 1 touches stored data: treat
it with Risky care. Read `COMMON.md` in this folder first; its rules and
checks apply. Review follows `~/.config/agents/review-protocol.md`. Base:
`4679242` on `ui05-port`. Fix every instance of each class, search for
siblings, and add tests. Do not start P4 work. Do not change the number
field rules in `Stepper.kt`.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`.

Owner decision (2026-11-02): the session date stays in the Date row at the
end of the page; no header date. That review item is closed.

## 1 (P1): session details lost by whole-row writes

`ui/screens/SessionScreen.kt:180,181` (finish sheet) and the page's
`SessionDetails` (`:150`, rows `:201-203`) save `s.copy(field = v)` of the
snapshot they hold. Two quick edits (effort, then note) overwrite each
other; Sol reproduced effort 7 lost to 2. Sibling:
`timer/TimerService.kt:395` writes `it.copy(timeSec = elapsed)` from a
fresh read, which can still race a detail edit.

Decision: no whole-row writes for single fields of a session that the user
or the timer edits in place.
- Add targeted DAO queries (no schema change): `UPDATE sessions SET effort
  = :v WHERE id = :id`, the same for `note`, the date column(s) the Date
  row edits, `timeSec`, and `finishedAt`. Add matching repository methods.
- `finishSession` keeps its set-log step (pending -> skipped) and then sets
  only `finishedAt`, in one `@Transaction`.
- The finish sheet, `SessionDetails` and `TimerService:395` use the
  targeted methods. Search for every other `updateSession(x.copy(` of a
  single field and move it too. `CardioScreen.kt:45` saves the whole cardio
  form on purpose; leave it.
- The finish sheet and the page show the same live values (both read the
  session flow), so an edit in one shows in the other.

Tests: add Robolectric and `androidx.room:room-testing` (or an in-memory
Room database through Robolectric) as test dependencies, and test against
a real in-memory database: effort then note then Save keeps both; note
then effort keeps both; Save after an edit keeps the edit and sets
`finishedAt`; pending sets become skipped; a timer `timeSec` write keeps a
concurrent effort. Each test fails on the base commit's write pattern.

## 2 (P2): History editing enters the live flow

`SessionScreen.kt:120-123` calls `afterCheck` while editing a saved
session in History; checking its last skipped set closes every block.

Decision: automatic block changes and automatic focus run only for a live
(running) session. In History edit mode a check changes the set and
nothing else. Put the guard in the pure rule (`afterCheck` takes `live`)
and test both modes.

## 3 (P2): focus requested before the target is composed

`SessionScreen.kt:101-105,112` requests focus 60 ms after starting the
scroll; a distant wrap target may not be composed yet, and the request is
lost.

Decision: a pending-focus target (set log id) in screen state. The target
row's field requests focus itself once it is composed and placed, then
clears the pending target. No timed delay.

## 4 (P2): content does not fit at large text or with the keyboard

`SessionScreen.kt:231,236` gives the progress row 18 dp for a 28.8 dp line
at font scale 1.6. The finish sheet content (`:171-183`) sits in the
non-scrolling sheet column (`ui/screens/Common.kt:123`).

Decision: the progress row's height and the fold distance come from the
measured text line height. The finish sheet content scrolls inside its own
column with IME padding, so Save stays reachable with the keyboard open.
Do not make the shared sheet column scroll (pick sheets hold lazy lists).

## 5 (P2): missing spoken names

The finish sheet's Effort and Note inputs (`:180,181`) have no spoken
names. Give every editable node in the sheet and the page its name; check
the page's Date, Effort and Note rows too.

## 6 (P2): test gaps

Besides the tests above: return to a running session keeps the open block;
the fold math at font scale 1.6.

## Done

`COMMON.md` checks pass. Report under 30 lines: commits, the command and
the tail of its output, the test count, each item with every instance and
the file:line of its fix and test, and the sibling search result for item 1.
