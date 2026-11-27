package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TodayRolloverTest {
    @get:Rule val compose = createComposeRule()

    private class MovingClock(private val zoneId: ZoneId, var now: Instant) : Clock() {
        override fun getZone(): ZoneId = zoneId
        override fun withZone(zone: ZoneId): Clock = MovingClock(zone, now)
        override fun instant(): Instant = now
    }

    private fun rollsOver(zone: ZoneId, day: LocalDate, hours: Long) {
        val clock = MovingClock(zone, day.atStartOfDay(zone).toInstant())
        compose.setContent { Text(rememberToday(clock).toString()) }
        compose.waitForIdle()
        compose.onNodeWithText(day.toString()).assertExists()
        val elapsed = Duration.ofHours(hours).plusSeconds(5)
        compose.runOnIdle { clock.now = clock.now.plus(elapsed) }
        compose.mainClock.advanceTimeBy(elapsed.toMillis())
        compose.waitForIdle()
        compose.onNodeWithText(day.plusDays(1).toString()).assertExists()
    }

    @Test fun aDayOf23HoursRollsOverAfter23Hours() = rollsOver(ZoneId.of("America/New_York"), LocalDate.of(2026, 3, 8), 23)

    @Test fun aPlainDayRollsOverAfter24Hours() = rollsOver(ZoneId.of("Asia/Tashkent"), LocalDate.of(2026, 3, 8), 24)
}
