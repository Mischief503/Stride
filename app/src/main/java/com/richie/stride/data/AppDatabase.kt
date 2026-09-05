package com.richie.stride.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HabitEntity::class,
        CompletionEntity::class,
        NoteEntity::class,
        RoutineEntity::class,
        RoutineHabitCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun completionDao(): CompletionDao
    abstract fun noteDao(): NoteDao
    abstract fun routineDao(): RoutineDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_tracker.db"
                ).build().also { instance = it }
            }
        }
    }
}
