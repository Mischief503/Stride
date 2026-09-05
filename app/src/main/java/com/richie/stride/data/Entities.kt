package com.richie.stride.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,          // Category enum name
    val goalType: String,          // GoalType enum name
    val target: Int,
    val unit: String,
    val scheduleType: String,      // ScheduleType enum name
    val scheduleDays: String,      // comma-separated day-of-week ints (1=Mon..7=Sun), only for SPECIFIC
    val scheduleInterval: Int,     // only for INTERVAL
    val scheduleTimesPerWeek: Int, // only for TIMES_PER_WEEK
    val grace: Boolean,
    val reminderTime: String?,     // "HH:mm" or null
    val archived: Boolean,
    val pausedUntil: String?,      // ISO date "yyyy-MM-dd" or null
    val createdAt: String          // ISO date
)

@Entity(tableName = "completions", primaryKeys = ["habitId", "date"])
data class CompletionEntity(
    val habitId: String,
    val date: String,   // ISO date "yyyy-MM-dd"
    val value: Int,      // progress amount; for YES_NO, 1 = done
    val isGrace: Boolean
)

@Entity(tableName = "notes", primaryKeys = ["habitId", "date"])
data class NoteEntity(
    val habitId: String,
    val date: String,
    val mood: String?,  // Mood enum name or null
    val text: String
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val timeLabel: String, // TimeLabel enum name
    val sortOrder: Int
)

@Entity(tableName = "routine_habit_cross_ref", primaryKeys = ["routineId", "habitId"])
data class RoutineHabitCrossRef(
    val routineId: String,
    val habitId: String,
    val position: Int
)
