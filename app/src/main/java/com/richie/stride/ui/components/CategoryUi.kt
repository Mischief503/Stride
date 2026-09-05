package com.richie.stride.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.richie.stride.data.Category
import com.richie.stride.ui.theme.CategoryColor

fun Category.colorSet(): CategoryColor = when (this) {
    Category.HEALTH -> CategoryColor.HEALTH
    Category.MIND -> CategoryColor.MIND
    Category.FOCUS -> CategoryColor.FOCUS
    Category.OTHER -> CategoryColor.OTHER
}

fun Category.icon(): ImageVector = when (this) {
    Category.HEALTH -> Icons.Filled.FavoriteBorder
    Category.MIND -> Icons.Filled.Psychology
    Category.FOCUS -> Icons.Filled.CenterFocusStrong
    Category.OTHER -> Icons.Filled.Star
}

@Composable
fun CategorySwatch(category: Category, size: androidx.compose.ui.unit.Dp = 34.dp, modifier: Modifier = Modifier) {
    val colors = category.colorSet()
    Box(
        modifier = modifier
            .size(size)
            .background(colors.tint, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(category.icon(), contentDescription = null, tint = colors.color)
    }
}
