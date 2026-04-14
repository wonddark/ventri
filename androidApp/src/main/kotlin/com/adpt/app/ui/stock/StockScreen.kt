package com.ventri.app.ui.stock

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
import com.ventri.app.ui.components.AnimatedListItem
import com.ventri.app.ui.design.VentriShapes
import com.ventri.app.ui.design.VentriTheme
import com.ventri.app.ui.design.LocalNavBarHeight
import com.ventri.app.ui.design.components.VentriCard
import com.ventri.app.ui.design.components.VentriDialog
import com.ventri.app.ui.design.components.VentriIcon
import com.ventri.app.ui.design.components.VentriProgressIndicator
import com.ventri.app.ui.design.components.VentriText
import com.ventri.app.ui.design.components.VentriTextButton
import com.ventri.app.ui.design.components.VentriTopBar
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
            VentriDialog(
                onDismissRequest = { depletingItem = null },
                title = { VentriText("Mark as depleted?", style = VentriTheme.typography.titleSmall) },
                text = { VentriText("I'll calculate your consumption rate based on how long this lasted. This helps me predict when you'll run out next time.") },
                confirmButton = {
                    VentriTextButton(onClick = {
                        viewModel.markDepleted(item.id, updateRate = true)
                        depletingItem = null
                    }) { VentriText("Mark depleted", color = VentriTheme.colors.accent) }
                },
                dismissButton = {
                    VentriTextButton(onClick = { depletingItem = null }) {
                        VentriText("Cancel", color = VentriTheme.colors.onSurface.copy(alpha = 0.6f))
                    }
                },
            )
        } else {
            VentriDialog(
                onDismissRequest = { depletingItem = null },
                title = { VentriText("Update consumption rate?", style = VentriTheme.typography.titleSmall) },
                text = { VentriText("Would you like me to recalculate the consumption rate based on actual usage since the last purchase?") },
                confirmButton = {
                    VentriTextButton(onClick = {
                        viewModel.markDepleted(item.id, updateRate = true)
                        depletingItem = null
                    }) { VentriText("Yes", color = VentriTheme.colors.accent) }
                },
                dismissButton = {
                    VentriTextButton(onClick = {
                        viewModel.markDepleted(item.id, updateRate = false)
                        depletingItem = null
                    }) { VentriText("No", color = VentriTheme.colors.onSurface.copy(alpha = 0.6f)) }
                },
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(VentriTheme.colors.background)) {
        when (val state = uiState) {
            StockUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(top = topBarHeightDp),
                contentAlignment = Alignment.Center,
            ) { VentriProgressIndicator() }

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
                        VentriText(
                            text = "${state.items.size} item${if (state.items.size != 1) "s" else ""} in stock",
                            style = VentriTheme.typography.bodySmall,
                            color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
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
        VentriTopBar(
            title = { VentriText("Stock", style = VentriTheme.typography.titleLarge) },
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
        VentriIcon(
            imageVector = Icons.Default.Inventory,
            contentDescription = null,
            tint = VentriTheme.colors.accent,
            modifier = Modifier
                .background(VentriTheme.colors.accentMuted, shape = VentriShapes.pill)
                .padding(20.dp),
        )
        Spacer(Modifier.height(20.dp))
        VentriText(
            text = "Nothing in stock",
            style = VentriTheme.typography.titleLarge,
            color = VentriTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        VentriText(
            text = "This is where I keep track of what you currently have at home and how long it will last.",
            style = VentriTheme.typography.bodyMedium,
            color = VentriTheme.colors.onSurface.copy(alpha = 0.6f),
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
    VentriCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
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
                VentriText(
                    body,
                    style = VentriTheme.typography.bodySmall,
                    color = VentriTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun StockItemCard(item: StockItemUiModel, onMarkDepleted: () -> Unit) {
    val colors = VentriTheme.colors
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 100.dp.toPx() }
    val offsetX = remember(item.id) { Animatable(0f) }
    var cardWidth by remember { mutableIntStateOf(0) }

    LaunchedEffect(item.id) { offsetX.snapTo(0f) }

    Box(modifier = Modifier.fillMaxWidth().onSizeChanged { cardWidth = it.width }) {
        // Right swipe background — mark as depleted
        Box(modifier = Modifier.matchParentSize().clip(VentriShapes.card)) {
            Box(
                modifier = Modifier.matchParentSize()
                    .graphicsLayer { alpha = (offsetX.value / thresholdPx).coerceIn(0f, 1f) }
                    .background(colors.warning),
                contentAlignment = Alignment.CenterStart,
            ) {
                VentriIcon(
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

        VentriCard(
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
                    VentriText(item.name, style = VentriTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    VentriText(
                        text = item.daysRemainingLabel,
                        style = VentriTheme.typography.bodySmall,
                        color = colors.onSurface.copy(alpha = 0.5f),
                    )
                }
                VentriText(
                    text = "${item.remainingQuantity.formatQuantity()} ${item.unit.name}",
                    style = VentriTheme.typography.bodyMedium,
                    color = colors.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun Double.formatQuantity(): String =
    if (this % 1.0 == 0.0) toLong().toString() else "%.1f".format(this)
