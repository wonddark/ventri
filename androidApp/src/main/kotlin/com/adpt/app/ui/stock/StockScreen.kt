package com.adpt.app.ui.stock

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
import androidx.compose.material.icons.outlined.RemoveShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adpt.app.ui.components.AnimatedListItem
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.components.AdptCard
import com.adpt.app.ui.design.components.AdptDialog
import com.adpt.app.ui.design.components.AdptIcon
import com.adpt.app.ui.design.components.AdptIconButton
import com.adpt.app.ui.design.components.AdptProgressIndicator
import com.adpt.app.ui.design.components.AdptScaffold
import com.adpt.app.ui.design.components.AdptText
import com.adpt.app.ui.design.components.AdptTextButton
import com.adpt.app.ui.design.components.AdptTopBar

@Composable
fun StockScreen(viewModel: StockViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var depletingItem by remember { mutableStateOf<StockItemUiModel?>(null) }

    depletingItem?.let { item ->
        AdptDialog(
            onDismissRequest = { depletingItem = null },
            title = { AdptText("Update consumption rate?", style = AdptTheme.typography.titleSmall) },
            text = { AdptText("Would you like to recalculate the consumption rate based on actual usage since the last purchase?") },
            confirmButton = {
                AdptTextButton(onClick = {
                    viewModel.markDepleted(item.id, updateRate = true)
                    depletingItem = null
                }) { AdptText("Yes", color = AdptTheme.colors.accent) }
            },
            dismissButton = {
                AdptTextButton(onClick = {
                    viewModel.markDepleted(item.id, updateRate = false)
                    depletingItem = null
                }) { AdptText("No", color = AdptTheme.colors.onSurface.copy(alpha = 0.6f)) }
            },
        )
    }

    AdptScaffold(
        topBar = { AdptTopBar(title = { AdptText("Stock", style = AdptTheme.typography.titleLarge) }) },
    ) { innerPadding ->
        when (val state = uiState) {
            StockUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { AdptProgressIndicator() }

            is StockUiState.Success -> if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    AdptText(
                        "Nothing here to show",
                        style = AdptTheme.typography.bodyMedium,
                        color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        AnimatedListItem(index = state.items.indexOf(item)) {
                            StockItemCard(item = item, onMarkDepleted = { depletingItem = item })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockItemCard(item: StockItemUiModel, onMarkDepleted: () -> Unit) {
    AdptCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AdptText(item.name, style = AdptTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                AdptText(
                    text = item.daysRemainingLabel,
                    style = AdptTheme.typography.bodySmall,
                    color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
            AdptText(
                text = "${item.remainingQuantity.formatQuantity()} ${item.unit.name}",
                style = AdptTheme.typography.bodyMedium,
                color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
            )
            AdptIconButton(onClick = onMarkDepleted) {
                AdptIcon(
                    imageVector = Icons.Outlined.RemoveShoppingCart,
                    contentDescription = "Mark as depleted",
                    tint = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun Double.formatQuantity(): String =
    if (this % 1.0 == 0.0) toLong().toString() else "%.1f".format(this)
