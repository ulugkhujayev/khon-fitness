package dev.mirzohidkhon.khonfitness.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
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

    // modalities
    @Query("SELECT * FROM modalities ORDER BY name") fun modalities(): Flow<List<Modality>>
    @Query("SELECT * FROM modalities") suspend fun modalitiesOnce(): List<Modality>
    @Upsert suspend fun upsert(modality: Modality)
    @Upsert suspend fun upsertModalities(list: List<Modality>)

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
    @Upsert suspend fun upsert(block: Block)
    @Upsert suspend fun upsertBlocks(list: List<Block>)
    @Query("DELETE FROM blocks WHERE id = :id") suspend fun deleteBlock(id: String)
    @Query("DELETE FROM blocks WHERE programId = :programId") suspend fun deleteBlocksFor(programId: String)

    @Query("SELECT * FROM block_exercises ORDER BY slot") fun blockExercises(): Flow<List<BlockExercise>>
    @Query("SELECT * FROM block_exercises WHERE blockId = :blockId ORDER BY slot") suspend fun blockExercisesFor(blockId: String): List<BlockExercise>
    @Query("SELECT * FROM block_exercises") suspend fun blockExercisesOnce(): List<BlockExercise>
    @Upsert suspend fun upsert(be: BlockExercise)
    @Upsert suspend fun upsertBlockExercises(list: List<BlockExercise>)
    @Query("DELETE FROM block_exercises WHERE id = :id") suspend fun deleteBlockExercise(id: String)
    @Query("DELETE FROM block_exercises WHERE blockId = :blockId") suspend fun deleteBlockExercisesFor(blockId: String)

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

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY `order`") fun setLogs(sessionId: String): Flow<List<SetLog>>
    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY `order`") suspend fun setLogsOnce(sessionId: String): List<SetLog>
    @Query("SELECT * FROM set_logs") fun allSetLogs(): Flow<List<SetLog>>
    @Query("SELECT * FROM set_logs") suspend fun allSetLogsOnce(): List<SetLog>
    @Upsert suspend fun upsert(log: SetLog)
    @Upsert suspend fun upsertSetLogs(list: List<SetLog>)
    @Query("DELETE FROM set_logs WHERE sessionId = :sessionId") suspend fun deleteSetLogsFor(sessionId: String)

    @Query("SELECT * FROM interval_logs WHERE sessionId = :sessionId ORDER BY `index`") fun intervalLogs(sessionId: String): Flow<List<IntervalLog>>
    @Query("SELECT * FROM interval_logs") suspend fun intervalLogsOnce(): List<IntervalLog>
    @Query("SELECT * FROM interval_logs") fun allIntervalLogs(): Flow<List<IntervalLog>>
    @Upsert suspend fun upsertIntervalLogs(list: List<IntervalLog>)
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
    @Transaction suspend fun saveWeekPlan(frozen: List<DayOverride>, plan: WeekPlan) { upsertOverrides(frozen); upsert(plan) }

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

    @Transaction
    suspend fun clearAll() {
        clearSetLogs(); clearIntervalLogs(); clearSessions(); clearBlockExercises(); clearBlocks(); clearPrograms()
        clearExercises(); clearModalities(); clearBodyweights(); clearWeekPlan(); clearOverrides()
        clearRoutineStretches(); clearStretchSessions(); clearStretchRoutines(); clearStretches(); clearEatingWindow(); clearWindowDays()
    }
}
