package com.richie.stride.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.richie.stride.data.DayStatus
import com.richie.stride.data.StatsCalculator

/**
 * A small, fixed-size (35 cells for a 5-week heatmap) grid - deliberately NOT built with
 * LazyVerticalGrid, since this is always nested inside a LazyColumn item {} elsewhere
 * (DetailScreen, InsightsScreen). Nesting one lazy scrollable inside another throws
 * "Vertically scrollable component was measured with an infinity maximum height constraints"
 * at runtime - a real crash, not just a lint warning - so a plain Column-of-Rows is used
 * instead. There's no virtualization benefit to lose at this size anyway.
 */
@Composable
fun HeatmapGrid(
    cells: List<StatsCalculator.HeatmapCell>,
    doneColor: Color,
    onCellClick: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val graceColor = MaterialTheme.colorScheme.tertiary
    val emptyColor = MaterialTheme.colorScheme.outline
    val notDueColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier.fillMaxWidth()) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    val color = when (cell.status) {
                        DayStatus.DONE -> doneColor
                        DayStatus.GRACE -> graceColor.copy(alpha = 0.5f)
                        DayStatus.NOT_DUE, DayStatus.FUTURE -> notDueColor
                        DayStatus.NONE -> emptyColor
                    }
                    val clickable = cell.status != DayStatus.FUTURE && cell.status != DayStatus.NOT_DUE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(1.5.dp)
                            .background(color, RoundedCornerShape(4.dp))
                            .then(if (clickable) Modifier.clickable { onCellClick(cell.date) } else Modifier)
                    ) {}
                }
            }
        }
    }
}
