package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchMode
import dev.mirzohidkhon.khonfitness.data.StretchRoutine
import dev.mirzohidkhon.khonfitness.data.newId
import dev.mirzohidkhon.khonfitness.timer.Phase
import dev.mirzohidkhon.khonfitness.timer.TimerState
import dev.mirzohidkhon.khonfitness.timer.mmss

/** The name a new routine gets when its Name field is left empty. */
const val NEW_ROUTINE_NAME = "New routine"

/**
 * A routine editor's draft (DESIGN.md, Editors). A new routine ([isNew]) gets its [id] once, when the editor opens, and
 * stays a draft until Done or its first stretch creates a routine with that id: leaving before that writes nothing and
 * there is no Delete. Saved or draft is read from the database, never kept here: the editor edits the saved routine
 * exactly when the routines list holds [id] (`stored`). So a create that is still pending, refused, or lost with the
 * process leaves a draft that saves under the same id. [Saver] keeps the [id], the typed [name] and the [activeChoice]
 * over a rotation and process death.
 */
class RoutineDraft(val isNew: Boolean, val id: String) {
    /** The Name field while a draft, null until edited. */
    var name: String? by mutableStateOf(null)
    /** A new routine's Active switch, null until touched. */
    var activeChoice: Boolean? by mutableStateOf(null)

    /** A new routine that the database does not hold yet ([stored] is the listed routine with [id], if any). */
    fun isDraft(stored: StretchRoutine?): Boolean = isNew && stored == null

    /** The Active switch's state: a draft's choice, by default on only when there is no routine yet (as 0.4.8). */
    fun shownActive(stored: StretchRoutine?, noRoutines: Boolean): Boolean = if (isDraft(stored)) activeChoice ?: noRoutines else stored?.active == true

    /** The first stretch picked: the routine to create with it while a draft, else null (the pick adds a row). */
    fun forFirstStretch(stored: StretchRoutine?, noRoutines: Boolean): StretchRoutine? = if (isDraft(stored)) routine(noRoutines) else null

    /** Done: a draft's routine to create, else null (a saved routine wrote each edit as it was made). */
    fun done(stored: StretchRoutine?, noRoutines: Boolean): StretchRoutine? = if (isDraft(stored)) routine(noRoutines) else null

    /** The new routine with the typed name (or [NEW_ROUTINE_NAME]) and the Active choice. */
    private fun routine(noRoutines: Boolean) =
        StretchRoutine(id, name?.trim()?.takeIf { it.isNotEmpty() } ?: NEW_ROUTINE_NAME, active = activeChoice ?: noRoutines)

    companion object {
        /** The draft for a route id: `new` makes the new routine's id now, once; any other id is that routine's. */
        fun open(routeId: String) = if (routeId == "new") RoutineDraft(true, newId()) else RoutineDraft(false, routeId)

        val Saver = listSaver<RoutineDraft, Any?>(
            save = { listOf(it.isNew, it.id, it.name, it.activeChoice) },
            restore = { RoutineDraft(it[0] as Boolean, it[1] as String).apply { name = it[2] as String?; activeChoice = it[3] as Boolean? } },
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
