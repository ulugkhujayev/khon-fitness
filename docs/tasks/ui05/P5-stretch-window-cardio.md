# UI05 P5: stretching, stretch demo and player, eating window, cardio

Class: Normal. Read `COMMON.md` in this folder first. Review follows
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
as they are.

## Contract

| # | Action | Data | Expected |
|---|---|---|---|
| 1 | Open Stretching | routines, some archived | Groups and drafts per `stretching.tsx` / `routine/[id].tsx` |
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

## Writes (lessons from P3 and P4)

These P5 screens still save whole rows from the copy they hold:
`ui/screens/StretchEditors.kt:69,70,81,92,93,149` (`saveRoutine`,
`saveRoutineStretch(es)`, `saveStretch`) and `ui/screens/WindowSheet.kt:46`
(`saveEatingWindow(n)`). Move each to targeted updates or a merge of the
changed fields onto the current row in one `@Transaction`, as
`saveExerciseEdits`, `saveCardioEdits` and the `setSet*` queries do. A row
deleted meanwhile is not re-created. Routine drafts follow the program
rule from P4-FIX item 1: the first stretch saves the routine as a real
routine, and the editor keeps its id in saved state. Deletes do not orphan
rows (a routine's stretches, a stretch used by routines or past stretch
sessions keeps their history). Test these against a real in-memory Room
database, as `test/.../data/EditorWritesTest.kt` does; each test fails on
the old whole-row write. No Room schema change, no migration.

Unit test the window state text (open, closed, off, crossing midnight if
the rules allow it), the kept-days count text, the run progress text and
segments, and the routine draft rules.

## Done

`COMMON.md` checks pass; root `DESIGN.md` "Stretch player", "Eating
window", stretch demo and routine editor parts, and `SPEC.md` (routine
drafts) updated; report as `COMMON.md` says.
