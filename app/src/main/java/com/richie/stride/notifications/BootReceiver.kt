package com.richie.stride.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.richie.stride.StrideApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? StrideApp ?: return
        val appContext = context.applicationContext

        // Re-arm every habit's reminder alarm; a reboot wipes AlarmManager's pending alarms.
        GlobalScope.launch {
            val habits = app.repository.habits.first()
            habits.filter { !it.archived && it.reminderTime != null }.forEach { habit ->
                val time = habit.reminderTime!!
                ReminderScheduler.schedule(appContext, habit.id, habit.name, time.hour, time.minute)
            }
        }
    }
}
