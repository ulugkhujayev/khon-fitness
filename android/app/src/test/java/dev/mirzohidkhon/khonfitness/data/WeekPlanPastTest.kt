package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** A week plan edit on Plan applies from today on: past weeks keep their planned items and their planned count. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WeekPlanPastTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao)
    }

    @After fun close() = db.close()

    private val today = LocalDate.of(2026, 11, 4) // a Wednesday

    private suspend fun data() = KhonRepository.PlanData(dao.weekPlanOnce(), dao.overridesOnce(), dao.programsOnce(), dao.exercisesOnce())

    /** Planned (not rest) days of the week that starts on [monday]. */
    private suspend fun planned(monday: LocalDate): List<String> = data().let { d -> (0L until 7L).map { monday.plusDays(it) }.filter { !d.itemFor(it).isRest }.map { it.toString() } }

    @Test fun `Friday to rest keeps last week's planned count and changes Fridays from today on`() = runBlocking {
        dao.upsertPrograms(listOf(Program("push", "Push", active = true)))
        dao.upsertWeekPlan(listOf(WeekPlan(1, ItemType.PROGRAM, "push"), WeekPlan(3, ItemType.PROGRAM, "push"), WeekPlan(5, ItemType.PROGRAM, "push")))
        dao.upsert(Session("s1", "2026-10-26", 1000L, itemType = ItemType.PROGRAM, programId = "push"))
        val lastWeek = LocalDate.of(2026, 10, 26); val thisWeek = LocalDate.of(2026, 11, 2)
        assertEquals(listOf("2026-10-26", "2026-10-28", "2026-10-30"), planned(lastWeek))

        repo.saveWeekPlan(5, ItemType.REST, null, today)
        repo.saveWeekPlan(3, ItemType.REST, null, today) // today's weekday: today follows the new plan

        assertEquals(listOf("2026-10-26", "2026-10-28", "2026-10-30"), planned(lastWeek))
        assertEquals(listOf("2026-11-02"), planned(thisWeek))
        assertEquals(listOf("2026-11-09"), planned(LocalDate.of(2026, 11, 9)))
    }
}
