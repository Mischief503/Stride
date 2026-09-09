package com.richie.stride.data

import java.time.LocalDate
import java.time.LocalTime

data class Schedule(
    val type: ScheduleType = ScheduleType.DAILY,
    val days: Set<Int> = emptySet(),   // ISO day-of-week values, 1=Monday..7=Sunday. Only used for SPECIFIC.
    val interval: Int = 2,             // only used for INTERVAL
    val timesPerWeek: Int = 3          // only used for TIMES_PER_WEEK
)

/**
 * A named daily occurrence of a habit (e.g. "Morning" / "Evening" for a habit done twice a
 * day, each tracked independently). Every habit has at least one slot; single-occurrence
 * habits (still the common case) have exactly one, with id=DEFAULT_SLOT_ID and an empty label
 * (the habit's own name is shown instead of a sub-label in that case).
 */
data class HabitSlot(
    val id: String,
    val label: String,
    val reminderTime: LocalTime?
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
    val slots: List<HabitSlot>,   // always non-empty
    val archived: Boolean,
    val pausedUntil: LocalDate?,
    val createdAt: LocalDate
) {
    /** The single slot for a not-yet-multi-occurrence habit - the overwhelmingly common case. */
    val primarySlot: HabitSlot get() = slots.first()
    val hasMultipleSlots: Boolean get() = slots.size > 1
}

data class Completion(
    val habitId: String,
    val slotId: String,
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
