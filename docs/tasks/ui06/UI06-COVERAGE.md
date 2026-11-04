# UI06 coverage list

Source: Luna inventory, 2026-11-03 (READ unless marked GUESS). Paths are under
`android/app/src/main/java/dev/mirzohidkhon/khonfitness/` unless they start with `android/`.

## Today
- `ui/screens/TodayScreen.kt:31-113`: reads plan, modalities, exercises, blocks, bodyweight, unfinished sessions, sessions, set logs; calls `WeekStrip`, `TodayCard`, `BodyweightSection`, `PlanSheet`.
- `ui/screens/TodayScreen.kt:117`: `PlanSheet`; also called from `ui/screens/HistoryScreen.kt:278`.
- `ui/screens/TodayParts.kt:52` week strip; `:88-122` TodayCard Start / Resume / "Start again"; `:170-220` bodyweight chart + entry.
- `ui/screens/TodayRules.kt:21` session timing/progress; `:90-125` bodyweight ruler, start value, change, save states.
- `ui/components/RulerRules.kt:9`, `ui/components/RulerPicker.kt:76`: only caller `TodayParts.kt:218` (GUESS: no other caller).
- `ui/components/LineChart.kt:29`: callers `TodayParts.kt:186`, `HistoryScreen.kt:224,230,254,259,265,271`.
- `ui/screens/TodayScreen.kt:47-48`: Resume opens the timer only when timer state matches, else the cardio form.

## History
- `ui/screens/HistoryScreen.kt:56`: reads sessions, plan data, modalities, bodyweights, set logs, exercises, interval logs, stretch sessions (`ui/AppViewModel.kt:18`).
- `:80-205` month calendar + day list; `:285-290` `dayDots`, `sessionColor`.
- `:218-224` bodyweight chart; `:226-234` exercise progress; `:366-375` `ProgressPoint`, `progressFor` (e1RM).
- `:293-309` `muscleVolume`; `:338-363` `MuscleVolumeList`.
- `:242-272` cardio trends (pace units by swim modality `:247-254`; HR, interval HR, time charts `:256-271`).

## Programs → Plan, modalities
- `ui/screens/ProgramsScreen.kt:39-60`: programs, Library, Routines, Modalities, Week plan, Settings.
- `ui/KhonNav.kt:62` routes; `:109-128` route host.
- Modalities read at `ProgramsScreen.kt:40-43,66-68,103-129,140-142`; exercise editor modality picker `ui/screens/Editors.kt:188,226,265`; modality editor `Editors.kt:286-325`; colors `ui/screens/Common.kt:48-56` used by Today `TodayParts.kt:52-118` and History `HistoryScreen.kt:107-172,200,276`.
- Data: `data/Entities.kt:46` Modality entity; seed `data/Repository.kt:377`; backup import `data/Dao.kt:475`. Decision: keep the table; remove only the Modalities screen. Creating a custom type moves into the cardio workout editor.

## Session flow
- Finish summary `ui/screens/SessionScreen.kt:123`; Save calls `finishSession` + unguarded `nav.popBackStack()` at `:190` (double tap pops Today).
- Set toggles DONE with no weight/reps check `SessionScreen.kt:114-120`; summary rules `ui/screens/SessionRules.kt:10`.
- Swap `SessionScreen.kt:164-166` → `data/Dao.kt:229-237` matches the program row by exercise id. Decision: match the program row by the set log's `blockIndex` + `slot` (`data/Entities.kt:100-119`), no schema change.

## Cardio
- Manual save `ui/screens/CardioScreen.kt:47-52`, no required-field check; `data/Repository.kt:44-50` `mergeCardioEdits` sets `finishedAt`.
- Timer Stop `ui/screens/TimerScreen.kt:82-84` → `timer/TimerService.kt:134,265-286` finishes and posts a result; `TimerScreen.kt:62-69` navigates to the form.

## Exercise merge
- `ui/screens/Editors.kt:248,271-273`: pick target merges and pops, no confirm.
- `data/Dao.kt:42-46`: moves set logs, cardio sessions, program exercises, week plan, overrides; deletes source.
- Decision: confirm sheet first; Undo snackbar for the rest of the app session, from an in-memory snapshot of the moved row ids and the deleted exercise row. No schema change.

## Eating window
- `ui/components/WindowRules.kt:12-28` strip + summary; `WindowSheet` calls at `:79-84`.
- `data/Stretching.kt:62-83` EatingWindow has no enable date; `data/Dao.kt:404-408` enable writes the boolean.
- Decision: store the first-enable date in DataStore preferences (key `window_since`); fallback for an existing user = the earliest WindowDay date, else today. Days before it are not counted.

## Polish
- Samsung note `ui/screens/TimerScreen.kt:103-106`.
- Label width `ui/components/Components.kt:189-215` (104 dp below 400 dp width); "Avg heart rate" `CardioScreen.kt:59-62`.
- Stretch Skip without Back `ui/screens/StretchScreen.kt:104-113`; `timer/TimerState.kt:41-44`.
- Colors `ui/theme/Theme.kt:35-41`; `K.Dim`/`K.AccentInk` users listed by Luna (Components, Kit, LineChart, Shell, StretchParts, Common, History, Import, Programs, Session, Today).

## Tests that touch these areas
- `TodayRulesTest`, `RulerRulesTest`, `RulerMotionTest`, `DayDotsTest`, `DeletesKeepHistoryTest` (calls `progressFor`), `VolumeSharesTest`, `EditorRulesTest` (modality counts), `EditorWritesTest` (modality saves), `SessionRulesTest`, `SessionWritesTest` (swaps), `CardioEditsTest` (untouched save sets finishedAt), `CardioIntervalsKeptTest`, `WindowRulesTest`, `NumberFieldFitTest`, `StretchRulesTest`, `TimerStateTest`.
- No test today for: double-tap Save, merge/undo, timer Stop → form, Samsung note, contrast.
