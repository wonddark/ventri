package com.adpt.app.ui.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adpt.app.preferences.NotificationFrequency
import com.adpt.app.preferences.ThemeMode
import com.adpt.shared.model.ThresholdConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: PreferencesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Preferences") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Appearance ──────────────────────────────────────────────────
            SectionHeader("Appearance")
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size,
                        ),
                        label = {
                            Text(
                                text = when (mode) {
                                    ThemeMode.Light -> "Light"
                                    ThemeMode.Dark -> "Dark"
                                    ThemeMode.System -> "System"
                                }
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ── Notifications ───────────────────────────────────────────────
            SectionHeader("Notifications")
            Text(
                text = "Depletion check frequency",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            NotificationFrequency.entries.forEach { freq ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = uiState.notificationFrequency == freq,
                        onClick = { viewModel.setNotificationFrequency(freq) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (freq) {
                            NotificationFrequency.OncePerDay -> "Once per day (24 h)"
                            NotificationFrequency.TwicePerDay -> "Twice per day (12 h)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ── Depletion Thresholds ────────────────────────────────────────
            SectionHeader("Depletion Thresholds")
            Text(
                text = "Items are flagged when their estimated days remaining fall at or below each threshold.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            val t = uiState.thresholds
            ThresholdRow(
                label = "Critical",
                dotColor = MaterialTheme.colorScheme.error,
                days = t.criticalDays,
                onDecrement = { viewModel.decrementCritical() },
                onIncrement = { viewModel.incrementCritical() },
                decrementEnabled = t.criticalDays > 1,
                incrementEnabled = t.criticalDays + 1 < t.highDays,
            )
            ThresholdRow(
                label = "High",
                dotColor = MaterialTheme.colorScheme.tertiary,
                days = t.highDays,
                onDecrement = { viewModel.decrementHigh() },
                onIncrement = { viewModel.incrementHigh() },
                decrementEnabled = t.highDays - 1 > t.criticalDays,
                incrementEnabled = t.highDays + 1 < t.normalDays,
            )
            ThresholdRow(
                label = "Normal",
                dotColor = MaterialTheme.colorScheme.primary,
                days = t.normalDays,
                onDecrement = { viewModel.decrementNormal() },
                onIncrement = { viewModel.incrementNormal() },
                decrementEnabled = t.normalDays - 1 > t.highDays,
                incrementEnabled = true,
            )
            ThresholdRow(
                label = "Low",
                dotColor = MaterialTheme.colorScheme.secondary,
                days = t.normalDays + 1,
                onDecrement = {},
                onIncrement = {},
                decrementEnabled = false,
                incrementEnabled = false,
                readOnly = true,
                daysSuffix = "+",
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Circle,
            contentDescription = null,
            tint = dotColor,
            modifier = Modifier.size(10.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = if (readOnly) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
        )
        if (readOnly) {
            Text(
                text = "${days}d$daysSuffix",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            IconButton(onClick = onDecrement, enabled = decrementEnabled) {
                Text(
                    text = "−",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (decrementEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
            Text(
                text = "${days}d",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(36.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onIncrement, enabled = incrementEnabled) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (incrementEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
        }
    }
}
