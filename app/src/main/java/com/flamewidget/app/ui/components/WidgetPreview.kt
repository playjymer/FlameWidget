package com.flamewidget.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.flamewidget.app.data.BackgroundStyle
import com.flamewidget.app.data.ScaleMode
import com.flamewidget.app.data.WidgetConfig

enum class PreviewAspectRatio(val title: String, val ratio: Float, val widthDp: Int, val heightDp: Int) {
    SQUARE_2X2("2×2 Квадрат", 1.0f, 180, 180),
    WIDE_4X2("4×2 Широкий", 2.0f, 320, 160),
    STANDARD_3X2("3×2 Стандарт", 1.5f, 260, 170)
}

@Composable
fun WidgetPreviewCard(
    config: WidgetConfig,
    modifier: Modifier = Modifier
) {
    var selectedRatio by remember { mutableStateOf(PreviewAspectRatio.SQUARE_2X2) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Живой предпросмотр виджета",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "MD3 Preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Aspect Ratio Selector Chips - evenly weighted across row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewAspectRatio.entries.forEach { ratio ->
                    FilterChip(
                        selected = selectedRatio == ratio,
                        onClick = { selectedRatio = ratio },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = ratio.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulated Home Screen Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E2638),
                                Color(0xFF151922),
                                Color(0xFF0F1116)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Widget Container with selected ratio
                Box(
                    modifier = Modifier
                        .width(selectedRatio.widthDp.dp)
                        .height(selectedRatio.heightDp.dp)
                        .clip(RoundedCornerShape(config.cornerRadiusDp.dp))
                        .background(getComposeBackgroundColor(config.backgroundStyle))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(config.cornerRadiusDp.dp)
                        )
                ) {
                    // Blurred background if scale mode is BLUR_BACKGROUND
                    if (config.scaleMode == ScaleMode.BLUR_BACKGROUND || config.backgroundStyle == BackgroundStyle.BLUR_ACCENT) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(config.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(20.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                        )
                    }

                    // Main Image with padding and scale
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(config.paddingDp.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val innerRadius = (config.cornerRadiusDp - config.paddingDp).coerceAtLeast(0)

                        if (config.imageUrl.isBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(innerRadius.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "FlameWidget 🔥",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Вставьте ссылку ниже",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(config.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Widget Image",
                                contentScale = when (config.scaleMode) {
                                    ScaleMode.FIT_CENTER -> ContentScale.Fit
                                    ScaleMode.CENTER_CROP -> ContentScale.Crop
                                    ScaleMode.BLUR_BACKGROUND -> ContentScale.Fit
                                    ScaleMode.FIT_XY -> ContentScale.FillBounds
                                },
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF2C2420)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Ошибка ссылки\nНажмите для повтора",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(innerRadius.dp))
                            )
                        }
                    }

                    // Optional Refresh Button in Preview
                    if (config.showRefreshButton) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Обновить",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Optional Title Badge in Preview
                    if (config.showTitleBadge && config.title.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = config.title,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getComposeBackgroundColor(style: BackgroundStyle): Color {
    return when (style) {
        BackgroundStyle.TRANSPARENT -> Color.Transparent
        BackgroundStyle.DARK -> Color(0xFF141210)
        BackgroundStyle.SURFACE_MD3 -> Color(0xFF231E1B)
        BackgroundStyle.LIGHT -> Color(0xFFF7F2EC)
        BackgroundStyle.BLUR_ACCENT -> Color(0x66000000)
    }
}
