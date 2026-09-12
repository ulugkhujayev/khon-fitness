package dev.mirzohidkhon.khonfitness.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Exercise::class, Modality::class, Program::class, Block::class, BlockExercise::class,
        Session::class, SetLog::class, IntervalLog::class, Bodyweight::class, WeekPlan::class, DayOverride::class,
        Stretch::class, StretchRoutine::class, RoutineStretch::class, StretchSession::class, EatingWindow::class, WindowDay::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class KhonDatabase : RoomDatabase() {
    abstract fun dao(): KhonDao

    companion object {
        fun build(context: Context): KhonDatabase =
            Room.databaseBuilder(context, KhonDatabase::class.java, "khon.db").build()
    }
}
