# UI06 inventory question (Luna read)

Report only; change no file. Repo: `/Users/mirzohidkhon/deepvision/personal/khon-fitness`,
app code in `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`, tests in `android/app/src/test/`.

The next build rewrites the Today tab, the History tab (new Log and Progress
pages), and the Programs tab (renamed Plan), and fixes some bugs. For each item
below list every file and `file:line` range that holds it, every caller, and
every test that covers it. Mark each claim READ or GUESS.

1. Today tab: `TodayScreen.kt`, `TodayParts.kt`, `TodayRules.kt`: the week strip,
   `PlanSheet`, `TodayCard` (Start, Resume, "Start again"), `BodyweightSection`,
   `BodyweightEntry`, `RulerPicker`/`Ruler*` (who else uses the ruler?), `LineChart` users.
2. History tab: `HistoryScreen.kt`: calendar, day list, bodyweight chart,
   exercise progress (`progressFor`, `ProgressPoint`, e1RM), `muscleVolume`,
   `MuscleVolumeList`, cardio trends. Which data flows from `AppViewModel` it reads.
3. Programs tab: `ProgramsScreen.kt` and the routes it opens (`Routes.*` in
   `KhonNav.kt`): library, routines, modalities, week plan, settings. Where
   modalities are read or written in the UI (editors, pickers, colors on Today
   and History). Can the Modalities screen go away while the `modalities`
   table stays (who creates a modality today)?
4. Session flow: the Finish summary Save handler (double tap leaves a blank
   Today: find the `popBackStack`/navigate calls that run twice), the
   "New best" and set-done rules for 0 reps or 0 kg, `swapExerciseInSession`
   / `Dao.swapExercise` (a second swap after a session-only swap does not
   update the program; what data would fix it).
5. Cardio: manual entry save with empty fields marks the session done; the
   interval timer Stop then "Resume" opens a form. Files and lines.
6. Exercise library Merge (no confirm, no undo): files and lines, and what an
   undo would need.
7. Eating window history strip: `WindowRules.kt` `windowStrip`/`stripSummary`;
   where the date the window was turned on could come from (is it stored?).
8. Polish items: the Samsung battery note in `TimerScreen.kt`; label width
   rule in `Components.kt` (`FieldRow` or similar) that wraps "Avg heart rate";
   stretch player Skip without Back in `StretchScreen.kt`; colors `K.Dim`,
   `K.AccentInk` users for contrast.
9. Tests: list the screen tests under `android/app/src/test/` that touch items
   1-8, by file, with one line on what each asserts.

Output: grouped by item number, compact, under 150 lines.
