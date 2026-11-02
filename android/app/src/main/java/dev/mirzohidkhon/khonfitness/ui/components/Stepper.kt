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
 * step and saves the last one once, when the hold ends ([release]). Saves whose value has not come back yet are
 * kept, so a late echo of an earlier save does not pull the shown number back ([arrived]).
 */
class Stepper(var step: Double, var min: Double = 0.0, var max: Double = Double.POSITIVE_INFINITY) {
    /** The step a hold shows and has not saved yet. */
    var held: Double? = null
        private set
    private val sent = ArrayDeque<Double>()

    /** One press result: the number to show, and the number to save now (null while a hold only shows it). */
    data class Step(val shown: Double, val save: Double?)

    /** One step of − ([sign] -1) or + (1) from what the field shows. A tap ([save]) saves it; a hold keeps it. */
    fun step(sign: Int, save: Boolean, typed: String, value: Double?, placeholder: String?): Step {
        val next = stepValue(stepBase(held, typed, value, placeholder), sign, step, min, max)
        return if (save) { held = null; Step(next, send(next, value)) } else { held = next; Step(next, null) }
    }

    /** The hold ended: the number to save once, or null when nothing is held. */
    fun release(value: Double?): Double? = held?.let { held = null; send(it, value) }

    /** A typed number, bounded, to save; null for an empty field. */
    fun typed(v: Double?, value: Double?): Double? = v?.let { send(bound(it, min, max), value) }

    /** [v] inside this stepper's bounds. */
    fun bounded(v: Double): Double = bound(v, min, max)

    /**
     * A new [value] came from outside. False while a hold shows its own step, and for the echo of a save made here
     * (that echo and every older one are done). True for any other value: the field should show it.
     */
    fun arrived(value: Double?): Boolean {
        if (held != null) return false
        val i = if (value == null) -1 else sent.indexOf(value)
        if (i < 0) { sent.clear(); return true }
        repeat(i + 1) { sent.removeFirst() }
        return false
    }

    private fun send(v: Double, value: Double?): Double {
        // A save that equals the stored value changes nothing, so no echo comes back for it.
        if (v != value) { sent.addLast(v); if (sent.size > MAX_SENT) sent.removeFirst() }
        return v
    }

    private companion object { const val MAX_SENT = 16 }
}

/**
 * What one press of a − or + does as its pointer events come in: a tap when it ends before [REPEAT_AFTER_MS], or a
 * hold that repeats and releases once however it ends (lifted, dragged off, cancelled, or the button left the screen).
 */
class PressFlow(private val onTap: () -> Unit, private val onRepeat: () -> Unit, private val onRelease: () -> Unit) {
    private var holding = false

    /** The press ended before it became a hold: [lifted] on the button it taps; dragged off or cancelled, nothing. */
    fun endEarly(lifted: Boolean) { if (lifted && !holding) onTap() }

    /** The press became a hold. */
    fun hold() { holding = true }

    /** One repeat of a hold. */
    fun repeat() { if (holding) onRepeat() }

    /** The hold ended in any way: releases once. A press that never became a hold releases nothing. */
    fun end() { if (holding) { holding = false; onRelease() } }
}
