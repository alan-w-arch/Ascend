package com.ascend.fitness

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class HydrationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val enabled = runBlocking {
            context.ascendPreferences.data.first()[booleanPreferencesKey("hydration_reminders_enabled")] ?: false
        }
        if (!enabled) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel("hydration", "Hydration reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = NotificationCompat.Builder(context, "hydration")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time for a water break")
            .setContentText("Take a sip and log it in Ascend when you’re ready.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(410, notification) }
    }
}
