# UI05 P5-FIX-NUMBERFIELD: number fields clip at large text

Class: Normal (protocol step 6, fixer), from the Sol deep review of P5C.
Read `COMMON.md` in this folder first. Base: `42126fd` on `ui05-port`.
Do not change the number field rules in `Stepper.kt` (`cleanTyped`,
`parseTyped`, stepping, hold-to-repeat) or any save path.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`
unless they start with `android/app/src/test/` or `docs/`.

## Finding (P2, layout broken at font scale 1.6)

`ui/components/Components.kt:266-315` (`NumberField`) reserves fixed
widths for the two step buttons and the unit, and the typed value gets
what is left of the caller's fixed width (`Modifier.width(176.dp)` or
`150.dp`). At font scale 1.6 the value, the unit or the placeholder is
cut. Seen: cardio Time, Avg heart rate, interval heart rate and watts
("bpm", "W" cut), Effort placeholder "1–10" cut. Sol's instances:
`ui/screens/CardioScreen.kt:59-64,85,87,95`, `ui/screens/WindowSheet.kt:76`.
The same widget is used by `SessionScreen.kt` (weight, reps) and
`StretchEditors.kt` (rep count, hold, routine seconds): fix every caller.
`FieldRow` (`Components.kt:~185-203`) gives the label a fixed 104/128 dp
column, which leaves the value too little room at large text.

## Decision

1. `FieldRow` stacks at large text, with the same threshold `ListRow`
   uses (`fontScale >= 1.5`, `Components.kt:159`): the label (and note)
   on its own line, the value below it, aligned to the end, with the
   full row width.
2. `NumberField` takes its base width as a parameter (`width: Dp`)
   instead of the callers' `Modifier.width(...)`. It lays out at
   `width × fontScale`, but never wider than the room it gets (so at 1.0
   nothing changes). Callers pass the width they pass today.
3. Last guard: when the value, unit or placeholder still does not fit
   (for example font scale 1.3, where rows do not stack yet), the field
   shrinks that text to fit, down to 75 percent of its size; the unit
   stays visible. A one-line value never wraps or clips.
4. Keep the 44 dp touch targets, the spoken labels, the compact mode for
   two fields in one row (`maxWidth < 176.dp`), and the session screen's
   look at font scale 1.0 (compare before and after there).

## Tests

Robolectric Compose tests at font scales 1.0, 1.3 and 1.6 on a 360 dp
wide screen, for every caller listed above (render the real field inside
a `FieldRow` with that caller's width, unit, decimals and placeholder;
fill it with the longest value the caller allows, and once empty to show
the placeholder): the value text, the unit and the placeholder are not
clipped (their laid-out width fits their measured width, and
`TextLayoutResult.hasVisualOverflow` is false), and both step buttons
keep at least 44 dp. The 1.6 cases fail on `42126fd`.

## Done

`COMMON.md` checks pass. Report under 25 lines: commits, the command and
the tail of its output, the test count, the file:line of each change,
which tests fail on the base with their failure lines, and a note on how
each caller looks at 1.0 (unchanged or not).
