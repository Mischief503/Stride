package com.richie.stride.ui.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.richie.stride.data.StatsCalculator
import com.richie.stride.ui.MainViewModel
import com.richie.stride.ui.components.CategorySwatch
import com.richie.stride.ui.components.ConfirmDialog
import com.richie.stride.ui.components.colorSet
import java.time.LocalDate

@Composable
fun HabitsScreen(
    viewModel: MainViewModel,
    onOpenDetail: (String) -> Unit,
    onNewRoutine: () -> Unit,
    onEditRoutine: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val today = remember { LocalDate.now() }

    val active = state.habits.filter { !it.archived && it.name.contains(query, ignoreCase = true) }
    val archived = state.habits.filter { it.archived }

    LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search habits") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)
            )
        }

        if (active.isEmpty()) {
            item {
                Text(
                    if (query.isNotBlank()) "No habits match your search." else "No habits yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(active) { habit ->
                val completions = state.completionsByHabit[habit.id] ?: emptyMap()
                val streak = StatsCalculator.computeStreak(habit, completions, today)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDetail(habit.id) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategorySwatch(habit.category)
                            Text(habit.name, modifier = Modifier.padding(start = 10.dp))
                        }
                        if (habit.pausedUntil != null) {
                            Text("Paused", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = habit.category.colorSet().color)
                                Text("$streak", modifier = Modifier.padding(start = 3.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Routines", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        }
        if (state.routines.isEmpty()) {
            item { Text("No routines yet.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp)) }
        } else {
            items(state.routines) { routine ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { onEditRoutine(routine.id) }
                ) {
                    Text(
                        "${routine.name} \u2014 ${routine.habitIds.size} habits",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        item {
            OutlinedButton(onClick = onNewRoutine, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Text("New routine")
            }
        }

        if (archived.isNotEmpty()) {
            item {
                Text("Archived (${archived.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(archived) { habit ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(habit.name)
                        Row {
                            IconButton(onClick = { viewModel.restoreHabit(habit.id) }) {
                                Icon(Icons.Filled.Restore, contentDescription = "Restore")
                            }
                            IconButton(onClick = { pendingDelete = habit.id }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(40.dp)) }
    }

    pendingDelete?.let { id ->
        val habit = state.habits.find { it.id == id }
        ConfirmDialog(
            title = "Delete ${habit?.name ?: ""} permanently?",
            body = "This removes its full history. This can't be undone.",
            confirmLabel = "Delete",
            onDismiss = { pendingDelete = null },
            onConfirm = { viewModel.deletePermanently(id); pendingDelete = null }
        )
    }
}
