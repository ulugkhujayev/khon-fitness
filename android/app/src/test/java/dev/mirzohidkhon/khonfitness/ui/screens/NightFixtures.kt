package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.FakeClocks
import dev.mirzohidkhon.khonfitness.data.HrRange
import dev.mirzohidkhon.khonfitness.data.NightStore
import dev.mirzohidkhon.khonfitness.data.NoSleepAccess
import dev.mirzohidkhon.khonfitness.data.SleepSource
import dev.mirzohidkhon.khonfitness.data.SleepSpan
import dev.mirzohidkhon.khonfitness.data.StretchSession
import dev.mirzohidkhon.khonfitness.data.iso
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation

/** Thursday: the week of the Log fixtures runs Monday Nov 23 to Sunday Nov 29. */
val NIGHT_TODAY: LocalDate = LocalDate.of(2026, 11, 26)

fun nightTestClock(zone: ZoneId = ZoneId.systemDefault()) = object : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = LocalDateTime.of(NIGHT_TODAY, java.time.LocalTime.of(21, 0)).atZone(zone).toInstant()
}

/** What the fake Health Connect does on a read. */
enum class SleepMode { ANSWER, NO_ACCESS, FAIL, HANG }

class FakeSleepSource(var spans: List<SleepSpan> = emptyList(), var mode: SleepMode = SleepMode.ANSWER) : SleepSource {
    val reads = AtomicInteger()
    override suspend fun spans(range: HrRange): List<SleepSpan> {
        reads.incrementAndGet()
        return when (mode) {
            SleepMode.ANSWER -> spans
            SleepMode.NO_ACCESS -> throw NoSleepAccess("test")
            SleepMode.FAIL -> throw IllegalStateException("test")
            SleepMode.HANG -> awaitCancellation()
        }
    }
}

/** A night of [asleepMin] minutes that ends at [endH]:[endM] on [day], no awake time. */
fun nightSpan(day: LocalDate, asleepMin: Int, zone: ZoneId, endH: Int = 6, endM: Int = 15): SleepSpan {
    val end = day.atTime(endH, endM).atZone(zone).toInstant().toEpochMilli()
    return SleepSpan("com.mi.health", end - asleepMin * 60_000L, end)
}

fun newApp(): KhonApp = KhonApp().also { app ->
    ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
        .invoke(app, ApplicationProvider.getApplicationContext<Application>())
}

/** Puts a store on [source] into [app]; call before the view model is made. */
fun installNights(app: KhonApp, source: SleepSource, scope: CoroutineScope, zone: ZoneId = ZoneId.systemDefault()): NightStore {
    val now = LocalDateTime.of(NIGHT_TODAY, java.time.LocalTime.of(21, 0)).atZone(zone).toInstant().toEpochMilli()
    val store = NightStore(source, scope, FakeClocks(wall = now), zone = { zone })
    KhonApp::class.java.getDeclaredField("nightStore\$delegate").apply { isAccessible = true }.set(app, lazyOf(store))
    return store
}

fun stretchRun(day: LocalDate, zone: ZoneId) =
    StretchSession("st-$day", "r1", "Morning", day.iso(), day.atTime(7, 30).atZone(zone).toInstant().toEpochMilli(), 600, 5, 0)

/** The Log week: no night on Mon 23, nights on Tue 24 (7 h 05 m) and Wed 25 (10 h 05 m · 23:10 – 09:15), none on Thu 26. */
fun logWeekSpans(zone: ZoneId) = listOf(
    nightSpan(NIGHT_TODAY.minusDays(2), 425, zone),
    nightSpan(NIGHT_TODAY.minusDays(1), 605, zone, endH = 9, endM = 15),
)

/** Gym and a zone 2 run on Tue 24 and a stretch run that day; Wed 25 and Thu 26 are rest. */
fun logWeekFixtures(zone: ZoneId) = listOf(
    gymFixture(NIGHT_TODAY.minusDays(2), zone, id = "log-g"),
    zoneFixture(NIGHT_TODAY.minusDays(2), zone, "log-z", h = 19),
)

val LONG_TITLE = "Upper body strength and core"

/**
 * 11 nights in the 14 days to Thu Nov 26 (none on Nov 15, Nov 20, Nov 26); the longest 7 h 40 m, the shortest 5 h 40 m on
 * Nov 18. Gym on Nov 17, 19, 21, 24; a zone 2 run on Nov 18, 19, 24; intervals on Nov 19 only; [LONG_TITLE] on Nov 22.
 */
fun chartSpans(zone: ZoneId, longestMin: Int = 460): List<SleepSpan> {
    val minutes = mapOf(13 to 420, 14 to 395, 16 to 410, 17 to 430, 18 to 340, 19 to 400, 21 to longestMin, 22 to 415, 23 to 385, 24 to 425, 25 to 440)
    return minutes.map { (d, m) -> nightSpan(LocalDate.of(2026, 11, d), m, zone) }
}

fun chartFixtures(zone: ZoneId): List<PictureFixture> {
    fun day(d: Int) = LocalDate.of(2026, 11, d)
    val long = gymFixture(day(22), zone, id = "c-long")
    return listOf(17, 19, 21, 24).map { gymFixture(day(it), zone, id = "c-g$it") } +
        listOf(18, 19, 24).map { zoneFixture(day(it), zone, "c-z$it") } +
        intervalFixture(day(19), zone, "c-i19", h = 20) +
        long.copy(session = long.session.copy(programName = LONG_TITLE))
}

class RecordingRegistryOwner(private val onGrant: () -> Unit) : ActivityResultRegistryOwner {
    val inputs = mutableListOf<Any?>()
    override val activityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            inputs += input
            onGrant()
            dispatchResult(requestCode, input)
        }
    }
}
