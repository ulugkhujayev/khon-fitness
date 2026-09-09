package dev.mirzohidkhon.khonfitness.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun newId(): String = UUID.randomUUID().toString().substring(0, 8)
val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
fun LocalDate.iso(): String = format(ISO)
fun String.toDate(): LocalDate = LocalDate.parse(this, ISO)

/** What a day holds once template and override are resolved. */
data class PlanItem(val itemType: String, val program: Program? = null, val cardio: Exercise? = null, val overridden: Boolean = false) {
    val name: String get() = program?.name ?: cardio?.name ?: "Rest"
    val isRest: Boolean get() = itemType == ItemType.REST
}

data class ProgramDetail(val program: Program, val blocks: List<BlockDetail>)
data class BlockDetail(val block: Block, val exercises: List<Pair<BlockExercise, Exercise>>)

@Serializable
data class Backup(
    val version: Int = 1,
    val exercises: List<Exercise>, val modalities: List<Modality>, val programs: List<Program>,
    val blocks: List<Block>, val blockExercises: List<BlockExercise>, val sessions: List<Session>,
    val setLogs: List<SetLog>, val intervalLogs: List<IntervalLog>, val bodyweights: List<Bodyweight>,
    val weekPlan: List<WeekPlan>, val overrides: List<DayOverride>,
)

class KhonRepository(private val dao: KhonDao) {
    val exercises: Flow<List<Exercise>> = dao.exercises()
    val modalities: Flow<List<Modality>> = dao.modalities()
    val programs: Flow<List<Program>> = dao.programs()
    val sessions: Flow<List<Session>> = dao.sessions()
    val bodyweights: Flow<List<Bodyweight>> = dao.bodyweights()
    val weekPlan: Flow<List<WeekPlan>> = dao.weekPlan()
    val overrides: Flow<List<DayOverride>> = dao.overrides()
    val unfinishedSession: Flow<Session?> = dao.unfinishedSession()
    val allSetLogs: Flow<List<SetLog>> = dao.allSetLogs()
    val blocks: Flow<List<Block>> = dao.blocks()
    val blockExercises: Flow<List<BlockExercise>> = dao.blockExercises()
    val allIntervalLogs: Flow<List<IntervalLog>> = dao.allIntervalLogs()

    fun setLogs(sessionId: String): Flow<List<SetLog>> = dao.setLogs(sessionId)
    fun session(sessionId: String): Flow<Session?> = dao.sessionFlow(sessionId)
    fun intervalLogs(sessionId: String): Flow<List<IntervalLog>> = dao.intervalLogs(sessionId)

    /** Resolved plan for a date: override wins over the weekday template. */
    val planData: Flow<PlanData> = combine(weekPlan, overrides, programs, exercises) { plan, ov, progs, exs -> PlanData(plan, ov, progs, exs) }

    class PlanData(val plan: List<WeekPlan>, val overrides: List<DayOverride>, val programs: List<Program>, val exercises: List<Exercise>) {
        fun itemFor(date: LocalDate): PlanItem {
            val o = overrides.find { it.date == date.iso() }
            val type: String; val id: String?; val overridden: Boolean
            if (o != null) { type = o.itemType; id = o.itemId; overridden = true }
            else { val w = plan.find { it.weekday == date.dayOfWeek.value }; type = w?.itemType ?: ItemType.REST; id = w?.itemId; overridden = false }
            return when (type) {
                ItemType.PROGRAM -> programs.find { it.id == id }?.let { PlanItem(type, program = it, overridden = overridden) } ?: PlanItem(ItemType.REST)
                ItemType.CARDIO -> exercises.find { it.id == id }?.let { PlanItem(type, cardio = it, overridden = overridden) } ?: PlanItem(ItemType.REST)
                else -> PlanItem(ItemType.REST, overridden = overridden)
            }
        }
    }

    suspend fun programDetail(programId: String): ProgramDetail? {
        val program = dao.program(programId) ?: return null
        val exercises = dao.exercisesOnce().associateBy { it.id }
        val blocks = dao.blocksFor(programId).map { block ->
            BlockDetail(block, dao.blockExercisesFor(block.id).mapNotNull { be -> exercises[be.exerciseId]?.let { be to it } })
        }
        return ProgramDetail(program, blocks)
    }

    suspend fun exercise(id: String) = dao.exercise(id)
    suspend fun saveExercise(e: Exercise) = dao.upsert(e)
    suspend fun exerciseUsage(id: String): Int = dao.setLogCountFor(id) + dao.cardioSessionCountFor(id)
    suspend fun deleteExercise(id: String) { if (exerciseUsage(id) == 0) dao.deleteExercise(id) }
    suspend fun mergeExercise(from: String, into: String) {
        dao.moveSetLogs(from, into); dao.moveCardioSessions(from, into); dao.moveBlockExercises(from, into)
        dao.movePlanCardio(from, into); dao.moveOverrideCardio(from, into); dao.deleteExercise(from)
    }
    suspend fun saveModality(m: Modality) = dao.upsert(m)

    suspend fun saveProgram(p: Program) = dao.upsert(p)
    suspend fun setActiveProgram(id: String) = dao.setActiveProgram(id)
    suspend fun deleteProgram(id: String) {
        dao.blocksFor(id).forEach { dao.deleteBlockExercisesFor(it.id) }
        dao.deleteBlocksFor(id); dao.deleteProgram(id)
    }
    suspend fun saveBlock(b: Block) = dao.upsert(b)
    suspend fun saveBlocks(list: List<Block>) = dao.upsertBlocks(list)
    suspend fun deleteBlock(id: String) { dao.deleteBlockExercisesFor(id); dao.deleteBlock(id) }
    suspend fun saveBlockExercise(be: BlockExercise) = dao.upsert(be)
    suspend fun deleteBlockExercise(id: String) = dao.deleteBlockExercise(id)

    suspend fun saveWeekPlan(weekday: Int, itemType: String, itemId: String?) = dao.upsert(WeekPlan(weekday, itemType, itemId))
    suspend fun saveOverride(date: String, itemType: String, itemId: String?) = dao.upsert(DayOverride(date, itemType, itemId))
    suspend fun clearOverride(date: String) = dao.deleteOverride(date)
    suspend fun swapDays(a: LocalDate, b: LocalDate) {
        val data = planData.first()
        val ia = data.itemFor(a); val ib = data.itemFor(b)
        saveOverride(a.iso(), ib.itemType, ib.program?.id ?: ib.cardio?.id)
        saveOverride(b.iso(), ia.itemType, ia.program?.id ?: ia.cardio?.id)
    }

    suspend fun saveBodyweight(date: String, kg: Double) = dao.upsert(Bodyweight(date, kg))

    /** Starts a program session: snapshots every block into set logs in execution order. */
    suspend fun startProgramSession(programId: String, date: LocalDate = LocalDate.now()): Session? {
        val detail = programDetail(programId) ?: return null
        val session = Session(id = newId(), date = date.iso(), startedAt = System.currentTimeMillis(), itemType = ItemType.PROGRAM, programId = programId, programName = detail.program.name)
        val logs = mutableListOf<SetLog>()
        var order = 0
        detail.blocks.forEachIndexed { bi, bd ->
            val exs = bd.exercises
            if (bd.block.superset && exs.size > 1) {
                val maxSets = exs.maxOf { it.first.sets }
                for (s in 0 until maxSets) exs.forEachIndexed { slot, (be, ex) -> if (s < be.sets) logs += setLog(session.id, bi, true, slot, s, order++, be, ex) }
            } else {
                exs.forEachIndexed { slot, (be, ex) -> for (s in 0 until be.sets) logs += setLog(session.id, bi, bd.block.superset, slot, s, order++, be, ex) }
            }
        }
        // prefill from last time
        val previous = previousLogsByKey(programId, session.id)
        val filled = logs.map { l -> previous[l.key()]?.let { p -> l.copy(weightKg = p.weightKg, reps = p.reps) } ?: l.copy(reps = l.minReps) }
        dao.upsert(session); dao.upsertSetLogs(filled)
        return session
    }

    private fun setLog(sessionId: String, bi: Int, superset: Boolean, slot: Int, s: Int, order: Int, be: BlockExercise, ex: Exercise) = SetLog(
        id = newId(), sessionId = sessionId, blockIndex = bi, blockSuperset = superset, slot = slot, setIndex = s, order = order,
        exerciseId = ex.id, exerciseName = ex.name, minReps = be.minReps, maxReps = be.maxReps, stepKg = ex.stepKg,
    )

    /** Last done set for the same exercise and set index, from the most recent finished session. */
    suspend fun previousLogsByKey(programId: String?, excludeSessionId: String): Map<String, SetLog> {
        val sessions = dao.sessionsOnce().filter { it.finished && it.id != excludeSessionId && it.itemType == ItemType.PROGRAM }.sortedByDescending { it.startedAt }
        val result = HashMap<String, SetLog>()
        for (s in sessions) {
            for (l in dao.setLogsOnce(s.id)) if (l.status == SetStatus.DONE && !result.containsKey(l.key())) result[l.key()] = l
        }
        return result
    }

    suspend fun startCardioSession(exercise: Exercise, date: LocalDate = LocalDate.now()): Session {
        val session = Session(id = newId(), date = date.iso(), startedAt = System.currentTimeMillis(), itemType = ItemType.CARDIO, exerciseId = exercise.id, exerciseName = exercise.name)
        dao.upsert(session)
        if (exercise.intervals) dao.upsertIntervalLogs((0 until exercise.rounds).map { IntervalLog(newId(), session.id, it) })
        return session
    }

    suspend fun updateSetLog(log: SetLog) = dao.upsert(log)
    suspend fun updateSession(session: Session) = dao.upsert(session)
    suspend fun saveIntervals(list: List<IntervalLog>) = dao.upsertIntervalLogs(list)
    suspend fun finishSession(sessionId: String) {
        val session = dao.session(sessionId) ?: return
        val logs = dao.setLogsOnce(sessionId).map { if (it.status == SetStatus.PENDING) it.copy(status = SetStatus.SKIPPED) else it }
        dao.upsertSetLogs(logs)
        dao.upsert(session.copy(finishedAt = System.currentTimeMillis()))
    }
    suspend fun deleteSession(sessionId: String) { dao.deleteSetLogsFor(sessionId); dao.deleteIntervalLogsFor(sessionId); dao.deleteSession(sessionId) }
    suspend fun swapExerciseInSession(sessionId: String, fromExerciseId: String, to: Exercise, updateProgram: Boolean, programId: String?) {
        val logs = dao.setLogsOnce(sessionId).filter { it.exerciseId == fromExerciseId }.map { it.copy(exerciseId = to.id, exerciseName = to.name, stepKg = to.stepKg) }
        dao.upsertSetLogs(logs)
        if (updateProgram && programId != null) {
            dao.blocksFor(programId).forEach { b -> dao.blockExercisesFor(b.id).filter { it.exerciseId == fromExerciseId }.forEach { dao.upsert(it.copy(exerciseId = to.id)) } }
        }
    }

    suspend fun importedSourceIds(): List<String> = dao.importedSourceIds().flatMap { it.split(",") }

    /** Creates a finished cardio session from a Health Connect draft, with per-interval heart rate when the exercise has intervals. */
    suspend fun importDraft(d: dev.mirzohidkhon.khonfitness.health.ImportDraft, ex: Exercise) {
        val fields = ex.fieldList
        val pool = d.poolLength ?: 25
        val laps = when { "laps" !in fields -> null; d.laps != null -> d.laps; d.distanceM != null -> d.distanceM / pool; else -> null }
        val session = Session(
            id = newId(), date = d.date.iso(), startedAt = d.start.toEpochMilli(), finishedAt = d.end.toEpochMilli(), itemType = ItemType.CARDIO,
            exerciseId = ex.id, exerciseName = ex.name, timeSec = d.seconds, distanceM = d.distanceM, avgHr = d.avgHr,
            laps = laps, poolLength = if (laps != null) pool else null, sourceId = d.mergedIds.joinToString(","),
            note = if (d.origins.size > 1) "From " + d.originLabel else "",
        )
        dao.upsert(session)
        val hrs = dev.mirzohidkhon.khonfitness.health.HealthImport.intervalHeartRates(d, ex)
        if (hrs.isNotEmpty()) dao.upsertIntervalLogs(hrs.mapIndexed { i, hr -> IntervalLog(newId(), session.id, i, avgHr = hr) })
    }

    suspend fun exportJson(): String {
        val backup = Backup(
            exercises = dao.exercisesOnce(), modalities = dao.modalitiesOnce(), programs = dao.programsOnce(), blocks = dao.blocksOnce(),
            blockExercises = dao.blockExercisesOnce(), sessions = dao.sessionsOnce(), setLogs = dao.allSetLogsOnce(), intervalLogs = dao.intervalLogsOnce(),
            bodyweights = dao.bodyweightsOnce(), weekPlan = dao.weekPlanOnce(), overrides = dao.overridesOnce(),
        )
        return Json { prettyPrint = true }.encodeToString(backup)
    }

    /** Parses a backup and describes what it holds. Throws when the file is not a Khon backup. */
    fun describeBackup(text: String): String {
        val b = Json { ignoreUnknownKeys = true }.decodeFromString<Backup>(text)
        fun n(c: Int, one: String, many: String) = "$c " + if (c == 1) one else many
        return listOf(n(b.sessions.size, "session", "sessions"), n(b.bodyweights.size, "bodyweight entry", "bodyweight entries"), n(b.programs.size, "program", "programs"), n(b.exercises.size, "exercise", "exercises")).joinToString(" · ")
    }

    suspend fun importJson(text: String, replace: Boolean) {
        val b = Json { ignoreUnknownKeys = true }.decodeFromString<Backup>(text)
        if (replace) dao.clearAll()
        dao.upsertModalities(b.modalities); dao.upsertExercises(b.exercises); dao.upsertPrograms(b.programs); dao.upsertBlocks(b.blocks)
        dao.upsertBlockExercises(b.blockExercises); dao.upsertSessions(b.sessions); dao.upsertSetLogs(b.setLogs); dao.upsertIntervalLogs(b.intervalLogs)
        dao.upsertBodyweights(b.bodyweights); dao.upsertWeekPlan(b.weekPlan); dao.upsertOverrides(b.overrides)
    }

    suspend fun seedIfEmpty() { if (dao.programsOnce().isEmpty() && dao.exercisesOnce().isEmpty()) Seed.apply(dao) }
    suspend fun resetToSeed() { dao.clearAll(); Seed.apply(dao) }
}

fun SetLog.key(): String = "$exerciseId:$setIndex"
fun epley(weight: Double, reps: Int): Double = weight * (1 + reps / 30.0)

object Seed {
    private fun strength(id: String, name: String, primary: String, secondary: String = "", step: Double = 2.5) =
        Exercise(id = id, name = name, kind = Kind.STRENGTH, primaryMuscle = primary, secondaryMuscles = secondary, stepKg = step, builtin = true)

    suspend fun apply(dao: KhonDao) {
        dao.upsertModalities(listOf(
            Modality("bike", "Bike", 0xFFB39DFF), Modality("run", "Run", 0xFF7CE0A8), Modality("swim", "Swim", 0xFF71B7FF), Modality("row", "Row", 0xFFFF8FA3),
        ))
        val ex = listOf(
            strength("bench", "Barbell Bench Press", "chest", "shoulders,triceps"),
            strength("legpress", "Leg Press", "quads", "glutes", 5.0),
            strength("shoulderpress", "Shoulder Press", "shoulders", "triceps"),
            strength("cablerow", "Cable Seated Row", "upper back", "lats,biceps"),
            strength("lunge", "Walking Lunge", "quads", "glutes,hamstrings"),
            strength("latpull", "Lat Pulldown", "lats", "biceps,upper back"),
            strength("latraise", "Lateral Raise", "shoulders", "", 1.0),
            strength("legcurl", "Leg Curl", "hamstrings", "", 5.0),
            strength("inclinedb", "Incline DB Press", "chest", "shoulders,triceps", 2.0),
            strength("calf", "Calf Raises", "calves", "", 5.0),
            strength("curl", "Biceps Curl", "biceps", "forearms", 1.0),
            strength("triext", "Triceps Extension", "triceps", "", 2.5),
            strength("rdl", "Romanian Deadlift", "hamstrings", "glutes,lower back"),
            strength("crunch", "Crunch", "core", "", 2.5),
            strength("hyperext", "Hyperextension", "lower back", "glutes,hamstrings"),
            strength("goblet", "Goblet Squat", "quads", "glutes,core", 2.0),
            strength("dbrow", "Dumbbell Row", "upper back", "lats,biceps", 2.0),
            strength("pushup", "Push-up", "chest", "triceps,shoulders", 0.0),
            strength("hipthrust", "Hip Thrust", "glutes", "hamstrings", 5.0),
            strength("facepull", "Face Pull", "upper back", "shoulders", 2.5),
            strength("plank", "Plank", "core", "", 0.0),
            Exercise("nor4x4", "Norwegian 4x4", Kind.CARDIO, builtin = true, modalityId = "bike", intensity = Intensity.HIGH, intervals = true, workSec = 240, restSec = 180, rounds = 4, fields = "time,avgHr,watts"),
            Exercise("z2run", "Zone 2 run", Kind.CARDIO, builtin = true, modalityId = "run", intensity = Intensity.LOW, fields = "distance,time,avgHr"),
            Exercise("z2swim", "Zone 2 swim", Kind.CARDIO, builtin = true, modalityId = "swim", intensity = Intensity.LOW, fields = "poolLength,laps,time,stroke"),
            Exercise("tabata", "Tabata bike", Kind.CARDIO, builtin = true, modalityId = "bike", intensity = Intensity.HIGH, intervals = true, workSec = 20, restSec = 10, rounds = 8, fields = "time,avgHr,watts"),
        )
        dao.upsertExercises(ex)
        val tp = Program("tpv3", "TP v3", active = true, sortOrder = 0)
        dao.upsertPrograms(listOf(tp))
        val pairs = listOf("bench" to "legpress", "shoulderpress" to "cablerow", "lunge" to "latpull", "latraise" to "legcurl", "inclinedb" to "calf", "curl" to "triext", "rdl" to "crunch")
        pairs.forEachIndexed { i, (a, b) ->
            val block = Block("tpv3-b$i", tp.id, i, superset = true)
            dao.upsert(block)
            dao.upsertBlockExercises(listOf(BlockExercise("${block.id}-a", block.id, 0, a), BlockExercise("${block.id}-b", block.id, 1, b)))
        }
        dao.upsertWeekPlan(listOf(
            WeekPlan(1, ItemType.CARDIO, "z2swim"), WeekPlan(2, ItemType.PROGRAM, tp.id), WeekPlan(3, ItemType.CARDIO, "z2run"),
            WeekPlan(4, ItemType.CARDIO, "nor4x4"), WeekPlan(5, ItemType.CARDIO, "z2swim"), WeekPlan(6, ItemType.PROGRAM, tp.id), WeekPlan(7, ItemType.REST),
        ))
    }
}
