package dev.mirzohidkhon.khonfitness.data

import java.time.ZoneId

const val STAMP_TAIL_MS = 5 * 60_000L

/**
 * The range gym Finish stores: from `startedAt` to the last set stamp inside `[startedAt, finishedAt]` plus 5 minutes,
 * at most `finishedAt`. Null with no such stamp, or when the clocks are not in step at Finish since the session's anchor.
 */
fun gymRange(session: Session, stamps: List<Long>, finishedAt: Long, elapsedMs: Long): HrRange? {
    if (!inStep(session.anchor, finishedAt, elapsedMs)) return null
    val last = stamps.filter { it in session.startedAt..finishedAt }.maxOrNull() ?: return null
    return HrRange(session.startedAt, minOf(finishedAt, last + STAMP_TAIL_MS)).takeIf { it.toMs > it.fromMs }
}

enum class RangeRefusal { MISSING, NOT_GYM, NOT_FINISHED, HAS_RANGE, DATE_MOVED, LENGTH, BOUNDS_CHANGED }

sealed interface ConfirmRangeResult {
    data object Confirmed : ConfirmRangeResult
    data class Refused(val reason: RangeRefusal) : ConfirmRangeResult
}

/**
 * Why a session cannot take "Use this time range", or null when it can: it must be a finished gym session without a
 * range, dated on the local date of its `startedAt`, lasting 10 minutes to 4 hours from `startedAt` to `finishedAt`.
 */
fun confirmableRange(session: Session, zone: ZoneId): RangeRefusal? {
    val finishedAt = session.finishedAt
    return when {
        session.itemType != ItemType.PROGRAM -> RangeRefusal.NOT_GYM
        finishedAt == null -> RangeRefusal.NOT_FINISHED
        session.timedFrom != null || session.timedTo != null -> RangeRefusal.HAS_RANGE
        session.date != localDate(session.startedAt, zone) -> RangeRefusal.DATE_MOVED
        finishedAt - session.startedAt !in GYM_RANGE_MS -> RangeRefusal.LENGTH
        else -> null
    }
}

/** The bounds "Use this time range" shows and passes back to [KhonDao.confirmRange], or null when it is refused. */
fun confirmableBounds(session: Session, zone: ZoneId): HrRange? =
    if (confirmableRange(session, zone) == null) HrRange(session.startedAt, session.finishedAt!!) else null
