package com.adpt.app.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adpt.app.ui.components.AnimatedListItem
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.components.AdptCard
import com.adpt.app.ui.design.components.AdptChip
import com.adpt.app.ui.design.components.AdptCircleBadge
import com.adpt.app.ui.design.components.AdptFab
import com.adpt.app.ui.design.components.AdptIcon
import com.adpt.app.ui.design.components.AdptIconButton
import com.adpt.app.ui.design.components.AdptProgressIndicator
import com.adpt.app.ui.design.components.AdptScaffold
import com.adpt.app.ui.design.components.AdptSnackbarHost
import com.adpt.app.ui.design.components.AdptSurface
import com.adpt.app.ui.design.components.AdptText
import com.adpt.app.ui.design.components.AdptTopBar
import com.adpt.app.ui.design.components.rememberAdptSnackbarHostState
import com.adpt.shared.model.Severity
import kotlin.math.abs

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

@Composable
fun OverviewScreen(viewModel: OverviewViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = rememberAdptSnackbarHostState()

    LaunchedEffect(Unit) {
        viewModel.errors.collect { snackbarState.showSnackbar(it) }
    }

    val successItems = (uiState as? OverviewUiState.Success)?.items

    AdptScaffold(
        topBar = {
            AdptTopBar(
                title = {
                    AdptText("Overview", style = AdptTheme.typography.titleLarge)
                },
                actions = {
                    AdptIconButton(onClick = { viewModel.refresh() }) {
                        AdptIcon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        snackbarHost = { AdptSnackbarHost(snackbarState) },
        floatingActionButton = {
            if (!successItems.isNullOrEmpty()) {
                AdptFab(onClick = { viewModel.addAllToShoppingList(successItems.map { it.id }) }) {
                    AdptIcon(
                        Icons.Default.AddShoppingCart,
                        contentDescription = null,
                        tint = AdptTheme.colors.onAccent,
                    )
                    AdptText(
                        "Add all to list",
                        style = AdptTheme.typography.labelMedium,
                        color = AdptTheme.colors.onAccent,
                    )
                }
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            OverviewUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { AdptProgressIndicator() }

            is OverviewUiState.Success -> if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    AdptText(
                        "Nothing to show here",
                        style = AdptTheme.typography.bodyMedium,
                        color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                    )
                }
            } else {
                val criticalCount = state.items.count { it.severity == Severity.Critical }
                val highCount = state.items.count { it.severity == Severity.High }
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SummaryChip(
                            count = criticalCount,
                            label = "Critical",
                            containerColor = AdptTheme.colors.criticalContainer,
                            contentColor = AdptTheme.colors.onCriticalContainer,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryChip(
                            count = highCount,
                            label = "High",
                            containerColor = AdptTheme.colors.warningContainer,
                            contentColor = AdptTheme.colors.onWarningContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            AnimatedListItem(
                                index = state.items.indexOf(item),
                                animationKey = state.listVersion,
                            ) {
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
    }
}

@Composable
private fun SummaryChip(
    count: Int,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    AdptSurface(
        modifier = modifier,
        color = containerColor,
        shape = AdptTheme.shapes.card,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AdptText(count.toString(), style = AdptTheme.typography.titleLarge, color = contentColor)
            AdptText(label, style = AdptTheme.typography.labelMedium, color = contentColor)
        }
    }
}

@Composable
private fun OverviewItemCard(
    item: OverviewItemUiModel,
    onAddToShoppingList: () -> Unit,
    onIgnore: () -> Unit,
) {
    val colors = AdptTheme.colors
    val accentColor = when (item.severity) {
        Severity.Critical -> colors.critical
        Severity.High -> colors.warning
        Severity.Normal, Severity.Low -> colors.ok
    }

    AdptCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdptCircleBadge(borderColor = accentColor) {
                val label = when {
                    item.deltaMillis == null -> "--"
                    item.deltaMillis <= 0 -> "0d"
                    else -> "${item.deltaMillis / MILLIS_PER_DAY}d"
                }
                AdptText(label, style = AdptTheme.typography.labelMedium, color = accentColor)
            }
            Column(modifier = Modifier.weight(1f)) {
                AdptText(item.name, style = AdptTheme.typography.titleMedium)
                AdptText(
                    text = item.deltaMillis?.toDaysText() ?: "Not in stock",
                    style = AdptTheme.typography.bodySmall,
                    color = colors.onSurface.copy(alpha = 0.5f),
                )
            }
            if (item.isInShoppingList) {
                AdptChip(containerColor = colors.criticalContainer) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AdptIcon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = colors.onCriticalContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        AdptText(
                            "In list",
                            style = AdptTheme.typography.labelSmall,
                            color = colors.onCriticalContainer,
                        )
                    }
                }
            } else {
                AdptIconButton(onClick = onAddToShoppingList) {
                    AdptIcon(Icons.Default.ShoppingCart, contentDescription = "Add to shopping list")
                }
            }
            AdptIconButton(onClick = onIgnore) {
                AdptIcon(Icons.Default.Close, contentDescription = "Ignore item")
            }
        }
    }
}

private fun Long.toDaysText(): String {
    val days = abs(this / MILLIS_PER_DAY)
    return when {
        this <= 0 && days == 0L -> "Depleting today"
        this <= 0 -> "$days day(s) overdue"
        this < MILLIS_PER_DAY -> "Less than a day remaining"
        days == 1L -> "1 day remaining"
        else -> "$days days remaining"
    }
}
