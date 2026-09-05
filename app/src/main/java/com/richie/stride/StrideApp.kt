package com.richie.stride

import android.app.Application
import com.richie.stride.data.AppDatabase
import com.richie.stride.data.HabitRepository
import com.richie.stride.data.SettingsRepository
import com.richie.stride.util.BackupManager

class StrideApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: HabitRepository by lazy { HabitRepository(database) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val backupManager: BackupManager by lazy { BackupManager(repository) }
}
