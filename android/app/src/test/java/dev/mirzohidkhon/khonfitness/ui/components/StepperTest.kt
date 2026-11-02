package dev.mirzohidkhon.khonfitness.ui.components

import org.junit.Assert.assertEquals
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
            onRelease = { stepper.release()?.let { text = show(it); saves.add(it) } },
        )

        /** A new value from outside, as NumberField's LaunchedEffect takes it (field not focused). */
        fun arrive(v: Double?) { value = v; if (stepper.showsOutside(v, text, focused = false)) text = show(v) }

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

    @Test fun disposeWhileHeldSavesNothingAndALaterPressWorks() {
        val f = Field(20.0, 5.0, 5.0, 600.0)
        val p = f.hold(1, 2)
        // repeatPress calls dispose when the button leaves the screen mid-hold: the row may be gone.
        p.dispose()
        p.end()
        p.repeat()
        assertTrue(f.saves.isEmpty())
        f.tap(1)
        assertEquals("35", f.text)
        assertEquals(listOf(35.0), f.saves)
    }

    @Test fun holdThenLiftSavesOnce() {
        val f = Field(20.0, 5.0, 5.0, 600.0)
        f.hold(-1, 2).run { end(); end(); dispose() }
        assertEquals("10", f.text)
        assertEquals(listOf(10.0), f.saves)
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

    @Test fun cleanTypedLeavesOnlyEmptyOrANumber() {
        assertEquals("5.", cleanTyped("5..", 1))
        assertEquals("5.2", cleanTyped("5.2.", 1))
        assertEquals("0.", cleanTyped(".", 1))
        assertEquals("12.5", cleanTyped("12,5", 1))
        assertEquals("125", cleanTyped("12.5", 0))
        assertEquals("80", cleanTyped("8a0 ", 0))
        assertEquals("", cleanTyped("-.", 0))
        // Every cleaned text is empty or reads as a number.
        for (raw in listOf("5..", ".", "..", ",", "1.2.3", "-", "007", "")) for (d in 0..2) {
            val t = cleanTyped(raw, d)
            assertTrue("'$raw' -> '$t'", t.isEmpty() || typedNumber(t) != null)
        }
    }

    @Test fun cleanTypedDropsDigitsTheParserCannotRead() {
        // Arabic-Indic, Persian and full-width five.
        for (five in listOf("\u0665", "\u06F5", "\uFF15")) {
            assertEquals("", cleanTyped(five, 0))
            assertEquals("3", cleanTyped("3$five", 1))
            assertTrue(Stepper(1.0).showsOutside(null, cleanTyped("4$five", 0), focused = true))
        }
    }

    @Test fun cleanOffsetMovesTheCursorWithTheCleaning() {
        // "." becomes "0." with the cursor after the dot, so the next 5 gives 0.5.
        assertEquals(2, cleanOffset(".", 1, 1))
        val next = "0." + "5"
        assertEquals("0.5", cleanTyped(next, 1))
        // A dropped dot in a whole-number field: "4.5" -> "45", cursor after the 4 and after the 5.
        assertEquals(1, cleanOffset("4.5", 2, 0))
        assertEquals(2, cleanOffset("4.5", 3, 0))
        // A second dot typed in "5.2" at the end: "5.2." -> "5.2", cursor at the end.
        assertEquals(3, cleanOffset("5.2.", 4, 1))
        // Paste "1,5 kg" over everything: "1.5", cursor at the end.
        assertEquals("1.5", cleanTyped("1,5 kg", 1))
        assertEquals(3, cleanOffset("1,5 kg", 6, 1))
        // A selection maps both ends; offsets past the text are clamped.
        assertEquals(0, cleanOffset("a12", 1, 0))
        assertEquals(2, cleanOffset("a12", 3, 0))
        assertEquals(2, cleanOffset("a12", 9, 0))
    }

    @Test fun cleanTypedKeepsOnlyWhatTheFieldShows() {
        // No more decimals than the field shows: 1.25 cannot be typed where 1.3 would show.
        assertEquals("1.2", cleanTyped("1.25", 1))
        assertEquals("125", cleanTyped("1.25", 0))
        assertEquals("1.25", cleanTyped("1.25", 2))
        // At most six digits before the dot: no huge or infinite numbers.
        assertEquals("120", cleanTyped("1e20", 0))
        assertEquals("999999", cleanTyped("9".repeat(400), 0))
        assertEquals("999999.9", cleanTyped("9".repeat(400) + ".99", 1))
        // Save, blur and step read the same number afterwards.
        val kg = Stepper(0.1)
        val t = cleanTyped("1.25", 1)
        assertEquals(1.2, kg.parseTyped(t)!!, 0.0)
        assertEquals(1.3, kg.step(1, true, t, null, null).shown, 1e-9)
        // The cursor still moves with the cleaning: typing a third decimal leaves it after the second.
        assertEquals(3, cleanOffset("1.25", 4, 1))
    }

    @Test fun stepDecimalsShowEveryStepExactly() {
        assertEquals(0, stepDecimals(5.0))
        assertEquals(1, stepDecimals(2.5))
        assertEquals(2, stepDecimals(1.25))
        assertEquals(2, stepDecimals(0.333))
        // A 1.25 kg step shows and types 61.25, so the next step reads 61.25, not a rounded 61.3.
        val t = cleanTyped("61.25", stepDecimals(1.25))
        assertEquals(62.5, Stepper(1.25).step(1, true, t, 61.25, null).shown, 1e-9)
    }

    @Test fun parseTypedReadsCleanTextInsideTheBounds() {
        val sets = Stepper(1.0, 1.0, 10.0)
        assertNull(sets.parseTyped(""))
        assertEquals(5.0, sets.parseTyped(cleanTyped("5..", 0))!!, 0.0)
        assertEquals(8.0, sets.parseTyped("8.")!!, 0.0)
        assertEquals(1.0, sets.parseTyped("0")!!, 0.0)
        assertEquals(10.0, sets.parseTyped("15")!!, 0.0)
        assertEquals(12.5, Stepper(0.5).parseTyped("12.5")!!, 0.0)
        assertEquals(0.0, Stepper(0.5).parseTyped(cleanTyped(".", 1))!!, 0.0)
    }

    @Test fun outsideNullReplacesTypedText() {
        val kg = Stepper(0.1)
        for (raw in listOf("5..", ".", "5.", "75")) {
            val t = cleanTyped(raw, 1)
            assertTrue("'$t' must give way to an outside null", kg.showsOutside(null, t, focused = true))
            assertTrue("'$t' must give way to an outside null", kg.showsOutside(null, t, focused = false))
        }
        // The text already means the value: no sync, the cursor stays.
        assertTrue(!kg.showsOutside(5.0, "5.", focused = true))
        // A focused field the user cleared stays empty.
        assertTrue(!kg.showsOutside(0.0, "", focused = true))
    }

    @Test fun lateSaveIsShownAndALaterOutsideValueToo() {
        val f = Field(3.0, 1.0)
        f.tap(1); f.tap(1); f.tap(-1)
        assertEquals(listOf(4.0, 5.0, 4.0), f.saves)
        // Only 4 comes back: the field already shows 4.
        f.arrive(4.0)
        assertEquals("4", f.text)
        // A later outside 5 is shown.
        f.arrive(5.0)
        assertEquals("5", f.text)
    }

    @Test fun sameNumberSavedTwice() {
        val f = Field(9.0, 1.0, 1.0, 10.0)
        f.tap(1); f.tap(1)
        assertEquals(listOf(10.0, 10.0), f.saves)
        f.arrive(10.0)
        assertEquals("10", f.text)
        f.arrive(7.0)
        assertEquals("7", f.text)
    }

    @Test fun quickTapsStepFromTheShownTextBeforeTheirSaveComesBack() {
        val f = Field(3.0, 1.0)
        f.tap(1)
        // The write of 4 has not come back: the next tap steps from the shown 4, not the stored 3.
        f.tap(1)
        assertEquals("5", f.text)
        assertEquals(listOf(4.0, 5.0), f.saves)
    }

    @Test fun outsideValueIsShownWhenNoHoldIsInProgress() {
        val f = Field(3.0, 1.0)
        f.arrive(7.0)
        assertEquals("7", f.text)
        f.arrive(null)
        assertEquals("", f.text)
    }

    @Test fun outsideValueDuringAHoldIsNotShownAndTheReleaseStillSaves() {
        val f = Field(3.0, 1.0)
        val p = f.hold(1, 2)
        f.arrive(8.0)
        assertEquals("5", f.text)
        p.end()
        assertEquals("5", f.text)
        assertEquals(listOf(5.0), f.saves)
    }

    @Test fun releaseBoundsTheHeldNumberWithTheCurrentLimits() {
        // "Opens" held up to 19:00, then "Closes" moves so Opens may reach only 18:45.
        val f = Field(1110.0, 15.0, 0.0, 1140.0)
        val p = f.hold(1, 3)
        assertEquals("1140", f.text)
        f.stepper.max = 1125.0
        p.end()
        assertEquals(listOf(1125.0), f.saves)
        assertEquals("1125", f.text)
    }

    @Test fun timeStepperKeepsQuarterHoursInsideItsRange() {
        // "Opens" with the window closing at 20:00: 00:00 to 19:00.
        val opens = Stepper(15.0, 0.0, 1140.0)
        val saves = mutableListOf<Double>()
        var shown: Int? = null
        fun press(minute: Int, sign: Int) = PressFlow(
            onTap = { opens.step(sign, true, shown?.toString() ?: "", minute.toDouble(), null).also { shown = it.shown.toInt() }.save?.let(saves::add) },
            onRepeat = { shown = opens.step(sign, false, shown?.toString() ?: "", minute.toDouble(), null).shown.toInt() },
            onRelease = { opens.release()?.let { shown = it.toInt(); saves.add(it) } },
        )
        press(1140, 1).run { endEarly(true); end() }
        assertEquals(1140, shown)
        assertEquals(listOf(1140.0), saves)

        shown = null; saves.clear()
        press(480, 1).let { p -> p.hold(); repeat(3) { p.repeat() }; assertTrue(saves.isEmpty()); p.end() }
        assertEquals(525, shown)
        assertEquals(listOf(525.0), saves)
        assertTrue(opens.arrived())

        shown = null; saves.clear()
        press(15, -1).let { p -> p.hold(); repeat(4) { p.repeat() }; p.end() }
        assertEquals(0, shown)
        assertEquals(listOf(0.0), saves)
    }
}
