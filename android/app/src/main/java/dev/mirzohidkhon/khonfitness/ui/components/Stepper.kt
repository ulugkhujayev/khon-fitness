package dev.mirzohidkhon.khonfitness.ui.components

/** [v] kept inside [min]..[max]. Never throws: a [min] above [max] gives [max]. */
fun bound(v: Double, min: Double = 0.0, max: Double = Double.POSITIVE_INFINITY): Double = minOf(max, maxOf(min, v))

private val PLAIN_NUMBER = Regex("""\d+(\.\d+)?""")

/** The number a placeholder shows ("75", "62.5"), or null when it is not a plain number ("1–10"). */
fun placeholderNumber(placeholder: String?): Double? = placeholder?.takeIf { PLAIN_NUMBER.matches(it) }?.toDouble()

/** The number a field's cleaned text stands for: "80." is 80, empty is null. */
fun typedNumber(text: String): Double? = text.trimEnd('.').toDoubleOrNull()

/**
 * What a − or + steps from: the step a hold shows, else the typed text (a database write may not have come back
 * into the value yet), else the value, else the number the placeholder shows (last time, latest bodyweight), else 0.
 */
fun stepBase(held: Double?, typed: String, value: Double?, placeholder: String?): Double =
    held ?: typedNumber(typed) ?: value ?: placeholderNumber(placeholder) ?: 0.0

/**
 * The saving rule of a number widget's − and +, inside [min]..[max]. A tap steps once and saves. A hold shows each
 * step and saves the last one once, when the hold ends ([release]). While a hold shows its step, an outside value
 * waits ([arrived]).
 */
class Stepper(var step: Double, var min: Double = 0.0, var max: Double = Double.POSITIVE_INFINITY) {
    /** The step a hold shows and has not saved yet. */
    var held: Double? = null
        private set

    /** One press result: the number to show, and the number to save now (null while a hold only shows it). */
    data class Step(val shown: Double, val save: Double?)

    /** One step of − ([sign] -1) or + (1) from what the field shows. A tap ([save]) saves it; a hold keeps it. */
    fun step(sign: Int, save: Boolean, typed: String, value: Double?, placeholder: String?): Step {
        val next = stepValue(stepBase(held, typed, value, placeholder), sign, step, min, max)
        return if (save) { held = null; Step(next, next) } else { held = next; Step(next, null) }
    }

    /**
     * The hold ended: the held number inside the current bounds (they may have moved during the hold), to show and
     * save once; null when nothing is held.
     */
    fun release(): Double? = held?.let { held = null; bounded(it) }

    /** The number typed [text] stands for, bounded, to save and show on blur; null when unreadable ("", ".", "5.."). */
    fun parseTyped(text: String): Double? = text.toDoubleOrNull()?.let(::bounded)

    /** [v] inside this stepper's bounds. */
    fun bounded(v: Double): Double = bound(v, min, max)

    /** A new value came from outside. False while a hold shows its own step; true otherwise: the field may show it. */
    fun arrived(): Boolean = held == null

    /**
     * A field showing [text] gets [value] from outside: show it unless a hold is in progress or the text already
     * means it. A [focused] field the user cleared stays empty: the owner may push a minimum back that would prefix
     * the next digit.
     */
    fun showsOutside(value: Double?, text: String, focused: Boolean): Boolean =
        arrived() && parseTyped(text) != value && !(focused && text.isEmpty())
}

/**
 * What one press of a − or + does as its pointer events come in: a tap when it ends before [REPEAT_AFTER_MS], or a
 * hold that repeats and releases once when it ends (lifted, dragged off, or cancelled). A hold whose button left the
 * screen ([dispose]) saves nothing.
 */
class PressFlow(private val onTap: () -> Unit, private val onRepeat: () -> Unit, private val onRelease: () -> Unit) {
    private var holding = false

    /** The press ended before it became a hold: [lifted] on the button it taps; dragged off or cancelled, nothing. */
    fun endEarly(lifted: Boolean) { if (lifted && !holding) onTap() }

    /** The press became a hold. */
    fun hold() { holding = true }

    /** One repeat of a hold. */
    fun repeat() { if (holding) onRepeat() }

    /** The hold ended on the screen: releases once. A press that never became a hold releases nothing. */
    fun end() { if (holding) { holding = false; onRelease() } }

    /** The button left the screen: the hold stops and saves nothing (its row may be gone). */
    fun dispose() { holding = false }
}
