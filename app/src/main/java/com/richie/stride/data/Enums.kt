package com.richie.stride.data

enum class GoalType { YES_NO, COUNTER, DURATION }

enum class ScheduleType { DAILY, WEEKDAYS, WEEKENDS, SPECIFIC, INTERVAL, TIMES_PER_WEEK }

enum class Category { HEALTH, MIND, FOCUS, OTHER }

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class Mood { GREAT, GOOD, OKAY, HARD }

enum class TimeLabel { MORNING, AFTERNOON, EVENING, ANYTIME }

enum class AppLanguage(val tag: String) {
    ENGLISH("en"), SPANISH("es"), FRENCH("fr")
}
