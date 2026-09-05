package com.richie.stride.ui.theme

import androidx.compose.ui.graphics.Color

// Light theme surfaces
val CreamBackground = Color(0xFFFBF7EF)
val CardWhite = Color(0xFFFFFFFF)
val TextDark = Color(0xFF26261F)
val TextSecondaryLight = Color(0xFF8D8875)
val BorderLight = Color(0xFFEFE9DA)

// Dark theme surfaces
val DarkBackground = Color(0xFF1C1A13)
val DarkCard = Color(0xFF26231A)
val TextLight = Color(0xFFF3EEE0)
val TextSecondaryDark = Color(0xFFA79E89)
val BorderDark = Color(0xFF37331F)

// Accent options (selectable in Settings)
enum class AccentOption(val color: Color, val tint: Color, val onTint: Color) {
    TEAL(Color(0xFF1EAE98), Color(0x261EAE98), Color(0xFF0E7A67)),
    CORAL(Color(0xFFFF6B4A), Color(0x26FF6B4A), Color(0xFFC64726)),
    AMBER(Color(0xFFFFB627), Color(0x33FFB627), Color(0xFF8A5A00)),
    PURPLE(Color(0xFF8C6FF7), Color(0x268C6FF7), Color(0xFF5B3FC4)),
    BLUE(Color(0xFF3B82F6), Color(0x263B82F6), Color(0xFF1D4ED8))
}

// Category colors (fixed, not user-selectable)
enum class CategoryColor(val color: Color, val tint: Color, val onTint: Color) {
    HEALTH(Color(0xFF1EAE98), Color(0x261EAE98), Color(0xFF0E7A67)),
    MIND(Color(0xFFFF6B4A), Color(0x26FF6B4A), Color(0xFFC64726)),
    FOCUS(Color(0xFFFFB627), Color(0x33FFB627), Color(0xFF8A5A00)),
    OTHER(Color(0xFF8C6FF7), Color(0x268C6FF7), Color(0xFF5B3FC4))
}

val DangerColor = Color(0xFFC64726)
