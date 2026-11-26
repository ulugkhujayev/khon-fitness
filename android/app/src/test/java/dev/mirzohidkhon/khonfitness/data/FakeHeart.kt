package dev.mirzohidkhon.khonfitness.data

import kotlinx.coroutines.CompletableDeferred

/**
 * A heart source a test controls: [answer] gives the records of a range (a throw is a failed read). [hold] makes the
 * next read stop until [release]; [reached] completes when it stopped.
 */
class FakeHeart(var answer: suspend (HrRange) -> List<HeartRecord> = { emptyList() }) : HeartSource {
    val calls = mutableListOf<HrRange>()
    @Volatile var active = 0
    @Volatile var maxActive = 0
    private var gate: CompletableDeferred<Unit>? = null
    private var held = CompletableDeferred<Unit>()
    var reached = CompletableDeferred<Unit>()

    fun hold() { held = CompletableDeferred(); gate = held; reached = CompletableDeferred() }
    fun release() { held.complete(Unit) }

    override suspend fun records(range: HrRange): List<HeartRecord> {
        synchronized(this) { calls += range; active++; maxActive = maxOf(maxActive, active) }
        try {
            gate?.let { g -> gate = null; reached.complete(Unit); g.await() }
            return answer(range)
        } finally {
            synchronized(this) { active-- }
        }
    }
}

/** One sample per minute of [range] at [bpm] from [origin]: a complete read. */
fun everyMinute(range: HrRange, bpm: Int = 130, origin: String = "mi") =
    listOf(HeartRecord(origin, (range.fromMs until range.toMs step MIN).map { HeartPoint(it, bpm) }))

/** Three samples at the start of [range]: a read that is not complete. */
fun sparse(range: HrRange, bpm: Int = 120) = listOf(HeartRecord("mi", (0 until 3).map { HeartPoint(range.fromMs + it * MIN, bpm) }))
