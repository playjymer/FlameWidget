package com.flamewidget.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.flamewidget.app.update.UpdateInfo
import com.flamewidget.app.update.UpdateState
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
    val updateState by viewModel.updateState.collectAsState()
    val previewRatio by viewModel.previewRatio.collectAsState()
    val selectedWidgetId by viewModel.selectedWidgetId.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf("Создать / Настроить", "Активные (${activeWidgets.size})", "Инструкция")

    Column(modifier = modifier.fillMaxSize()) {

        // Auto Update Banner if an update is available or downloading
        UpdateBanner(
            updateState = updateState,
            onUpdateClick = { info -> viewModel.downloadAndInstallUpdate(context, info) },
            onDismiss = { viewModel.dismissUpdate() }
        )

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
            0 -> ConfigTabContent(
                viewModel = viewModel,
                config = currentConfig,
                selectedWidgetId = selectedWidgetId,
                activeWidgets = activeWidgets,
                previewRatio = previewRatio
            )
            1 -> ActiveWidgetsTabContent(
                activeWidgets = activeWidgets,
                onRefresh = { viewModel.refreshWidget(it) },
                onEdit = { widgetId ->
                    viewModel.selectWidgetToEdit(widgetId)
                    selectedTabIndex = 0
                }
            )
            2 -> InstructionsTabContent()
        }
    }
}

@Composable
fun UpdateBanner(
    updateState: UpdateState,
    onUpdateClick: (UpdateInfo) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = updateState is UpdateState.Available || updateState is UpdateState.Downloading || updateState is UpdateState.ReadyToInstall,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        when (updateState) {
            is UpdateState.Available -> {
                val info = updateState.info
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Доступно обновление v${info.latestVersion}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (info.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = info.notes.take(160) + if (info.notes.length > 160) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { onUpdateClick(info) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Обновить сейчас", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            is UpdateState.Downloading -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Загрузка обновления: ${updateState.progressPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (updateState.progressPercent > 0) {
                            LinearProgressIndicator(
                                progress = updateState.progressPercent / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
            is UpdateState.ReadyToInstall -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Файл обновления загружен. Запуск установки...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun ConfigTabContent(
    viewModel: MainViewModel,
    config: WidgetConfig,
    selectedWidgetId: Int?,
    activeWidgets: List<WidgetConfig>,
    previewRatio: com.flamewidget.app.ui.components.PreviewAspectRatio
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val clipText = clipboardManager.getText()?.text?.trim()
    val isClipWidgetUrl = !clipText.isNullOrBlank() &&
            (clipText.startsWith("http://", ignoreCase = true) || clipText.startsWith("https://", ignoreCase = true)) &&
            clipText != config.imageUrl

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Widget Selector (if multiple or active widgets exist)
        if (activeWidgets.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Цель настройки:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedWidgetId == null,
                            onClick = { viewModel.selectWidgetToEdit(null) },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Новый виджет", fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )

                        activeWidgets.forEach { widget ->
                            val isSelected = selectedWidgetId == widget.id
                            val typeLabel = if (widget.isWide) "4×2" else "2×2"
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectWidgetToEdit(widget.id) },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                label = {
                                    Text(
                                        text = "${widget.title.ifBlank { "Виджет #${widget.id}" }} ($typeLabel)",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Live Preview Card (With ratio kept in sync and auto-detecting wide/square)
        item {
            WidgetPreviewCard(
                config = config,
                ratioOverride = previewRatio,
                onRatioChange = { viewModel.setPreviewRatio(it) }
            )
        }

        // Smart Clipboard Quick Insert Card
        if (isClipWidgetUrl) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Найдена ссылка в буфере",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = clipText ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.updateUrl(clipText ?: "") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        ) {
                            Text("Вставить", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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

        // Primary Action Button (Add new or Save existing)
        item {
            val isEditing = selectedWidgetId != null
            Button(
                onClick = {
                    viewModel.saveOrPinWidget(context) {}
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEditing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    contentColor = if (isEditing) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Сохранить изменения (Виджет #$selectedWidgetId)" else "Добавить на рабочий стол",
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
                ActiveWidgetCard(
                    widget = widget,
                    onRefresh = { onRefresh(widget.id) },
                    onEdit = { onEdit(widget.id) }
                )
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
            val thumbWidth = if (widget.isWide) 80.dp else 60.dp
            Box(
                modifier = Modifier
                    .size(width = thumbWidth, height = 60.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = widget.title.ifBlank { "Виджет #${widget.id}" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (widget.isWide) "4×2" else "2×2",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
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
                            text = "Как работают виджеты?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. Автоматический выбор типа виджета:\n" +
                                " • Если вы вставляете ссылку на широкий баннер или статус FlameBot — приложение автоматически выбирает широкий формат 4×2.\n" +
                                " • Для квадратных карточек и стриков выбирается компактный формат 2×2.\n\n" +
                                "2. Поддержка ссылок и Deep Link:\n" +
                                " • Вы можете открыть ссылку из Telegram, и FlameWidget откроется сразу с заполненной ссылкой.\n" +
                                " • Приложение автоматически распознает скопированные ссылки из буфера обмена.\n\n" +
                                "3. Независимая настройка каждого виджета:\n" +
                                " • Каждый виджет на вашем экране настраивается полностью независимо. Изменение одного виджета не затрагивает другие.\n\n" +
                                "4. Быстрое обновление:\n" +
                                " • Нажмите на кнопку обновления прямо на виджете, чтобы моментально запросить свежие данные.",
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
