package com.richie.stride.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.richie.stride.ui.MainViewModel
import com.richie.stride.ui.addedit.AddEditScreen
import com.richie.stride.ui.calendar.CalendarScreen
import com.richie.stride.ui.detail.DetailScreen
import com.richie.stride.ui.habits.HabitsScreen
import com.richie.stride.ui.insights.InsightsScreen
import com.richie.stride.ui.onboarding.OnboardingScreen
import com.richie.stride.ui.routine.RoutineEditScreen
import com.richie.stride.ui.settings.SettingsScreen
import com.richie.stride.ui.today.TodayScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrideNavHost(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()

    if (state.loaded && !state.settings.onboarded) {
        OnboardingScreen(onGetStarted = { viewModel.setOnboarded(true) })
        return
    }
    if (!state.loaded) return

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isMainRoute = currentRoute in Destinations.bottomNavRoutes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleFor(currentRoute)) },
                navigationIcon = {
                    if (!isMainRoute) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isMainRoute) {
                        IconButton(onClick = { navController.navigate(Destinations.SETTINGS) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isMainRoute) {
                NavigationBar {
                    val items = listOf(
                        Triple(Destinations.TODAY, Icons.Filled.CheckCircle, "Today"),
                        Triple(Destinations.CALENDAR, Icons.Filled.CalendarMonth, "Calendar"),
                        Triple(Destinations.INSIGHTS, Icons.Filled.BarChart, "Insights"),
                        Triple(Destinations.HABITS, Icons.Filled.List, "Habits")
                    )
                    items.forEach { (route, icon, label) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(Destinations.TODAY) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Destinations.TODAY || currentRoute == Destinations.HABITS) {
                FloatingActionButton(onClick = { navController.navigate(Destinations.addHabit()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add habit")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.TODAY,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destinations.TODAY) {
                TodayScreen(
                    viewModel = viewModel,
                    onAddHabit = { navController.navigate(Destinations.addHabit()) },
                    onOpenDetail = { id -> navController.navigate(Destinations.detail(id)) },
                    onEditHabit = { id -> navController.navigate(Destinations.editHabit(id)) }
                )
            }
            composable(Destinations.CALENDAR) {
                CalendarScreen(
                    viewModel = viewModel,
                    onOpenDetail = { id -> navController.navigate(Destinations.detail(id)) },
                    onEditHabit = { id -> navController.navigate(Destinations.editHabit(id)) }
                )
            }
            composable(Destinations.INSIGHTS) {
                InsightsScreen(viewModel = viewModel)
            }
            composable(Destinations.HABITS) {
                HabitsScreen(
                    viewModel = viewModel,
                    onOpenDetail = { id -> navController.navigate(Destinations.detail(id)) },
                    onNewRoutine = { navController.navigate(Destinations.newRoutine()) },
                    onEditRoutine = { id -> navController.navigate(Destinations.editRoutine(id)) }
                )
            }
            composable(Destinations.SETTINGS) {
                SettingsScreen(viewModel = viewModel, onResetDone = { navController.popBackStack(Destinations.TODAY, false) })
            }
            composable(
                Destinations.DETAIL,
                arguments = listOf(navArgument(Destinations.DETAIL_ARG) { type = NavType.StringType })
            ) { entry ->
                val habitId = entry.arguments?.getString(Destinations.DETAIL_ARG) ?: return@composable
                DetailScreen(
                    viewModel = viewModel,
                    habitId = habitId,
                    onEdit = { navController.navigate(Destinations.editHabit(habitId)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Destinations.ADD_EDIT,
                arguments = listOf(navArgument(Destinations.ADD_EDIT_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { entry ->
                val habitId = entry.arguments?.getString(Destinations.ADD_EDIT_ARG)
                AddEditScreen(viewModel = viewModel, habitId = habitId, onDone = { navController.popBackStack() })
            }
            composable(
                Destinations.ROUTINE_EDIT,
                arguments = listOf(navArgument(Destinations.ROUTINE_EDIT_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { entry ->
                val routineId = entry.arguments?.getString(Destinations.ROUTINE_EDIT_ARG)
                RoutineEditScreen(viewModel = viewModel, routineId = routineId, onDone = { navController.popBackStack() })
            }
        }
    }
}

private fun titleFor(route: String?): String = when (route) {
    Destinations.TODAY -> "Today"
    Destinations.CALENDAR -> "Calendar"
    Destinations.INSIGHTS -> "Insights"
    Destinations.HABITS -> "Habits"
    Destinations.SETTINGS -> "Settings"
    Destinations.DETAIL -> "Habit"
    Destinations.ADD_EDIT -> "Habit"
    Destinations.ROUTINE_EDIT -> "Routine"
    else -> "Stride"
}
