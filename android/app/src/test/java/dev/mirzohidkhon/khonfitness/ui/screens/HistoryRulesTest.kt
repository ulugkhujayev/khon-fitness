package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.PlanItem
import dev.mirzohidkhon.khonfitness.data.Program
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.iso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** History rules: the Log week, session lines, records, bodyweight range and cardio types (UI06 P3). */
class HistoryRulesTest {
    // Wednesday, Nov 4, 2026; the week runs Nov 2 – 8.
    private val today = LocalDate.of(2026, 11, 4)
    private val monday = LocalDate.of(2026, 11, 2)
    private val push = PlanItem(ItemType.PROGRAM, program = Program("push", "Push"))
    private val run = PlanItem(ItemType.CARDIO, cardio = Exercise("run", "Easy run", Kind.CARDIO, modalityId = "running"))
    private val rest = PlanItem(ItemType.REST)
    /** Mon push, Tue run, Wed push, Thu rest, Fri run, Sat push, Sun rest. */
    private val plan: (LocalDate) -> PlanItem = { listOf(push, run, push, rest, run, push, rest)[it.dayOfWeek.value - 1] }

    private fun gym(id: String, date: LocalDate, start: Long = 0, finished: Boolean = true) =
        Session(id, date.iso(), startedAt = start, finishedAt = if (finished) start + 58 * 60_000 else null, itemType = ItemType.PROGRAM, programId = "push", programName = "Push")
    private fun cardio(id: String, date: LocalDate, sec: Int?, m: Int?, ex: String = "run", start: Long = 0, source: String? = null) =
        Session(id, date.iso(), startedAt = start, finishedAt = start + 1, itemType = ItemType.CARDIO, exerciseId = ex, exerciseName = "Easy run", timeSec = sec, distanceM = m, sourceId = source)
    private var n = 0
    private fun set(session: String, kg: Double?, reps: Int?, ex: String = "bench", status: String = SetStatus.DONE) =
        SetLog("l${n++}", session, 0, false, 0, n, n, ex, "Bench press", 8, 12, 2.5, weightKg = kg, reps = reps, status = status)

    // ---- Week header and bounds

    @Test fun `week title within a month, across months and across years`() {
        assertEquals("Nov 2 – 8, 2026", weekTitle(monday))
        assertEquals("Oct 26 – Nov 1, 2026", weekTitle(LocalDate.of(2026, 10, 26)))
        assertEquals("Dec 28, 2026 – Jan 3, 2027", weekTitle(LocalDate.of(2026, 12, 28)))
    }

    @Test fun `weeks run from the week of the first session to this week, only this week without sessions`() {
        assertEquals(monday..monday, logWeeks(emptyList(), today))
        val weeks = logWeeks(listOf(gym("a", LocalDate.of(2026, 10, 14)), gym("b", LocalDate.of(2026, 11, 3))), today)
        assertEquals(LocalDate.of(2026, 10, 12), weeks.start)
        assertEquals(monday, weeks.endInclusive)
    }

    // ---- Rows

    @Test fun `each day gets its state, two sessions on one day give two rows`() {
        val sessions = listOf(gym("mon", monday), cardio("tue2", monday.plusDays(1), 1800, 5000, start = 20), cardio("tue1", monday.plusDays(1), 900, 2000, start = 10))
        val rows = logRows(monday, today, sessions, plan)
        assertEquals(
            listOf(DayKind.DONE, DayKind.DONE, DayKind.DONE, DayKind.TODAY, DayKind.REST, DayKind.PLANNED, DayKind.PLANNED, DayKind.REST),
            rows.map { it.kind },
        )
        assertEquals(listOf("mon", "tue1", "tue2"), rows.take(3).map { it.session?.id })
    }

    @Test fun `a past planned day without a session is skipped, a running session is in progress`() {
        val rows = logRows(monday, today, listOf(gym("now", today, finished = false)), plan)
        assertEquals(DayKind.SKIPPED, rows[0].kind)
        assertEquals("Push", rows[0].item.name)
        assertEquals(DayKind.RUNNING, rows[2].kind)
        assertEquals("now", rows[2].session?.id)
    }

    @Test fun `first use shows planned rows only`() {
        val rows = logRows(monday, monday, emptyList(), plan)
        assertEquals(DayKind.TODAY, rows[0].kind)
        assertEquals(listOf(DayKind.PLANNED, DayKind.PLANNED, DayKind.REST, DayKind.PLANNED, DayKind.PLANNED, DayKind.REST), rows.drop(1).map { it.kind })
        val w = weekSummary(monday, monday, emptyList(), emptyList(), plan)
        assertEquals(0, w.done); assertEquals(5, w.planned)
        assertEquals("5 to go", weekTail(w))
        assertEquals("0 sets · 0 t lifted · 0 min cardio · 0 km", weekTotals(w))
    }

    // ---- Week card

    @Test fun `this week counts done, to go and missed from the plan each day had`() {
        // Mon done, Tue missed, Wed (today) open, Fri and Sat ahead; a session on rest Thursday is not a planned day.
        val sessions = listOf(gym("mon", monday), cardio("thu", monday.plusDays(3), 1200, 3000))
        val w = weekSummary(monday, today, sessions, emptyList(), plan)
        assertEquals(1, w.done); assertEquals(5, w.planned); assertEquals(1, w.missed); assertEquals(3, w.toGo)
        assertEquals("3 to go", weekTail(w))
    }

    @Test fun `a past week says missed or full week`() {
        val last = monday.minusWeeks(1)
        val all = listOf(0L, 1, 2, 4, 5).map { gym("d$it", last.plusDays(it)) }
        assertEquals("Full week", weekTail(weekSummary(last, today, all, emptyList(), plan)))
        assertEquals("2 missed", weekTail(weekSummary(last, today, all.drop(2), emptyList(), plan)))
    }

    @Test fun `a one-day change counts as the plan of that day`() {
        val changed: (LocalDate) -> PlanItem = { if (it == monday) rest else plan(it) }
        assertEquals(4, weekSummary(monday, today, emptyList(), emptyList(), changed).planned)
    }

    @Test fun `totals count done sets, kg lifted, cardio minutes and km of finished sessions`() {
        val sessions = listOf(gym("g", monday), gym("open", today, finished = false), cardio("c", monday.plusDays(1), 1800, 5200), cardio("hc", monday.plusDays(1), 600, 1000, source = "x"))
        val logs = listOf(set("g", 60.0, 10), set("g", 62.5, 8), set("g", 60.0, 8, status = SetStatus.SKIPPED), set("open", 100.0, 10))
        val w = weekSummary(monday, today, sessions, logs, plan)
        assertEquals(2, w.sets)
        assertEquals("2 sets · 1.1 t lifted · 40 min cardio · 6.2 km", weekTotals(w))
    }

    @Test fun `the bar colors done days, grays open days and darkens rest`() {
        val bar = weekBar(monday, listOf(gym("mon", monday), gym("open", today, finished = false)), plan)
        assertEquals(listOf(BarPart.DONE, BarPart.OPEN, BarPart.OPEN, BarPart.REST, BarPart.OPEN, BarPart.OPEN, BarPart.REST), bar.map { it.first })
        assertEquals("mon", bar[0].second?.id)
    }

    // ---- Session lines

    @Test fun `gym line has done of all sets, tons and minutes`() {
        val logs = listOf(set("g", 60.0, 10), set("g", 60.0, 10), set("g", 60.0, null, status = SetStatus.SKIPPED))
        assertEquals("2 of 3 sets · 1.2 t · 58 min", sessionLine(gym("g", monday), logs, null))
        assertEquals("2 of 3 sets", setsLine("g", logs))
    }

    @Test fun `cardio line has km, minutes, pace and the Health Connect source`() {
        assertEquals("5.2 km · 28 min · 5:23\u00A0/\u2060km", sessionLine(cardio("c", monday, 28 * 60, 5200), emptyList(), run.cardio))
        assertEquals("1 km · 10 min · 10:00\u00A0/\u2060km · Health Connect", sessionLine(cardio("c", monday, 600, 1000, source = "hc1"), emptyList(), run.cardio))
    }

    @Test fun `swim pace is per 100 m and laps count as distance`() {
        val swim = Exercise("sw", "Swim", Kind.CARDIO, modalityId = "swim")
        val s = Session("s", monday.iso(), 0, 1, ItemType.CARDIO, exerciseId = "sw", timeSec = 1500, laps = 40, poolLength = 25)
        assertEquals("1 km · 25 min · 2:30\u00A0/\u2060100\u00A0m", sessionLine(s, emptyList(), swim))
        assertNull(pace(600, 0, false))
    }

    // ---- Records

    private fun day(i: Long) = monday.minusDays(30 - i)

    @Test fun `a record is the heaviest weight, then more reps`() {
        val sessions = listOf(gym("a", day(0)), gym("b", day(2)), gym("c", day(4)))
        val logs = listOf(set("a", 60.0, 8), set("a", 62.5, 5), set("b", 62.5, 6), set("c", 60.0, 12))
        val h = liftHistory("bench", logs, sessions)
        assertEquals(listOf(true, true, false), h.map { it.record })
        assertEquals("62.5 kg × 6", setText(h[1].best))
        assertEquals(1, recordCount("b", liftHistories(logs, sessions)))
        assertEquals(0, recordCount("c", liftHistories(logs, sessions)))
    }

    @Test fun `a tie on weight and reps keeps the record with the earlier session`() {
        val sessions = listOf(gym("a", day(0)), gym("b", day(2)))
        val logs = listOf(set("a", 62.5, 6), set("b", 62.5, 6))
        val histories = liftHistories(logs, sessions)
        assertEquals(listOf(true, false), histories.getValue("bench").map { it.record })
        assertEquals("a", personalRecords(histories, emptyList()).single().session.id)
    }

    @Test fun `0-rep sets, skipped sets and open sessions never count`() {
        val sessions = listOf(gym("a", day(0)), gym("open", day(2), finished = false))
        val logs = listOf(set("a", 100.0, 0), set("a", 120.0, 5, status = SetStatus.SKIPPED), set("a", 60.0, 8), set("open", 200.0, 5))
        val h = liftHistory("bench", logs, sessions)
        assertEquals(1, h.size)
        assertEquals("60 kg × 8", setText(h.single().best))
        assertEquals(1, h.single().sets.size)
    }

    @Test fun `an exercise's first session is not counted as a PR in the Log`() {
        val histories = liftHistories(listOf(set("a", 60.0, 8)), listOf(gym("a", day(0))))
        assertEquals(0, recordCount("a", histories))
    }

    @Test fun `records list the newest record first with names from the library`() {
        val sessions = listOf(gym("a", day(0)), gym("b", day(5)))
        val logs = listOf(set("a", 60.0, 8, ex = "bench"), set("b", 40.0, 10, ex = "row"), set("b", 50.0, 10, ex = "bench").copy(exerciseName = "Bench press"))
        val recs = personalRecords(liftHistories(logs, sessions), listOf(Exercise("row", "Barbell row", Kind.STRENGTH)))
        assertEquals(listOf("Barbell row", "Bench press"), recs.map { it.name })
        assertEquals("a", recs[1].session.id)
    }

    @Test fun `a set without weight reads as reps`() {
        assertEquals("12 reps", setText(set("a", null, 12)))
        assertEquals("×12", shortSet(set("a", 0.0, 12)))
        assertEquals("62.5×6", shortSet(set("a", 62.5, 6)))
    }

    // ---- Bodyweight

    @Test fun `bodyweight range and change since its first entry`() {
        val all = listOf(Bodyweight("2026-07-01", 86.0), Bodyweight("2026-10-05", 84.6), Bodyweight("2026-10-30", 84.2))
        assertEquals(2, bodyweightsIn(BodyRange.MONTH, all, today).size)
        assertEquals(3, bodyweightsIn(BodyRange.ALL, all, today).size)
        assertEquals("−0.4 kg since Oct\u00A05", bodyweightChange(bodyweightsIn(BodyRange.MONTH, all, today)))
        assertNull(bodyweightChange(listOf(all.last())))
        assertEquals(0, bodyweightsIn(BodyRange.MONTH, all.take(1), today).size)
    }

    // ---- Cardio

    @Test fun `cardio of the last 30 days groups by type with totals and pace`() {
        val ex = listOf(run.cardio!!, Exercise("sw", "Swim", Kind.CARDIO, modalityId = "swim"))
        val sessions = listOf(
            cardio("r1", today.minusDays(3), 1800, 5000), cardio("r2", today.minusDays(1), 3600, 10000), cardio("old", today.minusDays(40), 1800, 5000),
            cardio("s1", today.minusDays(2), 1500, 1000, ex = "sw"),
        )
        val types = cardioTypes(sessions, ex, today, order = listOf("swim", "running"))
        assertEquals(listOf("swim", "running"), types.map { it.id })
        assertEquals(listOf("r1", "r2"), types[1].sessions.map { it.id })
        assertEquals("2 sessions · 15 km · 1 h 30 m · 6:00\u00A0/\u2060km", cardioLine(types[1]))
        assertEquals("1 session · 1 km · 25 m · 2:30\u00A0/\u2060100\u00A0m", cardioLine(types[0]))
    }
}
