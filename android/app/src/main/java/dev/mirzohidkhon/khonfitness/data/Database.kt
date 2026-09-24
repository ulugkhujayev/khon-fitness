package dev.mirzohidkhon.khonfitness.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Exercise::class, Modality::class, Program::class, Block::class, BlockExercise::class,
        Session::class, SetLog::class, IntervalLog::class, Bodyweight::class, WeekPlan::class, DayOverride::class,
        Stretch::class, StretchRoutine::class, RoutineStretch::class, StretchSession::class, EatingWindow::class, WindowDay::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class KhonDatabase : RoomDatabase() {
    abstract fun dao(): KhonDao

    companion object {
        /** Day overrides get the auto flag; existing rows are user edits. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `day_overrides` ADD COLUMN `auto` INTEGER NOT NULL DEFAULT 0") }
        }

        fun build(context: Context): KhonDatabase =
            Room.databaseBuilder(context, KhonDatabase::class.java, "khon.db").addMigrations(MIGRATION_3_4).build()
    }
}
