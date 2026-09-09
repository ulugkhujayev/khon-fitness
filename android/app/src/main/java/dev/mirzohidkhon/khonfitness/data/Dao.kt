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

    // wipe
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
    }
}
