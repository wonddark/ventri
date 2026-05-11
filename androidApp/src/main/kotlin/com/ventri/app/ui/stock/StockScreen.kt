package com.ventri.app.ui.stock

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ventri.app.R
import com.ventri.app.ui.components.AnimatedListItem
import com.ventri.app.ui.design.LocalNavBarHeight
import com.ventri.app.ui.design.VentriShapes
import com.ventri.app.ui.design.VentriTheme
import com.ventri.app.ui.design.components.VentriCard
import com.ventri.app.ui.design.components.VentriDialog
import com.ventri.app.ui.design.components.VentriIcon
import com.ventri.app.ui.design.components.VentriIconButton
import com.ventri.app.ui.design.components.VentriProgressIndicator
import com.ventri.app.ui.design.components.VentriText
import com.ventri.app.ui.design.components.VentriTextButton
import com.ventri.app.ui.design.components.VentriTopBar
import com.ventri.app.ui.util.displayName

@Composable
fun StockScreen(viewModel: StockViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navBarHeight = LocalNavBarHeight.current
    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    var depletedItemId by remember { mutableStateOf<String?>(null) }
    var showUpdateRateDialog by remember { mutableStateOf(false) }
    var pendingDepletedItemId by remember { mutableStateOf<String?>(null) }

    depletedItemId?.let { itemId ->
        VentriDialog(
            onDismissRequest = { depletedItemId = null },
            title = { VentriText(stringResource(R.string.stock_mark_depleted_title), style = VentriTheme.typography.titleSmall) },
            text = { VentriText(stringResource(R.string.stock_mark_depleted_body)) },
            confirmButton = {
                VentriTextButton(onClick = {
                    pendingDepletedItemId = itemId
                    depletedItemId = null
                    viewModel.markDepleted(itemId, updateRate = false)
                    showUpdateRateDialog = true
                }) { VentriText(stringResource(R.string.stock_mark_depleted_confirm), color = VentriTheme.colors.accent) }
            },
            dismissButton = {
                VentriTextButton(onClick = { depletedItemId = null }) {
                    VentriText(stringResource(R.string.common_cancel), color = VentriTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            },
        )
    }

    if (showUpdateRateDialog) {
        VentriDialog(
            onDismissRequest = { showUpdateRateDialog = false },
            title = { VentriText(stringResource(R.string.stock_update_rate_title), style = VentriTheme.typography.titleSmall) },
            text = { VentriText(stringResource(R.string.stock_update_rate_body)) },
            confirmButton = {
                VentriTextButton(onClick = {
                    pendingDepletedItemId?.let { viewModel.markDepleted(it, updateRate = true) }
                    pendingDepletedItemId = null
                    showUpdateRateDialog = false
                }) {
                    VentriText(stringResource(R.string.common_yes), color = VentriTheme.colors.accent)
                }
            },
            dismissButton = {
                VentriTextButton(onClick = {
                    pendingDepletedItemId = null
                    showUpdateRateDialog = false
                }) {
                    VentriText(stringResource(R.string.common_no), color = VentriTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(VentriTheme.colors.background)) {
        when (val s = state) {
            StockUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(top = topBarHeightDp),
                contentAlignment = Alignment.Center,
            ) { VentriProgressIndicator() }

            is StockUiState.Success -> if (s.items.isEmpty()) {
                StockEmptyState(topPadding = topBarHeightDp, bottomPadding = navBarHeight)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = topBarHeightDp,
                        bottom = navBarHeight + 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "header") {
                        VentriText(
                            text = pluralStringResource(R.plurals.stock_item_count, s.items.size, s.items.size),
                            style = VentriTheme.typography.bodySmall,
                            color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(s.items, key = { it.id }) { item ->
                        AnimatedListItem(index = s.items.indexOf(item), animationKey = Unit) {
                            StockItemCard(
                                item = item,
                                onMarkDepleted = { depletedItemId = item.id },
                            )
                        }
                    }
                }
            }
        }

        VentriTopBar(
            title = { VentriText(stringResource(R.string.stock_title), style = VentriTheme.typography.titleLarge) },
            modifier = Modifier.onSizeChanged { topBarHeightPx = it.height },
        )
    }
}

@Composable
private fun StockItemCard(item: StockItemUiModel, onMarkDepleted: () -> Unit) {
    VentriCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                VentriText(item.name, style = VentriTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                VentriText(
                    text = "${item.remainingQuantity.formatQuantity()} ${item.unit.displayName()}",
                    style = VentriTheme.typography.bodySmall,
                    color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                )
                VentriText(
                    text = item.daysLabel.toText(),
                    style = VentriTheme.typography.bodySmall,
                    color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
            VentriIconButton(onClick = onMarkDepleted) {
                VentriIcon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = VentriTheme.colors.accent,
                )
            }
        }
    }
}

@Composable
private fun StockDaysLabel.toText(): String = when (this) {
    StockDaysLabel.TrackingUsage -> stringResource(R.string.stock_tracking_usage)
    StockDaysLabel.LessThanADay -> stringResource(R.string.stock_less_than_a_day)
    is StockDaysLabel.Days -> pluralStringResource(R.plurals.stock_days_remaining, count.toInt(), count)
}

@Composable
private fun StockEmptyState(topPadding: Dp, bottomPadding: Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topPadding, bottom = bottomPadding, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        VentriIcon(
            imageVector = Icons.Default.Inventory2,
            contentDescription = null,
            tint = VentriTheme.colors.accent,
            modifier = Modifier
                .background(VentriTheme.colors.accentMuted, shape = VentriShapes.pill)
                .padding(20.dp),
        )
        Spacer(Modifier.height(20.dp))
        VentriText(
            text = stringResource(R.string.stock_empty_title),
            style = VentriTheme.typography.titleLarge,
            color = VentriTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        VentriText(
            text = stringResource(R.string.stock_empty_body),
            style = VentriTheme.typography.bodyMedium,
            color = VentriTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(32.dp))
        StockTipCard(
            icon = Icons.Default.AddShoppingCart,
            title = stringResource(R.string.stock_onboarding_land_title),
            body = stringResource(R.string.stock_onboarding_land_body),
        )
        Spacer(Modifier.height(12.dp))
        StockTipCard(
            icon = Icons.Default.Speed,
            title = stringResource(R.string.stock_onboarding_track_title),
            body = stringResource(R.string.stock_onboarding_track_body),
        )
        Spacer(Modifier.height(12.dp))
        StockTipCard(
            icon = Icons.Default.Timer,
            title = stringResource(R.string.stock_onboarding_deplete_title),
            body = stringResource(R.string.stock_onboarding_deplete_body),
        )
    }
}

@Composable
private fun StockTipCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    VentriCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            VentriIcon(
                imageVector = icon,
                contentDescription = null,
                tint = VentriTheme.colors.accent,
                modifier = Modifier
                    .background(VentriTheme.colors.accentMuted, shape = VentriShapes.small)
                    .padding(8.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                VentriText(title, style = VentriTheme.typography.titleSmall)
                VentriText(body, style = VentriTheme.typography.bodySmall, color = VentriTheme.colors.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

private fun Double.formatQuantity(): String =
    if (this % 1.0 == 0.0) toLong().toString() else "%.1f".format(this)
