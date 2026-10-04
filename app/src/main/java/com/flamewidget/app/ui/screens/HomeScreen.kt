package com.flamewidget.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flamewidget.app.data.WidgetConfig
import com.flamewidget.app.ui.MainViewModel
import com.flamewidget.app.ui.components.BackgroundStyleSection
import com.flamewidget.app.ui.components.BehaviorSettingsSection
import com.flamewidget.app.ui.components.DimensionsSliderSection
import com.flamewidget.app.ui.components.ScaleModeSection
import com.flamewidget.app.ui.components.UrlInputSection
import com.flamewidget.app.ui.components.WidgetPreviewCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onEditWidget: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentConfig by viewModel.currentConfig.collectAsState()
    val activeWidgets by viewModel.activeWidgets.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf("Создать / Настроить", "Активные (${activeWidgets.size})", "Инструкция")

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        if (index == 1) viewModel.loadActiveWidgets()
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTabIndex) {
            0 -> ConfigTabContent(viewModel, currentConfig)
            1 -> ActiveWidgetsTabContent(activeWidgets, onRefresh = { viewModel.refreshWidget(it) }, onEdit = onEditWidget)
            2 -> InstructionsTabContent()
        }
    }
}

@Composable
fun ConfigTabContent(
    viewModel: MainViewModel,
    config: WidgetConfig
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Preview Card
        item {
            WidgetPreviewCard(config = config)
        }

        // URL & Title Input
        item {
            UrlInputSection(
                url = config.imageUrl,
                onUrlChange = { viewModel.updateUrl(it) },
                title = config.title,
                onTitleChange = { viewModel.updateTitle(it) }
            )
        }

        // Primary MD3 Action Button: Pin to Home Screen
        item {
            Button(
                onClick = {
                    viewModel.pinWidgetToHomeScreen(context) {}
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Добавить на рабочий стол",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Sizing & Scale Mode
        item {
            ScaleModeSection(
                currentMode = config.scaleMode,
                onModeSelected = { viewModel.updateScaleMode(it) }
            )
        }

        // Geometry & Sliders
        item {
            DimensionsSliderSection(
                cornerRadiusDp = config.cornerRadiusDp,
                onCornerRadiusChange = { viewModel.updateCornerRadius(it) },
                paddingDp = config.paddingDp,
                onPaddingChange = { viewModel.updatePadding(it) }
            )
        }

        // Background Style
        item {
            BackgroundStyleSection(
                currentStyle = config.backgroundStyle,
                onStyleSelected = { viewModel.updateBackgroundStyle(it) }
            )
        }

        // Behavior & Periodic Refresh
        item {
            BehaviorSettingsSection(
                config = config,
                onConfigChange = { viewModel.updateConfig(it) }
            )
        }

        item {
            AppFooter()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ActiveWidgetsTabContent(
    activeWidgets: List<WidgetConfig>,
    onRefresh: (Int) -> Unit,
    onEdit: (Int) -> Unit
) {
    if (activeWidgets.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Пока нет добавленных виджетов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Нажмите «Добавить на рабочий стол» во вкладке настройки или добавьте виджет через домашний экран.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(activeWidgets) { widget ->
                ActiveWidgetCard(widget = widget, onRefresh = { onRefresh(widget.id) }, onEdit = { onEdit(widget.id) })
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ActiveWidgetCard(
    widget: WidgetConfig,
    onRefresh: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = widget.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = widget.title.ifBlank { "Виджет #${widget.id}" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${widget.scaleMode.title} • ${widget.cornerRadiusDp}dp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val dateStr = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()).format(Date(widget.lastUpdated))
                Text(
                    text = "Обновлен: $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
            }
        }
    }
}

@Composable
fun InstructionsTabContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Как идеально подогнать по размерам?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. Режимы масштабирования:\n" +
                                " • «По размеру (Fit)» — сохраняет оригинальные пропорции изображения (1:1 и др.) без сплющивания и обрезки.\n" +
                                " • «Размытый фон» — если виджет широкий (например 4x2), по центру будет четкая карточка, а по бокам стильный размытый фон в цвет картинки.\n" +
                                " • «Заполнение (Crop)» — заполняет всю ячейку целиком.\n\n" +
                                "2. Изменение размера на рабочем столе:\n" +
                                " • Зажмите виджет пальцем на домашнем экране на 1 секунду.\n" +
                                " • Появятся маркеры изменения размера (рамка с кружками).\n" +
                                " • Потяните за маркеры, чтобы сделать виджет 2x2, 3x2, 4x2 или 4x4.\n\n" +
                                "3. Быстрое обновление:\n" +
                                " • Нажмите на иконку перезагрузки в углу виджета, чтобы моментально обновить картинку по ссылке.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        item {
            AppFooter()
        }
    }
}

@Composable
fun AppFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Создано для https://t.me/flameintgbot",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/flameintgbot"))
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
