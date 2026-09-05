package com.richie.stride.data

import java.time.LocalDate
import java.time.LocalTime

data class Schedule(
    val type: ScheduleType = ScheduleType.DAILY,
    val days: Set<Int> = emptySet(),   // ISO day-of-week values, 1=Monday..7=Sunday. Only used for SPECIFIC.
    val interval: Int = 2,             // only used for INTERVAL
    val timesPerWeek: Int = 3          // only used for TIMES_PER_WEEK
)

data class Habit(
    val id: String,
    val name: String,
    val category: Category,
    val goalType: GoalType,
    val target: Int,
    val unit: String,
    val schedule: Schedule,
    val grace: Boolean,
    val reminderTime: LocalTime?,
    val archived: Boolean,
    val pausedUntil: LocalDate?,
    val createdAt: LocalDate
)

data class Completion(
    val habitId: String,
    val date: LocalDate,
    val value: Int,
    val isGrace: Boolean
)

data class HabitNote(
    val habitId: String,
    val date: LocalDate,
    val mood: Mood?,
    val text: String
)

data class Routine(
    val id: String,
    val name: String,
    val timeLabel: TimeLabel,
    val sortOrder: Int,
    val habitIds: List<String>
)

/** A habit bundled with all of its logged data, the shape most stats functions operate on. */
data class HabitWithData(
    val habit: Habit,
    val completionsByDate: Map<LocalDate, Completion>,
    val notesByDate: Map<LocalDate, HabitNote>
)
