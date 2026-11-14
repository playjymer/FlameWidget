package com.flamewidget.app.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.flamewidget.app.data.BackgroundStyle
import com.flamewidget.app.data.ScaleMode
import com.flamewidget.app.data.TapAction
import com.flamewidget.app.data.WidgetConfig
import com.flamewidget.app.data.WidgetPreferences
import com.flamewidget.app.ui.components.PreviewAspectRatio
import com.flamewidget.app.update.UpdateInfo
import com.flamewidget.app.update.UpdateManager
import com.flamewidget.app.update.UpdateState
import com.flamewidget.app.widget.FlameWideWidgetProvider
import com.flamewidget.app.widget.FlameWidgetProvider
import com.flamewidget.app.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(private val context: Context) : ViewModel() {

    private val prefs = WidgetPreferences(context)

    // Editing Target: null means "New Widget", Int means editing active widget with that appWidgetId
    private val _selectedWidgetId = MutableStateFlow<Int?>(null)
    val selectedWidgetId: StateFlow<Int?> = _selectedWidgetId.asStateFlow()

    private val _currentConfig = MutableStateFlow(WidgetConfig())
    val currentConfig: StateFlow<WidgetConfig> = _currentConfig.asStateFlow()

    private val _previewRatio = MutableStateFlow(PreviewAspectRatio.SQUARE_2X2)
    val previewRatio: StateFlow<PreviewAspectRatio> = _previewRatio.asStateFlow()

    private val _activeWidgets = MutableStateFlow<List<WidgetConfig>>(emptyList())
    val activeWidgets: StateFlow<List<WidgetConfig>> = _activeWidgets.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        loadActiveWidgets()
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            val info = UpdateManager.checkForUpdates(context)
            if (info != null && info.hasUpdate) {
                _updateState.value = UpdateState.Available(info)
            } else {
                _updateState.value = UpdateState.Idle
            }
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }

    fun downloadAndInstallUpdate(context: Context, info: UpdateInfo) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Downloading(0)
            val file = UpdateManager.downloadApk(
                context = context,
                downloadUrl = info.downloadUrl,
                onProgress = { percent ->
                    _updateState.value = UpdateState.Downloading(percent)
                }
            )
            if (file != null && file.exists()) {
                _updateState.value = UpdateState.ReadyToInstall(file)
                UpdateManager.installApk(context, file)
            } else {
                _updateState.value = UpdateState.Error("Не удалось скачать обновление. Попробуйте позже.")
            }
        }
    }

    fun loadActiveWidgets() {
        viewModelScope.launch {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val standardIds = appWidgetManager.getAppWidgetIds(ComponentName(context, FlameWidgetProvider::class.java))
            val wideIds = appWidgetManager.getAppWidgetIds(ComponentName(context, FlameWideWidgetProvider::class.java))
            val allIds = (standardIds.toList() + wideIds.toList()).distinct()

            val list = allIds.map { id ->
                val cfg = prefs.loadWidgetConfig(id)
                val isWideProvider = wideIds.contains(id)
                if (isWideProvider && !cfg.isWide) cfg.copy(isWide = true) else cfg
            }
            _activeWidgets.value = list
        }
    }

    fun selectWidgetToEdit(widgetId: Int?) {
        _selectedWidgetId.value = widgetId
        if (widgetId == null) {
            val draft = prefs.getLatestDraft()
            _currentConfig.value = draft
            _previewRatio.value = if (draft.isWide) PreviewAspectRatio.WIDE_4X2 else PreviewAspectRatio.SQUARE_2X2
        } else {
            val cfg = prefs.loadWidgetConfig(widgetId)
            _currentConfig.value = cfg
            _previewRatio.value = if (cfg.isWide) PreviewAspectRatio.WIDE_4X2 else PreviewAspectRatio.SQUARE_2X2
        }
    }

    fun updateConfig(newConfig: WidgetConfig) {
        _currentConfig.value = newConfig
    }

    fun setPreviewRatio(ratio: PreviewAspectRatio) {
        _previewRatio.value = ratio
        _currentConfig.value = _currentConfig.value.copy(
            isWide = (ratio == PreviewAspectRatio.WIDE_4X2)
        )
    }

    fun updateUrl(url: String) {
        val trimmed = url.trim()
        _currentConfig.value = _currentConfig.value.copy(imageUrl = trimmed)

        // Automatically detect if wide or square when URL is entered/pasted
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            viewModelScope.launch {
                detectImageDimensions(trimmed)
            }
        }
    }

    private suspend fun detectImageDimensions(url: String) = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val w = result.drawable.intrinsicWidth
                val h = result.drawable.intrinsicHeight
                if (w > 0 && h > 0) {
                    val ratio = w.toFloat() / h.toFloat()
                    val isWide = ratio >= 1.35f
                    withContext(Dispatchers.Main) {
                        _currentConfig.value = _currentConfig.value.copy(isWide = isWide)
                        _previewRatio.value = if (isWide) PreviewAspectRatio.WIDE_4X2 else PreviewAspectRatio.SQUARE_2X2
                    }
                }
            }
        } catch (_: Exception) {}
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

    fun saveOrPinWidget(context: Context, onSuccess: () -> Unit = {}) {
        val selectedId = _selectedWidgetId.value
        if (selectedId != null && selectedId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Save modifications strictly for this existing widget
            val updated = _currentConfig.value.copy(
                id = selectedId,
                lastUpdated = System.currentTimeMillis()
            )
            prefs.saveWidgetConfig(updated, updateDraft = false)

            val appWidgetManager = AppWidgetManager.getInstance(context)
            WidgetUpdater.updateWidget(context, appWidgetManager, selectedId)
            loadActiveWidgets()
            Toast.makeText(context, "Виджет #$selectedId успешно обновлен!", Toast.LENGTH_SHORT).show()
            onSuccess()
        } else {
            // Pin as new widget
            pinWidgetToHomeScreen(context, onSuccess)
        }
    }

    fun pinWidgetToHomeScreen(context: Context, onSuccess: () -> Unit) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val isWide = _currentConfig.value.isWide || _previewRatio.value == PreviewAspectRatio.WIDE_4X2
        val targetProvider = if (isWide) {
            ComponentName(context, FlameWideWidgetProvider::class.java)
        } else {
            ComponentName(context, FlameWidgetProvider::class.java)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                // Save current configuration as the latest draft
                val current = _currentConfig.value.copy(isWide = isWide)
                prefs.saveLatestDraft(current)

                val successCallbackIntent = Intent(context, targetProvider.javaClass).apply {
                    action = FlameWidgetProvider.ACTION_REFRESH_WIDGET
                    data = Uri.parse("flamewidget://pin/${System.currentTimeMillis()}")
                }

                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                val successCallback = PendingIntent.getBroadcast(
                    context,
                    9999,
                    successCallbackIntent,
                    flags
                )

                appWidgetManager.requestPinAppWidget(targetProvider, null, successCallback)
                val typeName = if (isWide) "широкий (4×2)" else "квадратный (2×2)"
                Toast.makeText(context, "Виджет $typeName добавляется на рабочий стол!", Toast.LENGTH_SHORT).show()
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
        loadActiveWidgets()
        Toast.makeText(context, "Виджет обновлен!", Toast.LENGTH_SHORT).show()
    }
}
