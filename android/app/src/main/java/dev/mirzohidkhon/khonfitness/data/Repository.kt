package dev.mirzohidkhon.khonfitness.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    val stretches: List<Stretch> = emptyList(), val stretchRoutines: List<StretchRoutine> = emptyList(),
    val routineStretches: List<RoutineStretch> = emptyList(), val stretchSessions: List<StretchSession> = emptyList(),
    val eatingWindow: EatingWindow? = null, val windowDays: List<WindowDay> = emptyList(),
)

/**
 * The cardio form's edits merged onto [current], the row as it is now: each field the form edits takes [edited]'s value
 * when it differs from [opened]'s (what the form opened with), else keeps [current]'s. [Session.finishedAt] is set to
 * [now] only when the row has none.
 */
fun mergeCardioEdits(current: Session, opened: Session, edited: Session, now: Long): Session {
    fun <T> pick(field: (Session) -> T): T = if (field(edited) != field(opened)) field(edited) else field(current)
    return current.copy(
        date = pick { it.date }, timeSec = pick { it.timeSec }, distanceM = pick { it.distanceM }, avgHr = pick { it.avgHr },
        watts = pick { it.watts }, laps = pick { it.laps }, poolLength = pick { it.poolLength }, stroke = pick { it.stroke },
        effort = pick { it.effort }, note = pick { it.note }, finishedAt = current.finishedAt ?: now,
    )
}

/**
 * The exercise editor's edits merged onto [current], the row as it is now: each field the editor edits takes [edited]'s
 * value when it differs from [opened]'s, else keeps [current]'s. Archived is not one of them: Archive writes it alone.
 */
fun mergeExerciseEdits(current: Exercise, opened: Exercise, edited: Exercise): Exercise {
    fun <T> pick(field: (Exercise) -> T): T = if (field(edited) != field(opened)) field(edited) else field(current)
    return current.copy(
        name = pick { it.name }, kind = pick { it.kind }, notes = pick { it.notes }, primaryMuscle = pick { it.primaryMuscle },
        secondaryMuscles = pick { it.secondaryMuscles }, stepKg = pick { it.stepKg }, modalityId = pick { it.modalityId },
        intensity = pick { it.intensity }, intervals = pick { it.intervals }, warmupSec = pick { it.warmupSec },
        workSec = pick { it.workSec }, restSec = pick { it.restSec }, rounds = pick { it.rounds }, fields = pick { it.fields },
    )
}

/** One form field: [Untouched], or [Set] to a value the user typed (null when cleared), even one equal to the stored value. */
sealed interface Edit<out T> {
    data object Untouched : Edit<Nothing>
    data class Set<T>(val value: T) : Edit<T>
}

/** The typed value when touched, else [current]. */
fun <T> Edit<T>.or(current: T): T = if (this is Edit.Set) value else current

/** The cardio form's typed fields of one interval; Save writes only the touched ones ([KhonDao.saveCardioEdits]). */
data class IntervalEdit(val avgHr: Edit<Int?> = Edit.Untouched, val watts: Edit<Int?> = Edit.Untouched)

/** The modality editor's edits (name, color) merged onto [current] the same way as [mergeExerciseEdits]. */
fun mergeModalityEdits(current: Modality, opened: Modality, edited: Modality): Modality {
    fun <T> pick(field: (Modality) -> T): T = if (field(edited) != field(opened)) field(edited) else field(current)
    return current.copy(name = pick { it.name }, color = pick { it.color })
}

/**
 * The stretch editor's edits merged onto [current] the same way as [mergeExerciseEdits]. A blank name keeps the stored
 * one. Built-in and archived are not editor fields.
 */
fun mergeStretchEdits(current: Stretch, opened: Stretch, edited: Stretch): Stretch {
    fun <T> pick(field: (Stretch) -> T): T = if (field(edited) != field(opened)) field(edited) else field(current)
    return current.copy(
        name = if (edited.name.isBlank()) current.name else pick { it.name }, sided = pick { it.sided }, mode = pick { it.mode },
        seconds = pick { it.seconds }, reps = pick { it.reps }, figure = pick { it.figure }, muscles = pick { it.muscles },
        cue = pick { it.cue }, link = pick { it.link },
    )
}

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
    val stretches: Flow<List<Stretch>> = dao.stretches()
    val stretchRoutines: Flow<List<StretchRoutine>> = dao.stretchRoutines()
    val routineStretches: Flow<List<RoutineStretch>> = dao.routineStretches()
    val stretchSessions: Flow<List<StretchSession>> = dao.stretchSessions()
    val eatingWindow: Flow<EatingWindow?> = dao.eatingWindow()
    val windowDays: Flow<List<WindowDay>> = dao.windowDays()

    // stretching: only guarded commands ([KhonDao]); no method here writes a whole row a screen holds, except the
    // two creates (new ids), the editor's merge, and the run result (written once).
    suspend fun createStretch(s: Stretch): Boolean = dao.createStretch(s)
    suspend fun saveStretchEdits(opened: Stretch, edited: Stretch) = dao.saveStretchEdits(opened, edited)
    /** False when refused: a built-in stretch, or one a routine row uses at this moment. */
    suspend fun deleteStretchIfUnused(id: String): Boolean = dao.deleteStretchIfUnused(id)
    suspend fun stretchUseCount(id: String) = dao.routineUseCount(id)
    /** A new routine ([KhonDao.createRoutine]); false when [firstStretchId] is gone, and then nothing is written. */
    suspend fun createRoutine(r: StretchRoutine, firstStretchId: String? = null, rowId: String? = firstStretchId?.let { newId() }): Boolean =
        dao.createRoutine(r, firstStretchId, rowId)
    /** "+" on Stretching: a saved, empty routine ([KhonDao.createNewRoutine]). */
    suspend fun createNewRoutine(id: String) = dao.createNewRoutine(id)
    suspend fun setRoutineName(id: String, name: String) = dao.setRoutineName(id, name)
    suspend fun setRoutineActive(id: String, on: Boolean) = dao.setRoutineActive(id, on)
    suspend fun deleteRoutine(id: String) = dao.deleteRoutineWithStretches(id)
    suspend fun addRoutineStretch(routineId: String, stretchId: String, rowId: String = newId()) = dao.addRoutineStretch(routineId, stretchId, rowId)
    suspend fun setRoutineStretchSeconds(rowId: String, seconds: Int?) = dao.setRoutineStretchSeconds(rowId, seconds)
    suspend fun moveRoutineStretch(rowId: String, up: Boolean) = dao.moveRoutineStretch(rowId, up)
    suspend fun removeRoutineStretch(rowId: String) = dao.removeRoutineStretch(rowId)
    suspend fun recordStretchSession(s: StretchSession) = dao.recordStretchSession(s)
    suspend fun deleteStretchSession(id: String) = dao.deleteStretchSession(id)

    // eating window
    suspend fun setWindowEnabled(on: Boolean) = dao.setWindowEnabled(on)
    suspend fun setWindowRemind(min: Int) = dao.setWindowRemind(min)
    suspend fun setWindowStart(minute: Int): Boolean = dao.setWindowStart(minute)
    suspend fun setWindowEnd(minute: Int): Boolean = dao.setWindowEnd(minute)
    suspend fun eatingWindowOnce(): EatingWindow = dao.eatingWindowOnce() ?: EatingWindow()
    suspend fun windowDay(date: String): WindowDay? = dao.windowDay(date)
    suspend fun openWindowNow(date: String, now: Long) = dao.openWindowNow(date, now)
    suspend fun closeWindowNow(date: String, now: Long) = dao.closeWindowNow(date, now)

    fun setLogs(sessionId: String): Flow<List<SetLog>> = dao.setLogs(sessionId)
    fun session(sessionId: String): Flow<Session?> = dao.sessionFlow(sessionId)
    fun intervalLogs(sessionId: String): Flow<List<IntervalLog>> = dao.intervalLogs(sessionId)

    /** Resolved plan for a date: override wins over the weekday template. */
    val planData: Flow<PlanData> = combine(weekPlan, overrides, programs, exercises) { plan, ov, progs, exs -> PlanData(plan, ov, progs, exs) }

    class PlanData(val plan: List<WeekPlan>, val overrides: List<DayOverride>, val programs: List<Program>, val exercises: List<Exercise>) {
        fun itemFor(date: LocalDate): PlanItem {
            val o = overrides.find { it.date == date.iso() }
            val type: String; val id: String?; val overridden: Boolean
            if (o != null) { type = o.itemType; id = o.itemId; overridden = !o.auto }
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
    /** The exercise editor's Done: a new exercise ([opened] null) is written whole, an existing one gets only its changed fields. */
    suspend fun saveExerciseEdits(opened: Exercise?, edited: Exercise) = if (opened == null) dao.upsert(edited) else dao.saveExerciseEdits(opened, edited)
    suspend fun setExerciseArchived(id: String, archived: Boolean) = dao.setExerciseArchived(id, archived)
    suspend fun archiveExerciseWithEdits(opened: Exercise, edited: Exercise, archived: Boolean) = dao.archiveExerciseWithEdits(opened, edited, archived)
    suspend fun exerciseUsage(id: String): Int = dao.exerciseUsage(id)
    suspend fun deleteExercise(id: String): Boolean = dao.deleteExerciseIfUnused(id)
    suspend fun mergeExercise(from: String, into: String) = dao.mergeExercise(from, into)
    /** The modality editor's Done: a new modality ([opened] null) is written whole, an existing one gets only its changed fields. */
    suspend fun saveModalityEdits(opened: Modality?, edited: Modality) = if (opened == null) dao.upsert(edited) else dao.saveModalityEdits(opened, edited)
    suspend fun setModalityArchived(id: String, archived: Boolean) = dao.setModalityArchived(id, archived)
    suspend fun archiveModalityWithEdits(opened: Modality, edited: Modality, archived: Boolean) = dao.archiveModalityWithEdits(opened, edited, archived)

    suspend fun saveProgram(p: Program) = dao.upsert(p)
    suspend fun createProgram(p: Program) = dao.createProgram(p)
    suspend fun setProgramName(id: String, name: String) = dao.setProgramName(id, name)
    suspend fun setActiveProgram(id: String) = dao.setActiveProgram(id)
    suspend fun deleteProgram(id: String) = dao.deleteProgramFully(id)
    suspend fun saveBlock(b: Block) = dao.upsert(b)
    suspend fun saveBlocks(list: List<Block>) = dao.upsertBlocks(list)
    suspend fun createBlock(b: Block, first: BlockExercise) = dao.createBlock(b, first)
    suspend fun setBlockSuperset(id: String, superset: Boolean) = dao.setBlockSuperset(id, superset)
    suspend fun moveBlock(id: String, delta: Int) = dao.moveBlock(id, delta)
    suspend fun deleteBlock(id: String) = dao.deleteBlockFully(id)
    suspend fun saveBlockExercise(be: BlockExercise) = dao.upsert(be)
    suspend fun setBlockExerciseExercise(id: String, exerciseId: String) = dao.setBlockExerciseExercise(id, exerciseId)
    /** A block editor pick for [slot], resolved against the stored rows ([KhonDao.setSlotExercise]). */
    suspend fun setSlotExercise(blockId: String, slot: Int, exerciseId: String, newRowId: String = newId()) = dao.setSlotExercise(blockId, slot, exerciseId, newRowId)
    suspend fun setBlockExerciseSets(id: String, sets: Int) = dao.setBlockExerciseSets(id, sets)
    suspend fun setBlockExerciseMinReps(id: String, reps: Int) = dao.setBlockExerciseMinReps(id, reps)
    suspend fun setBlockExerciseMaxReps(id: String, reps: Int) = dao.setBlockExerciseMaxReps(id, reps)
    suspend fun deleteBlockExercise(id: String) = dao.deleteBlockExercise(id)

    /** Changes a weekday in the template; past dates keep their plan ([KhonDao.saveWeekPlan]). */
    suspend fun saveWeekPlan(weekday: Int, itemType: String, itemId: String?, today: LocalDate = LocalDate.now()) = dao.saveWeekPlan(weekday, itemType, itemId, today)
    suspend fun saveOverride(date: String, itemType: String, itemId: String?) = dao.upsert(DayOverride(date, itemType, itemId))
    suspend fun clearOverride(date: String) = dao.deleteOverride(date)
    suspend fun swapDays(a: LocalDate, b: LocalDate) = dao.swapDays(a, b)

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
        dao.insertSessionWithLogs(session, filled, emptyList())
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
        val intervals = if (exercise.intervals) (0 until exercise.rounds).map { IntervalLog(newId(), session.id, it) } else emptyList()
        dao.insertSessionWithLogs(session, emptyList(), intervals)
        return session
    }

    suspend fun setSetStatus(id: String, status: String) = dao.setSetStatus(id, status)
    suspend fun setSetWeight(id: String, weightKg: Double?) = dao.setSetWeight(id, weightKg)
    suspend fun setSetReps(id: String, reps: Int?) = dao.setSetReps(id, reps)
    /** The cardio form's Save: only what the user changed since [opened] and [openedIntervals] is written, in one transaction. */
    suspend fun saveCardioEdits(opened: Session, edited: Session, intervalEdits: Map<String, IntervalEdit>, now: Long = System.currentTimeMillis()) =
        dao.saveCardioEdits(opened, edited, intervalEdits, now)
    // Single fields of a session edited in place (session page, finish sheet, timer): each writes its own column only.
    suspend fun setSessionDate(sessionId: String, date: LocalDate) = dao.setSessionDate(sessionId, date.iso())
    suspend fun setSessionEffort(sessionId: String, effort: Int?) = dao.setSessionEffort(sessionId, effort)
    suspend fun setSessionNote(sessionId: String, note: String) = dao.setSessionNote(sessionId, note)
    suspend fun setSessionTime(sessionId: String, timeSec: Int?) = dao.setSessionTime(sessionId, timeSec)
    /** Pending sets become skipped and the session gets its finish time, in one transaction; nothing else is written. */
    suspend fun finishSession(sessionId: String, now: Long = System.currentTimeMillis()) = dao.finishSession(sessionId, now)
    suspend fun deleteSession(sessionId: String) = dao.deleteSessionWithLogs(sessionId)
    suspend fun swapExerciseInSession(sessionId: String, fromExerciseId: String, to: Exercise, updateProgram: Boolean, programId: String?) {
        // Only the exercise columns change: a set's weight, reps or check saved meanwhile stays.
        dao.swapExercise(sessionId, fromExerciseId, to, if (updateProgram) programId else null)
    }

    suspend fun importedSourceIds(): List<String> = dao.importedSourceIds().flatMap { it.split(",") }

    /**
     * Creates a finished cardio session from a Health Connect draft, with per-interval heart rate when the exercise has
     * intervals. A draft already imported writes nothing ([KhonDao.insertImportedSession]).
     */
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
        val hrs = dev.mirzohidkhon.khonfitness.health.HealthImport.intervalHeartRates(d, ex)
        dao.insertImportedSession(session, hrs.mapIndexed { i, hr -> IntervalLog(newId(), session.id, i, avgHr = hr) })
    }

    /** Every table as one snapshot ([KhonDao.backupSnapshot]), as JSON. */
    suspend fun exportJson(): String = Json { prettyPrint = true }.encodeToString(dao.backupSnapshot())

    /** Parses a backup and describes what it holds. Throws when the file is not a Khon backup. */
    fun describeBackup(text: String): String {
        val b = Json { ignoreUnknownKeys = true }.decodeFromString<Backup>(text)
        fun n(c: Int, one: String, many: String) = "$c " + if (c == 1) one else many
        return listOf(n(b.sessions.size, "session", "sessions"), n(b.stretchSessions.size, "stretch session", "stretch sessions"), n(b.bodyweights.size, "bodyweight entry", "bodyweight entries"), n(b.programs.size, "program", "programs"), n(b.exercises.size, "exercise", "exercises")).joinToString(" · ")
    }

    /** Merges or replaces from a backup in one transaction ([KhonDao.importBackup]). Throws when the file is not a Khon backup. */
    suspend fun importJson(text: String, replace: Boolean) = dao.importBackup(Json { ignoreUnknownKeys = true }.decodeFromString<Backup>(text), replace)

    suspend fun seedIfEmpty() = dao.seedIfEmpty()
    suspend fun seedStretchesIfEmpty() = dao.seedStretchesIfEmpty()
    suspend fun resetToSeed() = dao.resetToSeed()
}

object PlanRules {
    /** First day the app was in use: the earliest date any record carries, or null on a fresh install. */
    fun firstUse(dates: Iterable<String>): LocalDate? = dates.filter { it.isNotBlank() }.minOrNull()?.toDate()

    /** Where freezing starts: the first-use date or one year back, whichever is earlier, so no visible past day moves. */
    fun freezeFrom(dates: Iterable<String>, today: LocalDate): LocalDate = listOfNotNull(firstUse(dates), today.minusYears(1)).min()

    /** Past dates of [weekday] from [first] to yesterday that still follow the template. */
    fun datesToFreeze(weekday: Int, first: LocalDate?, today: LocalDate, overridden: Set<String>): List<LocalDate> {
        if (first == null) return emptyList()
        return generateSequence(first) { it.plusDays(1) }.takeWhile { it < today }.filter { it.dayOfWeek.value == weekday && it.iso() !in overridden }.toList()
    }

    /** Two days can swap when they differ and neither is in the past. */
    fun canSwap(a: LocalDate, b: LocalDate, today: LocalDate): Boolean = a != b && a >= today && b >= today
}

/** [ids] in order with [id] moved [delta] places, kept inside the list; unchanged when [id] is not in it. */
fun moveInOrder(ids: List<String>, id: String, delta: Int): List<String> {
    val from = ids.indexOf(id)
    if (from < 0) return ids
    val to = (from + delta).coerceIn(0, ids.size - 1)
    return ids.toMutableList().apply { removeAt(from); add(to, id) }
}

fun SetLog.key(): String = "$exerciseId:$setIndex"
fun epley(weight: Double, reps: Int): Double = weight * (1 + reps / 30.0)

object Seed {
    /** The owner's daily mobility routine (2026-09-12) with reference clips. Seconds are per side for sided stretches. */
    suspend fun applyStretches(dao: KhonDao) {
        fun st(id: String, name: String, sided: Boolean, mode: String, seconds: Int, reps: Int, figure: String, muscles: String, cue: String, link: String) =
            Stretch(id, name, sided, mode, seconds, reps, figure, muscles, cue, "https://www.youtube.com/watch?v=" + link, builtin = true)
        val list = listOf(
            st("elephant", "Elephant walks", false, StretchMode.REPS, 60, 10, "elephant", "hamstrings", "Hands on the floor, pedal the heels down one at a time", "bIS8e2ZI-u0"),
            st("palmsfloor", "Hamstring stretch, palms on ground", false, StretchMode.HOLD, 45, 0, "fold", "hamstrings,lower back", "Fold from the hips, reach the palms to the floor", "2nfsR9PC7hQ"),
            st("wgs", "World's greatest stretch", true, StretchMode.REPS, 40, 5, "wgs", "glutes,quads,upper back", "Lunge, elbow to instep, rotate and reach up", "7XheaZERvBQ"),
            st("needle", "Thread the needle", true, StretchMode.HOLD, 30, 0, "needle", "upper back,shoulders", "On all fours, slide one arm under and rest the shoulder down", "gyew25Vaqj8"),
            st("hipflexor", "Hip flexor stretch", true, StretchMode.HOLD, 45, 0, "hipflexor", "quads,glutes", "Half kneel, tuck the pelvis, squeeze the back glute", "ktgtEWGhFd8"),
            st("ninety", "90/90 stretch", true, StretchMode.HOLD, 45, 0, "ninety", "glutes", "Both knees at 90, sit tall, lean over the front shin", "FM7-7-a0FLg"),
            st("shoulderir", "Shoulder internal rotation", true, StretchMode.HOLD, 30, 0, "shoulderir", "shoulders", "Hand behind the back, walk it up the spine", "3Av8-RLNvDk"),
            st("catcow", "Cat cow", false, StretchMode.REPS, 40, 10, "catcow", "lower back,core", "Round the spine on the exhale, arch on the inhale", "2of247Kt0tU"),
            st("plow", "Plow pose", false, StretchMode.HOLD, 45, 0, "plow", "lower back,hamstrings", "Legs over the head, toes toward the floor, breathe", "IoURFlXOuqg"),
        )
        dao.upsertStretches(list)
        if (dao.stretchRoutinesOnce().isEmpty()) {
            dao.upsert(StretchRoutine("daily", "Daily mobility", active = true, sortOrder = 0))
            dao.upsertRoutineStretches(list.mapIndexed { i, s -> RoutineStretch("daily-" + s.id, "daily", s.id, i) })
        }
        if (dao.eatingWindowOnce() == null) dao.upsert(EatingWindow())
    }

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
