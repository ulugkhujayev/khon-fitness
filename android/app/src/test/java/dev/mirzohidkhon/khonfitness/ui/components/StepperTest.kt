package dev.mirzohidkhon.khonfitness.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepperTest {
    /** A number widget's − and + as NumberField wires them: the shown text is the typed text the next step reads. */
    private class Field(var value: Double?, step: Double, min: Double = 0.0, max: Double = Double.POSITIVE_INFINITY, val placeholder: String? = null) {
        val stepper = Stepper(step, min, max)
        val saves = mutableListOf<Double>()
        var text = show(value)

        fun press(sign: Int) = PressFlow(
            onTap = { show(stepper.step(sign, true, text, value, placeholder)) },
            onRepeat = { show(stepper.step(sign, false, text, value, placeholder)) },
            onRelease = { stepper.release(value)?.let(saves::add) },
        )

        fun tap(sign: Int) = press(sign).run { endEarly(lifted = true); end() }

        fun hold(sign: Int, repeats: Int): PressFlow = press(sign).also { p -> p.hold(); repeat(repeats) { p.repeat() } }

        private fun show(s: Stepper.Step) { text = show(s.shown); s.save?.let(saves::add) }

        /** The text the field shows, like fmt with one decimal, without loading the Compose kit. */
        private fun show(v: Double?) = when { v == null -> ""; v % 1.0 == 0.0 -> v.toLong().toString(); else -> v.toString() }
    }

    @Test fun boundKeepsInsideBothEnds() {
        assertEquals(10.0, bound(11.0, 1.0, 10.0), 0.0)
        assertEquals(1.0, bound(-3.0, 1.0, 10.0), 0.0)
        assertEquals(0.0, bound(-0.5), 0.0)
        assertEquals(3.0, bound(5.0, 10.0, 3.0), 0.0)
    }

    @Test fun stepValueStopsAtTheFieldsLimits() {
        assertEquals(10.0, stepValue(10.0, 1, 1.0, 1.0, 10.0), 0.0)
        assertEquals(1.0, stepValue(1.0, -1, 1.0, 1.0, 10.0), 0.0)
        assertEquals(1.0, stepValue(0.0, 1, 1.0, 1.0, 10.0), 0.0)
        assertEquals(600.0, stepValue(598.0, 1, 5.0, 5.0, 600.0), 0.0)
    }

    @Test fun stepBaseOrderIsHeldTypedValuePlaceholderZero() {
        assertEquals(9.0, stepBase(9.0, "8", 7.0, "6"), 0.0)
        assertEquals(8.0, stepBase(null, "8.", 7.0, "6"), 0.0)
        assertEquals(7.0, stepBase(null, "", 7.0, "6"), 0.0)
        assertEquals(75.0, stepBase(null, "", null, "75"), 0.0)
        assertEquals(62.5, stepBase(null, "", null, "62.5"), 0.0)
        assertEquals(0.0, stepBase(null, "", null, "1–10"), 0.0)
        assertEquals(0.0, stepBase(null, "", null, null), 0.0)
    }

    @Test fun placeholderCountsOnlyAsAPlainNumber() {
        assertEquals(75.0, placeholderNumber("75")!!, 0.0)
        assertNull(placeholderNumber("1–10"))
        assertNull(placeholderNumber("75."))
        assertNull(placeholderNumber(""))
        assertNull(placeholderNumber(null))
    }

    @Test fun emptyFieldStepsFromItsPlaceholder() {
        val kg = Field(null, 0.1, placeholder = "75")
        kg.text = ""
        kg.tap(1)
        assertEquals("75.1", kg.text)
        assertEquals(listOf(75.1), kg.saves)
    }

    @Test fun emptyEffortStepsIntoItsRange() {
        val effort = Field(null, 1.0, 1.0, 10.0, placeholder = "1–10")
        effort.text = ""
        effort.tap(-1)
        assertEquals(listOf(1.0), effort.saves)
    }

    @Test fun tapSavesOnce() {
        val f = Field(3.0, 1.0)
        f.tap(1)
        assertEquals("4", f.text)
        assertEquals(listOf(4.0), f.saves)
    }

    @Test fun holdShowsStepsAndSavesOnceOnRelease() {
        val f = Field(3.0, 1.0)
        val p = f.hold(1, 3)
        assertEquals("6", f.text)
        assertEquals(6.0, f.stepper.held!!, 0.0)
        assertTrue(f.saves.isEmpty())
        p.end()
        p.end()
        assertEquals(listOf(6.0), f.saves)
        assertNull(f.stepper.held)
    }

    @Test fun dragOffBeforeTheHoldDoesNothing() {
        val f = Field(3.0, 1.0)
        f.press(1).run { endEarly(lifted = false); end() }
        assertEquals("3", f.text)
        assertTrue(f.saves.isEmpty())
    }

    @Test fun dragOffOrCancelDuringAHoldSavesTheShownStepOnce() {
        val f = Field(3.0, 1.0)
        val p = f.hold(-1, 2)
        // A drag off the button and a cancel both end the hold through PressFlow.end.
        p.end()
        p.repeat()
        assertEquals("1", f.text)
        assertEquals(listOf(1.0), f.saves)
    }

    @Test fun disposeWhileHeldSavesTheShownStep() {
        val f = Field(20.0, 5.0, 5.0, 600.0)
        val p = f.hold(1, 2)
        // repeatPress calls end from `finally` when the button leaves the screen mid-hold.
        p.end()
        assertEquals(listOf(30.0), f.saves)
    }

    @Test fun stepsNeverShowAValuePastTheLimits() {
        val effort = Field(9.0, 1.0, 1.0, 10.0)
        effort.tap(1)
        effort.tap(1)
        assertEquals("10", effort.text)
        assertEquals(listOf(10.0, 10.0), effort.saves)

        val sets = Field(3.0, 1.0, 1.0, 10.0)
        sets.hold(-1, 6).end()
        assertEquals("1", sets.text)
        assertEquals(listOf(1.0), sets.saves)

        val remind = Field(110.0, 5.0, 0.0, 120.0)
        remind.hold(1, 9).end()
        assertEquals(listOf(120.0), remind.saves)
    }

    @Test fun typedNumbersAreBounded() {
        val s = Stepper(1.0, 1.0, 10.0)
        assertEquals(10.0, s.typed(15.0, 3.0)!!, 0.0)
        assertEquals(1.0, s.typed(0.0, 3.0)!!, 0.0)
        assertNull(s.typed(null, 3.0))
        assertEquals(10.0, s.bounded(15.0), 0.0)
    }

    @Test fun lateDatabaseValueDoesNotPullTheFieldBack() {
        val f = Field(3.0, 1.0)
        f.tap(1)
        // The write of 4 has not come back: the next tap steps from the shown 4, not the stored 3.
        f.tap(1)
        assertEquals("5", f.text)
        assertEquals(listOf(4.0, 5.0), f.saves)
        // The echo of 4 arrives late: the field keeps 5.
        assertFalse(f.stepper.arrived(4.0))
        assertFalse(f.stepper.arrived(5.0))
        // A value from somewhere else is shown.
        assertTrue(f.stepper.arrived(7.0))
    }

    @Test fun lateEchoesAfterAHoldAreSkippedInOrder() {
        val f = Field(3.0, 1.0)
        f.tap(1)
        f.hold(1, 2).end()
        assertEquals(listOf(4.0, 6.0), f.saves)
        // Only the newest echo comes back (Compose skips the older): it clears both, so a later 4 is an outside value.
        assertFalse(f.stepper.arrived(6.0))
        assertTrue(f.stepper.arrived(4.0))
    }

    @Test fun outsideValueWaitsWhileAHoldShowsItsStep() {
        val f = Field(3.0, 1.0)
        val p = f.hold(1, 1)
        assertFalse(f.stepper.arrived(8.0))
        p.end()
        assertEquals(listOf(4.0), f.saves)
        assertTrue(f.stepper.arrived(null))
    }

    @Test fun saveOfTheStoredValueWaitsForNoEcho() {
        val f = Field(10.0, 1.0, 1.0, 10.0)
        f.tap(1)
        assertEquals(listOf(10.0), f.saves)
        // No echo comes for an unchanged value, so a later outside 10 still shows.
        assertTrue(f.stepper.arrived(10.0))
    }

    @Test fun timeStepperKeepsQuarterHoursInsideItsRange() {
        // "Opens" with the window closing at 20:00: 00:00 to 19:00.
        val opens = Stepper(15.0, 0.0, 1140.0)
        val saves = mutableListOf<Double>()
        var shown: Int? = null
        fun press(minute: Int, sign: Int) = PressFlow(
            onTap = { opens.step(sign, true, shown?.toString() ?: "", minute.toDouble(), null).also { shown = it.shown.toInt() }.save?.let(saves::add) },
            onRepeat = { shown = opens.step(sign, false, shown?.toString() ?: "", minute.toDouble(), null).shown.toInt() },
            onRelease = { opens.release(minute.toDouble())?.let(saves::add) },
        )
        press(1140, 1).run { endEarly(true); end() }
        assertEquals(1140, shown)
        assertEquals(listOf(1140.0), saves)

        shown = null; saves.clear()
        press(480, 1).let { p -> p.hold(); repeat(3) { p.repeat() }; assertTrue(saves.isEmpty()); p.end() }
        assertEquals(525, shown)
        assertEquals(listOf(525.0), saves)
        // Its echo comes back: the shown time stays.
        assertFalse(opens.arrived(525.0))

        shown = null; saves.clear()
        press(15, -1).let { p -> p.hold(); repeat(4) { p.repeat() }; p.end() }
        assertEquals(0, shown)
        assertEquals(listOf(0.0), saves)
    }
}
