package com.flamewidget.app.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val title: String,
    val notes: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data class Downloading(val progressPercent: Int) : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

object UpdateManager {

    private const val GITHUB_REPO = "playjymer/FlameWidget"
    private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.1.0"
        } catch (_: Exception) {
            "1.1.0"
        }
    }

    suspend fun checkForUpdates(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersionName(context)
            val request = Request.Builder()
                .url(RELEASES_API_URL)
                .header("User-Agent", "FlameWidget/$currentVersion (Android)")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)

                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val title = json.optString("name", "Новая версия")
                val notes = json.optString("body", "")

                val assets = json.optJSONArray("assets")
                var downloadUrl = ""
                var apkSize = 0L

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                if (downloadUrl.isBlank()) return@withContext null

                val isNewer = isVersionNewer(latest = tagName, current = currentVersion)

                UpdateInfo(
                    hasUpdate = isNewer,
                    latestVersion = tagName,
                    title = title,
                    notes = notes,
                    downloadUrl = downloadUrl,
                    sizeBytes = apkSize
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "FlameWidget-Updater")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                val contentLength = body.contentLength()

                val dir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
                val apkFile = File(dir, "FlameWidget-latest.apk")
                if (apkFile.exists()) apkFile.delete()

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var totalRead = 0L
                        var lastReportedPercent = -1

                        while (true) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) break
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead.toLong()
                            if (contentLength > 0L) {
                                val percent = ((totalRead * 100L) / contentLength).toInt()
                                if (percent != lastReportedPercent) {
                                    lastReportedPercent = percent
                                    onProgress(percent)
                                }
                            }
                        }
                        output.flush()
                    }
                }
                onProgress(100)
                apkFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        try {
            val latestParts = latest.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
        } catch (_: Exception) {}
        return false
    }
}
