package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.iso
import dev.mirzohidkhon.khonfitness.data.toDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Random

class ProgressBaseResultsTest {
    private val today = LocalDate.of(2026, 11, 27)
    private val exercises = listOf(
        Exercise("press", "Press", Kind.STRENGTH, primaryMuscle = "chest"),
        Exercise("curl", "Curl", Kind.STRENGTH, primaryMuscle = "forearms"),
        Exercise("fly", "Fly", Kind.STRENGTH, primaryMuscle = "chest", secondaryMuscles = "shoulders,triceps"),
        Exercise("dip", "Dip", Kind.STRENGTH, primaryMuscle = "triceps", secondaryMuscles = "chest"),
        Exercise("run", "Run", Kind.CARDIO, modalityId = "run"),
        Exercise("swim", "Swim", Kind.CARDIO, modalityId = "swim"),
        Exercise("bike", "Bike", Kind.CARDIO, modalityId = "bike"),
    )

    private fun gym(id: String, date: String, finished: Boolean = true) =
        Session(id, date, 1L, if (finished) 2L else null, ItemType.PROGRAM)
    private var n = 0
    private fun set(session: String, exercise: String, status: String = SetStatus.DONE) =
        SetLog("l${n++}", session, 0, false, 0, 0, 0, exercise, exercise, 8, 12, 2.5, reps = 8, status = status)

    @Test fun `weekly muscles keep the base order for tied rows`() {
        val sessions = listOf(gym("last", "2026-11-20"), gym("now", "2026-11-27"))
        val logs = listOf(set("last", "press"), set("now", "curl"))
        val result = muscleVolume(logs, sessions, exercises, today)
        assertEquals(listOf("chest", "forearms"), result.map { it.muscle })
        assertEquals(baseMuscleVolume(logs, sessions, exercises, today), result)
    }

    @Test fun `weekly muscles count a repeated session id once, by its last finished snapshot`() {
        val sessions = listOf(gym("same", "2026-11-20"), gym("same", "2026-11-27"))
        val logs = listOf(set("same", "press"))
        assertEquals(listOf(MuscleVolume("chest", 1.0, 0.0)), muscleVolume(logs, sessions, exercises, today))
        assertEquals(baseMuscleVolume(logs, sessions, exercises, today), muscleVolume(logs, sessions, exercises, today))
    }

    @Test fun `weekly muscles equal the base result for generated inputs`() {
        val rnd = Random(7)
        val ids = listOf("a", "b", "c", "d", "e")
        val lifts = listOf("press", "curl", "fly", "dip", "gone")
        val statuses = listOf(SetStatus.DONE, SetStatus.DONE, SetStatus.SKIPPED, SetStatus.PENDING)
        repeat(300) { case ->
            val sessions = List(rnd.nextInt(7)) {
                gym(ids[rnd.nextInt(ids.size)], today.minusDays(rnd.nextInt(18).toLong() - 2).iso(), finished = rnd.nextInt(5) > 0)
            }
            val logs = List(rnd.nextInt(10)) { set(ids[rnd.nextInt(ids.size)], lifts[rnd.nextInt(lifts.size)], statuses[rnd.nextInt(statuses.size)]) }
            assertEquals("case $case", baseMuscleVolume(logs, sessions, exercises, today), muscleVolume(logs, sessions, exercises, today))
        }
    }

    private fun cardio(id: String, date: LocalDate, ex: String, sec: Int?, m: Int?, finished: Boolean = true) =
        Session(id, date.iso(), date.toEpochDay(), if (finished) date.toEpochDay() + 1 else null, ItemType.CARDIO, exerciseId = ex, timeSec = sec, distanceM = m)

    @Test fun `the cardio range with a negative days value keeps every historical type with zero totals`() {
        val run = cardio("r", today, "run", 300, 1000)
        assertEquals(listOf(CardioType("run", emptyList(), 0, 0, null)), cardioTypes(listOf(run), exercises, today, days = -1))
        val old = cardio("s", today.minusDays(40), "swim", 900, 750)
        assertEquals(listOf(CardioType("swim", emptyList(), 0, 0, null)), cardioTypes(listOf(old), exercises, today))
    }

    @Test fun `the cardio range equals the base result for generated inputs`() {
        val rnd = Random(11)
        val lifts = listOf("run", "swim", "bike", "gone")
        repeat(300) { case ->
            val sessions = List(rnd.nextInt(8)) {
                cardio("c$it", today.minusDays(rnd.nextInt(70).toLong() - 5), lifts[rnd.nextInt(lifts.size)],
                    rnd.nextInt(3).takeIf { it > 0 }?.let { it * 600 }, rnd.nextInt(3).takeIf { it > 0 }?.let { it * 1500 }, finished = rnd.nextInt(6) > 0)
            }
            val days = rnd.nextInt(70).toLong() - 10
            val order = listOf("swim", "run")
            assertEquals("case $case", baseCardioTypes(sessions, exercises, today, days, order), cardioTypes(sessions, exercises, today, days, order))
        }
    }

    private fun baseMuscleVolume(logs: List<SetLog>, sessions: List<Session>, exercises: List<Exercise>, today: LocalDate): List<MuscleVolume> {
        val monday = today.with(DayOfWeek.MONDAY); val lastMonday = monday.minusWeeks(1)
        val dates = sessions.filter { it.finished }.associate { it.id to it.date.toDate() }
        val exById = exercises.associateBy { it.id }
        val acc = HashMap<String, DoubleArray>()
        for (l in logs) {
            if (l.status != SetStatus.DONE) continue
            val d = dates[l.sessionId] ?: continue
            val idx = when { d in monday..today -> 0; d in lastMonday..<monday -> 1; else -> continue }
            val ex = exById[l.exerciseId] ?: continue
            ex.primaryMuscle?.let { acc.getOrPut(it) { DoubleArray(2) }[idx] += 1.0 }
            ex.secondaryList.forEach { acc.getOrPut(it) { DoubleArray(2) }[idx] += 0.5 }
        }
        return acc.map { (m, v) -> MuscleVolume(m, v[0], v[1]) }.sortedByDescending { maxOf(it.thisWeek, it.lastWeek) }
    }

    private fun baseCardioTypes(sessions: List<Session>, exercises: List<Exercise>, today: LocalDate, days: Long, order: List<String>): List<CardioType> {
        val from = today.minusDays(days)
        val byId = exercises.associateBy { it.id }
        return sessions.filter { it.finished && it.itemType == ItemType.CARDIO && it.date.toDate() <= today }
            .groupBy { byId[it.exerciseId]?.modalityId ?: "" }
            .map { (type, all) ->
                val sorted = all.filter { it.date.toDate() >= from }.sortedWith(compareBy({ it.date }, { it.startedAt }))
                val both = sorted.filter { (it.timeSec ?: 0) > 0 && (meters(it) ?: 0) > 0 }
                CardioType(type, sorted, sorted.sumOf { meters(it) ?: 0 }, sorted.sumOf { it.timeSec ?: 0 },
                    pace(both.sumOf { it.timeSec ?: 0 }, both.sumOf { meters(it) ?: 0 }, type == "swim"))
            }
            .sortedBy { order.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }
}
