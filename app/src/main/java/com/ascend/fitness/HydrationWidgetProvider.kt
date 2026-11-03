package com.ascend.fitness

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class HydrationWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ADD_WATER) {
            val today = LocalDate.now().toString()
            runBlocking {
                context.ascendPreferences.edit { prefs ->
                    val key = intPreferencesKey("water_ml_$today")
                    prefs[key] = (prefs[key] ?: 0) + 250
                }
            }
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, HydrationWidgetProvider::class.java)).forEach { update(context, manager, it) }
        }
    }
    private fun update(context: Context, manager: AppWidgetManager, id: Int) {
        val today = LocalDate.now().toString()
        val values = runBlocking { context.ascendPreferences.data.first() }
        val amount = values[intPreferencesKey("water_ml_$today")] ?: 0
        val target = values[intPreferencesKey("water_target_ml")] ?: 2000
        val views = RemoteViews(context.packageName, R.layout.hydration_widget)
        views.setTextViewText(R.id.widget_water_amount, "$amount / $target ml")
        val addIntent = Intent(context, HydrationWidgetProvider::class.java).setAction(ACTION_ADD_WATER)
        views.setOnClickPendingIntent(R.id.widget_add_water, PendingIntent.getBroadcast(context, id, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { open ->
            views.setOnClickPendingIntent(R.id.widget_title, PendingIntent.getActivity(context, id+1000, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        manager.updateAppWidget(id, views)
    }
    companion object { const val ACTION_ADD_WATER = "com.ascend.fitness.ADD_WATER" }
}
