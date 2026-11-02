# UI05 P1-FIX: fixes from the Sol deep review of P1

Class: Normal (protocol step 6, fixer). Read `COMMON.md` in this folder
first; its rules and checks apply. Review follows
`~/.config/agents/review-protocol.md`. Base: `73a6c2d` on `ui05-port`.
Fix every instance of each class, search for siblings, and add tests. Do
not start P2 work.

Paths below are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/ui/`.

## Class 1 (P2): stepper ignores the field's limits

`components/Components.kt:251` `stepBy` changes the shown text before the
caller clamps. At effort 10, "+" shows 11 while storage keeps 10.
Instances: `screens/Editors.kt:98,99,100,157` (sets, reps, rounds),
`screens/SessionScreen.kt:127` and `screens/CardioScreen.kt:89` (effort),
`screens/StretchEditors.kt:81,160,161` (seconds, reps),
`screens/WindowSheet.kt:55` (reminder minutes). Search for every other
`NumberField` / `TimeStepper` caller that clamps in its `onValueChange`.

Fix: `NumberField` takes `min` and `max` (defaults 0 and no limit). Each
shown step and each typed commit is bounded by them, so the field never
shows a value that storage does not keep. Move each caller's clamp into
these parameters. The pure step function (`stepValue` or a new one) takes
the bounds and has unit tests for both ends.

## Class 2 (P2): stepper ignores a numeric placeholder

`components/Components.kt:253` steps from 0 when the field is empty, even
when a numeric placeholder is shown. An empty kg field showing 75 gives
0.1 after "+". Instances: `screens/TodayScreen.kt:144`,
`screens/SessionScreen.kt:200,204`. Use the placeholder's number as the
base before zero, as `expo/src/components/ui/NumberField.tsx:65` does. Put
the base rule (held value, typed text, value, numeric placeholder, zero) in
a pure function with unit tests.

## Class 3 (P2): no K logo splash on Android 9 to 11

`res/values/themes.xml:3` gives a plain black window. Add a launch
background (layer-list drawable: black plus the K logo, centered, a size
that does not crop) for API 28 to 30, and switch the activity to the normal
theme when it starts, without breaking the API 31+ splash in
`values-v31/themes.xml`. Use the existing launcher foreground art; do not
add new image files unless one is missing.

## Class 4 (P2): test gaps

Add unit tests for the logic that has none yet. Where the logic sits in a
composable, extract it to a pure function or a small plain class first:
- stepper gesture saving: tap saves once; hold shows steps and saves once
  on release; drag-off or cancel; dispose while held; limits (class 1);
  a value update that arrives late from the database;
- `screens/WindowSheet.kt:82` time stepper the same way;
- `KhonNav.kt:137` tab shift direction;
- `screens/Common.kt:102` scroll offset to hairline alpha;
- `screens/Common.kt:176` confirm state (request, confirm runs the action
  once, cancel runs nothing).

## Done

`COMMON.md` checks pass. Report: commits, the command and the tail of its
output, the test count, and for each class every instance with the
file:line of its fix and the test that covers it.
