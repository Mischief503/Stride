package com.richie.stride.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DailyScoreCard(
    score: Int,
    doneCount: Int,
    total: Int,
    momentumDays: Int,
    subMessage: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Column {
                    Text(
                        "$score",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 32.sp)
                    )
                    Text(
                        "Daily Score",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                ProgressRing(doneCount = doneCount, total = total)
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text(
                    "$momentumDays day momentum",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            if (subMessage.isNotBlank()) {
                Text(
                    subMessage,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(doneCount: Int, total: Int) {
    val pct = if (total > 0) doneCount.toFloat() / total.toFloat() else 0f
    Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(50.dp)) {
            val strokeWidth = 6.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * pct,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text("$doneCount/$total", color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
