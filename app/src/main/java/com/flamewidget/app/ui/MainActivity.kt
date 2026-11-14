package com.flamewidget.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.flamewidget.app.ui.screens.HomeScreen
import com.flamewidget.app.ui.theme.FlameWidgetTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(applicationContext) as T
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)

        setContent {
            FlameWidgetTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = "FlameWidget MD3",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Flame Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) { innerPadding ->
                    HomeScreen(
                        viewModel = viewModel,
                        onEditWidget = { widgetId ->
                            val editIntent = Intent(this, WidgetConfigActivity::class.java).apply {
                                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            }
                            startActivity(editIntent)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        var extractedUrl: String? = null

        // 1. Direct Data URI (Deep Link)
        val data: Uri? = intent.data
        if (data != null) {
            val scheme = data.scheme?.lowercase()
            if (scheme == "flamewidget") {
                // e.g. flamewidget://widget?url=https%3A%2F%2Fapi.flamebot.app%2F...
                extractedUrl = data.getQueryParameter("url") ?: data.getQueryParameter("imageUrl")
                if (extractedUrl.isNullOrBlank()) {
                    val ssp = data.schemeSpecificPart.removePrefix("//").trim()
                    if (ssp.startsWith("http://", ignoreCase = true) || ssp.startsWith("https://", ignoreCase = true)) {
                        extractedUrl = ssp
                    }
                }
            } else if (scheme == "http" || scheme == "https") {
                // e.g. https://api.flamebot.app/dev/widget/...
                extractedUrl = if (data.host == "flamewidget.app") {
                    data.getQueryParameter("url") ?: data.toString()
                } else {
                    data.toString()
                }
            }
        }

        // 2. Intent Action SEND (Shared text from another app or Telegram)
        if (extractedUrl.isNullOrBlank() && intent.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val urlRegex = "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)".toRegex()
                val match = urlRegex.find(sharedText)
                if (match != null) {
                    extractedUrl = match.value
                }
            }
        }

        // 3. String Extra
        if (extractedUrl.isNullOrBlank()) {
            val extraUrl = intent.getStringExtra("OPEN_PREVIEW_URL") ?: intent.getStringExtra("imageUrl")
            if (!extraUrl.isNullOrBlank()) {
                extractedUrl = extraUrl
            }
        }

        if (!extractedUrl.isNullOrBlank()) {
            viewModel.updateUrl(extractedUrl)
            Toast.makeText(this, "Ссылка на виджет загружена!", Toast.LENGTH_SHORT).show()
        }
    }
}
