package dev.mirzohidkhon.khonfitness.data

data class HeartPoint(val timeMs: Long, val bpm: Int)

/** One heart rate record of the provider: the package that wrote it and all of its samples. */
data class HeartRecord(val origin: String, val samples: List<HeartPoint>)

/** One page of a provider read; [next] is the token of the next page, null or empty after the last. */
data class HeartPage(val records: List<HeartRecord>, val next: String?)

/** Where heart rate comes from: Health Connect in the app, a fake in tests. */
fun interface HeartSource {
    /**
     * Every heart rate record that touches [range], also one that begins before it or ends after it, from every page.
     * Throws when the read fails: no provider, no heart rate permission, or any error on any page.
     */
    suspend fun records(range: HrRange): List<HeartRecord>
}

/** The records of every page, the first page asked with a null token. A throw on any page throws. */
suspend fun readAllPages(page: suspend (token: String?) -> HeartPage): List<HeartRecord> {
    val all = mutableListOf<HeartRecord>()
    var token: String? = null
    do {
        val p = page(token)
        all += p.records
        token = p.next?.takeIf { it.isNotEmpty() }
    } while (token != null)
    return all
}

/** What one read of a range found ([readHeart]). */
data class HeartReading(
    val buckets: List<HrSample>,
    val rawCount: Int,
    val sampleCount: Int,
    val origin: String?,
    val origins: List<String>,
    val firstMs: Long?,
    val lastMs: Long?,
    val maxGapSec: Int?,
    val avgHr: Int?,
    val maxHr: Int?,
    val complete: Boolean,
)

const val BUCKET_SEC = 5
const val COMPLETE_INSTANTS = 10

/**
 * The reading of [records] for [range]. [HeartReading.rawCount] counts every sample of every record. A sample is
 * accepted inside `[from, to)` with a bpm from [BPM_MIN] to [BPM_MAX]; exact duplicates (time, bpm, package) count
 * once. The origin is the package with the most accepted samples (a tie: the first by name), and only its samples
 * count, one per instant (the first read). Complete: [COMPLETE_INSTANTS] or more instants whose first to last spans at
 * least half of the range. Buckets of [BUCKET_SEC] seconds from `from` hold the half-up mean of their samples.
 */
fun readHeart(records: List<HeartRecord>, range: HrRange): HeartReading {
    val accepted = records.flatMap { r ->
        r.samples.filter { it.timeMs >= range.fromMs && it.timeMs < range.toMs && it.bpm in BPM_MIN..BPM_MAX }.map { r.origin to it }
    }.distinct()
    val byOrigin = accepted.groupBy({ it.first }, { it.second })
    val origin = byOrigin.entries.sortedBy { it.key }.maxByOrNull { it.value.size }?.key
    val points = byOrigin[origin].orEmpty().distinctBy { it.timeMs }.sortedBy { it.timeMs }
    val first = points.firstOrNull()?.timeMs
    val last = points.lastOrNull()?.timeMs
    val buckets = points.groupBy { ((it.timeMs - range.fromMs) / (BUCKET_SEC * 1000L)).toInt() }.toSortedMap()
        .map { (bucket, inside) -> HrSample(bucket * BUCKET_SEC, meanHalfUp(inside.map { it.bpm })!!) }
    val complete = first != null && last != null && points.size >= COMPLETE_INSTANTS && 2 * (last - first) >= range.lengthMs
    return HeartReading(
        buckets = buckets,
        rawCount = records.sumOf { it.samples.size },
        sampleCount = points.size,
        origin = origin,
        origins = records.map { it.origin }.distinct().sorted(),
        firstMs = first,
        lastMs = last,
        maxGapSec = points.zipWithNext { a, b -> b.timeMs - a.timeMs }.maxOrNull()?.let { (it / 1000).toInt() },
        avgHr = meanHalfUp(points.map { it.bpm }),
        maxHr = points.maxOfOrNull { it.bpm },
        complete = complete,
    )
}

const val DAY_MS = 24 * 60 * 60_000L
const val WAITING_MS = 7 * DAY_MS
const val FILL_AGE_MS = 30 * DAY_MS

/** Whether an automatic pass reads a range that ended at [toMs]: in the last 30 days, not in the future. */
fun fillDue(toMs: Long, now: Long): Boolean = toMs <= now && now - toMs <= FILL_AGE_MS

/**
 * The row a read writes: `filled` with its average and maximum when complete; else `waiting` while the range ended 7
 * days ago or less, then `none`, with samples and counts and no values. Its `fetchedAt` is later than [from]'s.
 */
fun hrRow(sessionId: String, range: HrRange, reading: HeartReading, now: Long, from: SessionHr?): SessionHr {
    val state = when {
        reading.complete -> HrState.FILLED
        now - range.toMs <= WAITING_MS -> HrState.WAITING
        else -> HrState.NONE
    }
    val filled = state == HrState.FILLED
    return SessionHr(
        sessionId = sessionId, fromMs = range.fromMs, toMs = range.toMs, state = state,
        samples = formatSamples(reading.buckets), rawCount = reading.rawCount, sampleCount = reading.sampleCount,
        origin = reading.origin, origins = reading.origins.joinToString(","),
        firstMs = reading.firstMs, lastMs = reading.lastMs, maxGapSec = reading.maxGapSec,
        avgHr = reading.avgHr.takeIf { filled }, maxHr = reading.maxHr.takeIf { filled },
        fetchedAt = laterThan(from, now),
    )
}

/** [now], or one ms after [row]'s `fetchedAt` when the clock has not passed it: a new row never repeats the old stamp. */
fun laterThan(row: SessionHr?, now: Long): Long = if (row == null) now else maxOf(now, row.fetchedAt + 1)

/**
 * Whether a heart rate row from a backup file holds what a read or "Remove" could have written: `fromMs < toMs`, one of
 * the four states, samples that parse with every offset inside the range and every bpm from [BPM_MIN] to [BPM_MAX];
 * `filled` with at least one sample and both values in that bpm range; `off` with no sample and no value.
 */
fun validFileHr(row: SessionHr): Boolean {
    if (row.fromMs >= row.toMs) return false
    val samples = parseSamples(row.samples) ?: return false
    if (samples.any { it.offsetSec < 0 || row.fromMs + it.offsetSec * 1000L >= row.toMs || it.bpm !in BPM_MIN..BPM_MAX }) return false
    fun bpm(v: Int?) = v != null && v in BPM_MIN..BPM_MAX
    return when (row.state) {
        HrState.FILLED -> samples.isNotEmpty() && bpm(row.avgHr) && bpm(row.maxHr)
        HrState.OFF -> samples.isEmpty() && row.avgHr == null && row.maxHr == null
        HrState.WAITING, HrState.NONE -> true
        else -> false
    }
}
