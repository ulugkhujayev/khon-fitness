package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchMode
import dev.mirzohidkhon.khonfitness.timer.Phase
import dev.mirzohidkhon.khonfitness.timer.TimerState
import dev.mirzohidkhon.khonfitness.timer.mmss

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
