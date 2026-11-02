# UI05 P5: stretching, stretch demo and player, eating window, cardio

Class: Risky (it rewrites stored data; protocol steps 1-7). Read `COMMON.md` in this folder first. Review follows
`~/.config/agents/review-protocol.md`. P1 added the shared kit; use it.

## Expo reference

- Stretching: `expo/src/app/stretching.tsx`, `stretches.tsx`,
  `routine/[id].tsx`, `stretch-editor/[id].tsx`, `stretch/[routineId].tsx`,
  `expo/src/components/stretch/*` (`StretchDemonstration`, `StepTimeline`,
  `DemoFrame`, `DemoControl`, `DemoPoster`, `PlayerButton`,
  `RoutineProgress`, `RoutineOrderList`, `RoutineStretches`,
  `StretchForm`, `StretchPlayerView`).
- Eating window: `expo/src/app/window.tsx`,
  `expo/src/components/today/WindowSheet.tsx`, `WindowDayBar.tsx`,
  `ui/TimeStepper.tsx`.
- Cardio: `expo/src/app/cardio/[id].tsx`, `timer/[id].tsx`,
  `expo/src/components/cardio/*`.
- Rules: `expo/docs/DESIGN.md` sections "Stretch player", "Eating window",
  "Editors" (routine drafts and order rows).

## Kotlin targets

`ui/screens/StretchScreen.kt`, `StretchEditors.kt`, `WindowSheet.kt`,
`CardioScreen.kt`, `TimerScreen.kt`, `ui/components/StretchDemonstration.kt`,
`ui/components/*`. The timer service, cue gate, scheduler and widgets stay
as they are, except the two narrow changes in "Writes".

## Contract

| # | Action | Data | Expected |
|---|---|---|---|
| 1 | Open Stretching | routines, one active or none; stretches, some archived | Layout per `stretching.tsx` / `routine/[id].tsx`. Routines have no archive state (schema); archived stretches are hidden from the library and the pick sheet |
| 2 | Edit a routine's order | routine with stretches | Order rows carry up, down and remove as bare symbols with 44 px targets; remove asks first |
| 3 | New routine, leave without Done and without a stretch | — | Nothing saved |
| 4 | New routine, add the first stretch | — | Routine saved |
| 5 | Open a stretch demo | any figure | Play/Pause and Change view as 40 px round controls over the image's bottom corners, white symbol on black at 45 percent |
| 6 | Watch the demo | multi-step figure | Step timeline: 26 px numbered nodes on a 3 px line, titles under nodes footnote muted; done nodes accent with white numbers, shown node in a 34 px accent-tinted halo with its title white semibold, later nodes Surface3 with muted numbers; line Surface2 filling in accent as the pose moves into each step |
| 7 | Run the stretch player | routine | Stop (X) and Skip (forward-end) as 40 px round controls, white on white 18 percent, readable on black and green; 10 px under the bar one 3 px segment per stretch and side (done white 60 percent, current white, rest white 18 percent) and "3 of 12 · 6:30 remaining"; line under the name "Left · Hold · 0:40"; the ready screen shows no status line of its own |
| 8 | Open the eating window sheet | open / closed / off | Card with the state in words ("Open · closes in 2 h 10 m", 10 px dot green when open) over an 8 px day bar 00:00 to 24:00, fixed hours green with times under the ends, a white mark at now; Open now / Close now as a tinted button; On, Opens, Closes, Remind before on an elevated group; then the last 28 days: four weeks of seven 12 px dots on an elevated card (today white) with "Kept the hours on 20 of 27 days" and what a ring means |
| 9 | Change window times | time steppers | P1 stepper behavior; spoken labels "Opens 15 minutes earlier" stay |
| 10 | Open cardio form and interval timer | any | Layout per the Expo cardio components where they differ from Kotlin; timer behavior unchanged |
| 11 | Delete a stretch or routine | any | Confirm sheet first |
| 12 | Delete a stretch | built-in, or used by a routine row at confirm time | Refused; the editor shows why ("Built-in stretches cannot be deleted." / "In use by N routine rows."); nothing is deleted |
| 13 | Delete a routine | with stretches and past stretch sessions | Routine and its routine rows go; stretch sessions stay (they hold the routine name and totals, no stretch ids) and History shows them |
| 14 | Stretch editor | new or existing | Name, Guide, Per side, Reps, Rep count, Time/Hold, Muscles, Cue, Link, Watch the reference: every 0.4.8 field kept; Done on a new stretch with a blank name saves nothing |
| 15 | Stretch library | search text | Filters by name; "Nothing matches" when empty |
| 16 | Routine Done | new routine, no stretch | Saves the routine (name, Active) with no stretches |
| 17 | Routine Active | switch On / Off | On makes it the only active routine; Off leaves none active (as 0.4.8; Today falls back to the first routine, the widget shows none) |
| 18 | Import | backups with zero, one or two active routines | Merge keeps this phone's active routine and clears the mark on imported ones; replace keeps the backup's first active routine by sortOrder, else none. At most one active after any import |
| 19 | Stretch run result | the same result recorded twice (restore after process death) | One stretch session, unchanged by the second record |
| 20 | Stretch run | routine deleted during a live run, then finish in the live service | One stretch session with the captured name; no routine or routine rows re-created (a restored snapshot of a deleted routine is dropped: attack case P4) |
| 21 | Stretch demo | play, pause, change view, pick a step | Demo playback never starts the run clock or writes anything |
| 22 | Cardio form | exercise fields removed after values were stored | Stored fields, swim laps math, interval inputs and the delete confirm stay as in 0.4.8 (`git show 2b73707:<path>`); the timer's background-permission action stays |

## Writes: one set of guarded commands (closed design)

Rule: a P5 screen never writes a row it holds. Every stored write from the
stretch, routine, window and player paths goes through one of the DAO
commands below; each is one `@Transaction` (or one SQL statement), reads
what it checks inside itself, and writes nothing when its row is gone. The
repository exposes only these, and removes `saveStretch(Stretch)`,
`saveRoutine(StretchRoutine)`, `saveRoutineStretch(es)`,
`saveEatingWindow(EatingWindow)` and `saveStretchSession`, so a whole-row
write from a held copy does not compile. Seed and import keep their own
transactions. No Room schema change, no migration.

| Command | Does |
|---|---|
| `createStretch(s)` | insert; refused when the name is blank |
| `saveStretchEdits(opened, edited)` | merge the fields that differ onto the current row (as `saveExerciseEdits`); deleted row: nothing |
| `deleteStretchIfUnused(id): Boolean` | in one transaction: refuse if built-in or any routine row uses it; else delete |
| `createRoutine(r, firstStretchId?, rowId?): Boolean` | when a first stretch is given and that stretch is gone: write nothing, return false. Else insert the routine with `sortOrder` = max + 1 read inside; when `r.active`, `active = (id = :id)` for all; insert the first routine row |
| `setRoutineName(id, name)` | targeted update |
| `setRoutineActive(id, on)` | row missing: nothing; on: `active = (id = :id)`; off: that row only |
| `deleteRoutine(id)` | already one transaction (`deleteRoutineWithStretches`) |
| `addRoutineStretch(routineId, stretchId, rowId)` | routine or stretch gone: nothing; `sortOrder` = max + 1 of that routine's rows, read inside |
| `setRoutineStretchSeconds(rowId, s)` | targeted update |
| `moveRoutineStretch(rowId, up)` | read the routine's rows inside, ordered by `sortOrder, id`; swap `sortOrder` with the row's current neighbour in that order (not the neighbour the screen showed) by two targeted updates; when both have the same `sortOrder`, first renumber the routine's rows 0..n-1 in that order; no neighbour or row gone: nothing |
| `removeRoutineStretch(rowId)` | delete by id |
| `recordStretchSession(s)` | `INSERT OR IGNORE`: a session id is written once; a replay never overwrites or re-creates a changed row |
| `setWindowEnabled(on)`, `setWindowRemind(min)` | one transaction: insert the default settings row (id 1) when missing, then a targeted update |
| `setWindowStart(m)`, `setWindowEnd(m)` | one transaction: insert the default row when missing; read the current row; write only when the hours keep at least 60 minutes between them (the second of two crossing edits is refused) |
| `openWindowNow`, `closeWindowNow` | from P4-FIX2 (targeted, one transaction) |
| `backupSnapshot(): Backup` | export reads every table inside one `@Transaction` |

Ordering: every list of routines or routine rows sorts by `sortOrder`,
then `id`, so ties have one order everywhere.

Activation: the last command wins. `createRoutine` with Active after
`setRoutineActive(B)` leaves only the new routine active; the reverse
order leaves only B.

Import and seed are the only bulk writers. They keep their own
transactions and add these repairs inside them:
- `importBackup`: routines normalized per row 18; the settings row is
  inserted with defaults when the backup has none and none exists;
  routine rows whose routine or stretch is missing are deleted. Import
  overwrites stretch sessions with the backup's rows on purpose (the one
  exception to insert-once); a later `recordStretchSession` of the same id
  then changes nothing.
- After a successful import, restore or reset, the caller runs
  `WindowScheduler.reschedule` and the widget refresh (`ProgramsScreen`
  backup paths and Reset), so alarms and widgets show the new settings.
- Seed (`seedIfEmpty`, `seedStretchesIfEmpty`, `resetToSeed`) is one
  transaction each per P4-FIX2.

Guard: a JVM unit test reads the Kotlin sources outside `data/` and fails
when one calls `.dao()`, or a repository method that takes a whole
`Stretch`, `StretchRoutine`, `RoutineStretch`, `StretchSession` or
`EatingWindow` row (only `createStretch`, `createRoutine` and
`recordStretchSession` take a row, and only for new ids).

Routine drafts follow the program rule from P4-FIX item 1: a new routine
(`new`) keeps name and Active in `rememberSaveable`; leaving without Done
and without a stretch writes nothing; the first stretch or Done calls
`createRoutine`, and from then the editor keeps the id in
`rememberSaveable` and edits the existing routine.

Allowed outside the P5 screens (narrow, nothing else in those files):
`timer/TimerService.kt` `recordTimer` calls `recordStretchSession`;
`window/WindowScheduler.kt` `reschedule` runs under one process-wide
`Mutex`, so an older reschedule cannot finish after a newer one (the
receiver already checks `enabled` at delivery).

## Attack cases (from `P5-ATTACK.md`)

Each becomes an in-memory Room test (as `test/.../data/EditorWritesTest.kt`)
or a pure-rule test, and each Room test fails on the old write where a
sequence can show it.

- E1: open a stretch; import renames it; edit only Cue; Done: both kept. Delete the row first: Done writes nothing.
- E2: open the delete confirm of an unused custom stretch; another write adds it to a routine; confirm: refused. Reverse order: `addRoutineStretch` of the deleted stretch writes nothing.
- E3: a routine uses a stretch and has a past session: delete the stretch is refused; remove the row, delete stretch and routine: the session is unchanged.
- E4: new routine, set name and Active, leave: no rows. Again with Done: one routine with those values, the only active one if Active.
- E5: new routine, add the first stretch, restore saved state, Done: same routine id, one routine row.
- E6: open Add stretch; delete the routine; pick a stretch: no routine row, no routine. Same for seconds and order changes: nothing written.
- E7: import changes a row's seconds; move another row: the imported seconds stay. Rows A,B,C,D; remove B; then move C up (screen still shows B above C): C swaps with A, the current neighbour.
- E8: move and remove of the same row, both orders: no row re-created, no partial swap, seconds unchanged.
- E9: rows with equal `sortOrder` (from an import): a move renumbers, then swaps; the result order is strict.
- E10: routines with orders 0,1,2; delete order 1; create a routine: it gets 3.
- E11: `createRoutine` with a first stretch deleted meanwhile: no routine, no row, no activation change.
- D1: hold routine A's editor; make B active; rename A: B stays the only active. `setRoutineActive` of a deleted id: no flag changes. Activate B, then create active A: only A. Create active A, then activate B: only B.
- D2: export with A active; make B active; merge the export: one active (B). Replace with zero, one, two active in the backup: none, that one, the first by `sortOrder`.
- D3: export is one snapshot (`backupSnapshot`); round trip through merge and replace keeps every table equal and leaves no routine row whose routine or stretch is missing.
- D4: import a backup with a routine row whose routine is missing: the row is dropped; the rest imports.
- D5: replace with a backup without `eatingWindow`: a default settings row exists afterwards, and `setWindowEnabled(true)` turns it on.
- D6: record S; import a changed S; record S again: the imported S stays.
- P1: record a result after its routine was deleted: one session with the captured name; no routine re-created.
- P2: record the same result twice: one unchanged session. Delete it (by the History delete, an import replace or a reset), record again: it comes back once. Accepted limit: the timer clears its snapshot after recording, so this needs a replay of a stale snapshot; say so in `DESIGN.md`.
- P4: a restored snapshot (after process death) whose routine was deleted meanwhile is dropped by the existing `stillOpen` gate (`timer/TimerService.kt:400`); no session is recorded. Accepted limit, stated in `DESIGN.md`; row 20 covers only a run that finishes in the live service.
- P3: skip every stretch before 30 s: no session (existing rule in `recordTimer`, test it).
- W1: hold the sheet; import changes the hours; toggle On: only `enabled` changes. From 12:00-20:00, set start 19:00 and end 13:00 in both orders: the first is written, the second refused.
- W5: import different hours with the window on: `reschedule` runs after the import (test the backup action's result mapping calls it, or a pure rule that lists the follow-up actions).
- W2: covered by P4-FIX2; keep its tests passing.
- W4: window state text at a boundary and across midnight per `EatingWindowRules`; the kept-days count excludes today; fixed hours never cross midnight (the steppers' ranges).

Unit test the window state text (open, closed, off), the kept-days count
text, the run progress text and segments, and the routine draft rules.

## Coverage list

Contract rows 1-22 and attack cases; files: `ui/screens/StretchScreen.kt`,
`StretchEditors.kt`, `WindowSheet.kt`, `CardioScreen.kt`, `TimerScreen.kt`,
`TodayScreen.kt` (routine fallback), `ProgramsScreen.kt` (backup and
Reset follow-ups), `ui/components/StretchDemonstration.kt`; write paths:
the command table, `data/Dao.kt` `importBackup`, `clearAll`, seed,
`backupSnapshot`, `data/Repository.kt` stretch, routine, window and
stretch-session methods, `timer/TimerService.kt` `recordTimer` and
`stillOpen`, `window/WindowScheduler.kt` `reschedule` and the receiver;
readers: `widget/TodayWidget.kt`, `widget/WindowWidget.kt`.

## Done

`COMMON.md` checks pass; root `DESIGN.md` "Stretch player", "Eating
window", stretch demo and routine editor parts, and `SPEC.md` (routine
drafts) updated; report as `COMMON.md` says.
