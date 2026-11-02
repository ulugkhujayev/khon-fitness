# UI05 P1-FIX2: defects found by the Sol verify of P1-FIX

Class: Normal (protocol step 6, second and last fix round). Read
`COMMON.md` in this folder first; its rules and checks apply. Review
follows `~/.config/agents/review-protocol.md`. Base: `5d6f226` on
`ui05-port`. Files: `ui/components/Stepper.kt`, `ui/components/Components.kt`,
`ui/screens/WindowSheet.kt`, and their tests. Nothing else.

The design below is decided. It removes special cases; do not add new ones.

## 1. Remove the echo suppression

`Stepper.kt:54` `arrived` and the `sent` queue hide real outside changes
(save 4, 5, 4; only 4 comes back; a later outside 5 is hidden).

Decision: delete the `sent` queue and `send`. `arrived(value)` returns
true unless a hold is in progress. `NumberField` (`Components.kt:250`) and
`TimeStepper` (`WindowSheet.kt:82`) go back to the rule from before P1:
show an outside value unless a hold is in progress or the typed text
already means that value (and keep the existing "focused and cleared"
exception). `stepBase` keeps the typed text before the value, so quick taps
still step from what the field shows.

Tests: save 4, 5, 4 with only 4 coming back, then outside 5 is shown;
the same number twice; an outside change while no hold is in progress is
shown; an outside change during a hold is not shown, and the hold's
release still saves.

## 2. A hold that leaves the screen saves nothing

`Components.kt:352` saves when disposal ends a hold. A removed row can
come back through the upsert (`Editors.kt:96-100`, `StretchEditors.kt:81,94`).

Decision: on dispose, end the hold without a save (as the Expo app does).
Lift and drag-off still release and save once; a pointer cancel releases
and saves once too. Give `PressFlow` a separate `dispose()` that clears
the hold and calls nothing.

Tests: hold then dispose saves nothing and a later press works; hold then
lift saves once; hold then drag-off saves once.

## 3. Release uses the current bounds

`Stepper.kt:42` `release` returns the held number without the current
bounds; the window time steppers change bounds while held
(`WindowSheet.kt:53,54,79,89`).

Decision: `release` bounds the held number with the current `min`/`max`,
and the field shows the bounded number it saves.

Test: hold to 1140, max changes to 1125, release saves and shows 1125.

## 4. One parser for save and blur

`Components.kt:276` and `Components.kt:297` parse differently: Sets `1`,
input `5..` saves the fallback 1 but blur shows 5.

Decision: one function `parseTyped(text): Double?` used by both. Text it
cannot read saves nothing; blur then shows the saved value.

Tests: `5..`, `.`, empty, `12.5`, `0`, a value over max.

## Done

`COMMON.md` checks pass. Report under 25 lines: commits, the command and
the tail of its output, the test count, and per item the file:line of the
change and its tests.
