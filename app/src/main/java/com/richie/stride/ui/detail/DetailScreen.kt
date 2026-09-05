package com.richie.stride.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.richie.stride.data.StatsCalculator
import com.richie.stride.ui.MainViewModel
import com.richie.stride.ui.components.CategorySwatch
import com.richie.stride.ui.components.ConfirmDialog
import com.richie.stride.ui.components.HeatmapGrid
import com.richie.stride.ui.components.NoteDialog
import com.richie.stride.ui.components.PauseDialog
import com.richie.stride.ui.components.ValueDialog
import com.richie.stride.ui.components.colorSet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DetailScreen(
    viewModel: MainViewModel,
    habitId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val habit = state.habits.find { it.id == habitId } ?: return
    val completions = state.completionsByHabit[habitId] ?: emptyMap()
    val notes = state.notesByHabit[habitId] ?: emptyMap()
    val today = remember { LocalDate.now() }
    val weekStart = state.settings.weekStart

    var showConfirmArchive by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }
    var showPause by remember { mutableStateOf(false) }
    var noteDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var valueDialogDate by remember { mutableStateOf<LocalDate?>(null) }

    val streak = StatsCalculator.computeStreak(habit, completions, today)
    val longest = StatsCalculator.longestStreak(habit, completions, today)
    val rate = StatsCalculator.completionRate(habit, completions, 30, today)
    val cells = StatsCalculator.heatmapData(habit, completions, 5, weekStart, today)

    LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        item {
            Row(Modifier.padding(top = 8.dp, bottom = 12.dp)) {
                CategorySwatch(habit.category)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                    val goalLine = if (habit.goalType == com.richie.stride.data.GoalType.YES_NO) "" else "${habit.target} ${habit.unit}"
                    if (goalLine.isNotBlank()) Text(goalLine, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (habit.pausedUntil != null) {
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Paused until ${habit.pausedUntil!!.format(DateTimeFormatter.ofPattern("MMM d"))}")
                        OutlinedButton(onClick = { viewModel.resumeHabit(habit.id) }) { Text("Resume") }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                StatCard("Current streak", "$streak", Modifier.weight(1f))
                StatCard("Longest streak", "$longest", Modifier.weight(1f))
                StatCard("30-day rate", rate?.let { "$it%" } ?: "\u2014", Modifier.weight(1f))
            }
            Text("Last 5 weeks", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
            HeatmapGrid(
                cells = cells,
                doneColor = habit.category.colorSet().color,
                onCellClick = { date ->
                    if (!date.isAfter(today)) {
                        if (habit.goalType == com.richie.stride.data.GoalType.YES_NO) {
                            viewModel.cycleYesNoBackfill(habit.id, date)
                        } else {
                            valueDialogDate = date
                        }
                    }
                },
                modifier = Modifier.padding(bottom = 18.dp)
            )
            Text("Notes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        }

        val noteDates = notes.keys.sortedDescending()
        if (noteDates.isEmpty()) {
            item { Text("No notes yet \u2014 add one after completing this habit.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(noteDates.take(10)) { date ->
                val note = notes.getValue(date)
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { noteDialogDate = date }) {
                    Column(Modifier.padding(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(date.format(DateTimeFormatter.ofPattern("MMM d")), style = MaterialTheme.typography.bodySmall)
                            note.mood?.let { Text(it.name.lowercase().replaceFirstChar { c -> c.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                        }
                        if (note.text.isNotBlank()) Text(note.text, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                if (habit.archived) {
                    OutlinedButton(onClick = { viewModel.restoreHabit(habit.id) }, modifier = Modifier.weight(1f).padding(end = 8.dp)) { Text("Restore") }
                    OutlinedButton(onClick = { showConfirmDelete = true }, modifier = Modifier.weight(1f)) { Text("Delete") }
                } else {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).padding(end = 8.dp)) { Text("Edit") }
                    OutlinedButton(onClick = { showPause = true }, modifier = Modifier.weight(1f).padding(end = 8.dp)) { Text("Pause") }
                    OutlinedButton(onClick = { showConfirmArchive = true }, modifier = Modifier.weight(1f)) { Text("Archive") }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(30.dp))
        }
    }

    if (showConfirmArchive) {
        ConfirmDialog(
            title = "Archive ${habit.name}?",
            body = "It'll move out of Today and Habits until you restore it.",
            confirmLabel = "Archive",
            onDismiss = { showConfirmArchive = false },
            onConfirm = { viewModel.archiveHabit(habit.id); showConfirmArchive = false; onBack() }
        )
    }
    if (showConfirmDelete) {
        ConfirmDialog(
            title = "Delete ${habit.name} permanently?",
            body = "This removes its full history. This can't be undone.",
            confirmLabel = "Delete",
            onDismiss = { showConfirmDelete = false },
            onConfirm = { viewModel.deletePermanently(habit.id); showConfirmDelete = false; onBack() }
        )
    }
    if (showPause) {
        PauseDialog(
            habitName = habit.name,
            onDismiss = { showPause = false },
            onConfirm = { until -> viewModel.pauseHabit(habit.id, until); showPause = false }
        )
    }
    valueDialogDate?.let { date ->
        val current = completions[date]?.takeIf { !it.isGrace }?.value ?: 0
        ValueDialog(
            habitName = habit.name, date = date, target = habit.target, unit = habit.unit,
            initialValue = current,
            onDismiss = { valueDialogDate = null },
            onSave = { v -> viewModel.setValue(habit.id, date, v); valueDialogDate = null },
            onClear = { viewModel.clearCompletion(habit.id, date); valueDialogDate = null }
        )
    }
    noteDialogDate?.let { date ->
        val existing = notes[date]
        NoteDialog(
            habitName = habit.name, date = date,
            initialMood = existing?.mood, initialText = existing?.text ?: "",
            onDismiss = { noteDialogDate = null },
            onSave = { mood, text -> viewModel.saveNote(habit.id, date, mood, text); noteDialogDate = null }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(end = 6.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
