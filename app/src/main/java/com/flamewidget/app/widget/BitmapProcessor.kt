package com.flamewidget.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.flamewidget.app.data.BackgroundStyle
import com.flamewidget.app.data.ScaleMode
import com.flamewidget.app.data.WidgetConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

object BitmapProcessor {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    suspend fun fetchAndProcessBitmap(
        context: Context,
        config: WidgetConfig,
        targetWidthPx: Int,
        targetHeightPx: Int
    ): Bitmap = withContext(Dispatchers.IO) {
        val width = if (targetWidthPx > 0) targetWidthPx else 600
        val height = if (targetHeightPx > 0) targetHeightPx else 600

        var rawBitmap: Bitmap? = null
        if (config.imageUrl.isNotBlank()) {
            rawBitmap = downloadBitmap(config.imageUrl, config.bypassCache)
            if (rawBitmap != null) {
                saveCachedBitmap(context, config.id, rawBitmap)
            } else {
                rawBitmap = loadCachedBitmap(context, config.id)
            }
        } else {
            rawBitmap = loadCachedBitmap(context, config.id)
        }

        if (rawBitmap == null) {
            return@withContext createPlaceholderBitmap(context, config, width, height)
        }

        renderWidgetBitmap(context, rawBitmap, config, width, height)
    }

    fun createPlaceholderBitmap(
        context: Context,
        config: WidgetConfig,
        targetW: Int,
        targetH: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val cornerRadiusPx = config.cornerRadiusDp * density
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val bgColor = when (config.backgroundStyle) {
            BackgroundStyle.TRANSPARENT -> 0
            BackgroundStyle.DARK -> 0xFF141210.toInt()
            BackgroundStyle.SURFACE_MD3 -> 0xFF231E1B.toInt()
            BackgroundStyle.LIGHT -> 0xFFF7F2EC.toInt()
            BackgroundStyle.BLUR_ACCENT -> 0xFF2A231E.toInt()
        }
        if (config.backgroundStyle != BackgroundStyle.TRANSPARENT) {
            drawRoundedRectBackground(canvas, targetW, targetH, cornerRadiusPx, bgColor)
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
        val strokeRect = RectF(1f * density, 1f * density, targetW - 1f * density, targetH - 1f * density)
        canvas.drawRoundRect(strokeRect, cornerRadiusPx, cornerRadiusPx, strokePaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14 * density
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xAAFFFFFF.toInt()
            textSize = 11 * density
            textAlign = Paint.Align.CENTER
        }

        val centerY = targetH / 2f
        canvas.drawText("FlameWidget 🔥", targetW / 2f, centerY - 6 * density, titlePaint)
        canvas.drawText("Нажмите для настройки ссылки", targetW / 2f, centerY + 14 * density, subPaint)

        return output
    }

    private fun downloadBitmap(urlString: String, bypassCache: Boolean): Bitmap? {
        return try {
            val finalUrl = if (bypassCache) {
                val sep = if (urlString.contains("?")) "&" else "?"
                "$urlString${sep}_ts=${System.currentTimeMillis()}"
            } else {
                urlString
            }

            val request = Request.Builder()
                .url(finalUrl)
                .header("User-Agent", "FlameWidget/1.0 (Android)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun renderWidgetBitmap(
        context: Context,
        source: Bitmap,
        config: WidgetConfig,
        targetW: Int,
        targetH: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val paddingPx = (config.paddingDp * density).toInt()
        val cornerRadiusPx = config.cornerRadiusDp * density

        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Draw Background
        when (config.backgroundStyle) {
            BackgroundStyle.TRANSPARENT -> {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            }
            BackgroundStyle.DARK -> {
                drawRoundedRectBackground(canvas, targetW, targetH, cornerRadiusPx, 0xFF141210.toInt())
            }
            BackgroundStyle.SURFACE_MD3 -> {
                drawRoundedRectBackground(canvas, targetW, targetH, cornerRadiusPx, 0xFF231E1B.toInt())
            }
            BackgroundStyle.LIGHT -> {
                drawRoundedRectBackground(canvas, targetW, targetH, cornerRadiusPx, 0xFFF7F2EC.toInt())
            }
            BackgroundStyle.BLUR_ACCENT -> {
                val blurred = createFastBlur(source, 16)
                val bgRect = Rect(0, 0, targetW, targetH)
                val roundCanvas = Canvas(output)
                val path = android.graphics.Path().apply {
                    addRoundRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), cornerRadiusPx, cornerRadiusPx, android.graphics.Path.Direction.CW)
                }
                roundCanvas.save()
                roundCanvas.clipPath(path)
                roundCanvas.drawBitmap(blurred, null, bgRect, paint)
                roundCanvas.drawColor(0x55000000) // Dimming overlay
                roundCanvas.restore()
                blurred.recycle()
            }
        }

        // Available area for image content after applying padding
        val contentW = max(1, targetW - paddingPx * 2)
        val contentH = max(1, targetH - paddingPx * 2)

        val srcW = source.width.toFloat()
        val srcH = source.height.toFloat()

        when (config.scaleMode) {
            ScaleMode.FIT_CENTER -> {
                val scale = min(contentW / srcW, contentH / srcH)
                val drawW = (srcW * scale).toInt()
                val drawH = (srcH * scale).toInt()
                val left = paddingPx + (contentW - drawW) / 2
                val top = paddingPx + (contentH - drawH) / 2
                val dstRect = Rect(left, top, left + drawW, top + drawH)

                val contentRadius = max(0f, cornerRadiusPx - paddingPx)
                drawRoundedBitmap(canvas, source, dstRect, contentRadius, paint)
            }
            ScaleMode.CENTER_CROP -> {
                val scale = max(contentW / srcW, contentH / srcH)
                val scaledW = (srcW * scale).toInt()
                val scaledH = (srcH * scale).toInt()
                val cropLeft = (scaledW - contentW) / 2
                val cropTop = (scaledH - contentH) / 2

                val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)
                val cropped = Bitmap.createBitmap(scaled, cropLeft, cropTop, contentW, contentH)

                val dstRect = Rect(paddingPx, paddingPx, paddingPx + contentW, paddingPx + contentH)
                val contentRadius = max(0f, cornerRadiusPx - paddingPx)
                drawRoundedBitmap(canvas, cropped, dstRect, contentRadius, paint)

                if (scaled != source) scaled.recycle()
                cropped.recycle()
            }
            ScaleMode.BLUR_BACKGROUND -> {
                // Blurred background already filled or create it now
                if (config.backgroundStyle != BackgroundStyle.BLUR_ACCENT) {
                    val blurred = createFastBlur(source, 14)
                    val bgRect = Rect(0, 0, targetW, targetH)
                    val roundPath = android.graphics.Path().apply {
                        addRoundRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), cornerRadiusPx, cornerRadiusPx, android.graphics.Path.Direction.CW)
                    }
                    canvas.save()
                    canvas.clipPath(roundPath)
                    canvas.drawBitmap(blurred, null, bgRect, paint)
                    canvas.drawColor(0x66000000)
                    canvas.restore()
                    blurred.recycle()
                }

                // Crisp sharp image in the center
                val scale = min(contentW / srcW, contentH / srcH)
                val drawW = (srcW * scale).toInt()
                val drawH = (srcH * scale).toInt()
                val left = paddingPx + (contentW - drawW) / 2
                val top = paddingPx + (contentH - drawH) / 2
                val dstRect = Rect(left, top, left + drawW, top + drawH)
                val contentRadius = max(0f, cornerRadiusPx - paddingPx)
                drawRoundedBitmap(canvas, source, dstRect, contentRadius, paint)
            }
            ScaleMode.FIT_XY -> {
                val dstRect = Rect(paddingPx, paddingPx, paddingPx + contentW, paddingPx + contentH)
                val contentRadius = max(0f, cornerRadiusPx - paddingPx)
                drawRoundedBitmap(canvas, source, dstRect, contentRadius, paint)
            }
        }

        return output
    }

    private fun drawRoundedRectBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        radius: Float,
        color: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rectF, radius, radius, paint)
    }

    private fun drawRoundedBitmap(
        canvas: Canvas,
        source: Bitmap,
        dstRect: Rect,
        radius: Float,
        paint: Paint
    ) {
        val path = android.graphics.Path().apply {
            addRoundRect(
                RectF(dstRect),
                radius,
                radius,
                android.graphics.Path.Direction.CW
            )
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(source, null, dstRect, paint)
        canvas.restore()
    }

    private fun createFastBlur(sentBitmap: Bitmap, scaleRatio: Int): Bitmap {
        val width = max(1, sentBitmap.width / scaleRatio)
        val height = max(1, sentBitmap.height / scaleRatio)
        val small = Bitmap.createScaledBitmap(sentBitmap, width, height, true)
        return small
    }

    private fun saveCachedBitmap(context: Context, widgetId: Int, bitmap: Bitmap) {
        try {
            val dir = File(context.filesDir, "widgets").apply { if (!exists()) mkdirs() }
            val file = File(dir, "widget_${widgetId}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCachedBitmap(context: Context, widgetId: Int): Bitmap? {
        return try {
            val file = File(File(context.filesDir, "widgets"), "widget_${widgetId}.png")
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        } catch (_: Exception) {
            null
        }
    }
}
