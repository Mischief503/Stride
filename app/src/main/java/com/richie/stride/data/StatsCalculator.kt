package com.richie.stride.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

enum class DayStatus { DONE, GRACE, NOT_DUE, NONE, FUTURE }

object StatsCalculator {

    fun isPaused(habit: Habit, date: LocalDate): Boolean {
        val until = habit.pausedUntil ?: return false
        return !date.isAfter(until)
    }

    fun isDueOn(habit: Habit, date: LocalDate): Boolean {
        if (isPaused(habit, date)) return false
        return when (habit.schedule.type) {
            ScheduleType.DAILY -> true
            ScheduleType.WEEKDAYS -> date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
            ScheduleType.WEEKENDS -> date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
            ScheduleType.SPECIFIC -> habit.schedule.days.contains(date.dayOfWeek.value)
            ScheduleType.INTERVAL -> {
                val n = ChronoUnit.DAYS.between(habit.createdAt, date)
                n >= 0 && n % max(1, habit.schedule.interval) == 0L
            }
            ScheduleType.TIMES_PER_WEEK -> true
        }
    }

    fun isGraceOn(completion: Completion?): Boolean = completion?.isGrace == true

    fun isDoneOn(habit: Habit, completion: Completion?): Boolean {
        if (completion == null || completion.isGrace) return false
        return if (habit.goalType == GoalType.YES_NO) completion.value >= 1 else completion.value >= habit.target
    }

    fun startOfWeek(date: LocalDate, weekStart: DayOfWeek): LocalDate {
        var d = date
        while (d.dayOfWeek != weekStart) d = d.minusDays(1)
        return d
    }

    /** Order of the 7 ISO day-of-week values starting from [weekStart]. */
    fun weekDayOrder(weekStart: DayOfWeek): List<DayOfWeek> {
        val all = DayOfWeek.entries
        val startIdx = all.indexOf(weekStart)
        return (0 until 7).map { all[(startIdx + it) % 7] }
    }

    fun computeStreak(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        today: LocalDate = LocalDate.now()
    ): Int {
        var streak = 0
        var d = today
        var guard = 0
        while (guard < 3650) {
            guard++
            if (!isDueOn(habit, d)) { d = d.minusDays(1); continue }
            val c = completions[d]
            if (isGraceOn(c)) { d = d.minusDays(1); continue }
            if (isDoneOn(habit, c)) { streak++; d = d.minusDays(1); continue }
            if (d == today) { d = d.minusDays(1); continue }
            break
        }
        return streak
    }

    fun longestStreak(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        today: LocalDate = LocalDate.now()
    ): Int {
        var longest = 0
        var current = 0
        var d = habit.createdAt
        var guard = 0
        while (!d.isAfter(today) && guard < 3650) {
            if (isDueOn(habit, d)) {
                val c = completions[d]
                if (isDoneOn(habit, c)) {
                    current++
                    if (current > longest) longest = current
                } else if (!isGraceOn(c)) {
                    current = 0
                }
            }
            d = d.plusDays(1)
            guard++
        }
        return longest
    }

    fun graceUsedThisWeek(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        weekStart: DayOfWeek,
        refDate: LocalDate = LocalDate.now()
    ): Int {
        val start = startOfWeek(refDate, weekStart)
        return (0 until 7).count { isGraceOn(completions[start.plusDays(it.toLong())]) }
    }

    fun graceAvailable(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        weekStart: DayOfWeek,
        refDate: LocalDate = LocalDate.now()
    ): Boolean = habit.grace && graceUsedThisWeek(habit, completions, weekStart, refDate) < 1

    data class WeeklyProgress(val done: Int, val target: Int)

    fun weeklyProgress(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        weekStart: DayOfWeek,
        refDate: LocalDate = LocalDate.now()
    ): WeeklyProgress {
        val start = startOfWeek(refDate, weekStart)
        val done = (0 until 7).count { isDoneOn(habit, completions[start.plusDays(it.toLong())]) }
        return WeeklyProgress(done, habit.schedule.timesPerWeek)
    }

    /** Completion rate over the last [days] days, as a percentage 0-100, or null if no trackable days yet. */
    fun completionRate(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        days: Int,
        today: LocalDate = LocalDate.now()
    ): Int? {
        val earliestStart = today.minusDays((days - 1).toLong())
        val start = if (habit.createdAt.isAfter(earliestStart)) habit.createdAt else earliestStart
        var tracked = 0
        var done = 0
        var d = start
        while (!d.isAfter(today)) {
            if (isDueOn(habit, d)) {
                val c = completions[d]
                if (!isGraceOn(c)) {
                    tracked++
                    if (isDoneOn(habit, c)) done++
                }
            }
            d = d.plusDays(1)
        }
        return if (tracked > 0) Math.round((done * 100.0) / tracked).toInt() else null
    }

    /** Completion rate per ISO day-of-week (1=Mon..7=Sun) over the last ~70 days, as percentages. */
    fun dayOfWeekStats(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        today: LocalDate = LocalDate.now()
    ): Map<DayOfWeek, Int> {
        val totals = mutableMapOf<DayOfWeek, Int>()
        val dones = mutableMapOf<DayOfWeek, Int>()
        for (i in 0 until 70) {
            val d = today.minusDays(i.toLong())
            if (d.isBefore(habit.createdAt)) continue
            if (!isDueOn(habit, d)) continue
            val c = completions[d]
            if (isGraceOn(c)) continue
            totals[d.dayOfWeek] = (totals[d.dayOfWeek] ?: 0) + 1
            if (isDoneOn(habit, c)) dones[d.dayOfWeek] = (dones[d.dayOfWeek] ?: 0) + 1
        }
        return DayOfWeek.entries.associateWith { dow ->
            val t = totals[dow] ?: 0
            if (t > 0) Math.round((dones[dow] ?: 0) * 100.0 / t).toInt() else 0
        }
    }

    data class HeatmapCell(val date: LocalDate, val status: DayStatus)

    fun heatmapData(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        weeks: Int,
        weekStart: DayOfWeek,
        today: LocalDate = LocalDate.now()
    ): List<HeatmapCell> {
        val start = startOfWeek(today.minusDays((weeks * 7L) - 7), weekStart)
        return (0 until weeks * 7).map { i ->
            val d = start.plusDays(i.toLong())
            val status = when {
                d.isAfter(today) -> DayStatus.FUTURE
                isGraceOn(completions[d]) -> DayStatus.GRACE
                !isDueOn(habit, d) -> DayStatus.NOT_DUE
                isDoneOn(habit, completions[d]) -> DayStatus.DONE
                else -> DayStatus.NONE
            }
            HeatmapCell(d, status)
        }
    }

    /** App-wide momentum: consecutive days where at least one due habit was completed. */
    fun momentumStreak(
        habits: List<Habit>,
        completionsByHabit: Map<String, Map<LocalDate, Completion>>,
        today: LocalDate = LocalDate.now()
    ): Int {
        var streak = 0
        var d = today
        var guard = 0
        while (guard < 3650) {
            guard++
            val due = habits.filter { isDueOn(it, d) }
            val doneCount = due.count { isDoneOn(it, completionsByHabit[it.id]?.get(d)) }
            if (doneCount > 0) { streak++; d = d.minusDays(1); continue }
            if (d == today) { d = d.minusDays(1); continue }
            break
        }
        return streak
    }

    fun stepFor(habit: Habit): Int = if (habit.goalType == GoalType.DURATION) 5 else 1

    fun timeBucket(time: java.time.LocalTime?): TimeLabel {
        if (time == null) return TimeLabel.ANYTIME
        return when {
            time.hour < 12 -> TimeLabel.MORNING
            time.hour < 18 -> TimeLabel.AFTERNOON
            else -> TimeLabel.EVENING
        }
    }
}
