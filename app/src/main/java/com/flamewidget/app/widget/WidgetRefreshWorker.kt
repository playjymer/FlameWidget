package com.flamewidget.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WidgetRefreshWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val providerComponent = ComponentName(appContext, FlameWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(providerComponent)

        if (appWidgetIds.isNotEmpty()) {
            for (appWidgetId in appWidgetIds) {
                WidgetUpdater.updateWidget(appContext, appWidgetManager, appWidgetId)
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "flame_widget_periodic_refresh"

        fun schedulePeriodicUpdates(context: Context, intervalMinutes: Long = 30) {
            val validInterval = if (intervalMinutes < 15) 15L else intervalMinutes

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
                validInterval,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
