package com.flamewidget.app.data

import android.appwidget.AppWidgetManager

enum class ScaleMode(val title: String, val subtitle: String) {
    FIT_CENTER("По размеру (Fit)", "Сохраняет пропорции 1:1, идеален для стриков и карточек"),
    CENTER_CROP("Заполнение (Crop)", "Заполняет весь виджет целиком, обрезая края"),
    BLUR_BACKGROUND("Размытый фон", "Оригинал по центру + атмосферный размытый фон по краям"),
    FIT_XY("Растянуть (FitXY)", "Растягивает картинку на весь размер ячейки")
}

enum class BackgroundStyle(val title: String, val colorHex: Long) {
    TRANSPARENT("Прозрачный", 0x00000000),
    DARK("Тёмный (AMOLED)", 0xFF141210),
    SURFACE_MD3("Поверхность MD3", 0xFF2A2420),
    LIGHT("Светлый", 0xFFF5F0EB),
    BLUR_ACCENT("Атмосферное размытие", 0x80000000)
}

enum class TapAction(val title: String, val subtitle: String) {
    REFRESH("Обновить картинку", "Мгновенно перезагружает виджет с сервера"),
    OPEN_EDITOR("Открыть редактор", "Изменить ссылку, размер или оформление"),
    OPEN_URL("Открыть в браузере", "Перейти по ссылке виджета"),
    VIEW_FULLSCREEN("Просмотр на весь экран", "Открыть полноразмерное изображение")
}

data class WidgetConfig(
    val id: Int = AppWidgetManager.INVALID_APPWIDGET_ID,
    val title: String = "",
    val imageUrl: String = "",
    val scaleMode: ScaleMode = ScaleMode.FIT_CENTER,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.TRANSPARENT,
    val cornerRadiusDp: Int = 24, // 0..36 dp
    val paddingDp: Int = 4, // 0..20 dp
    val refreshIntervalMinutes: Int = 30, // 0..1440 min
    val tapAction: TapAction = TapAction.REFRESH,
    val showRefreshButton: Boolean = true,
    val showTitleBadge: Boolean = false,
    val bypassCache: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)
