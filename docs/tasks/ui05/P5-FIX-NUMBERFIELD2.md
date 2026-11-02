# UI05 P5-FIX-NUMBERFIELD2: the longest accepted values still clip

Class: Normal (protocol step 6, fixer, second and last fix round before a
design alarm). Read `COMMON.md` and `P5-FIX-NUMBERFIELD.md` in this folder
first. Base: `20203f5` on `ui05-port`. Do not change the number rules in
`Stepper.kt` or any save path. Another builder fixes the routine draft in
`StretchEditors.kt`, `StretchRules.kt` and `AppViewModel.kt` on
`ui05-port`; do not edit those files.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`
unless they start with `android/app/src/test/` or `docs/`.

## Finding (Sol verify of P5-FIX-NUMBERFIELD)

`ui/components/Components.kt:302`: the shrink stops at 75 percent
(`MIN_FIT`). Values the fields accept (six whole digits, plus the
field's decimals, `Stepper.kt` `cleanTyped`) still clip: at font scale
1.3, cardio Time `999999.9` needs 265 px and gets 179 px; some cases clip
at 1.0 too. Instances: `ui/screens/CardioScreen.kt:59,60,61,62,64,85,87`;
`ui/screens/SessionScreen.kt:352,358` (also the weight and reps
placeholders); `ui/screens/Editors.kt:220,233`. The test
(`NumberFieldFitTest`) used the longest typical value, not the longest
accepted one.

## Decision

- When no step of `FIT_STEPS` fits, the value (or placeholder) and unit
  scale to exactly the size that fits the room (measured, as now), with
  no floor. Typical values keep their size (they fit at 100 or 75
  percent); only extreme values get small. The unit stays visible.
- Find every caller of `NumberField` and of any other widget that shows a
  typed number in a fixed box (Sol names `Editors.kt:220,233`); apply the
  same rule there, or route it through `NumberField`.

## Tests

`NumberFieldFitTest`: for every caller, the longest value the field
accepts (six whole digits plus its decimals, from its `decimals` and
`max`, whichever is shorter) and its placeholder, at font scales 1.0, 1.3
and 1.6 on a 360 dp screen: nothing clips (laid-out width fits measured
width; no visual overflow); step buttons keep their targets. Those cases
fail on `20203f5`.

## Done

`COMMON.md` checks pass. Report under 20 lines: commits, the command and
the tail of its output, the test count, the file:line of each change,
which tests fail on the base with their failure lines, and how typical
values look at 1.0 (unchanged or not).
