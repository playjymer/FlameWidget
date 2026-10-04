package com.flamewidget.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.flamewidget.app.data.WidgetPreferences

class FlameWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.flamewidget.app.ACTION_REFRESH_WIDGET"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = WidgetPreferences(context)
        val latestDraft = prefs.getLatestDraft()

        for (appWidgetId in appWidgetIds) {
            val existing = prefs.loadWidgetConfig(appWidgetId)
            // If new widget has empty URL, inherit the latest draft created in app
            if (existing.imageUrl.isBlank() && latestDraft.imageUrl.isNotBlank()) {
                prefs.saveWidgetConfig(latestDraft.copy(id = appWidgetId))
            }
            WidgetUpdater.updateWidget(context, appWidgetManager, appWidgetId)
        }
        WidgetRefreshWorker.schedulePeriodicUpdates(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // Automatically re-render when the user resizes the widget on the home screen
        WidgetUpdater.updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                WidgetUpdater.updateWidget(context, appWidgetManager, appWidgetId)
                Toast.makeText(context, "Обновление виджета...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = WidgetPreferences(context)
        for (appWidgetId in appWidgetIds) {
            prefs.removeWidgetConfig(appWidgetId)
        }
        super.onDeleted(context, appWidgetIds)
    }
}
