package dev.mirzohidkhon.khonfitness.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface KhonDao {
    // exercises
    @Query("SELECT * FROM exercises ORDER BY name") fun exercises(): Flow<List<Exercise>>
    @Query("SELECT * FROM exercises") suspend fun exercisesOnce(): List<Exercise>
    @Query("SELECT * FROM exercises WHERE id = :id") suspend fun exercise(id: String): Exercise?
    @Upsert suspend fun upsert(exercise: Exercise)
    @Upsert suspend fun upsertExercises(list: List<Exercise>)
    @Query("DELETE FROM exercises WHERE id = :id") suspend fun deleteExercise(id: String)
    @Query("SELECT COUNT(*) FROM set_logs WHERE exerciseId = :id") suspend fun setLogCountFor(id: String): Int
    @Query("SELECT COUNT(*) FROM sessions WHERE exerciseId = :id") suspend fun cardioSessionCountFor(id: String): Int
    @Query("SELECT COUNT(*) FROM block_exercises WHERE exerciseId = :id") suspend fun blockExerciseCountFor(id: String): Int
    @Query("SELECT COUNT(*) FROM week_plan WHERE itemType = 'cardio' AND itemId = :id") suspend fun weekPlanCardioCountFor(id: String): Int
    @Query("SELECT COUNT(*) FROM day_overrides WHERE itemType = 'cardio' AND itemId = :id") suspend fun overrideCardioCountFor(id: String): Int
    @Query("UPDATE set_logs SET exerciseId = :to WHERE exerciseId = :from") suspend fun moveSetLogs(from: String, to: String)
    @Query("UPDATE sessions SET exerciseId = :to WHERE exerciseId = :from") suspend fun moveCardioSessions(from: String, to: String)
    @Query("UPDATE block_exercises SET exerciseId = :to WHERE exerciseId = :from") suspend fun moveBlockExercises(from: String, to: String)
    @Query("UPDATE week_plan SET itemId = :to WHERE itemType = 'cardio' AND itemId = :from") suspend fun movePlanCardio(from: String, to: String)
    @Query("UPDATE day_overrides SET itemId = :to WHERE itemType = 'cardio' AND itemId = :from") suspend fun moveOverrideCardio(from: String, to: String)
    /** Every use that fixes an exercise's kind: set logs, cardio sessions, block exercises, plan days and dated changes. */
    @Transaction suspend fun exerciseUsage(id: String): Int =
        setLogCountFor(id) + cardioSessionCountFor(id) + blockExerciseCountFor(id) + weekPlanCardioCountFor(id) + overrideCardioCountFor(id)
    @Query("UPDATE exercises SET archived = :archived WHERE id = :id") suspend fun setExerciseArchived(id: String, archived: Boolean)
    /** Deletes the exercise only while nothing uses it, checked in the same transaction; returns whether it did. */
    @Transaction suspend fun deleteExerciseIfUnused(id: String): Boolean {
        if (exerciseUsage(id) > 0) return false
        deleteExercise(id)
        return true
    }
    /** Every use of [from] moves to [into], then [from] goes: all or nothing. */
    @Transaction suspend fun mergeExercise(from: String, into: String) {
        moveSetLogs(from, into); moveCardioSessions(from, into); moveBlockExercises(from, into)
        movePlanCardio(from, into); moveOverrideCardio(from, into); deleteExercise(from)
    }
    /** Archive (or unarchive) with the page's edits: the changed fields first ([saveExerciseEdits]), then the flag. */
    @Transaction suspend fun archiveExerciseWithEdits(opened: Exercise, edited: Exercise, archived: Boolean) {
        if (edited.name.isNotBlank()) saveExerciseEdits(opened, edited)
        setExerciseArchived(edited.id, archived)
    }
    /**
     * The exercise editor's Done: the fields changed from [opened] to [edited] go onto the row as it is now
     * ([mergeExerciseEdits]), so an import since the editor opened stays. A kind change is kept out when something
     * uses the exercise by now. A deleted exercise writes nothing.
     */
    @Transaction suspend fun saveExerciseEdits(opened: Exercise, edited: Exercise) {
        val current = exercise(edited.id) ?: return
        val merged = mergeExerciseEdits(current, opened, edited)
        upsert(if (merged.kind != current.kind && exerciseUsage(current.id) > 0) merged.copy(kind = current.kind) else merged)
    }

    // modalities
    @Query("SELECT * FROM modalities ORDER BY name") fun modalities(): Flow<List<Modality>>
    @Query("SELECT * FROM modalities") suspend fun modalitiesOnce(): List<Modality>
    @Upsert suspend fun upsert(modality: Modality)
    @Upsert suspend fun upsertModalities(list: List<Modality>)
    @Query("SELECT * FROM modalities WHERE id = :id") suspend fun modality(id: String): Modality?
    @Query("UPDATE modalities SET archived = :archived WHERE id = :id") suspend fun setModalityArchived(id: String, archived: Boolean)
    /** Archive (or unarchive) with the page's edits: the changed fields first ([saveModalityEdits]), then the flag. */
    @Transaction suspend fun archiveModalityWithEdits(opened: Modality, edited: Modality, archived: Boolean) {
        if (edited.name.isNotBlank()) saveModalityEdits(opened, edited)
        setModalityArchived(edited.id, archived)
    }
    /** The modality editor's Done: the changed fields onto the row as it is now ([mergeModalityEdits]); a deleted one writes nothing. */
    @Transaction suspend fun saveModalityEdits(opened: Modality, edited: Modality) {
        val current = modality(edited.id) ?: return
        upsert(mergeModalityEdits(current, opened, edited))
    }

    // programs
    @Query("SELECT * FROM programs ORDER BY sortOrder, name") fun programs(): Flow<List<Program>>
    @Query("SELECT * FROM programs") suspend fun programsOnce(): List<Program>
    @Query("SELECT * FROM programs WHERE id = :id") suspend fun program(id: String): Program?
    @Query("SELECT * FROM programs WHERE active = 1 LIMIT 1") suspend fun activeProgram(): Program?
    @Upsert suspend fun upsert(program: Program)
    @Upsert suspend fun upsertPrograms(list: List<Program>)
    @Query("UPDATE programs SET active = (id = :id)") suspend fun setActiveProgram(id: String)
    @Query("DELETE FROM programs WHERE id = :id") suspend fun deleteProgram(id: String)

    @Query("SELECT * FROM blocks ORDER BY sortOrder") fun blocks(): Flow<List<Block>>
    @Query("SELECT * FROM blocks WHERE programId = :programId ORDER BY sortOrder") suspend fun blocksFor(programId: String): List<Block>
    @Query("SELECT * FROM blocks") suspend fun blocksOnce(): List<Block>
    @Query("SELECT * FROM blocks WHERE id = :id") suspend fun block(id: String): Block?
    @Upsert suspend fun upsert(block: Block)
    @Upsert suspend fun upsertBlocks(list: List<Block>)
    @Query("DELETE FROM blocks WHERE id = :id") suspend fun deleteBlock(id: String)
    @Query("DELETE FROM blocks WHERE programId = :programId") suspend fun deleteBlocksFor(programId: String)

    @Query("SELECT * FROM block_exercises ORDER BY slot") fun blockExercises(): Flow<List<BlockExercise>>
    @Query("SELECT * FROM block_exercises WHERE blockId = :blockId ORDER BY slot") suspend fun blockExercisesFor(blockId: String): List<BlockExercise>
    @Query("SELECT * FROM block_exercises") suspend fun blockExercisesOnce(): List<BlockExercise>
    @Query("SELECT * FROM block_exercises WHERE blockId = :blockId AND slot = :slot ORDER BY id LIMIT 1") suspend fun blockExerciseAt(blockId: String, slot: Int): BlockExercise?
    @Upsert suspend fun upsert(be: BlockExercise)
    @Upsert suspend fun upsertBlockExercises(list: List<BlockExercise>)
    @Query("DELETE FROM block_exercises WHERE id = :id") suspend fun deleteBlockExercise(id: String)
    @Query("DELETE FROM block_exercises WHERE blockId = :blockId") suspend fun deleteBlockExercisesFor(blockId: String)

    // program editors: each edit writes its own columns onto the row as it is now, never a whole row the screen holds
    @Query("UPDATE programs SET name = :name WHERE id = :id") suspend fun setProgramName(id: String, name: String)
    @Query("SELECT COALESCE(MAX(sortOrder) + 1, 0) FROM programs") suspend fun nextProgramOrder(): Int
    @Query("UPDATE blocks SET superset = :superset WHERE id = :id") suspend fun setBlockSuperset(id: String, superset: Boolean)
    @Query("UPDATE blocks SET sortOrder = :sortOrder WHERE id = :id") suspend fun setBlockOrder(id: String, sortOrder: Int)
    @Query("SELECT COALESCE(MAX(sortOrder) + 1, 0) FROM blocks WHERE programId = :programId") suspend fun nextBlockOrder(programId: String): Int
    @Query("UPDATE block_exercises SET exerciseId = :exerciseId WHERE id = :id") suspend fun setBlockExerciseExercise(id: String, exerciseId: String)
    @Query("UPDATE block_exercises SET sets = :sets WHERE id = :id") suspend fun setBlockExerciseSets(id: String, sets: Int)
    /** Min reps, with max raised to it when lower, both against the stored row. */
    @Query("UPDATE block_exercises SET minReps = :reps, maxReps = MAX(maxReps, :reps) WHERE id = :id") suspend fun setBlockExerciseMinReps(id: String, reps: Int)
    /** Max reps, with min lowered to it when higher, both against the stored row. */
    @Query("UPDATE block_exercises SET maxReps = :reps, minReps = MIN(minReps, :reps) WHERE id = :id") suspend fun setBlockExerciseMaxReps(id: String, reps: Int)
    @Query("DELETE FROM block_exercises WHERE blockId IN (SELECT id FROM blocks WHERE programId = :programId)") suspend fun deleteBlockExercisesOfProgram(programId: String)
    @Query("UPDATE week_plan SET itemType = '${ItemType.REST}', itemId = NULL WHERE itemType = '${ItemType.PROGRAM}' AND itemId = :programId") suspend fun restPlanDaysOf(programId: String)
    @Query("UPDATE day_overrides SET itemType = '${ItemType.REST}', itemId = NULL WHERE itemType = '${ItemType.PROGRAM}' AND itemId = :programId") suspend fun restOverridesOf(programId: String)
    /** One program is always active: when none is, the first left in order takes the mark. */
    @Query("UPDATE programs SET active = 1 WHERE id = (SELECT id FROM programs ORDER BY sortOrder, name LIMIT 1) AND NOT EXISTS (SELECT 1 FROM programs WHERE active = 1)")
    suspend fun keepOneActiveProgram()

    /** A new program, after the others in order; made the active one when [Program.active]. */
    @Transaction suspend fun createProgram(p: Program) {
        upsert(p.copy(sortOrder = nextProgramOrder()))
        if (p.active) setActiveProgram(p.id)
    }

    /**
     * A program and all it holds: its blocks and their exercises go, plan days and dated changes that hold it become
     * rest days (they showed rest once it was gone anyway), and the active mark moves to the first program left.
     */
    @Transaction suspend fun deleteProgramFully(id: String) {
        deleteBlockExercisesOfProgram(id); deleteBlocksFor(id); deleteProgram(id)
        restPlanDaysOf(id); restOverridesOf(id); keepOneActiveProgram()
    }

    /** A new block with its first exercise, after its program's other blocks, so no block is ever saved empty. */
    @Transaction suspend fun createBlock(b: Block, first: BlockExercise) {
        upsert(b.copy(sortOrder = nextBlockOrder(b.programId)))
        upsert(first.copy(blockId = b.id))
    }

    /**
     * A pick for a block's slot, against the stored rows: the slot's row gets only its exercise, or a new row ([newRowId])
     * when the slot has none. A deleted block writes nothing.
     */
    @Transaction suspend fun setSlotExercise(blockId: String, slot: Int, exerciseId: String, newRowId: String) {
        if (block(blockId) == null) return
        val row = blockExerciseAt(blockId, slot)
        if (row != null) setBlockExerciseExercise(row.id, exerciseId) else upsert(BlockExercise(newRowId, blockId, slot, exerciseId))
    }

    @Transaction suspend fun deleteBlockFully(id: String) { deleteBlockExercisesFor(id); deleteBlock(id) }

    /** Moves a block [delta] places within its program, by the stored order, and numbers the program's blocks from 0. */
    @Transaction suspend fun moveBlock(id: String, delta: Int) {
        val block = blocksOnce().find { it.id == id } ?: return
        moveInOrder(blocksFor(block.programId).map { it.id }, id, delta).forEachIndexed { i, b -> setBlockOrder(b, i) }
    }

    // sessions
    @Query("SELECT * FROM sessions ORDER BY date DESC, startedAt DESC") fun sessions(): Flow<List<Session>>
    @Query("SELECT * FROM sessions") suspend fun sessionsOnce(): List<Session>
    @Query("SELECT * FROM sessions WHERE id = :id") suspend fun session(id: String): Session?
    @Query("SELECT * FROM sessions WHERE id = :id") fun sessionFlow(id: String): Flow<Session?>
    @Query("SELECT * FROM sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1") fun unfinishedSession(): Flow<Session?>
    @Query("SELECT * FROM sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1") suspend fun unfinishedSessionOnce(): Session?
    @Query("SELECT sourceId FROM sessions WHERE sourceId IS NOT NULL") suspend fun importedSourceIds(): List<String>
    @Upsert suspend fun upsert(session: Session)
    @Upsert suspend fun upsertSessions(list: List<Session>)
    @Query("DELETE FROM sessions WHERE id = :id") suspend fun deleteSession(id: String)
    // One field of a session the user or the timer edits in place: a whole-row write from a snapshot would undo an edit
    // that landed after the snapshot was read.
    @Query("UPDATE sessions SET date = :date WHERE id = :id") suspend fun setSessionDate(id: String, date: String)
    @Query("UPDATE sessions SET effort = :effort WHERE id = :id") suspend fun setSessionEffort(id: String, effort: Int?)
    @Query("UPDATE sessions SET note = :note WHERE id = :id") suspend fun setSessionNote(id: String, note: String)
    @Query("UPDATE sessions SET timeSec = :timeSec WHERE id = :id") suspend fun setSessionTime(id: String, timeSec: Int?)
    @Query("UPDATE sessions SET finishedAt = :finishedAt WHERE id = :id") suspend fun setSessionFinishedAt(id: String, finishedAt: Long?)
    @Query("UPDATE set_logs SET status = '${SetStatus.SKIPPED}' WHERE sessionId = :sessionId AND status = '${SetStatus.PENDING}'")
    suspend fun skipPendingSets(sessionId: String)
    /** Ends a session: its pending sets become skipped, then only [Session.finishedAt] is set. */
    @Transaction suspend fun finishSession(id: String, finishedAt: Long) { skipPendingSets(id); setSessionFinishedAt(id, finishedAt) }
    /**
     * The cardio form's Save: the columns changed from [opened] to [edited] go onto the row as it is now, so a timer
     * write since the form opened stays ([mergeCardioEdits]); then each interval field the user touched, alone, by id
     * ([IntervalEdit]): an untouched field is never written and an interval deleted meanwhile stays deleted.
     * A deleted session writes nothing.
     */
    @Transaction suspend fun saveCardioEdits(opened: Session, edited: Session, intervalEdits: Map<String, IntervalEdit>, now: Long) {
        val current = session(edited.id) ?: return
        upsert(mergeCardioEdits(current, opened, edited, now))
        intervalEdits.forEach { (id, e) ->
            (e.avgHr as? Edit.Set)?.let { setIntervalAvgHr(id, it.value) }
            (e.watts as? Edit.Set)?.let { setIntervalWatts(id, it.value) }
        }
    }
    /** A new session with its set and interval logs: all or nothing, so no session is ever saved without its logs. */
    @Transaction suspend fun insertSessionWithLogs(session: Session, setLogs: List<SetLog>, intervalLogs: List<IntervalLog>) {
        upsert(session); upsertSetLogs(setLogs); upsertIntervalLogs(intervalLogs)
    }
    /**
     * A Health Connect import ([insertSessionWithLogs]), written only while none of its source records is imported yet,
     * checked in the same transaction: a second tap on the same draft writes nothing. Returns whether it wrote.
     */
    @Transaction suspend fun insertImportedSession(session: Session, intervalLogs: List<IntervalLog>): Boolean {
        val ids = session.sourceId?.split(",").orEmpty().filter { it.isNotEmpty() }.toSet()
        if (importedSourceIds().any { stored -> stored.split(",").any { it in ids } }) return false
        insertSessionWithLogs(session, emptyList(), intervalLogs)
        return true
    }

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY `order`") fun setLogs(sessionId: String): Flow<List<SetLog>>
    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY `order`") suspend fun setLogsOnce(sessionId: String): List<SetLog>
    @Query("SELECT * FROM set_logs") fun allSetLogs(): Flow<List<SetLog>>
    @Query("SELECT * FROM set_logs") suspend fun allSetLogsOnce(): List<SetLog>
    @Upsert suspend fun upsert(log: SetLog)
    @Upsert suspend fun upsertSetLogs(list: List<SetLog>)
    // One field of a set the user edits in the live session: a whole-row write from the row on screen would undo
    // another field's save that has not come back yet (a weight typed, then the check tapped at once).
    @Query("UPDATE set_logs SET status = :status WHERE id = :id") suspend fun setSetStatus(id: String, status: String)
    @Query("UPDATE set_logs SET weightKg = :weightKg WHERE id = :id") suspend fun setSetWeight(id: String, weightKg: Double?)
    @Query("UPDATE set_logs SET reps = :reps WHERE id = :id") suspend fun setSetReps(id: String, reps: Int?)
    @Query("UPDATE set_logs SET exerciseId = :toId, exerciseName = :toName, stepKg = :stepKg WHERE sessionId = :sessionId AND exerciseId = :fromId")
    suspend fun swapSetExercise(sessionId: String, fromId: String, toId: String, toName: String, stepKg: Double)
    @Query("UPDATE block_exercises SET exerciseId = :toId WHERE exerciseId = :fromId AND blockId IN (SELECT id FROM blocks WHERE programId = :programId)")
    suspend fun swapProgramExercise(programId: String, fromId: String, toId: String)
    /** A swap in a session, and in its program when [programId] is given: only the exercise columns, in one transaction. */
    @Transaction suspend fun swapExercise(sessionId: String, fromId: String, to: Exercise, programId: String?) {
        swapSetExercise(sessionId, fromId, to.id, to.name, to.stepKg)
        if (programId != null) swapProgramExercise(programId, fromId, to.id)
    }
    /** A session with its set and interval logs: all or nothing. */
    @Transaction suspend fun deleteSessionWithLogs(id: String) { deleteSetLogsFor(id); deleteIntervalLogsFor(id); deleteSession(id) }
    @Query("DELETE FROM set_logs WHERE sessionId = :sessionId") suspend fun deleteSetLogsFor(sessionId: String)

    @Query("SELECT * FROM interval_logs WHERE sessionId = :sessionId ORDER BY `index`") fun intervalLogs(sessionId: String): Flow<List<IntervalLog>>
    @Query("SELECT * FROM interval_logs") suspend fun intervalLogsOnce(): List<IntervalLog>
    @Query("SELECT * FROM interval_logs WHERE sessionId = :sessionId ORDER BY `index`") suspend fun intervalLogsFor(sessionId: String): List<IntervalLog>
    @Query("SELECT * FROM interval_logs") fun allIntervalLogs(): Flow<List<IntervalLog>>
    @Upsert suspend fun upsertIntervalLogs(list: List<IntervalLog>)
    @Query("UPDATE interval_logs SET avgHr = :avgHr WHERE id = :id") suspend fun setIntervalAvgHr(id: String, avgHr: Int?)
    @Query("UPDATE interval_logs SET watts = :watts WHERE id = :id") suspend fun setIntervalWatts(id: String, watts: Int?)
    @Query("DELETE FROM interval_logs WHERE sessionId = :sessionId") suspend fun deleteIntervalLogsFor(sessionId: String)

    // bodyweight
    @Query("SELECT * FROM bodyweights ORDER BY date") fun bodyweights(): Flow<List<Bodyweight>>
    @Query("SELECT * FROM bodyweights") suspend fun bodyweightsOnce(): List<Bodyweight>
    @Upsert suspend fun upsert(bw: Bodyweight)
    @Upsert suspend fun upsertBodyweights(list: List<Bodyweight>)

    // plan
    @Query("SELECT * FROM week_plan ORDER BY weekday") fun weekPlan(): Flow<List<WeekPlan>>
    @Query("SELECT * FROM week_plan") suspend fun weekPlanOnce(): List<WeekPlan>
    @Upsert suspend fun upsert(plan: WeekPlan)
    @Upsert suspend fun upsertWeekPlan(list: List<WeekPlan>)
    @Query("SELECT * FROM day_overrides") fun overrides(): Flow<List<DayOverride>>
    @Query("SELECT * FROM day_overrides") suspend fun overridesOnce(): List<DayOverride>
    @Upsert suspend fun upsert(o: DayOverride)
    @Upsert suspend fun upsertOverrides(list: List<DayOverride>)
    @Query("DELETE FROM day_overrides WHERE date = :date") suspend fun deleteOverride(date: String)
    /**
     * Changes a weekday in the template. Past dates of that weekday first get auto overrides with the old value, so
     * history keeps its plan. Read and written in one transaction: a dated change saved meanwhile stays as it is.
     */
    @Transaction suspend fun saveWeekPlan(weekday: Int, itemType: String, itemId: String?, today: LocalDate) {
        val old = weekPlanOnce().find { it.weekday == weekday }
        if ((old?.itemType ?: ItemType.REST) == itemType && old?.itemId == itemId) return
        val overrides = overridesOnce()
        val first = PlanRules.freezeFrom(sessionsOnce().map { it.date } + stretchSessionsOnce().map { it.date } + bodyweightsOnce().map { it.date } + windowDaysOnce().map { it.date } + overrides.map { it.date }, today)
        val frozen = PlanRules.datesToFreeze(weekday, first, today, overrides.map { it.date }.toSet()).map { DayOverride(it.iso(), old?.itemType ?: ItemType.REST, old?.itemId, auto = true) }
        upsertOverrides(frozen); upsert(WeekPlan(weekday, itemType, itemId))
    }
    /** Two dates trade what they hold, each resolved from the plan as stored in the same transaction. */
    @Transaction suspend fun swapDays(a: LocalDate, b: LocalDate) {
        val data = KhonRepository.PlanData(weekPlanOnce(), overridesOnce(), programsOnce(), exercisesOnce())
        val ia = data.itemFor(a); val ib = data.itemFor(b)
        upsert(DayOverride(a.iso(), ib.itemType, ib.program?.id ?: ib.cardio?.id))
        upsert(DayOverride(b.iso(), ia.itemType, ia.program?.id ?: ia.cardio?.id))
    }

    // stretching
    @Query("SELECT * FROM stretches ORDER BY name") fun stretches(): Flow<List<Stretch>>
    @Query("SELECT * FROM stretches") suspend fun stretchesOnce(): List<Stretch>
    @Upsert suspend fun upsert(s: Stretch)
    @Upsert suspend fun upsertStretches(list: List<Stretch>)
    @Query("DELETE FROM stretches WHERE id = :id") suspend fun deleteStretch(id: String)
    @Query("SELECT COUNT(*) FROM routine_stretches WHERE stretchId = :id") suspend fun routineUseCount(id: String): Int
    @Query("SELECT * FROM stretch_routines ORDER BY sortOrder, name") fun stretchRoutines(): Flow<List<StretchRoutine>>
    @Query("SELECT * FROM stretch_routines") suspend fun stretchRoutinesOnce(): List<StretchRoutine>
    @Upsert suspend fun upsert(r: StretchRoutine)
    @Upsert suspend fun upsertStretchRoutines(list: List<StretchRoutine>)
    @Query("UPDATE stretch_routines SET active = (id = :id)") suspend fun setActiveRoutine(id: String)
    @Query("DELETE FROM stretch_routines WHERE id = :id") suspend fun deleteStretchRoutine(id: String)
    @Query("SELECT * FROM routine_stretches ORDER BY sortOrder") fun routineStretches(): Flow<List<RoutineStretch>>
    @Query("SELECT * FROM routine_stretches") suspend fun routineStretchesOnce(): List<RoutineStretch>
    @Upsert suspend fun upsert(rs: RoutineStretch)
    @Upsert suspend fun upsertRoutineStretches(list: List<RoutineStretch>)
    @Query("DELETE FROM routine_stretches WHERE id = :id") suspend fun deleteRoutineStretch(id: String)
    @Query("DELETE FROM routine_stretches WHERE routineId = :routineId") suspend fun deleteRoutineStretchesFor(routineId: String)
    /** A routine with its stretch rows: all or nothing. */
    @Transaction suspend fun deleteRoutineWithStretches(id: String) { deleteRoutineStretchesFor(id); deleteStretchRoutine(id) }
    @Query("SELECT * FROM stretch_sessions ORDER BY date DESC, startedAt DESC") fun stretchSessions(): Flow<List<StretchSession>>
    @Query("SELECT * FROM stretch_sessions") suspend fun stretchSessionsOnce(): List<StretchSession>
    @Upsert suspend fun upsert(s: StretchSession)
    @Upsert suspend fun upsertStretchSessions(list: List<StretchSession>)
    @Query("DELETE FROM stretch_sessions WHERE id = :id") suspend fun deleteStretchSession(id: String)

    // eating window
    @Query("SELECT * FROM eating_window WHERE id = 1") fun eatingWindow(): Flow<EatingWindow?>
    @Query("SELECT * FROM eating_window WHERE id = 1") suspend fun eatingWindowOnce(): EatingWindow?
    @Upsert suspend fun upsert(w: EatingWindow)
    @Query("SELECT * FROM window_days ORDER BY date") fun windowDays(): Flow<List<WindowDay>>
    @Query("SELECT * FROM window_days") suspend fun windowDaysOnce(): List<WindowDay>
    @Query("SELECT * FROM window_days WHERE date = :date") suspend fun windowDay(date: String): WindowDay?
    @Upsert suspend fun upsert(d: WindowDay)
    @Upsert suspend fun upsertWindowDays(list: List<WindowDay>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertWindowDayIfMissing(d: WindowDay)
    @Query("UPDATE window_days SET openedAt = :at, closedAt = NULL WHERE date = :date") suspend fun setWindowOpened(date: String, at: Long)
    @Query("UPDATE window_days SET closedAt = :at WHERE date = :date") suspend fun setWindowClosed(date: String, at: Long)
    // Open now and Close now write their own columns onto the day row as it is now: a note or an import stays.
    @Transaction suspend fun openWindowNow(date: String, now: Long) { insertWindowDayIfMissing(WindowDay(date)); setWindowOpened(date, now) }
    @Transaction suspend fun closeWindowNow(date: String, now: Long) { insertWindowDayIfMissing(WindowDay(date)); setWindowClosed(date, now) }

    // wipe
    @Query("DELETE FROM stretches") suspend fun clearStretches()
    @Query("DELETE FROM stretch_routines") suspend fun clearStretchRoutines()
    @Query("DELETE FROM routine_stretches") suspend fun clearRoutineStretches()
    @Query("DELETE FROM stretch_sessions") suspend fun clearStretchSessions()
    @Query("DELETE FROM eating_window") suspend fun clearEatingWindow()
    @Query("DELETE FROM window_days") suspend fun clearWindowDays()
    @Query("DELETE FROM exercises") suspend fun clearExercises()
    @Query("DELETE FROM modalities") suspend fun clearModalities()
    @Query("DELETE FROM programs") suspend fun clearPrograms()
    @Query("DELETE FROM blocks") suspend fun clearBlocks()
    @Query("DELETE FROM block_exercises") suspend fun clearBlockExercises()
    @Query("DELETE FROM sessions") suspend fun clearSessions()
    @Query("DELETE FROM set_logs") suspend fun clearSetLogs()
    @Query("DELETE FROM interval_logs") suspend fun clearIntervalLogs()
    @Query("DELETE FROM bodyweights") suspend fun clearBodyweights()
    @Query("DELETE FROM week_plan") suspend fun clearWeekPlan()
    @Query("DELETE FROM day_overrides") suspend fun clearOverrides()

    // seeds: each check runs in the transaction that writes, so data saved meanwhile is never seeded over
    @Transaction suspend fun seedIfEmpty() { if (programsOnce().isEmpty() && exercisesOnce().isEmpty()) Seed.apply(this); seedStretchesIfEmpty() }
    /** Older installs have no stretches yet; the routine arrives on first start after the update. */
    @Transaction suspend fun seedStretchesIfEmpty() { if (stretchesOnce().isEmpty()) Seed.applyStretches(this) }
    /** Wipe and built-in data: all or nothing, so a failed reset keeps the old data. */
    @Transaction suspend fun resetToSeed() { clearAll(); Seed.apply(this); Seed.applyStretches(this) }

    @Query("SELECT id FROM programs WHERE active = 1 ORDER BY sortOrder, name LIMIT 1") suspend fun firstActiveProgramId(): String?
    @Query("SELECT id FROM programs ORDER BY sortOrder, name LIMIT 1") suspend fun firstProgramId(): String?

    /**
     * A backup written in one transaction, after a wipe when [replace]. Afterwards exactly one program is active when
     * any exists: a merge keeps this phone's active program and clears the mark on imported ones; a replace (or a
     * merge onto a phone with none) takes the backup's first active program in list order, else the first program.
     */
    @Transaction suspend fun importBackup(b: Backup, replace: Boolean) {
        val kept = if (replace) null else firstActiveProgramId()
        if (replace) clearAll()
        upsertModalities(b.modalities); upsertExercises(b.exercises); upsertPrograms(b.programs); upsertBlocks(b.blocks)
        upsertBlockExercises(b.blockExercises); upsertSessions(b.sessions); upsertSetLogs(b.setLogs); upsertIntervalLogs(b.intervalLogs)
        upsertBodyweights(b.bodyweights); upsertWeekPlan(b.weekPlan); upsertOverrides(b.overrides)
        upsertStretches(b.stretches); upsertStretchRoutines(b.stretchRoutines); upsertRoutineStretches(b.routineStretches)
        upsertStretchSessions(b.stretchSessions); b.eatingWindow?.let { upsert(it) }; upsertWindowDays(b.windowDays)
        if (replace && stretchesOnce().isEmpty()) Seed.applyStretches(this)
        (kept ?: firstActiveProgramId() ?: firstProgramId())?.let { setActiveProgram(it) }
    }

    @Transaction
    suspend fun clearAll() {
        clearSetLogs(); clearIntervalLogs(); clearSessions(); clearBlockExercises(); clearBlocks(); clearPrograms()
        clearExercises(); clearModalities(); clearBodyweights(); clearWeekPlan(); clearOverrides()
        clearRoutineStretches(); clearStretchSessions(); clearStretchRoutines(); clearStretches(); clearEatingWindow(); clearWindowDays()
    }
}
