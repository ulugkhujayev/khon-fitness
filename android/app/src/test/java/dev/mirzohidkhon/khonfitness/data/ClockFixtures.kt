package dev.mirzohidkhon.khonfitness.data

import java.time.LocalDate
import java.time.ZoneId

val TEST_ZONE: ZoneId = ZoneId.of("Asia/Tashkent")

/** Epoch ms of [hour]:[minute] on [day] in [TEST_ZONE]. */
fun at(hour: Int, minute: Int, day: LocalDate = LocalDate.of(2026, 11, 20), second: Int = 0): Long =
    day.atTime(hour, minute, second).atZone(TEST_ZONE).toInstant().toEpochMilli()

const val MIN = 60_000L

/** Phone clocks a test moves: [pass] is true time, [moveWall] is the owner changing the phone clock. */
class FakeClocks(var wall: Long, var elapsed: Long = 5_000_000L) : Clocks {
    override fun wallMs(): Long = wall
    override fun elapsedMs(): Long = elapsed
    fun pass(ms: Long) { wall += ms; elapsed += ms }
    fun moveWall(ms: Long) { wall += ms }
}
