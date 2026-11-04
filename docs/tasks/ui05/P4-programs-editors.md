# UI05 P4: Programs tab, editors, modalities, week plan, settings

Class: Normal. Read `COMMON.md` in this folder first. Review follows
`~/.config/agents/review-protocol.md`. P1 added the shared kit; use it.

## Expo reference

- `expo/src/app/(tabs)/programs.tsx`, `library.tsx`, `exercise/[id].tsx`,
  `program/[id].tsx`, `block/[programId]/[blockId].tsx`, `modalities.tsx`,
  `modality/[id].tsx`, `week-plan.tsx`, `settings.tsx`.
- `expo/src/components/editors/*` (`ExerciseForm`, `ExerciseFields`,
  `exerciseChoices`, `BlockExerciseGroup`, `ColorGrid`, `ModalityForm`,
  `useDropOnLeave`), `expo/src/components/plan/*` (`DayPlanSheet`,
  `PlanItemSheet`).
- Rules: `expo/docs/DESIGN.md` sections "Lists" (icon tile rows, modality
  list, settings, week plan) and "Editors" (all).

## Kotlin targets

`ui/screens/ProgramsScreen.kt`, `ui/screens/Editors.kt`, `ui/screens/Common.kt`,
`ui/components/*`, `ui/AppViewModel.kt` and `data/Repository.kt` only for
the draft lifecycle (save on Done, no write before). No schema change.

## Contract

| # | Action | Data | Expected |
|---|---|---|---|
| 1 | Open the Programs tab | any | Rows that open a part of the app lead with a 30 px icon tile, grouped: Library (Exercises, Stretching, Modalities), Plan (Week plan), Settings on its own; programs listed as today |
| 2 | Open an exercise | used in a session | Fields in groups with titles (name and kind; Muscles; Details); Kind shows its value muted with a small lock, footnote says why |
| 3 | Open an exercise | not used | Kind editable |
| 4 | Edit a program | existing | Blocks numbered (index in a 24 px accent-tinted circle); Add block full-width 44 px tinted button 12 px under the list; Move up, Move down, Delete program as action rows (delete red, own group) |
| 5 | Edit a block | existing | Add exercise / Add second exercise tinted button; Move up, Move down, Delete block as action rows; Superset toggle with a muted footnote |
| 6 | Toggle Active | the active program | Fixed on; footnote says turning another program on moves it |
| 7 | New program, leave without Done and without a block | — | Nothing saved; no Delete shown while a draft |
| 8 | New program, tap Add block | — | Program saved so the block has a home; leaving without Done and without a block deletes it again; its Active choice waits for Done |
| 9 | New block, leave without adding an exercise | — | Nothing saved |
| 10 | New block, add the first exercise | — | Block saved |
| 11 | Delete exercise, program, block, exercise from a block | any | Confirm sheet first; cancel keeps everything |
| 12 | Open Modalities | some archived | Each row shows how many exercises it groups; footnote on what modalities are for; archived ones in their own group when shown |
| 13 | Edit a modality color | — | On the page under a Color title: two rows of six 34 px circles on a card, chosen one in a white ring |
| 14 | Open Week plan | any | Section title "5 training days · 2 rest days"; one row per weekday with an icon tile of what it holds in its color; today's weekday in accent; footnote that a change applies from today on (check the Kotlin plan rule really applies from today; if not, report it, do not change the rule) |
| 15 | Open Settings | any | Groups with icon tiles: Backup (Export, Import) with a footnote on what a backup holds or how the last export/import went (red on error); App (Version, Check for update with the APK updater's state); Reset to built-in data in red on its own with a footnote on what it deletes |
| 16 | Pick an exercise | block editor | `ChoiceSheet` with search and no-results (P1) |

Unit test the draft lifecycle rules (rows 7 to 10) as pure logic, the
week-at-a-glance text (singular and plural), and the kind-lock rule.

## Done

`COMMON.md` checks pass; root `DESIGN.md` "Lists" and "Editors" and
`SPEC.md` (drafts, confirm deletes) updated; report as `COMMON.md` says.
