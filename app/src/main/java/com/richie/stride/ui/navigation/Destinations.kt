package com.richie.stride.ui.navigation

object Destinations {
    const val ONBOARDING = "onboarding"
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val INSIGHTS = "insights"
    const val HABITS = "habits"
    const val SETTINGS = "settings"

    const val DETAIL_ARG = "habitId"
    const val DETAIL = "detail/{$DETAIL_ARG}"
    fun detail(habitId: String) = "detail/$habitId"

    const val ADD_EDIT_ARG = "habitId"
    const val ADD_EDIT = "addEdit?$ADD_EDIT_ARG={$ADD_EDIT_ARG}"
    fun addHabit() = "addEdit"
    fun editHabit(habitId: String) = "addEdit?$ADD_EDIT_ARG=$habitId"

    const val ROUTINE_EDIT_ARG = "routineId"
    const val ROUTINE_EDIT = "routineEdit?$ROUTINE_EDIT_ARG={$ROUTINE_EDIT_ARG}"
    fun newRoutine() = "routineEdit"
    fun editRoutine(routineId: String) = "routineEdit?$ROUTINE_EDIT_ARG=$routineId"

    val bottomNavRoutes = listOf(TODAY, CALENDAR, INSIGHTS, HABITS)
}
