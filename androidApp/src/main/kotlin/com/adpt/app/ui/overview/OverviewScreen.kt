package com.adpt.app.ui.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.adpt.app.ui.components.AnimatedListItem
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.LocalBarsVisible
import com.adpt.app.ui.design.LocalNavBarHeight
import com.adpt.app.ui.design.components.AdptCard
import com.adpt.app.ui.design.components.AdptFab
import com.adpt.app.ui.design.components.AdptIcon
import com.adpt.app.ui.design.components.AdptIconButton
import com.adpt.app.ui.design.components.AdptProgressIndicator
import com.adpt.app.ui.design.components.AdptSnackbarHost
import com.adpt.app.ui.design.components.AdptSurface
import com.adpt.app.ui.design.components.AdptText
import com.adpt.app.ui.design.components.AdptTopBar
import com.adpt.app.ui.design.components.rememberAdptSnackbarHostState
import com.adpt.app.ui.design.components.ripple
import com.adpt.shared.model.Severity
import kotlin.math.abs

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

@Composable
fun OverviewScreen(
    onOpenSettings: () -> Unit,
    viewModel: OverviewViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberAdptSnackbarHostState()
    val navBarHeight = LocalNavBarHeight.current
    val barsVisible = LocalBarsVisible.current
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        viewModel.errors.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val successItems = (uiState as? OverviewUiState.Success)?.items
    val colors = AdptTheme.colors

    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        when (val state = uiState) {
            OverviewUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(top = topBarHeightDp),
                contentAlignment = Alignment.Center,
            ) {
                AdptProgressIndicator()
            }

            is OverviewUiState.Success -> if (state.criticalCount == 0 && state.highCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topBarHeightDp, bottom = navBarHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    AdptText(
                        text = "Nothing to show here",
                        style = AdptTheme.typography.bodyMedium,
                        color = colors.onSurface.copy(alpha = 0.6f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = topBarHeightDp,
                        bottom = navBarHeight + 72.dp, // extra space for FAB
                        start = 16.dp,
                        end = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "chips") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val criticalSelected = state.severityFilter == Severity.Critical
                            SummaryChip(
                                count = state.criticalCount,
                                label = "Critical",
                                containerColor = if (criticalSelected) colors.critical else colors.criticalContainer,
                                contentColor = if (criticalSelected) colors.onCritical else colors.onCriticalContainer,
                                onClick = {
                                    viewModel.handleIntent(
                                        OverviewIntent.ToggleSeverityFilter(Severity.Critical)
                                    )
                                },
                            )
                            val highSelected = state.severityFilter == Severity.High
                            SummaryChip(
                                count = state.highCount,
                                label = "High",
                                containerColor = if (highSelected) colors.warning else colors.warningContainer,
                                contentColor = if (highSelected) colors.onWarning else colors.onWarningContainer,
                                onClick = {
                                    viewModel.handleIntent(
                                        OverviewIntent.ToggleSeverityFilter(Severity.High)
                                    )
                                },
                            )
                        }
                    }

                    if (state.items.isEmpty()) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                AdptText(
                                    text = "No items match this filter",
                                    style = AdptTheme.typography.bodyMedium,
                                    color = colors.onSurface.copy(alpha = 0.6f),
                                )
                            }
                        }
                    } else {
                        items(state.items, key = { it.id }) { item ->
                            AnimatedListItem(
                                index = state.items.indexOf(item),
                                animationKey = state.listVersion,
                            ) {
                                OverviewItemCard(
                                    item = item,
                                    onAddToShoppingList = {
                                        viewModel.handleIntent(
                                            OverviewIntent.AddToShoppingList(item.id)
                                        )
                                    },
                                    onIgnore = {
                                        viewModel.handleIntent(
                                            OverviewIntent.IgnoreItem(item.id)
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pinned top bar overlay
        AdptTopBar(
            title = { AdptText("Overview", style = AdptTheme.typography.titleMedium) },
            actions = {
                AdptIconButton(onClick = onOpenSettings) {
                    AdptIcon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                    )
                }
                AdptIconButton(onClick = { viewModel.refresh() }) {
                    AdptIcon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                    )
                }
            },
            modifier = Modifier.onSizeChanged { topBarHeightPx = it.height },
        )

        // FAB
        AnimatedVisibility(
            visible = barsVisible && !successItems.isNullOrEmpty(),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = navBarHeight + 16.dp),
        ) {
            AdptFab(
                onClick = {
                    viewModel.addAllToShoppingList(successItems!!.map { it.id })
                },
            ) {
                AdptIcon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = null,
                    tint = AdptTheme.colors.onAccent,
                )
            }
        }

        // Snackbar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarHeight + 8.dp, start = 16.dp, end = 16.dp),
        ) {
            AdptSnackbarHost(snackbarHostState)
        }
    }
}

@Composable
private fun SummaryChip(
    count: Int,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(AdptShapes.card)
            .background(color = containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            AdptText(
                text = count.toString(),
                style = AdptTheme.typography.labelMedium,
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(4.dp))
            AdptText(
                text = label,
                style = AdptTheme.typography.labelMedium,
                color = contentColor,
            )
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
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DaysBadge(deltaMillis = item.deltaMillis, color = accentColor)
                Column(modifier = Modifier.weight(1f)) {
                    AdptText(
                        text = item.name,
                        style = AdptTheme.typography.titleMedium,
                    )
                    AdptText(
                        text = item.deltaMillis?.toDaysText() ?: "Not in stock",
                        style = AdptTheme.typography.bodySmall,
                        color = colors.onSurface.copy(alpha = 0.6f),
                    )
                }
                if (item.isInShoppingList) {
                    InListBadge()
                } else {
                    AdptIconButton(onClick = onAddToShoppingList) {
                        AdptIcon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Add to shopping list",
                        )
                    }
                }
                AdptIconButton(onClick = onIgnore) {
                    AdptIcon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Ignore item",
                    )
                }
            }
        }
    }
}

@Composable
private fun InListBadge() {
    val colors = AdptTheme.colors
    AdptSurface(
        color = colors.accentMuted,
        shape = AdptShapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AdptIcon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = colors.accent,
            )
            AdptText(
                text = "In list",
                style = AdptTheme.typography.labelSmall,
                color = colors.accent,
            )
        }
    }
}

@Composable
private fun DaysBadge(deltaMillis: Long?, color: Color) {
    val label = when {
        deltaMillis == null -> "--"
        deltaMillis <= 0 -> "0d"
        else -> "${deltaMillis / MILLIS_PER_DAY}d"
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AdptText(
            text = label,
            style = AdptTheme.typography.labelMedium,
            color = color,
        )
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
