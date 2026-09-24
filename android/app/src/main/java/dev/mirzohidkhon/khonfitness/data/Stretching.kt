package dev.mirzohidkhon.khonfitness.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

object StretchMode { const val HOLD = "hold"; const val REPS = "reps" }

/** One stretch in the library. A sided stretch runs left then right. Reps mode runs as a timer with a rep cue. */
@Serializable
@Entity(tableName = "stretches")
data class Stretch(
    @PrimaryKey val id: String,
    val name: String,
    val sided: Boolean = false,
    val mode: String = StretchMode.HOLD,
    /** Hold seconds per side, or the total seconds for a reps stretch. */
    val seconds: Int = 45,
    /** Reps per side when mode is reps. */
    val reps: Int = 0,
    /** Key of the drawn figure. */
    val figure: String = "",
    val muscles: String = "",
    val cue: String = "",
    val link: String = "",
    val builtin: Boolean = false,
    val archived: Boolean = false,
)

@Serializable
@Entity(tableName = "stretch_routines")
data class StretchRoutine(@PrimaryKey val id: String, val name: String, val active: Boolean = false, val sortOrder: Int = 0)

@Serializable
@Entity(tableName = "routine_stretches")
data class RoutineStretch(
    @PrimaryKey val id: String,
    val routineId: String,
    val stretchId: String,
    val sortOrder: Int,
    /** Overrides the stretch default when set. */
    val seconds: Int? = null,
)

/** One finished (or stopped) run of a routine. */
@Serializable
@Entity(tableName = "stretch_sessions")
data class StretchSession(
    @PrimaryKey val id: String,
    val routineId: String,
    val routineName: String,
    val date: String,
    val startedAt: Long,
    val totalSec: Int,
    val completed: Int,
    val skipped: Int,
)

/** Single settings row, id = 1. Minutes since midnight. */
@Serializable
@Entity(tableName = "eating_window")
data class EatingWindow(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = false,
    val startMinute: Int = 12 * 60,
    val endMinute: Int = 20 * 60,
    val remindBeforeMin: Int = 15,
)

/** One row per day the state was touched: an early open, a late close, or a note. */
@Serializable
@Entity(tableName = "window_days")
data class WindowDay(
    @PrimaryKey val date: String,
    /** Epoch millis of a manual open, or null. */
    val openedAt: Long? = null,
    /** Epoch millis of a manual close, or null. */
    val closedAt: Long? = null,
    val note: String = "",
)

/** The eating-window state at one instant. */
data class WindowState(val open: Boolean, val changesAt: java.time.LocalDateTime, val kept: Boolean)

object EatingWindowRules {
    /** Open inside the hours or after an early open today, and not after an early close today. */
    fun state(w: EatingWindow, day: WindowDay?, now: java.time.LocalDateTime): WindowState {
        val minute = now.hour * 60 + now.minute
        val date = now.toLocalDate()
        val start = date.atStartOfDay().plusMinutes(w.startMinute.toLong())
        val end = date.atStartOfDay().plusMinutes(w.endMinute.toLong())
        val inHours = minute in w.startMinute until w.endMinute
        val zone = java.time.ZoneId.systemDefault()
        val openedEarly = day?.openedAt?.let { it < start.atZone(zone).toInstant().toEpochMilli() } == true && minute < w.startMinute
        val openedLate = day?.openedAt?.let { it >= end.atZone(zone).toInstant().toEpochMilli() } == true && minute >= w.endMinute
        val open = (inHours || openedEarly || openedLate) && day?.closedAt == null
        val changesAt = when {
            openedLate && open -> date.plusDays(1).atStartOfDay()
            open -> end
            minute < w.startMinute -> start
            else -> start.plusDays(1)
        }
        return WindowState(open, changesAt, kept = day?.openedAt == null && day?.closedAt == null)
    }

    fun hhmm(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

    /** "2 h 10 m" or "40 m". */
    fun untilText(from: java.time.LocalDateTime, to: java.time.LocalDateTime): String {
        val m = java.time.Duration.between(from, to).toMinutes().coerceAtLeast(0)
        return if (m >= 60) "${m / 60} h ${m % 60} m" else "$m m"
    }
}
