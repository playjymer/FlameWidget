package com.flamewidget.app.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, FlameWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(provider)
            if (ids.isNotEmpty()) {
                for (id in ids) {
                    WidgetUpdater.updateWidget(context, appWidgetManager, id)
                }
                WidgetRefreshWorker.schedulePeriodicUpdates(context)
            }
        }
    }
}
