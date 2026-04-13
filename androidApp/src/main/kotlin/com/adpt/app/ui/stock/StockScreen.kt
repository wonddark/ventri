package com.adpt.app.ui.stock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.outlined.RemoveShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adpt.app.ui.components.AnimatedListItem
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.LocalNavBarHeight
import com.adpt.app.ui.design.components.AdptCard
import com.adpt.app.ui.design.components.AdptDialog
import com.adpt.app.ui.design.components.AdptIcon
import com.adpt.app.ui.design.components.AdptProgressIndicator
import com.adpt.app.ui.design.components.AdptText
import com.adpt.app.ui.design.components.AdptTextButton
import com.adpt.app.ui.design.components.AdptTopBar
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun StockScreen(viewModel: StockViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navBarHeight = LocalNavBarHeight.current
    val density = LocalDensity.current

    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    var depletingItem by remember { mutableStateOf<StockItemUiModel?>(null) }

    depletingItem?.let { item ->
        if (!item.rateKnown) {
            AdptDialog(
                onDismissRequest = { depletingItem = null },
                title = { AdptText("Mark as depleted?", style = AdptTheme.typography.titleSmall) },
                text = { AdptText("I'll calculate your consumption rate based on how long this lasted. This helps me predict when you'll run out next time.") },
                confirmButton = {
                    AdptTextButton(onClick = {
                        viewModel.markDepleted(item.id, updateRate = true)
                        depletingItem = null
                    }) { AdptText("Mark depleted", color = AdptTheme.colors.accent) }
                },
                dismissButton = {
                    AdptTextButton(onClick = { depletingItem = null }) {
                        AdptText("Cancel", color = AdptTheme.colors.onSurface.copy(alpha = 0.6f))
                    }
                },
            )
        } else {
            AdptDialog(
                onDismissRequest = { depletingItem = null },
                title = { AdptText("Update consumption rate?", style = AdptTheme.typography.titleSmall) },
                text = { AdptText("Would you like me to recalculate the consumption rate based on actual usage since the last purchase?") },
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
    }

    Box(modifier = Modifier.fillMaxSize().background(AdptTheme.colors.background)) {
        when (val state = uiState) {
            StockUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(top = topBarHeightDp),
                contentAlignment = Alignment.Center,
            ) { AdptProgressIndicator() }

            is StockUiState.Success -> if (state.items.isEmpty()) {
                StockEmptyState(
                    topPadding = topBarHeightDp,
                    bottomPadding = navBarHeight + 16.dp,
                )
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
                        AdptText(
                            text = "${state.items.size} item${if (state.items.size != 1) "s" else ""} in stock",
                            style = AdptTheme.typography.bodySmall,
                            color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(state.items, key = { it.id }) { item ->
                        AnimatedListItem(index = state.items.indexOf(item)) {
                            StockItemCard(item = item, onMarkDepleted = { depletingItem = item })
                        }
                    }
                }
            }
        }

        // Pinned top bar overlay
        AdptTopBar(
            title = { AdptText("Stock", style = AdptTheme.typography.titleLarge) },
            modifier = Modifier.onSizeChanged { topBarHeightPx = it.height },
        )
    }
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
        AdptIcon(
            imageVector = Icons.Default.Inventory,
            contentDescription = null,
            tint = AdptTheme.colors.accent,
            modifier = Modifier
                .background(AdptTheme.colors.accentMuted, shape = AdptShapes.pill)
                .padding(20.dp),
        )
        Spacer(Modifier.height(20.dp))
        AdptText(
            text = "Nothing in stock",
            style = AdptTheme.typography.titleLarge,
            color = AdptTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        AdptText(
            text = "This is where I keep track of what you currently have at home and how long it will last.",
            style = AdptTheme.typography.bodyMedium,
            color = AdptTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(32.dp))
        StockTipCard(
            icon = Icons.Default.ShoppingCartCheckout,
            title = "Items land here after shopping",
            body = "When you mark something as purchased in your shopping list, I'll start tracking it here automatically.",
        )
        Spacer(Modifier.height(12.dp))
        StockTipCard(
            icon = Icons.Default.Schedule,
            title = "I track how long things last",
            body = "Based on the quantity you bought and your consumption rate, I'll tell you exactly how many days each item will last.",
        )
        Spacer(Modifier.height(12.dp))
        StockTipCard(
            icon = Icons.Outlined.RemoveShoppingCart,
            title = "Mark items as depleted",
            body = "Run out of something? Tap the cart icon to remove it from stock. I'll ask if you want me to recalibrate the consumption rate based on actual usage.",
        )
    }
}

@Composable
private fun StockTipCard(icon: ImageVector, title: String, body: String) {
    AdptCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AdptIcon(
                imageVector = icon,
                contentDescription = null,
                tint = AdptTheme.colors.accent,
                modifier = Modifier
                    .background(AdptTheme.colors.accentMuted, shape = AdptShapes.small)
                    .padding(8.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AdptText(title, style = AdptTheme.typography.titleSmall)
                AdptText(
                    body,
                    style = AdptTheme.typography.bodySmall,
                    color = AdptTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun StockItemCard(item: StockItemUiModel, onMarkDepleted: () -> Unit) {
    val colors = AdptTheme.colors
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 100.dp.toPx() }
    val offsetX = remember(item.id) { Animatable(0f) }
    var cardWidth by remember { mutableIntStateOf(0) }

    LaunchedEffect(item.id) { offsetX.snapTo(0f) }

    Box(modifier = Modifier.fillMaxWidth().onSizeChanged { cardWidth = it.width }) {
        // Right swipe background — mark as depleted
        Box(modifier = Modifier.matchParentSize().clip(AdptShapes.card)) {
            Box(
                modifier = Modifier.matchParentSize()
                    .graphicsLayer { alpha = (offsetX.value / thresholdPx).coerceIn(0f, 1f) }
                    .background(colors.warning),
                contentAlignment = Alignment.CenterStart,
            ) {
                AdptIcon(
                    imageVector = Icons.Outlined.RemoveShoppingCart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(start = 24.dp).graphicsLayer {
                        val p = (offsetX.value / thresholdPx).coerceIn(0f, 1f)
                        scaleX = 0.6f + 0.4f * p
                        scaleY = scaleX
                    },
                )
            }
        }

        AdptCard(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(item.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value > thresholdPx) {
                                    offsetX.animateTo(
                                        cardWidth.toFloat(),
                                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                                    )
                                    onMarkDepleted()
                                } else {
                                    offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceIn(0f, cardWidth.toFloat()))
                            }
                        },
                    )
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AdptText(item.name, style = AdptTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    AdptText(
                        text = item.daysRemainingLabel,
                        style = AdptTheme.typography.bodySmall,
                        color = colors.onSurface.copy(alpha = 0.5f),
                    )
                }
                AdptText(
                    text = "${item.remainingQuantity.formatQuantity()} ${item.unit.name}",
                    style = AdptTheme.typography.bodyMedium,
                    color = colors.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun Double.formatQuantity(): String =
    if (this % 1.0 == 0.0) toLong().toString() else "%.1f".format(this)
