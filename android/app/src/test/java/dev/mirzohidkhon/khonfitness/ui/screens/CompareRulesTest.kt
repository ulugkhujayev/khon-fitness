package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Program
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.StretchSession
import dev.mirzohidkhon.khonfitness.data.iso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CompareRulesTest {
    private val today = LocalDate.of(2026, 11, 27)
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)

    private val exercises = listOf(
        Exercise("bench", "Bench press", Kind.STRENGTH, primaryMuscle = "chest", secondaryMuscles = "shoulders,triceps"),
        Exercise("squat", "Back squat", Kind.STRENGTH, primaryMuscle = "quads", secondaryMuscles = "glutes"),
        Exercise("row", "Cable row", Kind.STRENGTH, primaryMuscle = "upper back", secondaryMuscles = "lats,biceps"),
        Exercise("run", "Easy run", Kind.CARDIO, modalityId = "run"),
        Exercise("bike", "Bike", Kind.CARDIO, modalityId = "bike"),
        Exercise("swim", "Easy swim", Kind.CARDIO, modalityId = "swim"),
        Exercise("erg", "Rower", Kind.CARDIO, modalityId = "rowing"),
    )

    private fun gym(id: String, date: LocalDate, finished: Boolean = true, program: String = "push") =
        Session(id, date.iso(), startedAt = date.toEpochDay(), finishedAt = if (finished) date.toEpochDay() + 1 else null, itemType = ItemType.PROGRAM, programId = program, programName = program)
    private fun cardio(id: String, date: LocalDate, ex: String, sec: Int? = null, m: Int? = null, laps: Int? = null, pool: Int? = null) =
        Session(id, date.iso(), startedAt = date.toEpochDay(), finishedAt = date.toEpochDay() + 1, itemType = ItemType.CARDIO, exerciseId = ex, exerciseName = ex, timeSec = sec, distanceM = m, laps = laps, poolLength = pool)
    private var n = 0
    private fun set(session: String, kg: Double?, reps: Int?, ex: String = "bench", status: String = SetStatus.DONE) =
        SetLog("l${n++}", session, 0, false, 0, n, n, ex, exercises.find { it.id == ex }?.name ?: "Lost lift", 8, 12, 2.5, weightKg = kg, reps = reps, status = status)

    private fun stats(p: Period, sessions: List<Session> = emptyList(), logs: List<SetLog> = emptyList(), stretches: List<StretchSession> = emptyList(), bw: List<Bodyweight> = emptyList()) =
        periodStats(p, sessions, logs, exercises, stretches, bw)

    @Test fun `C1 a finished session on the first or last day counts, one a day outside or unfinished does not`() {
        val p = Period(d(10, 1), d(10, 31))
        val sessions = listOf(gym("from", d(10, 1)), gym("to", d(10, 31)), gym("before", d(9, 30)), gym("after", d(11, 1)), gym("open", d(10, 15), finished = false))
        assertEquals(listOf("from", "to"), sessions.filter { it.countsIn(p) }.map { it.id })
        assertEquals(2, stats(p, sessions).gymSessions)
        assertEquals(31, p.days)
        assertEquals(31 / 7.0, p.weeks, 1e-9)
    }

    @Test fun `C2 presets from Nov 27, 2026, neighbors without gap or overlap`() {
        assertEquals(Period(d(10, 31), d(11, 27)), Preset.LAST_4.period(today))
        assertEquals(Period(d(10, 3), d(10, 30)), Preset.BEFORE_4.period(today))
        assertEquals(Period(d(9, 5), d(11, 27)), Preset.LAST_12.period(today))
        assertEquals(Period(d(6, 13), d(9, 4)), Preset.BEFORE_12.period(today))
        assertEquals(Period(d(1, 1), d(11, 27)), Preset.THIS_YEAR.period(today))
        assertEquals(Period(d(1, 1, 2025), d(12, 31, 2025)), Preset.LAST_YEAR.period(today))
        listOf(Preset.BEFORE_4 to Preset.LAST_4, Preset.BEFORE_12 to Preset.LAST_12, Preset.LAST_YEAR to Preset.THIS_YEAR).forEach { (a, b) ->
            assertEquals("$a then $b", a.period(today).to.plusDays(1), b.period(today).from)
        }
        assertEquals(28, Preset.LAST_4.period(today).days)
        assertEquals(84, Preset.BEFORE_12.period(today).days)
    }

    @Test fun `C3 totals count sessions, counted sets, lifted kg, cardio time, meters with laps, and stretches`() {
        val p = Preset.LAST_4.period(today)
        val sessions = listOf(gym("g1", d(11, 10)), gym("g2", d(11, 20)), cardio("c", d(11, 12), "run", sec = 1800, m = 5000), cardio("s", d(11, 14), "swim", sec = 1500, laps = 40, pool = 25))
        val logs = listOf(
            set("g1", 60.0, 8), set("g1", 62.5, 6), set("g1", 60.0, 5, status = SetStatus.SKIPPED),
            set("g2", 80.0, 0), set("g2", 100.0, 5, ex = "squat"), set("g2", 100.0, null, ex = "squat"), set("g2", 50.0, 10, status = SetStatus.PENDING),
        )
        val st = listOf(StretchSession("st", "daily", "Daily", d(11, 15).iso(), 1L, 600, 9, 0), StretchSession("old", "daily", "Daily", d(10, 1).iso(), 1L, 600, 9, 0))
        val s = stats(p, sessions, logs, st)
        assertEquals(2, s.gymSessions)
        assertEquals(3, s.sets)
        assertEquals(60.0 * 8 + 62.5 * 6 + 100.0 * 5, s.liftedKg, 1e-9)
        assertEquals(2, s.cardioSessions)
        assertEquals(3300, s.cardioSec)
        assertEquals(6000, s.meters)
        assertEquals(1, s.stretches)
        val first = compareGroups(s, s, exercises).first().rows.associate { it.label to it.left }
        assertEquals(mapOf("Days" to "28", "Gym sessions" to "2", "Sets" to "3", "Sets per week" to "0.8", "Lifted" to "1.4 t", "Cardio sessions" to "2",
            "Cardio time" to "55 m", "Distance" to "6.0 km", "Stretching" to "1"), first)
    }

    @Test fun `C4 sets per week over 28 and over 10 days`() {
        val logs = (0 until 14).map { set("g", 60.0, 8) }
        val four = stats(Period(d(11, 1), d(11, 28)), listOf(gym("g", d(11, 3))), logs)
        val ten = stats(Period(d(11, 1), d(11, 10)), listOf(gym("g", d(11, 3))), logs)
        assertEquals(3.5, four.setsPerWeek, 1e-9)
        assertEquals("3.5", compareGroups(four, ten, exercises).first().rows.single { it.label == "Sets per week" }.left)
        assertEquals("9.8", compareGroups(four, ten, exercises).first().rows.single { it.label == "Sets per week" }.right)
    }

    @Test fun `C5 bodyweight is the mean of the period's entries and their count`() {
        val p = Period(d(11, 1), d(11, 27))
        val bw = listOf(Bodyweight(d(11, 2).iso(), 83.0), Bodyweight(d(11, 10).iso(), 82.5), Bodyweight(d(11, 20).iso(), 82.0), Bodyweight(d(10, 20).iso(), 90.0))
        val with = stats(p, bw = bw)
        assertEquals(82.5, with.bodyweightKg!!, 1e-9)
        assertEquals(3, with.bodyweights)
        val none = stats(Period(d(9, 1), d(9, 30)), bw = bw)
        assertNull(none.bodyweightKg)
        val rows = compareGroups(none, with, exercises).single { it.title == "Bodyweight" }.rows
        assertEquals(listOf(CompareRow("Average", "–", "82.5 kg"), CompareRow("Entries", "0", "3")), rows)
        assertTrue(compareGroups(none, none, exercises).none { it.title == "Bodyweight" })
    }

    @Test fun `C6 sets per muscle match the weekly rule, and two weeks give half the sum`() {
        val lastMonday = weekOf(today).minusWeeks(1)
        val sessions = listOf(gym("a", lastMonday), gym("b", lastMonday.plusDays(3)), gym("c", lastMonday.plusDays(8)), gym("open", lastMonday.plusDays(4), finished = false))
        val logs = listOf(
            set("a", 60.0, 8), set("a", 60.0, 8), set("a", 60.0, 8, ex = "row"), set("a", 60.0, 8, status = SetStatus.SKIPPED),
            set("b", 100.0, 5, ex = "squat"), set("b", 60.0, 8), set("open", 60.0, 8),
            set("c", 60.0, 8), set("c", 60.0, 8, ex = "row"), set("c", 60.0, 8, ex = "row"),
        )
        val week = Period(lastMonday, lastMonday.plusDays(6))
        val weekly = muscleVolume(logs, sessions, exercises, today).associate { it.muscle to it.lastWeek }.filterValues { it > 0 }
        assertEquals(weekly, stats(week, sessions, logs).musclesPerWeek)
        assertEquals(mapOf("chest" to 3.0, "shoulders" to 1.5, "triceps" to 1.5, "upper back" to 1.0, "lats" to 0.5, "biceps" to 0.5, "quads" to 1.0, "glutes" to 0.5), weekly)
        val two = Period(lastMonday, lastMonday.plusDays(13))
        val sum = muscleSets(two, logs, sessions, exercises)
        assertEquals(sum.mapValues { it.value / 2 }, stats(two, sessions, logs).musclesPerWeek)
        assertEquals(4.0, sum["chest"]!!, 0.0)
        val rows = compareGroups(stats(week, sessions, logs), stats(two, sessions, logs), exercises).single { it.title == "Sets per muscle · per week" }.rows
        assertEquals(listOf("Chest", "Upper back", "Lats", "Shoulders", "Biceps", "Triceps", "Glutes", "Quads"), rows.map { it.label })
        assertEquals(CompareRow("Chest", "3.0", "2.0"), rows.first())
    }

    @Test fun `C7 best set per exercise in both periods, in one, and only uncounted`() {
        val l = Period(d(10, 1), d(10, 31)); val r = Period(d(11, 1), d(11, 27))
        val sessions = listOf(gym("a", d(10, 5)), gym("b", d(11, 5)))
        val logs = listOf(
            set("a", 60.0, 8), set("a", 62.5, 5), set("b", 65.0, 6), set("b", 65.0, 4),
            set("a", 100.0, 5, ex = "squat"),
            set("b", 50.0, 10, ex = "row", status = SetStatus.SKIPPED), set("b", 50.0, 0, ex = "row"),
            set("b", 20.0, 12, ex = "gone"),
        )
        val rows = compareGroups(stats(l, sessions, logs), stats(r, sessions, logs), exercises).single { it.title == "Best set" }.rows
        assertEquals(listOf(CompareRow("Back squat", "100 kg × 5", "–"), CompareRow("Bench press", "62.5 kg × 5", "65 kg × 6"), CompareRow("Lost lift", "–", "20 kg × 12")), rows)
    }

    @Test fun `C8 cardio per type with the existing pace, a row of two dashes left out`() {
        val l = Period(d(10, 1), d(10, 31)); val r = Period(d(11, 1), d(11, 27))
        val sessions = listOf(
            cardio("r1", d(10, 3), "run", 1800, 6000), cardio("r2", d(11, 3), "run", 1500, 5000), cardio("r3", d(11, 4), "run", 1500, 5000), cardio("r4", d(11, 5), "run", 600),
            cardio("b1", d(11, 6), "bike", 3600, 30000),
            cardio("e1", d(10, 7), "erg", 1200), cardio("e2", d(11, 7), "erg", 900),
        )
        val names = mapOf("run" to "Run", "bike" to "Bike", "rowing" to "Row")
        val rows = compareGroups(stats(l, sessions), stats(r, sessions), exercises, { names[it] ?: "Other" }, listOf("run", "bike", "rowing")).single { it.title == "Cardio" }.rows
        assertEquals(listOf(
            CompareRow("Run · sessions", "1", "3"), CompareRow("Run · distance", "6.0 km", "10.0 km"), CompareRow("Run · pace", pace(1800, 6000, false)!!, pace(3000, 10000, false)!!),
            CompareRow("Bike · sessions", "0", "1"), CompareRow("Bike · distance", "–", "30.0 km"), CompareRow("Bike · pace", "–", pace(3600, 30000, false)!!),
            CompareRow("Row · sessions", "1", "1"),
        ), rows)
        assertEquals("5:00 /⁠km", rows[2].left)
        val run = stats(r, sessions).cardio.single { it.id == "run" }
        assertEquals(listOf("r2", "r3", "r4"), run.sessions.map { it.id })
        assertEquals(1500 + 1500 + 600, run.seconds)
        assertEquals(10000, run.meters)
        assertEquals(pace(3000, 10000, false), run.pace)
        val erg = stats(r, sessions).cardio.single { it.id == "rowing" }
        assertEquals(listOf(900, 0, null), listOf(erg.seconds, erg.meters, erg.pace))
    }

    @Test fun `C9 a program's period runs from its first finished session to its last`() {
        val sessions = listOf(gym("a", d(9, 2)), gym("b", d(10, 1)), gym("c", d(11, 20)), gym("open", d(11, 25), finished = false), gym("x", d(11, 1), program = "pull"))
        assertEquals(Period(d(9, 2), d(11, 20)), programPeriod("push", sessions))
        assertNull(programPeriod("legs", sessions))
        val programs = listOf(Program("legs", "Legs"), Program("push", "Push"), Program("pull", "Pull"))
        assertEquals(listOf("push", "pull"), programChoices(programs, sessions).map { it.first.id })
        assertEquals("Sep 2 – Nov 20", periodRange(programPeriod("push", sessions)!!, today))
    }

    @Test fun `C10 a From after the To moves the To to the same day`() {
        val p = Period(d(10, 1), d(10, 10))
        assertEquals(Period(d(10, 20), d(10, 20)), withFrom(p, d(10, 20)))
        assertEquals(Period(d(9, 20), d(10, 10)), withFrom(p, d(9, 20)))
        assertEquals(Period(d(10, 1), d(10, 30)), withTo(p, d(10, 30)))
        assertEquals(Period(d(9, 1), d(9, 1)), withTo(p, d(9, 1)))
    }

    @Test fun `C11 with no data only the first group shows, with zeros`() {
        val empty = stats(Preset.LAST_4.period(today))
        val groups = compareGroups(stats(Preset.BEFORE_4.period(today)), empty, exercises)
        assertEquals(1, groups.size)
        assertNull(groups.single().title)
        assertEquals(listOf("28", "0", "0", "0.0", "0 t", "0", "0 m", "0.0 km", "0"), groups.single().rows.map { it.right })
        assertEquals(groups.single().rows.map { it.left }, groups.single().rows.map { it.right })
    }

    @Test fun `C13 no verdict words in any label`() {
        val sessions = listOf(gym("a", d(11, 5)), cardio("r", d(11, 6), "run", 1800, 6000))
        val logs = listOf(set("a", 60.0, 8))
        val p = Preset.LAST_4.period(today)
        val text = compareGroups(stats(p, sessions, logs, bw = listOf(Bodyweight(d(11, 2).iso(), 83.0))), stats(Preset.BEFORE_4.period(today)), exercises)
            .flatMap { g -> listOfNotNull(g.title) + g.rows.flatMap { listOf(it.label, it.left, it.right) } }.joinToString(" ").lowercase()
        for (w in listOf("better", "worse", "up", "down", "improve", "gain", "loss", "best period", "↑", "↓", "+", "should", "try")) assertFalse(w, Regex("\\b${Regex.escape(w)}\\b").containsMatchIn(text) || (w.length == 1 && w in text))
    }

    @Test fun `ranges carry the year only outside this year, spoken rows name both ranges`() {
        assertEquals("Oct 3 – Oct 30", periodRange(Preset.BEFORE_4.period(today), today))
        assertEquals("Jan 1, 2025 – Dec 31, 2025", periodRange(Preset.LAST_YEAR.period(today), today))
        assertEquals("Dec 1, 2025 – Jan 5", periodRange(Period(d(12, 1, 2025), d(1, 5)), today))
        assertEquals("Nov 27", periodRange(Period(today, today), today))
        assertEquals("Sets, 34 in Oct 3 – Oct 30, none in Oct 31 – Nov 27", compareSpoken(CompareRow("Sets", "34", "–"), "Oct 3 – Oct 30", "Oct 31 – Nov 27"))
    }

    @Test fun `a saved choice reads back, an unknown key reads as null`() {
        val choices = listOf(PeriodChoice.Fixed(Preset.LAST_12), PeriodChoice.OfProgram("push:2"), PeriodChoice.Custom(Period(d(10, 1), d(10, 9))))
        choices.forEach { assertEquals(it, parseChoice(choiceKey(it))) }
        assertNull(parseChoice("preset:NOPE"))
        assertNull(parseChoice("custom:2026-10-09:2026-10-01"))
        assertNull(choicePeriod(PeriodChoice.OfProgram("none"), today, emptyList()))
    }
}
