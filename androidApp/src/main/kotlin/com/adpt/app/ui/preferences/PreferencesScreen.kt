package com.adpt.app.ui.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adpt.app.preferences.NotificationFrequency
import com.adpt.app.preferences.ThemeMode
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.components.AdptIcon
import com.adpt.app.ui.design.components.AdptIconButton
import com.adpt.app.ui.design.components.AdptText
import com.adpt.app.ui.design.components.AdptTopBar
import com.adpt.app.ui.design.components.ripple

@Composable
fun PreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: PreferencesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AdptTheme.colors
    val typography = AdptTheme.typography
    val density = LocalDensity.current

    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = topBarHeightDp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Appearance ──────────────────────────────────────────────────
            SectionHeader("Appearance")
            AdptText(
                text = "Theme",
                style = typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            // Segmented control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.outline, AdptShapes.pill)
                    .clip(AdptShapes.pill),
            ) {
                ThemeMode.entries.forEach { mode ->
                    val selected = uiState.themeMode == mode
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (selected) colors.accentMuted else Color.Transparent,
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(),
                                onClick = { viewModel.setThemeMode(mode) },
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AdptText(
                            text = when (mode) {
                                ThemeMode.Light -> "Light"
                                ThemeMode.Dark -> "Dark"
                                ThemeMode.System -> "System"
                            },
                            style = typography.labelMedium,
                            color = if (selected) colors.accent else colors.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ── Notifications ───────────────────────────────────────────────
            SectionHeader("Notifications")
            AdptText(
                text = "Depletion check frequency",
                style = typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            NotificationFrequency.entries.forEach { freq ->
                val selected = uiState.notificationFrequency == freq
                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = { viewModel.setNotificationFrequency(freq) },
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Custom radio circle
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(
                                width = 2.dp,
                                color = if (selected) colors.accent else colors.outline,
                                shape = CircleShape,
                            )
                            .padding(4.dp)
                            .background(
                                color = if (selected) colors.accent else Color.Transparent,
                                shape = CircleShape,
                            ),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    AdptText(
                        text = when (freq) {
                            NotificationFrequency.OncePerDay -> "Once per day (24 h)"
                            NotificationFrequency.TwicePerDay -> "Twice per day (12 h)"
                        },
                        style = typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ── Depletion Thresholds ────────────────────────────────────────
            SectionHeader("Depletion Thresholds")
            AdptText(
                text = "Items are flagged when their estimated days remaining fall at or below each threshold.",
                style = typography.bodySmall,
                color = colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp),
            )

            val t = uiState.thresholds
            ThresholdRow(
                label = "Critical",
                dotColor = colors.critical,
                days = t.criticalDays,
                onDecrement = { viewModel.decrementCritical() },
                onIncrement = { viewModel.incrementCritical() },
                decrementEnabled = t.criticalDays > 1,
                incrementEnabled = t.criticalDays + 1 < t.highDays,
            )
            ThresholdRow(
                label = "High",
                dotColor = colors.warning,
                days = t.highDays,
                onDecrement = { viewModel.decrementHigh() },
                onIncrement = { viewModel.incrementHigh() },
                decrementEnabled = t.highDays - 1 > t.criticalDays,
                incrementEnabled = t.highDays + 1 < t.normalDays,
            )
            ThresholdRow(
                label = "Normal",
                dotColor = colors.ok,
                days = t.normalDays,
                onDecrement = { viewModel.decrementNormal() },
                onIncrement = { viewModel.incrementNormal() },
                decrementEnabled = t.normalDays - 1 > t.highDays,
                incrementEnabled = true,
            )
            ThresholdRow(
                label = "Low",
                dotColor = colors.onSurface.copy(alpha = 0.4f),
                days = t.normalDays + 1,
                onDecrement = {},
                onIncrement = {},
                decrementEnabled = false,
                incrementEnabled = false,
                readOnly = true,
                daysSuffix = "+",
            )
        }

        // Pinned top bar overlay
        AdptTopBar(
            title = { AdptText("Preferences", style = typography.titleMedium) },
            navigationIcon = {
                AdptIconButton(onClick = onNavigateUp) {
                    AdptIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            modifier = Modifier.onSizeChanged { topBarHeightPx = it.height },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    AdptText(
        text = title.uppercase(),
        style = AdptTheme.typography.labelSmall,
        color = AdptTheme.colors.accent,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun SectionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AdptTheme.colors.outline),
    )
}

@Composable
private fun ThresholdRow(
    label: String,
    dotColor: Color,
    days: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementEnabled: Boolean,
    incrementEnabled: Boolean,
    readOnly: Boolean = false,
    daysSuffix: String = "",
) {
    val colors = AdptTheme.colors
    val typography = AdptTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AdptIcon(
            imageVector = Icons.Default.Circle,
            contentDescription = null,
            tint = dotColor,
            modifier = Modifier.size(10.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        AdptText(
            text = label,
            style = typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = if (readOnly) colors.onSurface.copy(alpha = 0.5f) else colors.onSurface,
        )
        if (readOnly) {
            AdptText(
                text = "${days}d$daysSuffix",
                style = typography.bodyMedium,
                color = colors.onSurface.copy(alpha = 0.5f),
            )
        } else {
            AdptIconButton(
                onClick = onDecrement,
                enabled = decrementEnabled,
            ) {
                AdptText(
                    text = "−",
                    style = typography.titleLarge,
                    color = if (decrementEnabled) colors.accent
                            else colors.onSurface.copy(alpha = 0.38f),
                )
            }
            AdptText(
                text = "${days}d",
                style = typography.bodyMedium,
                modifier = Modifier.width(36.dp),
                color = colors.onSurface,
            )
            AdptIconButton(
                onClick = onIncrement,
                enabled = incrementEnabled,
            ) {
                AdptText(
                    text = "+",
                    style = typography.titleLarge,
                    color = if (incrementEnabled) colors.accent
                            else colors.onSurface.copy(alpha = 0.38f),
                )
            }
        }
    }
}
