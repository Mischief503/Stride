package com.richie.stride.data

import java.time.LocalDate
import java.time.LocalTime

/** Slots must be passed in separately - they live in their own table, not on HabitEntity. */
fun HabitEntity.toDomain(slots: List<HabitSlot>): Habit {
    val days = if (scheduleDays.isBlank()) emptySet() else
        scheduleDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    return Habit(
        id = id,
        name = name,
        category = runCatching { Category.valueOf(category) }.getOrDefault(Category.SELF_CARE),
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
        slots = slots.ifEmpty { listOf(HabitSlot(DEFAULT_SLOT_ID, "", null)) },
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
    reminderTime = null, // legacy column, no longer read - see HabitEntity's own comment
    archived = archived,
    pausedUntil = pausedUntil?.toString(),
    createdAt = createdAt.toString()
)

fun HabitSlotEntity.toDomain(): HabitSlot = HabitSlot(
    id = slotId,
    label = label,
    reminderTime = reminderTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
)

fun HabitSlot.toEntity(habitId: String, sortOrder: Int): HabitSlotEntity = HabitSlotEntity(
    habitId = habitId,
    slotId = id,
    label = label,
    reminderTime = reminderTime?.toString(),
    sortOrder = sortOrder
)

fun CompletionEntity.toDomain(): Completion = Completion(
    habitId = habitId,
    slotId = slotId,
    date = LocalDate.parse(date),
    value = value,
    isGrace = isGrace
)

fun Completion.toEntity(): CompletionEntity = CompletionEntity(
    habitId = habitId,
    slotId = slotId,
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
