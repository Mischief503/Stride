package com.richie.stride.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.richie.stride.data.DEFAULT_SLOT_ID
import com.richie.stride.data.GoalType
import com.richie.stride.data.Habit
import com.richie.stride.data.StatsCalculator
import com.richie.stride.ui.MainViewModel
import com.richie.stride.ui.components.CategorySwatch
import com.richie.stride.ui.components.HabitMenuDialog
import com.richie.stride.ui.components.HabitRow
import com.richie.stride.ui.components.NoteDialog
import com.richie.stride.ui.components.PauseDialog
import com.richie.stride.ui.components.ValueDialog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private sealed interface CalDialog {
    data class Menu(val habit: Habit, val date: LocalDate, val slotId: String) : CalDialog
    data class Note(val habit: Habit, val date: LocalDate) : CalDialog
    data class Value(val habit: Habit, val date: LocalDate, val slotId: String) : CalDialog
    data class Pause(val habit: Habit) : CalDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    onOpenDetail: (String) -> Unit,
    onEditHabit: (String) -> Unit,
    onAddHabitForDate: (LocalDate) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var dialog by remember { mutableStateOf<CalDialog?>(null) }
    val weekStart = state.settings.weekStart
    val today = remember { LocalDate.now() }
    val activeHabits = state.habits.filter { !it.archived }

    val order = StatsCalculator.weekDayOrder(weekStart)
    val firstOfMonth = month.atDay(1)
    val firstOffset = order.indexOf(firstOfMonth.dayOfWeek)
    val daysInMonth = month.lengthOfMonth()

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month") }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next month") }
        }
        Row(Modifier.fillMaxWidth()) {
            order.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth()) {
            items(firstOffset) { Box(Modifier.aspectRatio(1f)) }
            items(daysInMonth) { i ->
                val day = i + 1
                val date = month.atDay(day)
                val isFuture = date.isAfter(today)
                val due = activeHabits.filter { StatsCalculator.isDueOn(it, date) }
                val doneCount = due.count { StatsCalculator.isFullyDoneOn(it, state.completionsBySlot[it.id] ?: emptyMap(), date) }
                val graceCount = due.count { StatsCalculator.isGraceOn(state.completionsByHabit[it.id]?.get(date)) }
                val dotColor = when {
                    isFuture || due.isEmpty() -> Color.Transparent
                    doneCount + graceCount == due.size -> MaterialTheme.colorScheme.primary
                    doneCount > 0 -> Color(0xFFFFB627)
                    else -> Color(0xFFC64726)
                }
                Column(
                    Modifier
                        .aspectRatio(1f)
                        .clickable { selectedDate = date },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("$day", style = if (date == today) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
                    Box(Modifier.size(6.dp).background(dotColor, CircleShape))
                }
            }
        }
    }

    selectedDate?.let { date ->
        val sheetState = rememberModalBottomSheetState()
        val due = activeHabits.filter { StatsCalculator.isDueOn(it, date) }
        ModalBottomSheet(onDismissRequest = { selectedDate = null }, sheetState = sheetState) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Habits for ${date.format(DateTimeFormatter.ofPattern("MMM d"))}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (due.isEmpty()) {
                    Text("No habits scheduled this day.", modifier = Modifier.padding(bottom = 12.dp))
                } else {
                    due.forEach { habit ->
                        CalHabitRowGroup(habit, viewModel, date) { dlg -> dialog = dlg }
                    }
                }
                OutlinedButton(
                    onClick = { selectedDate = null; onAddHabitForDate(date) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Add a habit starting this day", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }

    when (val d = dialog) {
        is CalDialog.Menu -> {
            val slotCompletions = state.completionsBySlot[d.habit.id]?.get(d.slotId) ?: emptyMap()
            HabitMenuDialog(
                habitName = if (d.habit.hasMultipleSlots) {
                    val label = d.habit.slots.find { it.id == d.slotId }?.label?.trim()
                    if (label.isNullOrBlank()) d.habit.name else "${d.habit.name} ($label)"
                } else d.habit.name,
                dateLabel = d.date.format(DateTimeFormatter.ofPattern("MMM d")),
                canUseGrace = StatsCalculator.graceAvailable(d.habit, slotCompletions, weekStart, d.date) &&
                    !StatsCalculator.isDoneOn(d.habit, slotCompletions[d.date]),
                canLogValue = d.habit.goalType != GoalType.YES_NO,
                isPaused = d.habit.pausedUntil != null,
                onDismiss = { dialog = null },
                onUseGrace = { viewModel.useGrace(d.habit.id, d.date, d.slotId); dialog = null },
                onLogNote = { dialog = CalDialog.Note(d.habit, d.date) },
                onLogValue = { dialog = CalDialog.Value(d.habit, d.date, d.slotId) },
                onViewInsights = { dialog = null; selectedDate = null; onOpenDetail(d.habit.id) },
                onEdit = { dialog = null; selectedDate = null; onEditHabit(d.habit.id) },
                onPause = { dialog = CalDialog.Pause(d.habit) },
                onResume = { viewModel.resumeHabit(d.habit.id); dialog = null },
                onArchive = { viewModel.archiveHabit(d.habit.id); dialog = null; selectedDate = null }
            )
        }
        is CalDialog.Note -> {
            val existing = state.notesByHabit[d.habit.id]?.get(d.date)
            NoteDialog(
                habitName = d.habit.name, date = d.date,
                initialMood = existing?.mood, initialText = existing?.text ?: "",
                onDismiss = { dialog = null },
                onSave = { mood, text -> viewModel.saveNote(d.habit.id, d.date, mood, text); dialog = null }
            )
        }
        is CalDialog.Value -> {
            val current = state.completionsBySlot[d.habit.id]?.get(d.slotId)?.get(d.date)?.takeIf { !it.isGrace }?.value ?: 0
            ValueDialog(
                habitName = d.habit.name, date = d.date, target = d.habit.target, unit = d.habit.unit,
                initialValue = current,
                onDismiss = { dialog = null },
                onSave = { v -> viewModel.setValue(d.habit.id, d.date, v, d.slotId); dialog = null },
                onClear = { viewModel.clearCompletion(d.habit.id, d.date, d.slotId); dialog = null }
            )
        }
        is CalDialog.Pause -> {
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
private fun CalHabitRowGroup(
    habit: Habit,
    viewModel: MainViewModel,
    date: LocalDate,
    onOpenDialog: (CalDialog) -> Unit
) {
    val state by viewModel.state.collectAsState()

    if (!habit.hasMultipleSlots) {
        val completions = state.completionsBySlot[habit.id]?.get(DEFAULT_SLOT_ID) ?: emptyMap()
        val streak = StatsCalculator.computeStreak(habit, completions, date)
        HabitRow(
            habit = habit,
            completion = completions[date],
            streak = streak,
            onToggle = { viewModel.toggleYesNo(habit.id, date) },
            onStep = { delta -> viewModel.stepValue(habit.id, date, delta) },
            onMenu = { onOpenDialog(CalDialog.Menu(habit, date, DEFAULT_SLOT_ID)) },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        return
    }

    Column(Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            CategorySwatch(habit.category, size = 20.dp)
            Text(habit.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 6.dp))
        }
        habit.slots.forEachIndexed { index, slot ->
            val slotCompletions = state.completionsBySlot[habit.id]?.get(slot.id) ?: emptyMap()
            val streak = StatsCalculator.computeStreak(habit, slotCompletions, date)
            HabitRow(
                habit = habit,
                completion = slotCompletions[date],
                streak = streak,
                onToggle = { viewModel.toggleYesNo(habit.id, date, slot.id) },
                onStep = { delta -> viewModel.stepValue(habit.id, date, delta, slot.id) },
                onMenu = { onOpenDialog(CalDialog.Menu(habit, date, slot.id)) },
                modifier = Modifier.padding(bottom = 6.dp, start = 12.dp),
                displayName = slot.label.ifBlank { "Time ${index + 1}" }
            )
        }
    }
}
