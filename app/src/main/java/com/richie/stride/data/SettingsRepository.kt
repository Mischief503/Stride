package com.richie.stride.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.richie.stride.ui.theme.AccentOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentOption = AccentOption.TEAL,
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    val reduceMotion: Boolean = false,
    val sound: Boolean = true,
    val onboarded: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val WEEK_START = intPreferencesKey("week_start") // ISO value 1-7
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val SOUND = booleanPreferencesKey("sound")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.ENGLISH,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            accent = prefs[Keys.ACCENT]?.let { runCatching { AccentOption.valueOf(it) }.getOrNull() }
                ?: AccentOption.TEAL,
            weekStart = prefs[Keys.WEEK_START]?.let { v -> DayOfWeek.entries.find { it.value == v } }
                ?: DayOfWeek.MONDAY,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            sound = prefs[Keys.SOUND] ?: true,
            onboarded = prefs[Keys.ONBOARDED] ?: false
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccent(accent: AccentOption) {
        context.dataStore.edit { it[Keys.ACCENT] = accent.name }
    }

    suspend fun setWeekStart(day: DayOfWeek) {
        context.dataStore.edit { it[Keys.WEEK_START] = day.value }
    }

    suspend fun setReduceMotion(value: Boolean) {
        context.dataStore.edit { it[Keys.REDUCE_MOTION] = value }
    }

    suspend fun setSound(value: Boolean) {
        context.dataStore.edit { it[Keys.SOUND] = value }
    }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDED] = value }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
