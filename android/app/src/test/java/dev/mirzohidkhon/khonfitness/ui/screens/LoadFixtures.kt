package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.room.withTransaction
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Modality
import java.time.LocalDate
import java.time.ZoneId

const val LOAD_FIXTURE_MAX = 186
private val BREAK_WEEKS = setOf(9, 10, 30)
const val HEAVY_WEEK = 3
private const val EASY_LIFT = -25

/**
 * About 150 finished sessions over the 52 weeks to [today]: gym on Mondays, intervals on Thursdays, a zone 2 run on
 * Saturdays, easy every third week, with a gym session the same day every sixth week. Three weeks are off, and one week
 * has gym and a run each day. Some gym and interval sessions have no heart rate row.
 */
fun loadYearFixtures(today: LocalDate, zone: ZoneId): List<PictureFixture> {
    val out = mutableListOf<PictureFixture>()
    val current = weekOf(today)
    for (w in 0 until LOAD_WEEKS) {
        if (w in BREAK_WEEKS) continue
        val monday = current.minusWeeks(w.toLong())
        fun day(d: Long) = monday.plusDays(d).takeIf { it <= today }
        if (w == HEAVY_WEEK) {
            (0L until 7L).forEach { d ->
                out += gymFixture(day(d)!!, zone, id = "hg$w-$d")
                out += zoneFixture(day(d)!!, zone, id = "hz$w-$d", lift = d.toInt() * 2)
            }
            continue
        }
        day(0)?.let { out += gymFixture(it, zone, heart = w % 4 != 1, id = "g$w") }
        day(3)?.let { d -> out += intervalFixture(d, zone, "i$w", lift = (w * 7) % 13 - 6).let { if (w % 5 == 2) it.copy(row = null) else it } }
        day(5)?.let { d ->
            out += zoneFixture(d, zone, "z$w", lift = if (w % 3 == 1) EASY_LIFT else (w * 5) % 11 - 5)
            if (w % 6 == 0) out += gymFixture(d, zone, id = "gs$w")
        }
    }
    return out
}

/** Three sessions: a gym session and a run with heart rate, an interval session without. */
fun loadFewFixtures(today: LocalDate, zone: ZoneId): List<PictureFixture> {
    val current = weekOf(today)
    return listOf(
        gymFixture(current.minusWeeks(2), zone, id = "few-g"),
        zoneFixture(current.minusWeeks(5).plusDays(5), zone, "few-z"),
        intervalFixture(current.minusWeeks(20).plusDays(3), zone, "few-i").copy(row = null),
    )
}

suspend fun seedLoad(app: KhonApp, fixtures: List<PictureFixture>) {
    val dao = app.db.dao()
    app.db.withTransaction {
        dao.upsertExercises(listOf(HIGH_EXERCISE, LOW_EXERCISE))
        dao.upsert(Modality("bike", "Bike", 0xFFB39DFF))
        dao.upsert(Modality("run", "Run", 0xFF7CE0A8))
        fixtures.forEach { f ->
            dao.upsert(f.session)
            if (f.logs.isNotEmpty()) dao.upsertSetLogs(f.logs)
            if (f.rounds.isNotEmpty()) dao.upsertIntervalLogs(f.rounds)
            f.row?.let { dao.upsertSessionHr(it) }
        }
    }
}
