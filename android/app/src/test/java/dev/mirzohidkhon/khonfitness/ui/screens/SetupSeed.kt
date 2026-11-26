package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.KhonDao
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.iso
import java.time.LocalDate

/** A 40-character program name, for the ellipsis. */
const val LONG_PROGRAM = "Upper Body Strength Block Twelve Weeks A"

/**
 * Exercises with setup changes, dated back from [today]: the bench in three periods (TP v3 3 × 8–12, TP v4 4 × 5–8
 * with a one-day 5-set deviation, TP v4 4 × 6–10), pull-ups without weight in two, a row under [LONG_PROGRAM] in two,
 * and curls in one.
 */
suspend fun seedSetupChanges(dao: KhonDao, today: LocalDate) {
    dao.upsertExercises(listOf(
        Exercise("bench", "Barbell Bench Press", Kind.STRENGTH, primaryMuscle = "chest"),
        Exercise("pullup", "Pull-up", Kind.STRENGTH, primaryMuscle = "lats"),
        Exercise("row", "Cable Seated Row", Kind.STRENGTH, primaryMuscle = "upper back"),
        Exercise("curl", "Biceps Curl", Kind.STRENGTH, primaryMuscle = "biceps"),
    ))
    val sessions = mutableListOf<Session>()
    val logs = mutableListOf<SetLog>()
    var n = 0
    fun day(ex: String, name: String, back: Long, program: String, programName: String, sets: Int, min: Int, max: Int, kg: Double, reps: Int) {
        val date = today.minusDays(back)
        val id = "$ex-$back"
        val at = date.toEpochDay() * 86_400_000 + 7 * 3_600_000
        sessions += Session(id, date.iso(), at, at + 3_480_000, ItemType.PROGRAM, programId = program, programName = programName)
        repeat(sets) { k ->
            val last = k == sets - 1 && kg > 0
            logs += SetLog("sl${n++}", id, 0, false, 0, k, k, ex, name, min, max, 2.5, weightKg = if (last) kg - 10 else kg, reps = if (last) reps + 2 else reps, status = SetStatus.DONE)
        }
    }
    val bench = "Barbell Bench Press"
    listOf(70L to (60.0 to 10), 66L to (62.5 to 9), 62L to (62.5 to 10), 58L to (65.0 to 8)).forEach { (b, s) -> day("bench", bench, b, "tp3", "TP v3", 3, 8, 12, s.first, s.second) }
    listOf(54L to (70.0 to 6), 50L to (72.5 to 5), 46L to (72.5 to 6), 42L to (75.0 to 5), 38L to (75.0 to 6), 30L to (77.5 to 5)).forEach { (b, s) -> day("bench", bench, b, "tp4", "TP v4", 4, 5, 8, s.first, s.second) }
    day("bench", bench, 34, "tp4", "TP v4", 5, 5, 8, 75.0, 5)
    listOf(26L to (72.5 to 8), 22L to (72.5 to 9), 18L to (75.0 to 7), 14L to (75.0 to 8), 10L to (75.0 to 9), 6L to (77.5 to 7), 2L to (77.5 to 8)).forEach { (b, s) -> day("bench", bench, b, "tp4", "TP v4", 4, 6, 10, s.first, s.second) }
    listOf(40L to 8, 33L to 9, 26L to 10).forEach { (b, r) -> day("pullup", "Pull-up", b, "tp3", "TP v3", 3, 6, 10, 0.0, r) }
    listOf(19L to 6, 12L to 6, 5L to 7).forEach { (b, r) -> day("pullup", "Pull-up", b, "tp4", "TP v4", 4, 5, 8, 0.0, r) }
    listOf(40L, 33L, 26L).forEach { b -> day("row", "Cable Seated Row", b, "tp3", "TP v3", 3, 8, 12, 50.0, 10) }
    listOf(19L to 12, 12L to 10, 5L to 11).forEach { (b, r) -> day("row", "Cable Seated Row", b, "long", LONG_PROGRAM, 3, 5, 5, 57.5, r) }
    listOf(20L, 13L, 6L).forEach { b -> day("curl", "Biceps Curl", b, "tp3", "TP v3", 3, 8, 12, 15.0, 10) }
    dao.upsertSessions(sessions)
    dao.upsertSetLogs(logs)
}
