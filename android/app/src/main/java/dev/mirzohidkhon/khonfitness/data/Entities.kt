package dev.mirzohidkhon.khonfitness.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Kind of an exercise: strength lives in programs, cardio is planned directly on a day. */
object Kind { const val STRENGTH = "strength"; const val CARDIO = "cardio" }
object Intensity { const val LOW = "low"; const val MODERATE = "moderate"; const val HIGH = "high" }
object SetStatus { const val PENDING = "pending"; const val DONE = "done"; const val SKIPPED = "skipped" }
object ItemType { const val PROGRAM = "program"; const val CARDIO = "cardio"; const val REST = "rest" }

val MUSCLE_GROUPS = listOf(
    "chest", "upper back", "lats", "shoulders", "biceps", "triceps", "forearms",
    "core", "lower back", "glutes", "quads", "hamstrings", "calves",
)

@Serializable
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val notes: String = "",
    val archived: Boolean = false,
    val builtin: Boolean = false,
    // strength
    val primaryMuscle: String? = null,
    val secondaryMuscles: String = "", // comma separated
    val stepKg: Double = 2.5,
    // cardio
    val modalityId: String? = null,
    val intensity: String? = null,
    val intervals: Boolean = false,
    val warmupSec: Int = 0,
    val workSec: Int = 0,
    val restSec: Int = 0,
    val rounds: Int = 0,
    val fields: String = "", // comma separated: time,distance,avgHr,watts,laps,poolLength,stroke
) {
    val secondaryList: List<String> get() = secondaryMuscles.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val fieldList: List<String> get() = fields.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@Serializable
@Entity(tableName = "modalities")
data class Modality(@PrimaryKey val id: String, val name: String, val color: Long, val archived: Boolean = false)

@Serializable
@Entity(tableName = "programs")
data class Program(@PrimaryKey val id: String, val name: String, val active: Boolean = false, val sortOrder: Int = 0)

@Serializable
@Entity(tableName = "blocks")
data class Block(
    @PrimaryKey val id: String,
    val programId: String,
    val sortOrder: Int,
    val superset: Boolean = true,
)

@Serializable
@Entity(tableName = "block_exercises")
data class BlockExercise(
    @PrimaryKey val id: String,
    val blockId: String,
    val slot: Int, // 0 = A, 1 = B
    val exerciseId: String,
    val sets: Int = 3,
    val minReps: Int = 8,
    val maxReps: Int = 12,
)

@Serializable
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val id: String,
    val date: String,          // yyyy-MM-dd
    val startedAt: Long,
    val finishedAt: Long? = null,
    val itemType: String,      // program | cardio
    val programId: String? = null,
    val programName: String? = null,
    val exerciseId: String? = null, // cardio exercise
    val exerciseName: String? = null,
    val note: String = "",
    val effort: Int? = null,
    // cardio metrics
    val timeSec: Int? = null,
    val distanceM: Int? = null,
    val avgHr: Int? = null,
    val watts: Int? = null,
    val laps: Int? = null,
    val poolLength: Int? = null,
    val stroke: String? = null,
    val sourceId: String? = null, // Health Connect record id when imported
) { val finished: Boolean get() = finishedAt != null }

/** One set inside a program session. Carries a snapshot of names so history never changes. */
@Serializable
@Entity(tableName = "set_logs")
data class SetLog(
    @PrimaryKey val id: String,
    val sessionId: String,
    val blockIndex: Int,
    val blockSuperset: Boolean,
    val slot: Int,
    val setIndex: Int,
    val order: Int,            // execution order within the session
    val exerciseId: String,
    val exerciseName: String,
    val minReps: Int,
    val maxReps: Int,
    val stepKg: Double,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val status: String = SetStatus.PENDING,
)

@Serializable
@Entity(tableName = "interval_logs")
data class IntervalLog(
    @PrimaryKey val id: String,
    val sessionId: String,
    val index: Int,
    val avgHr: Int? = null,
    val watts: Int? = null,
)

@Serializable
@Entity(tableName = "bodyweights")
data class Bodyweight(@PrimaryKey val date: String, val kg: Double)

/** Template: one row per weekday, 1 = Monday .. 7 = Sunday. */
@Serializable
@Entity(tableName = "week_plan")
data class WeekPlan(@PrimaryKey val weekday: Int, val itemType: String, val itemId: String? = null)

@Serializable
@Entity(tableName = "day_overrides")
data class DayOverride(@PrimaryKey val date: String, val itemType: String, val itemId: String? = null)
