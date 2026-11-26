package dev.mirzohidkhon.khonfitness.data

import java.time.ZoneId

/** One band workout as Health Connect holds it: the record id, its range `[startMs, endMs)`, and its modality (null: unknown). */
data class BandRecord(val id: String, val startMs: Long, val endMs: Long, val modalityId: String?)

/** The [runRange] of an app cardio session with the modality of its exercise. */
data class RunSpan(val range: HrRange, val modalityId: String)

/**
 * What the import reads of this database, from one read transaction ([KhonDao.importLocal]): every record id a session
 * claims, and the run ranges of app cardio sessions.
 */
data class ImportLocal(val claimed: Set<String>, val runs: List<RunSpan>) {
    /** Whether the import may offer [r]: no session claims its id and no app run covers it. */
    fun available(r: BandRecord): Boolean = r.id !in claimed && !covered(r, runs)
}

/** The record ids [s] claims: its `sourceId` and `linkedIds`, each split on commas into whole ids. */
fun claimedIds(s: Session): List<String> =
    listOfNotNull(s.sourceId, s.linkedIds).flatMap { it.split(",") }.map { it.trim() }.filter { it.isNotEmpty() }

fun overlapMs(a: HrRange, b: HrRange): Long = spanMs(maxOf(a.fromMs, b.fromMs), minOf(a.toMs, b.toMs)).coerceAtLeast(0L)

/**
 * A band record is covered when an app run of the record's modality overlaps it, and the overlap is at least half of
 * the record and at least half of the run. A record of unknown modality, or with no length, is never covered.
 */
fun covered(r: BandRecord, runs: List<RunSpan>): Boolean {
    val modality = r.modalityId ?: return false
    if (r.startMs >= r.endMs) return false
    val record = HrRange(r.startMs, r.endMs)
    return runs.any { run ->
        val o = overlapMs(record, run.range)
        run.modalityId == modality && o > 0 && o >= record.lengthMs - o && o >= run.range.lengthMs - o
    }
}

/** [ImportLocal] from the rows of one read: [sessions] and the modality of each exercise id, archived ones too. */
fun importLocal(sessions: List<Session>, modalities: Map<String, String?>, zone: ZoneId): ImportLocal = ImportLocal(
    claimed = sessions.flatMap(::claimedIds).toSet(),
    runs = sessions.mapNotNull { s ->
        val range = runRange(s, zone) ?: return@mapNotNull null
        val modality = s.exerciseId?.let { modalities[it] } ?: return@mapNotNull null
        RunSpan(range, modality)
    },
)

/** What [KhonRepository.importDraft] did. Only [SAVED] wrote anything. */
enum class ImportResult { SAVED, ALREADY_IMPORTED, COVERED, TIMER_PENDING }
