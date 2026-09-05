package com.richie.stride.data

import java.time.LocalDate
import java.time.LocalTime

fun HabitEntity.toDomain(): Habit {
    val days = if (scheduleDays.isBlank()) emptySet() else
        scheduleDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    return Habit(
        id = id,
        name = name,
        category = runCatching { Category.valueOf(category) }.getOrDefault(Category.OTHER),
        goalType = runCatching { GoalType.valueOf(goalType) }.getOrDefault(GoalType.YES_NO),
        target = target,
        unit = unit,
        schedule = Schedule(
            type = runCatching { ScheduleType.valueOf(scheduleType) }.getOrDefault(ScheduleType.DAILY),
            days = days,
            interval = scheduleInterval,
            timesPerWeek = scheduleTimesPerWeek
        ),
        grace = grace,
        reminderTime = reminderTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
        archived = archived,
        pausedUntil = pausedUntil?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        createdAt = runCatching { LocalDate.parse(createdAt) }.getOrDefault(LocalDate.now())
    )
}

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    name = name,
    category = category.name,
    goalType = goalType.name,
    target = target,
    unit = unit,
    scheduleType = schedule.type.name,
    scheduleDays = schedule.days.joinToString(","),
    scheduleInterval = schedule.interval,
    scheduleTimesPerWeek = schedule.timesPerWeek,
    grace = grace,
    reminderTime = reminderTime?.toString(),
    archived = archived,
    pausedUntil = pausedUntil?.toString(),
    createdAt = createdAt.toString()
)

fun CompletionEntity.toDomain(): Completion = Completion(
    habitId = habitId,
    date = LocalDate.parse(date),
    value = value,
    isGrace = isGrace
)

fun Completion.toEntity(): CompletionEntity = CompletionEntity(
    habitId = habitId,
    date = date.toString(),
    value = value,
    isGrace = isGrace
)

fun NoteEntity.toDomain(): HabitNote = HabitNote(
    habitId = habitId,
    date = LocalDate.parse(date),
    mood = mood?.let { runCatching { Mood.valueOf(it) }.getOrNull() },
    text = text
)

fun HabitNote.toEntity(): NoteEntity = NoteEntity(
    habitId = habitId,
    date = date.toString(),
    mood = mood?.name,
    text = text
)

fun RoutineEntity.toDomain(habitIds: List<String>): Routine = Routine(
    id = id,
    name = name,
    timeLabel = runCatching { TimeLabel.valueOf(timeLabel) }.getOrDefault(TimeLabel.MORNING),
    sortOrder = sortOrder,
    habitIds = habitIds
)
