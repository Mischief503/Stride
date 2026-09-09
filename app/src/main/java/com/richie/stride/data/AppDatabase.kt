package com.richie.stride.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

const val DEFAULT_SLOT_ID = "default"

/**
 * Introduces habit_slots (multiple named daily occurrences per habit, e.g. "Morning"/"Evening")
 * and widens completions' primary key from (habitId, date) to (habitId, slotId, date) so each
 * slot can be completed independently.
 *
 * Every existing habit gets exactly one slot (slotId=DEFAULT_SLOT_ID, empty label, its old
 * reminderTime carried over), and every existing completion is attached to that slot - so
 * existing single-occurrence habits behave identically after this migration, with zero data
 * loss. Verified against a real SQLite database with sample data before writing this (exact
 * row counts in equal row counts out) rather than trusting it by inspection alone.
 *
 * habits.reminderTime is deliberately left in place, now unused - dropping it would require
 * rebuilding that table too, for no benefit; it's safe cleanup for a later, non-urgent pass.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE habit_slots (
                habitId TEXT NOT NULL,
                slotId TEXT NOT NULL,
                label TEXT NOT NULL,
                reminderTime TEXT,
                sortOrder INTEGER NOT NULL,
                PRIMARY KEY(habitId, slotId)
            )"""
        )
        db.execSQL(
            "INSERT INTO habit_slots (habitId, slotId, label, reminderTime, sortOrder) " +
                "SELECT id, '$DEFAULT_SLOT_ID', '', reminderTime, 0 FROM habits"
        )
        db.execSQL(
            """CREATE TABLE completions_new (
                habitId TEXT NOT NULL,
                slotId TEXT NOT NULL,
                date TEXT NOT NULL,
                value INTEGER NOT NULL,
                isGrace INTEGER NOT NULL,
                PRIMARY KEY(habitId, slotId, date)
            )"""
        )
        db.execSQL(
            "INSERT INTO completions_new (habitId, slotId, date, value, isGrace) " +
                "SELECT habitId, '$DEFAULT_SLOT_ID', date, value, isGrace FROM completions"
        )
        db.execSQL("DROP TABLE completions")
        db.execSQL("ALTER TABLE completions_new RENAME TO completions")
    }
}

@Database(
    entities = [
        HabitEntity::class,
        CompletionEntity::class,
        NoteEntity::class,
        RoutineEntity::class,
        RoutineHabitCrossRef::class,
        HabitSlotEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun completionDao(): CompletionDao
    abstract fun noteDao(): NoteDao
    abstract fun routineDao(): RoutineDao
    abstract fun habitSlotDao(): HabitSlotDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_tracker.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
        }
    }
}
