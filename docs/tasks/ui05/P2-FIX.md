# UI05 P2-FIX: fixes from the Sol deep review of P2

Class: Normal (protocol step 6, fixer). Read `COMMON.md` in this folder
first; its rules and checks apply. Review follows
`~/.config/agents/review-protocol.md`. Base: `2065a01` on `ui05-port`.
Fix every instance of each class, search for siblings, and add tests. Do
not start P3 work. Do not change the number field rules in `Stepper.kt`.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/`.
The 0.4.8 code is `git show 2b73707:<path>`.

## Class 1 (P2): existing behavior silently dropped

### 1a. Today update row

`screens/TodayScreen.kt:66` dropped the launch update check, the row, the
progress and the install action (0.4.8 `TodayScreen.kt:69-112`, view model
`AppViewModel.kt:39,44`). `DESIGN.md:199` records the removal.

Decision: restore all of it in the new layout.
- `LaunchedEffect(Unit) { vm.checkUpdate() }` on Today, as before.
- When `vm.update` is `Updater.Result.Available`, the first row of the
  home-row group is a `HomeRow`: accent symbol circle (download or arrow
  up), name "Update to <tag>", state line "Tap to install" in accent, or
  "Downloading 42%" while `vm.updateProgress` is not null.
- Tap runs `vm.runUpdate(...)` with the same callbacks as 0.4.8 (needs
  permission -> Settings; error -> as before). No tap while progress runs.
- Put the row's text rule in `TodayRules.kt` with tests (available,
  downloading, none).
- Fix `DESIGN.md:199` and the matching SPEC.md text to describe the row.

### 1b. Bodyweight entry saves on invalid input

`components/RulerPicker.kt:189` ignores blank and zero input, and
`screens/TodayParts.kt:215` then saves the previous positive value (clear,
type 0, tap Add: saves 70).

Decision: restore the 0.4.8 guard (read the old `TodayScreen.kt` for the
exact rule; if it had none beyond "> 0", use "> 0"). The button is off
("Add" dimmed) while the entry is blank or invalid; a tap then saves
nothing. The label rule (Add / Update / Saved) gets an "invalid" case in
`TodayRules.kt` with tests.

### Sibling search

Compare every 0.4.8 Today, History and Import feature against the new
screens (actions, rows, navigation, labels, large-text fixes, widget
refresh, session edit/delete in History, plan overrides). Report each with
"kept at file:line" or "restored at file:line". Sol traced the rest as
kept; a new gap you find is a finding, fix it.

## Class 2 (P2): ruler animation overwrites newer input

`components/RulerPicker.kt:96` ignores outside values while the ruler
moves, and the fling keeps calling `change` (line 107). Fling, then type a
value: the fling replaces it. The accessibility adjust actions (line 146)
have the same defect.

Decision: one owner at a time. An outside value (typed text, an
accessibility adjust, a reset) cancels any running drag settle or fling
job first, then moves the ruler to that value without calling `change`.
Only the user's own drag or fling calls `change`. Put the decision ("does
this outside value cancel and move?") and the snap/offset math in pure
functions with tests.

## Class 3 (P3): test gaps

Add tests for: `screens/TodayRules.kt:39` completed-session summaries
(program, cardio, stretch; singular and plural); ruler movement and outside
input (class 2, including cancel during a fling and both input methods);
invalid-input save (class 1b); the update row text (class 1a).

## Done

`COMMON.md` checks pass. Report under 30 lines: commits, the command and
the tail of its output, the test count, each class with every instance and
the file:line of its fix and test, and the sibling list for class 1.
