package com.flamewidget.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.flamewidget.app.R
import com.flamewidget.app.data.TapAction
import com.flamewidget.app.data.WidgetConfig
import com.flamewidget.app.data.WidgetPreferences
import com.flamewidget.app.ui.MainActivity
import com.flamewidget.app.ui.WidgetConfigActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max

object WidgetUpdater {

    private val scope = CoroutineScope(Dispatchers.Default)

    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = WidgetPreferences(context)
        val config = prefs.loadWidgetConfig(appWidgetId)

        scope.launch {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val (widthPx, heightPx) = calculateWidgetSizePx(context, options)

            val views = RemoteViews(context.packageName, R.layout.widget_flame_layout)

            // Show loading indicator
            views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
            views.setViewVisibility(R.id.widget_error_container, View.GONE)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)

            val updatedViews = RemoteViews(context.packageName, R.layout.widget_flame_layout)
            try {
                val bitmap = BitmapProcessor.fetchAndProcessBitmap(context, config, widthPx, heightPx)

                updatedViews.setViewVisibility(R.id.widget_progress, View.GONE)
                updatedViews.setViewVisibility(R.id.widget_error_container, View.GONE)
                updatedViews.setViewVisibility(R.id.widget_image, View.VISIBLE)
                updatedViews.setImageViewBitmap(R.id.widget_image, bitmap)

                // Optional title badge
                if (config.showTitleBadge && config.title.isNotBlank()) {
                    updatedViews.setViewVisibility(R.id.widget_title_badge, View.VISIBLE)
                    updatedViews.setTextViewText(R.id.widget_title_badge, config.title)
                } else {
                    updatedViews.setViewVisibility(R.id.widget_title_badge, View.GONE)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbackBitmap = BitmapProcessor.createPlaceholderBitmap(context, config, widthPx, heightPx)
                updatedViews.setViewVisibility(R.id.widget_progress, View.GONE)
                updatedViews.setViewVisibility(R.id.widget_error_container, View.GONE)
                updatedViews.setViewVisibility(R.id.widget_image, View.VISIBLE)
                updatedViews.setImageViewBitmap(R.id.widget_image, fallbackBitmap)
            }

            // Setup Refresh Button
            if (config.showRefreshButton) {
                updatedViews.setViewVisibility(R.id.widget_btn_refresh, View.VISIBLE)
                val refreshIntent = Intent(context, FlameWidgetProvider::class.java).apply {
                    action = FlameWidgetProvider.ACTION_REFRESH_WIDGET
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 1,
                    refreshIntent,
                    flags
                )
                updatedViews.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)
            } else {
                updatedViews.setViewVisibility(R.id.widget_btn_refresh, View.GONE)
            }

            // Setup Main Tap Action
            val tapPendingIntent = createTapPendingIntent(context, appWidgetId, config)
            updatedViews.setOnClickPendingIntent(R.id.widget_root, tapPendingIntent)
            updatedViews.setOnClickPendingIntent(R.id.widget_error_container, tapPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
        }
    }

    private fun createTapPendingIntent(
        context: Context,
        appWidgetId: Int,
        config: WidgetConfig
    ): PendingIntent {
        val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return when (config.tapAction) {
            TapAction.REFRESH -> {
                val intent = Intent(context, FlameWidgetProvider::class.java).apply {
                    action = FlameWidgetProvider.ACTION_REFRESH_WIDGET
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                PendingIntent.getBroadcast(context, appWidgetId * 10 + 2, intent, piFlags)
            }
            TapAction.OPEN_EDITOR -> {
                val intent = Intent(context, WidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                PendingIntent.getActivity(context, appWidgetId * 10 + 3, intent, piFlags)
            }
            TapAction.OPEN_URL -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config.imageUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                PendingIntent.getActivity(context, appWidgetId * 10 + 4, intent, piFlags)
            }
            TapAction.VIEW_FULLSCREEN -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("OPEN_PREVIEW_URL", config.imageUrl)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                PendingIntent.getActivity(context, appWidgetId * 10 + 5, intent, piFlags)
            }
        }
    }

    private fun calculateWidgetSizePx(context: Context, options: Bundle?): Pair<Int, Int> {
        val density = context.resources.displayMetrics.density
        var minWidth = 150
        var minHeight = 150

        if (options != null) {
            val optW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val optH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
            if (optW > 0) minWidth = optW
            if (optH > 0) minHeight = optH
        }

        val pxW = max(100, (minWidth * density).toInt())
        val pxH = max(100, (minHeight * density).toInt())
        return Pair(pxW, pxH)
    }
}
