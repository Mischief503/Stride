package com.richie.stride.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.richie.stride.ui.components.HeatmapGrid
import com.richie.stride.ui.components.colorSet
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val today = remember { LocalDate.now() }
    val weekStart = state.settings.weekStart
    val habits = state.habits.filter { !it.archived }

    var scopeAll by remember { mutableStateOf(true) }
    var selectedHabitId by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    if (habits.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(32.dp)) {
            Text("No habits yet", style = MaterialTheme.typography.titleMedium)
            Text("Add a habit on the Today tab to start seeing patterns.", modifier = Modifier.padding(top = 4.dp))
        }
        return
    }
    val selectedHabit = habits.find { it.id == selectedHabitId } ?: habits.first()

    LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        item {
            Row(Modifier.padding(vertical = 12.dp)) {
                FilterChip(selected = scopeAll, onClick = { scopeAll = true }, label = { Text("All habits") }, modifier = Modifier.padding(end = 8.dp))
                Box {
                    OutlinedButton(onClick = { scopeAll = false; menuOpen = true }) { Text(selectedHabit.name) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        habits.forEach { h ->
                            DropdownMenuItem(text = { Text(h.name) }, onClick = { selectedHabitId = h.id; scopeAll = false; menuOpen = false })
                        }
                    }
                }
            }
        }

        if (scopeAll) {
            val rated = habits.mapNotNull { h ->
                StatsCalculator.completionRate(h, state.completionsByHabit[h.id] ?: emptyMap(), 30, today)?.let { h to it }
            }
            val totalLogged = habits.sumOf { (state.completionsByHabit[it.id] ?: emptyMap()).size }
            if (rated.isEmpty() || totalLogged < habits.size) {
                item { LowDataCard(totalLogged.coerceAtMost(7), 7) }
            } else {
                val overall = rated.sumOf { it.second } / rated.size
                item {
                    Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Weekly consistency", style = MaterialTheme.typography.bodySmall)
                            Text("$overall%", style = MaterialTheme.typography.headlineSmall)
                            Text("across ${habits.size} habits", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                val best = rated.maxByOrNull { it.second }
                val worst = rated.minByOrNull { it.second }
                best?.let { (h, r) ->
                    item {
                        Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text("${h.name} has your strongest completion rate at $r%.", modifier = Modifier.padding(12.dp))
                        }
                    }
                }
                if (worst != null && best != null && worst.first.id != best.first.id) {
                    item {
                        Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text("${worst.first.name} could use some attention \u2014 ${worst.second}%.", modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        } else {
            val completions = state.completionsByHabit[selectedHabit.id] ?: emptyMap()
            if (completions.size < 5) {
                item { LowDataCard(completions.size, 7) }
            } else {
                val rate = StatsCalculator.completionRate(selectedHabit, completions, 30, today)
                val cells = StatsCalculator.heatmapData(selectedHabit, completions, 5, weekStart, today)
                val dow = StatsCalculator.dayOfWeekStats(selectedHabit, completions, today)
                val order = StatsCalculator.weekDayOrder(weekStart)
                val best = order.maxByOrNull { dow[it] ?: 0 }

                item {
                    Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Completion rate", style = MaterialTheme.typography.bodySmall)
                            Text(rate?.let { "$it%" } ?: "\u2014", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    Text("Last 5 weeks", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
                    HeatmapGrid(cells = cells, doneColor = selectedHabit.category.colorSet().color, onCellClick = {}, modifier = Modifier.padding(bottom = 16.dp))
                    Text("By day of week", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
                    Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly) {
                        order.forEach { day ->
                            val pct = dow[day] ?: 0
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Box(
                                    Modifier
                                        .width(18.dp)
                                        .height((pct.coerceAtLeast(4) * 0.5).dp)
                                        .background(
                                            if (day == best) androidx.compose.ui.graphics.Color(0xFFFFB627) else selectedHabit.category.colorSet().color,
                                            RoundedCornerShape(4.dp)
                                        )
                                ) {}
                                Text(day.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    best?.let {
                        Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Text(
                                "${it.getDisplayName(TextStyle.FULL, Locale.getDefault())} is your strongest day for this habit.",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(40.dp)) }
    }
}

@Composable
private fun LowDataCard(have: Int, need: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        Text("Patterns take a few days", style = MaterialTheme.typography.titleMedium)
        Text("Check in daily and insights unlock automatically.", modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                    Text("Days logged", style = MaterialTheme.typography.labelSmall)
                    Text("$have / $need", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
