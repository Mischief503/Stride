package com.richie.stride.ui.routine

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.richie.stride.data.Routine
import com.richie.stride.data.TimeLabel
import com.richie.stride.ui.MainViewModel

@Composable
fun RoutineEditScreen(
    viewModel: MainViewModel,
    routineId: String?,
    onDone: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val existing = routineId?.let { id -> state.routines.find { it.id == id } }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var timeLabel by remember(existing) { mutableStateOf(existing?.timeLabel ?: TimeLabel.MORNING) }
    var selectedHabitIds by remember(existing) { mutableStateOf(existing?.habitIds?.toSet() ?: emptySet()) }
    val activeHabits = state.habits.filter { !it.archived }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Routine name") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Time of day", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        Row {
            TimeLabel.entries.forEach { label ->
                FilterChip(
                    selected = timeLabel == label,
                    onClick = { timeLabel = label },
                    label = { Text(label.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
        Text("Habits in this routine", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        activeHabits.forEach { habit ->
            FilterChip(
                selected = selectedHabitIds.contains(habit.id),
                onClick = {
                    selectedHabitIds = if (selectedHabitIds.contains(habit.id)) selectedHabitIds - habit.id else selectedHabitIds + habit.id
                },
                label = { Text(habit.name) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            )
        }

        Button(
            onClick = {
                val routine = Routine(
                    id = existing?.id ?: ("r" + System.currentTimeMillis()),
                    name = name.trim().ifBlank { "Routine" },
                    timeLabel = timeLabel,
                    sortOrder = existing?.sortOrder ?: 0,
                    habitIds = selectedHabitIds.toList()
                )
                viewModel.saveRoutine(routine)
                onDone()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) { Text("Save routine") }

        if (existing != null) {
            OutlinedButton(
                onClick = { viewModel.deleteRoutine(existing.id); onDone() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("Delete routine") }
        }
    }
}
