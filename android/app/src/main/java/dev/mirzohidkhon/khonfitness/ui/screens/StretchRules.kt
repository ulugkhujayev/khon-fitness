package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchMode
import dev.mirzohidkhon.khonfitness.data.StretchRoutine
import dev.mirzohidkhon.khonfitness.timer.Phase
import dev.mirzohidkhon.khonfitness.timer.TimerState
import dev.mirzohidkhon.khonfitness.timer.mmss

/** The name a new routine gets when its Name field is left empty. */
const val NEW_ROUTINE_NAME = "New routine"

/**
 * A routine editor's draft (DESIGN.md, Editors). A new routine ([isNew]) stays a draft ([isDraft]) until Done or its
 * first stretch saves it: leaving before that writes nothing and there is no Delete. The first stretch saves it
 * exactly as Done would, with that stretch as its first row; from then on the screen edits the saved routine, with
 * Delete, and leaving without Done keeps it. [Saver] keeps the saved id, the typed [name] and the [activeChoice]
 * over a rotation and process death, so a restored editor edits the same row and never saves a second routine.
 */
class RoutineDraft(val isNew: Boolean) {
    /** The id the new routine is saved under (by its first stretch or Done), or null while nothing is saved. */
    var savedId: String? by mutableStateOf(null)
        private set
    /** The Name field while a draft, null until edited. */
    var name: String? by mutableStateOf(null)
    /** A new routine's Active switch, null until touched. */
    var activeChoice: Boolean? by mutableStateOf(null)

    /** A new routine that nothing has saved yet. */
    val isDraft: Boolean get() = isNew && savedId == null

    /** The routine the screen edits: the route's, or a new routine's once saved; null while it is only a draft. */
    fun routineId(routeId: String): String? = if (isNew) savedId else routeId

    /** The Active switch's state: a draft's choice, by default on only when there is no routine yet (as 0.4.8). */
    fun shownActive(savedActive: Boolean, noRoutines: Boolean): Boolean = if (isDraft) activeChoice ?: noRoutines else savedActive

    /** The first stretch picked: the routine to save with it, or null once saved (or not new). */
    fun forFirstStretch(id: String, noRoutines: Boolean): StretchRoutine? = if (isDraft) save(id, noRoutines) else null

    /** Done: a draft's routine to save, else null (a saved routine wrote each edit as it was made). */
    fun done(id: String, noRoutines: Boolean): StretchRoutine? = if (isDraft) save(id, noRoutines) else null

    /** The save was refused (its first stretch went meanwhile): nothing was written, so the screen is a draft again. */
    fun refused(id: String) { if (savedId == id) savedId = null }

    /** The new routine with the typed name (or [NEW_ROUTINE_NAME]) and the Active choice; the editor edits it from here on. */
    private fun save(id: String, noRoutines: Boolean): StretchRoutine {
        savedId = id
        return StretchRoutine(id, name?.trim()?.takeIf { it.isNotEmpty() } ?: NEW_ROUTINE_NAME, active = activeChoice ?: noRoutines)
    }

    companion object {
        val Saver = listSaver<RoutineDraft, Any?>(
            save = { listOf(it.isNew, it.savedId, it.name, it.activeChoice) },
            restore = { RoutineDraft(it[0] as Boolean).apply { savedId = it[1] as String?; name = it[2] as String?; activeChoice = it[3] as Boolean? } },
        )
    }
}

/** A routine's row on Stretching: "10 min · 9 stretches", "1 min · 1 stretch". */
fun routineSummary(routineId: String, routineStretches: List<RoutineStretch>, stretches: List<Stretch>): String {
    val count = routineStretches.count { it.routineId == routineId }
    return "${(routineSeconds(routineId, routineStretches, stretches) + 30) / 60} min · $count ${if (count == 1) "stretch" else "stretches"}"
}

/** A stretch's line in the library: "per side · 45 s", "10 reps". */
fun stretchLine(st: Stretch): String =
    listOfNotNull(if (st.sided) "per side" else null, if (st.mode == StretchMode.REPS) "${st.reps} reps" else "${st.seconds} s").joinToString(" · ")

/** The line under the stretch's name in the player: "Left · Hold · 0:40", "10 reps · 1:00", "Hold · 0:45". */
fun sideLine(p: Phase): String =
    listOfNotNull(p.side.ifEmpty { null }, if (p.reps > 0) "${p.reps} reps" else "Hold", mmss(p.seconds.toDouble())).joinToString(" · ")

/** The line under the demonstration while a stretch runs; none on a ready screen, whose length is in [sideLine]. */
fun playerStatus(s: TimerState, p: Phase): String? = when {
    s.awaitingStart -> null
    s.paused -> "Paused"
    s.preparing -> "Get ready"
    else -> if (p.reps > 0) "Move" else "Hold"
}

/** The bottom button: "Start left side", "Start stretch", "Start next stretch", "Resume", "Pause". */
fun playerAction(s: TimerState, p: Phase): String = when {
    s.awaitingStart && p.side.isNotEmpty() -> "Start ${p.side.lowercase()} side"
    s.awaitingStart -> if (s.index == 0) "Start stretch" else "Start next stretch"
    s.paused -> "Resume"
    else -> "Pause"
}

/** The run's place under the player's bar: "3 of 12 · 6:30 remaining". */
fun playerProgress(s: TimerState): String = "${s.index + 1} of ${s.phases.size} · ${mmss(s.workRemaining)} remaining"
