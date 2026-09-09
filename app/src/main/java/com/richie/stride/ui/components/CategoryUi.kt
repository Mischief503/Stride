package com.richie.stride.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.richie.stride.data.Category
import com.richie.stride.ui.theme.CategoryColor

fun Category.colorSet(): CategoryColor = when (this) {
    Category.MIND -> CategoryColor.MIND
    Category.EXERCISE -> CategoryColor.EXERCISE
    Category.FOOD -> CategoryColor.FOOD
    Category.SELF_CARE -> CategoryColor.SELF_CARE
}

fun Category.icon(): ImageVector = when (this) {
    Category.MIND -> Icons.Filled.Psychology
    Category.EXERCISE -> Icons.Filled.FitnessCenter
    Category.FOOD -> Icons.Filled.Restaurant
    Category.SELF_CARE -> Icons.Filled.Spa
}

fun Category.label(): String = when (this) {
    Category.MIND -> "Mind"
    Category.EXERCISE -> "Exercise"
    Category.FOOD -> "Food"
    Category.SELF_CARE -> "Self-care"
}

@Composable
fun CategorySwatch(
    category: Category,
    size: androidx.compose.ui.unit.Dp = 34.dp,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = category.colorSet()
    Box(
        modifier = modifier
            .size(size)
            .background(colors.tint, RoundedCornerShape(10.dp))
            .then(
                if (selected) Modifier.border(2.dp, colors.color, RoundedCornerShape(10.dp)) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(category.icon(), contentDescription = category.label(), tint = colors.color)
    }
}
