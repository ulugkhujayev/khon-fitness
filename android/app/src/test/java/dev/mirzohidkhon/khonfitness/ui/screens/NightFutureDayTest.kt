package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.NightsState
import dev.mirzohidkhon.khonfitness.data.SleepSpan
import dev.mirzohidkhon.khonfitness.data.nights
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NightFutureDayTest {
    private val zone = ZoneId.of("Asia/Tashkent")
    private val today = LocalDate.of(2026, 11, 26)

    @Test fun `an evening sleep that belongs to tomorrow gives the Log no line under tomorrow`() {
        fun at(hour: Int, minute: Int) = today.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        val nights = nights(listOf(SleepSpan("band", at(18, 30), at(20, 0))), zone)
        val tomorrow = today.plusDays(1)
        assertEquals(setOf(tomorrow), nights.keys)
        val state = NightsState.Loaded(nights)
        assertNull(nightLogLine(tomorrow, state, today, zone))
        assertNull(nightLogSpoken(tomorrow, state, today, zone))
        assertEquals("Night 1 h 30 m · 18:30 – 20:00", nightLogLine(tomorrow, state, tomorrow, zone))
    }

    @Test fun `a bar with no night speaks no session title`() {
        assertEquals("Friday, November 20, no night data", nightBarSpoken(LocalDate.of(2026, 11, 20), null, listOf("GYM")))
    }
}
