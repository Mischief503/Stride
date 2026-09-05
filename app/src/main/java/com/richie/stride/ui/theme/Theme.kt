package com.richie.stride.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun StrideTheme(
    accent: AccentOption = AccentOption.TEAL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent.color,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            secondaryContainer = accent.tint,
            onSecondaryContainer = accent.onTint,
            background = DarkBackground,
            surface = DarkCard,
            onBackground = TextLight,
            onSurface = TextLight,
            outline = BorderDark,
            error = DangerColor
        )
    } else {
        lightColorScheme(
            primary = accent.color,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            secondaryContainer = accent.tint,
            onSecondaryContainer = accent.onTint,
            background = CreamBackground,
            surface = CardWhite,
            onBackground = TextDark,
            onSurface = TextDark,
            outline = BorderLight,
            error = DangerColor
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
