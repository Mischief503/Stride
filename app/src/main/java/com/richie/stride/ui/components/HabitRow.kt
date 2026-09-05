package com.richie.stride.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.richie.stride.data.Completion
import com.richie.stride.data.GoalType
import com.richie.stride.data.Habit
import com.richie.stride.data.StatsCalculator

@Composable
fun HabitRow(
    habit: Habit,
    completion: Completion?,
    streak: Int,
    onToggle: () -> Unit,
    onStep: (Int) -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val done = StatsCalculator.isDoneOn(habit, completion)
    val grace = StatsCalculator.isGraceOn(completion)
    val colors = habit.category.colorSet()

    if (habit.goalType != GoalType.YES_NO && !done && !grace) {
        val current = completion?.takeIf { !it.isGrace }?.value ?: 0
        val pct = (current.toFloat() / habit.target.toFloat()).coerceIn(0f, 1f)
        Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(habit.name, style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = onMenu) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onStep(-StatsCalculator.stepFor(habit)) }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                    }
                    Column(Modifier.weight(1f)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(pct)
                                    .height(8.dp)
                                    .background(colors.color, RoundedCornerShape(4.dp))
                            ) {}
                        }
                        val unit = if (habit.goalType == GoalType.DURATION) "min" else habit.unit
                        Text(
                            "$current / ${habit.target} $unit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onStep(StatsCalculator.stepFor(habit)) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase")
                    }
                }
            }
        }
        return
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clickable(onClick = onToggle)
                    .background(
                        if (done) colors.color else Color.Transparent,
                        CircleShape
                    )
                    .border(
                        2.5.dp,
                        if (grace) colors.color else MaterialTheme.colorScheme.outline,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (done) Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White)
            }
            Text(
                habit.name,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            when {
                done -> Pill(text = "$streak", icon = Icons.Filled.LocalFireDepartment, tint = colors.color, bg = colors.tint)
                grace -> Pill(text = "grace used", icon = Icons.Filled.Shield, tint = MaterialTheme.colorScheme.tertiary, bg = MaterialTheme.colorScheme.tertiaryContainer)
                streak > 0 -> Pill(text = "due", icon = Icons.Filled.Schedule, tint = Color(0xFF8A5A00), bg = Color(0x33FFB627))
            }
            IconButton(onClick = onMenu) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
        }
    }
}

@Composable
private fun Pill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, bg: Color) {
    Row(
        Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(text, color = tint, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 3.dp))
    }
}
