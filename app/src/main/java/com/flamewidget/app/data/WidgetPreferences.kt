package com.flamewidget.app.data

import android.content.Context
import android.content.SharedPreferences

class WidgetPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "com.flamewidget.app.PREFERENCES"
        private const val KEY_WIDGET_IDS = "widget_ids"
        private const val KEY_LATEST_DRAFT = "latest_draft"

        private const val PREFIX_URL = "url_"
        private const val PREFIX_TITLE = "title_"
        private const val PREFIX_SCALE_MODE = "scale_mode_"
        private const val PREFIX_BG_STYLE = "bg_style_"
        private const val PREFIX_CORNER_RADIUS = "corner_radius_"
        private const val PREFIX_PADDING = "padding_"
        private const val PREFIX_INTERVAL = "interval_"
        private const val PREFIX_TAP_ACTION = "tap_action_"
        private const val PREFIX_SHOW_REFRESH = "show_refresh_"
        private const val PREFIX_SHOW_BADGE = "show_badge_"
        private const val PREFIX_BYPASS_CACHE = "bypass_cache_"
        private const val PREFIX_LAST_UPDATED = "last_updated_"
    }

    fun saveLatestDraft(config: WidgetConfig) {
        val editor = prefs.edit()
        editor.putString(KEY_LATEST_DRAFT + "_url", config.imageUrl)
        editor.putString(KEY_LATEST_DRAFT + "_title", config.title)
        editor.putString(KEY_LATEST_DRAFT + "_scale_mode", config.scaleMode.name)
        editor.putString(KEY_LATEST_DRAFT + "_bg_style", config.backgroundStyle.name)
        editor.putInt(KEY_LATEST_DRAFT + "_corner_radius", config.cornerRadiusDp)
        editor.putInt(KEY_LATEST_DRAFT + "_padding", config.paddingDp)
        editor.putInt(KEY_LATEST_DRAFT + "_interval", config.refreshIntervalMinutes)
        editor.putString(KEY_LATEST_DRAFT + "_tap_action", config.tapAction.name)
        editor.putBoolean(KEY_LATEST_DRAFT + "_show_refresh", config.showRefreshButton)
        editor.putBoolean(KEY_LATEST_DRAFT + "_show_badge", config.showTitleBadge)
        editor.putBoolean(KEY_LATEST_DRAFT + "_bypass_cache", config.bypassCache)
        editor.apply()
    }

    fun getLatestDraft(): WidgetConfig {
        val default = WidgetConfig()
        val url = prefs.getString(KEY_LATEST_DRAFT + "_url", default.imageUrl) ?: default.imageUrl
        val title = prefs.getString(KEY_LATEST_DRAFT + "_title", default.title) ?: default.title
        val scaleMode = try {
            ScaleMode.valueOf(prefs.getString(KEY_LATEST_DRAFT + "_scale_mode", default.scaleMode.name) ?: default.scaleMode.name)
        } catch (_: Exception) { default.scaleMode }
        val bgStyle = try {
            BackgroundStyle.valueOf(prefs.getString(KEY_LATEST_DRAFT + "_bg_style", default.backgroundStyle.name) ?: default.backgroundStyle.name)
        } catch (_: Exception) { default.backgroundStyle }
        val tapAction = try {
            TapAction.valueOf(prefs.getString(KEY_LATEST_DRAFT + "_tap_action", default.tapAction.name) ?: default.tapAction.name)
        } catch (_: Exception) { default.tapAction }

        return default.copy(
            imageUrl = url,
            title = title,
            scaleMode = scaleMode,
            backgroundStyle = bgStyle,
            cornerRadiusDp = prefs.getInt(KEY_LATEST_DRAFT + "_corner_radius", default.cornerRadiusDp),
            paddingDp = prefs.getInt(KEY_LATEST_DRAFT + "_padding", default.paddingDp),
            refreshIntervalMinutes = prefs.getInt(KEY_LATEST_DRAFT + "_interval", default.refreshIntervalMinutes),
            tapAction = tapAction,
            showRefreshButton = prefs.getBoolean(KEY_LATEST_DRAFT + "_show_refresh", default.showRefreshButton),
            showTitleBadge = prefs.getBoolean(KEY_LATEST_DRAFT + "_show_badge", default.showTitleBadge),
            bypassCache = prefs.getBoolean(KEY_LATEST_DRAFT + "_bypass_cache", default.bypassCache)
        )
    }

    fun saveWidgetConfig(config: WidgetConfig) {
        val editor = prefs.edit()
        val id = config.id

        editor.putString(PREFIX_URL + id, config.imageUrl)
        editor.putString(PREFIX_TITLE + id, config.title)
        editor.putString(PREFIX_SCALE_MODE + id, config.scaleMode.name)
        editor.putString(PREFIX_BG_STYLE + id, config.backgroundStyle.name)
        editor.putInt(PREFIX_CORNER_RADIUS + id, config.cornerRadiusDp)
        editor.putInt(PREFIX_PADDING + id, config.paddingDp)
        editor.putInt(PREFIX_INTERVAL + id, config.refreshIntervalMinutes)
        editor.putString(PREFIX_TAP_ACTION + id, config.tapAction.name)
        editor.putBoolean(PREFIX_SHOW_REFRESH + id, config.showRefreshButton)
        editor.putBoolean(PREFIX_SHOW_BADGE + id, config.showTitleBadge)
        editor.putBoolean(PREFIX_BYPASS_CACHE + id, config.bypassCache)
        editor.putLong(PREFIX_LAST_UPDATED + id, config.lastUpdated)

        val currentIds = getActiveWidgetIds().toMutableSet()
        if (id > 0) {
            currentIds.add(id.toString())
            editor.putStringSet(KEY_WIDGET_IDS, currentIds)
        }

        editor.apply()
        saveLatestDraft(config)
    }

    fun loadWidgetConfig(appWidgetId: Int): WidgetConfig {
        val id = appWidgetId
        val draft = getLatestDraft().copy(id = id)

        val url = prefs.getString(PREFIX_URL + id, draft.imageUrl) ?: draft.imageUrl
        val title = prefs.getString(PREFIX_TITLE + id, draft.title) ?: draft.title

        val scaleMode = try {
            ScaleMode.valueOf(prefs.getString(PREFIX_SCALE_MODE + id, draft.scaleMode.name) ?: draft.scaleMode.name)
        } catch (_: Exception) { draft.scaleMode }

        val bgStyle = try {
            BackgroundStyle.valueOf(prefs.getString(PREFIX_BG_STYLE + id, draft.backgroundStyle.name) ?: draft.backgroundStyle.name)
        } catch (_: Exception) { draft.backgroundStyle }

        val tapAction = try {
            TapAction.valueOf(prefs.getString(PREFIX_TAP_ACTION + id, draft.tapAction.name) ?: draft.tapAction.name)
        } catch (_: Exception) { draft.tapAction }

        val cornerRadius = prefs.getInt(PREFIX_CORNER_RADIUS + id, draft.cornerRadiusDp)
        val padding = prefs.getInt(PREFIX_PADDING + id, draft.paddingDp)
        val interval = prefs.getInt(PREFIX_INTERVAL + id, draft.refreshIntervalMinutes)
        val showRefresh = prefs.getBoolean(PREFIX_SHOW_REFRESH + id, draft.showRefreshButton)
        val showBadge = prefs.getBoolean(PREFIX_SHOW_BADGE + id, draft.showTitleBadge)
        val bypassCache = prefs.getBoolean(PREFIX_BYPASS_CACHE + id, draft.bypassCache)
        val lastUpdated = prefs.getLong(PREFIX_LAST_UPDATED + id, draft.lastUpdated)

        return WidgetConfig(
            id = id,
            title = title,
            imageUrl = url,
            scaleMode = scaleMode,
            backgroundStyle = bgStyle,
            cornerRadiusDp = cornerRadius,
            paddingDp = padding,
            refreshIntervalMinutes = interval,
            tapAction = tapAction,
            showRefreshButton = showRefresh,
            showTitleBadge = showBadge,
            bypassCache = bypassCache,
            lastUpdated = lastUpdated
        )
    }

    fun removeWidgetConfig(appWidgetId: Int) {
        val editor = prefs.edit()
        val id = appWidgetId
        editor.remove(PREFIX_URL + id)
        editor.remove(PREFIX_TITLE + id)
        editor.remove(PREFIX_SCALE_MODE + id)
        editor.remove(PREFIX_BG_STYLE + id)
        editor.remove(PREFIX_CORNER_RADIUS + id)
        editor.remove(PREFIX_PADDING + id)
        editor.remove(PREFIX_INTERVAL + id)
        editor.remove(PREFIX_TAP_ACTION + id)
        editor.remove(PREFIX_SHOW_REFRESH + id)
        editor.remove(PREFIX_SHOW_BADGE + id)
        editor.remove(PREFIX_BYPASS_CACHE + id)
        editor.remove(PREFIX_LAST_UPDATED + id)

        val currentIds = getActiveWidgetIds().toMutableSet()
        currentIds.remove(id.toString())
        editor.putStringSet(KEY_WIDGET_IDS, currentIds)

        editor.apply()
    }

    fun getActiveWidgetIds(): Set<String> {
        return prefs.getStringSet(KEY_WIDGET_IDS, emptySet()) ?: emptySet()
    }

    fun getAllConfigs(): List<WidgetConfig> {
        val ids = getActiveWidgetIds()
        return ids.mapNotNull {
            val id = it.toIntOrNull()
            if (id != null) loadWidgetConfig(id) else null
        }
    }
}
