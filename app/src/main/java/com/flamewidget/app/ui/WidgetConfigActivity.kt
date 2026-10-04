package com.flamewidget.app.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flamewidget.app.data.WidgetConfig
import com.flamewidget.app.data.WidgetPreferences
import com.flamewidget.app.ui.components.BackgroundStyleSection
import com.flamewidget.app.ui.components.BehaviorSettingsSection
import com.flamewidget.app.ui.components.DimensionsSliderSection
import com.flamewidget.app.ui.components.ScaleModeSection
import com.flamewidget.app.ui.components.UrlInputSection
import com.flamewidget.app.ui.components.WidgetPreviewCard
import com.flamewidget.app.ui.theme.FlameWidgetTheme
import com.flamewidget.app.widget.WidgetUpdater

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED first. If the user backs out, the widget host will cancel placement.
        setResult(Activity.RESULT_CANCELED)

        // Find the widget id from the intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        val prefs = WidgetPreferences(this)
        val initialConfig = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            prefs.loadWidgetConfig(appWidgetId)
        } else {
            WidgetConfig()
        }

        setContent {
            FlameWidgetTheme {
                var config by remember { mutableStateOf(initialConfig) }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) "Настройка виджета #$appWidgetId" else "Новый виджет",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            WidgetPreviewCard(config = config)
                        }

                        // Save & Apply Button
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { finish() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Отмена")
                                }

                                Button(
                                    onClick = {
                                        saveAndFinish(config)
                                    },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Применить", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        item {
                            UrlInputSection(
                                url = config.imageUrl,
                                onUrlChange = { config = config.copy(imageUrl = it) },
                                title = config.title,
                                onTitleChange = { config = config.copy(title = it) }
                            )
                        }

                        item {
                            ScaleModeSection(
                                currentMode = config.scaleMode,
                                onModeSelected = { config = config.copy(scaleMode = it) }
                            )
                        }

                        item {
                            DimensionsSliderSection(
                                cornerRadiusDp = config.cornerRadiusDp,
                                onCornerRadiusChange = { config = config.copy(cornerRadiusDp = it) },
                                paddingDp = config.paddingDp,
                                onPaddingChange = { config = config.copy(paddingDp = it) }
                            )
                        }

                        item {
                            BackgroundStyleSection(
                                currentStyle = config.backgroundStyle,
                                onStyleSelected = { config = config.copy(backgroundStyle = it) }
                            )
                        }

                        item {
                            BehaviorSettingsSection(
                                config = config,
                                onConfigChange = { config = it }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }

    private fun saveAndFinish(config: WidgetConfig) {
        val prefs = WidgetPreferences(this)
        val finalConfig = config.copy(
            id = appWidgetId,
            lastUpdated = System.currentTimeMillis()
        )
        prefs.saveWidgetConfig(finalConfig)

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            WidgetUpdater.updateWidget(this, appWidgetManager, appWidgetId)
        }

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}
