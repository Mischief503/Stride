package com.richie.stride.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.richie.stride.data.Completion
import com.richie.stride.data.Habit
import com.richie.stride.data.Mood
import com.richie.stride.data.StatsCalculator
import com.richie.stride.ui.MainViewModel
import com.richie.stride.ui.components.HabitMenuDialog
import com.richie.stride.ui.components.HabitRow
import com.richie.stride.ui.components.NoteDialog
import com.richie.stride.ui.components.PauseDialog
import com.richie.stride.ui.components.ValueDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private sealed interface ActiveDialog {
    data class Menu(val habit: Habit) : ActiveDialog
    data class Note(val habit: Habit) : ActiveDialog
    data class Value(val habit: Habit) : ActiveDialog
    data class Pause(val habit: Habit) : ActiveDialog
}

@Composable
fun TodayScreen(
    viewModel: MainViewModel,
    onAddHabit: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onEditHabit: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val today = remember { LocalDate.now() }
    val weekStart = state.settings.weekStart
    var dialog by remember { mutableStateOf<ActiveDialog?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val activeHabits = state.habits.filter { !it.archived }
    val dueToday = activeHabits.filter { StatsCalculator.isDueOn(it, today) }
    val doneCount = dueToday.count { StatsCalculator.isDoneOn(it, state.completionsByHabit[it.id]?.get(today)) }
    val total = dueToday.size
    val score = if (total > 0) Math.round(doneCount * 100.0 / total).toInt() else 0
    val momentum = StatsCalculator.momentumStreak(activeHabits, state.completionsByHabit, today)
    val graceAvailCount = dueToday.count {
        StatsCalculator.graceAvailable(it, state.completionsByHabit[it.id] ?: emptyMap(), weekStart, today)
    }

    val routinesWithMembers = state.routines.mapNotNull { routine ->
        val members = dueToday.filter { routine.habitIds.contains(it.id) }
        if (members.isEmpty()) null else routine to members
    }
    val routineHabitIds = routinesWithMembers.flatMap { it.second.map { h -> h.id } }.toSet()
    val leftover = dueToday.filterNot { routineHabitIds.contains(it.id) }
    val buckets = leftover.groupBy { StatsCalculator.timeBucket(it.reminderTime) }

    Box(Modifier.fillMaxSize()) {
        if (dueToday.isEmpty() && activeHabits.isEmpty()) {
            EmptyTodayState(onAddHabit)
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item {
                    Text(
                        today.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                    val subMessage = if (total > 0 && doneCount == total) "Every habit logged \u2014 see you tomorrow."
                        else if (total > 0) "${total - doneCount} more to a perfect day" else ""
                    com.richie.stride.ui.components.DailyScoreCard(
                        score = score, doneCount = doneCount, total = total,
                        momentumDays = momentum, subMessage = subMessage,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    if (graceAvailCount > 0) {
                        Row(
                            Modifier
                                .fillMaxSize()
                                .padding(bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Text(
                                if (graceAvailCount == 1) "1 habit still has a grace skip this week."
                                else "$graceAvailCount habits still have a grace skip this week.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                routinesWithMembers.forEach { (routine, members) ->
                    item {
                        val doneInRoutine = members.count { StatsCalculator.isDoneOn(it, state.completionsByHabit[it.id]?.get(today)) }
                        Text(
                            "${routine.name} \u2014 $doneInRoutine/${members.size}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(members) { habit ->
                        HabitRowContainer(habit, state.completionsByHabit[habit.id]?.get(today), viewModel, today) { dlg -> dialog = dlg }
                    }
                }

                listOf(
                    com.richie.stride.data.TimeLabel.MORNING to "Morning",
                    com.richie.stride.data.TimeLabel.AFTERNOON to "Afternoon",
                    com.richie.stride.data.TimeLabel.EVENING to "Evening",
                    com.richie.stride.data.TimeLabel.ANYTIME to "Anytime"
                ).forEach { (label, text) ->
                    val group = buckets[label].orEmpty()
                    if (group.isNotEmpty()) {
                        item {
                            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
                        }
                        items(group) { habit ->
                            HabitRowContainer(habit, state.completionsByHabit[habit.id]?.get(today), viewModel, today) { dlg -> dialog = dlg }
                        }
                    }
                }

                item { androidx.compose.foundation.layout.Spacer(Modifier.padding(40.dp)) }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    when (val d = dialog) {
        is ActiveDialog.Menu -> {
            val completions = state.completionsByHabit[d.habit.id] ?: emptyMap()
            HabitMenuDialog(
                habitName = d.habit.name,
                dateLabel = null,
                canUseGrace = StatsCalculator.graceAvailable(d.habit, completions, weekStart, today) &&
                    !StatsCalculator.isDoneOn(d.habit, completions[today]),
                canLogValue = d.habit.goalType != com.richie.stride.data.GoalType.YES_NO,
                isPaused = d.habit.pausedUntil != null,
                onDismiss = { dialog = null },
                onUseGrace = { viewModel.useGrace(d.habit.id, today); dialog = null },
                onLogNote = { dialog = ActiveDialog.Note(d.habit) },
                onLogValue = { dialog = ActiveDialog.Value(d.habit) },
                onViewInsights = { dialog = null; onOpenDetail(d.habit.id) },
                onEdit = { dialog = null; onEditHabit(d.habit.id) },
                onPause = { dialog = ActiveDialog.Pause(d.habit) },
                onResume = { viewModel.resumeHabit(d.habit.id); dialog = null },
                onArchive = { viewModel.archiveHabit(d.habit.id); dialog = null }
            )
        }
        is ActiveDialog.Note -> {
            NoteDialog(
                habitName = d.habit.name,
                date = today,
                initialMood = null,
                initialText = "",
                onDismiss = { dialog = null },
                onSave = { mood, text -> viewModel.saveNote(d.habit.id, today, mood, text); dialog = null }
            )
        }
        is ActiveDialog.Value -> {
            val current = state.completionsByHabit[d.habit.id]?.get(today)?.takeIf { !it.isGrace }?.value ?: 0
            ValueDialog(
                habitName = d.habit.name, date = today, target = d.habit.target, unit = d.habit.unit,
                initialValue = current,
                onDismiss = { dialog = null },
                onSave = { v -> viewModel.setValue(d.habit.id, today, v); dialog = null },
                onClear = { viewModel.clearCompletion(d.habit.id, today); dialog = null }
            )
        }
        is ActiveDialog.Pause -> {
            PauseDialog(
                habitName = d.habit.name,
                onDismiss = { dialog = null },
                onConfirm = { until -> viewModel.pauseHabit(d.habit.id, until); dialog = null }
            )
        }
        null -> {}
    }
}

@Composable
private fun HabitRowContainer(
    habit: Habit,
    completion: Completion?,
    viewModel: MainViewModel,
    date: LocalDate,
    onOpenDialog: (ActiveDialog) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val completions = state.completionsByHabit[habit.id] ?: emptyMap()
    val streak = StatsCalculator.computeStreak(habit, completions, date)

    HabitRow(
        habit = habit,
        completion = completion,
        streak = streak,
        onToggle = { viewModel.toggleYesNo(habit.id, date) },
        onStep = { delta -> viewModel.stepValue(habit.id, date, delta) },
        onMenu = { onOpenDialog(ActiveDialog.Menu(habit)) },
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun EmptyTodayState(onAddHabit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Start your first habit", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            "Add something small \u2014 you can build from there.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        Button(onClick = onAddHabit) { Text("Add a habit") }
    }
}
