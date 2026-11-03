# UI06 P4: Plan tab (was Programs)

Class: Normal. Read `COMMON.md` first. Review follows `~/.config/agents/review-protocol.md`.
Builder: ui-builder. Prototype: `prototype.html`, Plan tab. Code: `UI06-COVERAGE.md` "Programs → Plan".

Targets: `ui/screens/ProgramsScreen.kt` (rename the screen to `PlanScreen`),
`ui/KhonNav.kt`, `ui/screens/Editors.kt` (cardio exercise editor: type picker,
modality creation), the tab label in the shell. Do not touch the exercise
Merge code in `Editors.kt` (P1 changes it); keep your edits in other functions.

## Contract

| # | Action | Data | Expected |
|---|---|---|---|
| 1 | Tab bar | — | Third tab reads "Plan". Route ids may stay; only labels and screens change. |
| 2 | Open Plan | — | Title "Plan" with a "+" button (sheet: Gym program, Cardio workout, Stretch routine, Exercise; each opens the existing create flow). Then sections: This week, Gym programs, Cardio workouts, Stretch routines, Library. Settings is no longer on this tab (it stays behind the gear on Today). |
| 3 | This week | — | Section title with "Edit" (opens the existing week plan screen). A 7-cell grid Mon-Sun with a short item name; today's cell tinted; a day with a one-day override shows a small dot and its override item. |
| 4 | Week plan edit | change Fri to Rest | Applies from today on. Past days keep their planned item (keep `saveWeekPlan`'s freeze of past days; add a test that a past week's planned count does not change). The screen says "Each weekday repeats every week. To change one day only, use Change on Today." |
| 5 | Gym programs | — | One row per program: dot (filled = active), name, "n blocks · m sets · Every Mon, Thu" (from the week plan; "Not in the week" when absent). Opens the program editor. |
| 6 | Cardio workouts | — | One row per cardio exercise: type pill in its color (the modality), name, plan line. Opens the cardio exercise editor. Archived ones sit under "Archived" at the end. |
| 7 | Cardio editor type | — | A row "Type" opens a picker of the existing modalities plus "New type…" (name + color, the old modality editor's fields). Under the row: "Changes the color and type of past sessions too." No Modalities screen and no route to it remains. |
| 8 | Stretch routines | — | One row per routine (dot, name, "9 stretches · 10 min"), opens the routine screen as today. |
| 9 | Library | — | Two rows: Exercises (strength only, count) and Stretches (count). |
| 10 | Exercises list | — | Search field; strength exercises grouped by muscle with section headers (no All/Strength/Cardio switch, no Muscle filter row); "+ Add" in the bar; archived at the end. Each row opens the exercise editor; the editor keeps its actions (rename, muscle, archive, merge, delete). |
| 11 | Stretches list | — | The existing stretch library list, reached from Library. |
| 12 | Scroll | — | Plan keeps its own scroll position across tab switches. |
| 13 | First use | no programs | Gym programs shows "No programs yet" and a "Create program" row; the week grid shows "Rest" cells. |

Keep every action the old Programs tab and Library had; list them in the
report and where each one lives now.
