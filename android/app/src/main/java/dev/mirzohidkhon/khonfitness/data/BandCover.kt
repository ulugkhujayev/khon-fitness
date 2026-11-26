package dev.mirzohidkhon.khonfitness.data

import java.time.ZoneId

data class BandRecord(val id: String, val startMs: Long, val endMs: Long, val modalityId: String?)

data class RunSpan(val range: HrRange, val modalityId: String)

data class ImportLocal(val claimed: Set<String>, val runs: List<RunSpan>) {
    fun available(r: BandRecord): Boolean = r.id !in claimed && !covered(r, runs)
}

fun claimedIds(s: Session): List<String> =
    listOfNotNull(s.sourceId, s.linkedIds).flatMap { it.split(",") }.map { it.trim() }.filter { it.isNotEmpty() }

fun overlapMs(a: HrRange, b: HrRange): Long = spanMs(maxOf(a.fromMs, b.fromMs), minOf(a.toMs, b.toMs)).coerceAtLeast(0L)

fun covered(r: BandRecord, runs: List<RunSpan>): Boolean {
    val modality = r.modalityId ?: return false
    if (r.startMs >= r.endMs) return false
    val record = HrRange(r.startMs, r.endMs)
    return runs.any { run ->
        val o = overlapMs(record, run.range)
        run.modalityId == modality && o > 0 && o >= record.lengthMs - o && o >= run.range.lengthMs - o
    }
}

fun importLocal(sessions: List<Session>, modalities: Map<String, String?>, zone: ZoneId): ImportLocal = ImportLocal(
    claimed = sessions.flatMap(::claimedIds).toSet(),
    runs = sessions.mapNotNull { s ->
        val range = runRange(s, zone) ?: return@mapNotNull null
        val modality = s.exerciseId?.let { modalities[it] } ?: return@mapNotNull null
        RunSpan(range, modality)
    },
)

enum class ImportResult { SAVED, ALREADY_IMPORTED, COVERED, TIMER_PENDING }
