package com.flamewidget.app.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flamewidget.app.data.BackgroundStyle
import com.flamewidget.app.data.ScaleMode
import com.flamewidget.app.data.WidgetConfig
import com.flamewidget.app.data.WidgetPreferences
import com.flamewidget.app.widget.FlameWidgetProvider
import com.flamewidget.app.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val context: Context) : ViewModel() {

    private val prefs = WidgetPreferences(context)

    private val _currentConfig = MutableStateFlow(WidgetConfig())
    val currentConfig: StateFlow<WidgetConfig> = _currentConfig.asStateFlow()

    private val _activeWidgets = MutableStateFlow<List<WidgetConfig>>(emptyList())
    val activeWidgets: StateFlow<List<WidgetConfig>> = _activeWidgets.asStateFlow()

    init {
        loadActiveWidgets()
    }

    fun loadActiveWidgets() {
        viewModelScope.launch {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val providerComponent = ComponentName(context, FlameWidgetProvider::class.java)
            val activeIds = appWidgetManager.getAppWidgetIds(providerComponent)

            val list = activeIds.map { id ->
                prefs.loadWidgetConfig(id)
            }
            _activeWidgets.value = list
        }
    }

    fun updateConfig(newConfig: WidgetConfig) {
        _currentConfig.value = newConfig
    }

    fun updateUrl(url: String) {
        _currentConfig.value = _currentConfig.value.copy(imageUrl = url)
    }

    fun updateTitle(title: String) {
        _currentConfig.value = _currentConfig.value.copy(title = title)
    }

    fun updateScaleMode(mode: ScaleMode) {
        _currentConfig.value = _currentConfig.value.copy(scaleMode = mode)
    }

    fun updateCornerRadius(radiusDp: Int) {
        _currentConfig.value = _currentConfig.value.copy(cornerRadiusDp = radiusDp)
    }

    fun updatePadding(paddingDp: Int) {
        _currentConfig.value = _currentConfig.value.copy(paddingDp = paddingDp)
    }

    fun updateBackgroundStyle(style: BackgroundStyle) {
        _currentConfig.value = _currentConfig.value.copy(backgroundStyle = style)
    }

    fun pinWidgetToHomeScreen(context: Context, onSuccess: () -> Unit) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, FlameWidgetProvider::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                // Save current configuration as the latest draft
                val current = _currentConfig.value
                prefs.saveLatestDraft(current)

                val successCallbackIntent = Intent(context, FlameWidgetProvider::class.java).apply {
                    action = FlameWidgetProvider.ACTION_REFRESH_WIDGET
                }

                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                val successCallback = PendingIntent.getBroadcast(
                    context,
                    9999,
                    successCallbackIntent,
                    flags
                )

                appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                Toast.makeText(context, "Виджет добавляется на рабочий стол!", Toast.LENGTH_SHORT).show()
                onSuccess()
            } else {
                Toast.makeText(context, "Лаунчер не поддерживает быстрое закрепление. Добавьте через меню виджетов.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Добавьте виджет через долгое нажатие на рабочем столе.", Toast.LENGTH_LONG).show()
        }
    }

    fun refreshWidget(widgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        WidgetUpdater.updateWidget(context, appWidgetManager, widgetId)
        Toast.makeText(context, "Виджет обновлен!", Toast.LENGTH_SHORT).show()
    }
}
