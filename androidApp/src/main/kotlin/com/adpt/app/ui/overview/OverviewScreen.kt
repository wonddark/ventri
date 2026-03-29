package com.adpt.app.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adpt.shared.model.Severity
import kotlin.math.abs

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(viewModel: OverviewViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Overview") })

        when (val state = uiState) {
            OverviewUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is OverviewUiState.Success -> if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Nothing here to show",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        OverviewItemCard(
                            item = item,
                            onAddToShoppingList = {
                                viewModel.handleIntent(OverviewIntent.AddToShoppingList(item.id))
                            },
                            onIgnore = {
                                viewModel.handleIntent(OverviewIntent.IgnoreItem(item.id))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewItemCard(
    item: OverviewItemUiModel,
    onAddToShoppingList: () -> Unit,
    onIgnore: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SeverityBadge(severity = item.severity)
                    Text(
                        text = item.deltaMillis.toDaysText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onAddToShoppingList) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Add to shopping list",
                )
            }
            IconButton(onClick = onIgnore) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Ignore item",
                )
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: Severity) {
    val bgColor = when (severity) {
        Severity.Critical -> MaterialTheme.colorScheme.errorContainer
        Severity.Warning -> MaterialTheme.colorScheme.tertiaryContainer
        Severity.Good -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (severity) {
        Severity.Critical -> MaterialTheme.colorScheme.onErrorContainer
        Severity.Warning -> MaterialTheme.colorScheme.onTertiaryContainer
        Severity.Good -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(color = bgColor, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text = severity.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

private fun Long.toDaysText(): String {
    val days = abs(this / MILLIS_PER_DAY)
    return when {
        this <= 0 && days == 0L -> "Depleted today"
        this <= 0 -> "$days day(s) overdue"
        this < MILLIS_PER_DAY -> "Less than a day remaining"
        days == 1L -> "1 day remaining"
        else -> "$days days remaining"
    }
}
