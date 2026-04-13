package com.adpt.app.ui.shopping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.adpt.app.ui.components.AnimatedListItem
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.LocalBarsVisible
import com.adpt.app.ui.design.LocalNavBarHeight
import com.adpt.app.ui.design.components.AdptCard
import com.adpt.app.ui.design.components.AdptChip
import com.adpt.app.ui.design.components.AdptDialog
import com.adpt.app.ui.design.components.AdptFab
import com.adpt.app.ui.design.components.AdptIcon
import com.adpt.app.ui.design.components.AdptIconButton
import com.adpt.app.ui.design.components.AdptOutlinedButton
import com.adpt.app.ui.design.components.AdptProgressIndicator
import com.adpt.app.ui.design.components.AdptText
import com.adpt.app.ui.design.components.AdptTextField
import com.adpt.app.ui.design.components.AdptTextButton
import com.adpt.app.ui.design.components.AdptTopBar
import com.adpt.shared.model.ShoppingListStatus

@Composable
fun ShoppingScreen(
    navController: NavController,
    viewModel: ShoppingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingError by viewModel.pendingError.collectAsStateWithLifecycle()
    val navBarHeight = LocalNavBarHeight.current
    val barsVisible = LocalBarsVisible.current
    val density = LocalDensity.current

    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    pendingError?.let { error ->
        AdptDialog(
            onDismissRequest = { viewModel.clearPendingError() },
            title = { AdptText("Could Not Update Refill List", style = AdptTheme.typography.titleSmall) },
            text = { AdptText(error) },
            confirmButton = {
                AdptTextButton(onClick = { viewModel.clearPendingError() }) {
                    AdptText("OK", color = AdptTheme.colors.accent)
                }
            },
        )
    }

    var showEmptyConfirm by remember { mutableStateOf(false) }
    var purchasingItem by remember { mutableStateOf<ShoppingItemUiModel?>(null) }

    if (showEmptyConfirm) {
        AdptDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { AdptText("Empty Refills", style = AdptTheme.typography.titleSmall) },
            text = { AdptText("Remove all items from your refill list?") },
            confirmButton = {
                AdptTextButton(onClick = {
                    viewModel.handleIntent(ShoppingIntent.EmptyList)
                    showEmptyConfirm = false
                }) { AdptText("Empty", color = AdptTheme.colors.accent) }
            },
            dismissButton = {
                AdptTextButton(onClick = { showEmptyConfirm = false }) {
                    AdptText("Cancel", color = AdptTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            },
        )
    }

    purchasingItem?.let { item ->
        var quantity by remember { mutableStateOf("") }
        var quantityError by remember { mutableStateOf<String?>(null) }
        AdptDialog(
            onDismissRequest = { purchasingItem = null },
            title = { AdptText("Mark as Purchased", style = AdptTheme.typography.titleSmall) },
            text = {
                Column {
                    AdptText("How much ${item.name} did you buy?")
                    Spacer(Modifier.height(8.dp))
                    AdptTextField(
                        value = quantity,
                        onValueChange = { quantity = it; quantityError = null },
                        label = "Quantity",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = quantityError != null,
                        supportingText = quantityError,
                    )
                }
            },
            confirmButton = {
                AdptTextButton(onClick = {
                    val amount = quantity.toDoubleOrNull()
                    when {
                        quantity.isBlank() -> quantityError = "Quantity is required"
                        amount == null -> quantityError = "Enter a valid number"
                        amount <= 0.0 -> quantityError = "Must be greater than 0"
                        else -> {
                            viewModel.handleIntent(ShoppingIntent.MarkAsPurchased(item.entryId, item.itemId, amount))
                            purchasingItem = null
                        }
                    }
                }) { AdptText("Confirm", color = AdptTheme.colors.accent) }
            },
            dismissButton = {
                AdptTextButton(onClick = { purchasingItem = null }) {
                    AdptText("Cancel", color = AdptTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(AdptTheme.colors.background)) {
        when (val state = uiState) {
            ShoppingUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(top = topBarHeightDp),
                contentAlignment = Alignment.Center,
            ) { AdptProgressIndicator() }

            is ShoppingUiState.Success -> if (state.items.isEmpty()) {
                RefillsEmptyState(
                    topPadding = topBarHeightDp,
                    bottomPadding = navBarHeight + 72.dp,
                )
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
                    item(key = "header") {
                        AdptText(
                            text = "${state.items.size} item${if (state.items.size != 1) "s" else ""}",
                            style = AdptTheme.typography.bodySmall,
                            color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(state.items, key = { it.entryId }) { item ->
                        AnimatedListItem(index = state.items.indexOf(item)) {
                            RefillItemCard(
                                item = item,
                                onMarkAsPurchased = { purchasingItem = item },
                                onRemove = { viewModel.handleIntent(ShoppingIntent.RemoveEntry(item.entryId)) },
                            )
                        }
                    }
                    item(key = "clear") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AdptOutlinedButton(onClick = { viewModel.handleIntent(ShoppingIntent.ClearList) }) {
                                AdptText("Clear Purchased", color = AdptTheme.colors.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // Pinned top bar overlay
        AdptTopBar(
            title = { AdptText("Refills", style = AdptTheme.typography.titleLarge) },
            actions = {
                AdptIconButton(onClick = { showEmptyConfirm = true }) {
                    AdptIcon(Icons.Default.Delete, contentDescription = "Empty list")
                }
            },
            modifier = Modifier.onSizeChanged { topBarHeightPx = it.height },
        )

        // FAB
        AnimatedVisibility(
            visible = barsVisible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = navBarHeight + 16.dp),
        ) {
            AdptFab(onClick = { navController.navigate("items?selectionMode=true") }) {
                AdptIcon(Icons.Default.Add, contentDescription = null, tint = AdptTheme.colors.onAccent)
            }
        }
    }
}

@Composable
private fun RefillsEmptyState(topPadding: Dp, bottomPadding: Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topPadding, bottom = bottomPadding, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        AdptIcon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = AdptTheme.colors.accent,
            modifier = Modifier
                .background(AdptTheme.colors.accentMuted, shape = AdptShapes.pill)
                .padding(20.dp),
        )
        Spacer(Modifier.height(20.dp))
        AdptText(
            text = "Nothing to refill",
            style = AdptTheme.typography.titleLarge,
            color = AdptTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        AdptText(
            text = "This is your running list of things to buy. I'll help you keep it up to date so you never run out of what matters.",
            style = AdptTheme.typography.bodyMedium,
            color = AdptTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(32.dp))
        RefillsTipCard(
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            title = "Add what you need",
            body = "Tap + to pick items from your list and add them here. You can select multiple at once so the list is ready before you head out.",
        )
        Spacer(Modifier.height(12.dp))
        RefillsTipCard(
            icon = Icons.Default.Check,
            title = "Log when you buy something",
            body = "Tap ✓ next to an item and tell me how much you got. I'll move it to Stock right away and start tracking how long it'll last.",
        )
        Spacer(Modifier.height(12.dp))
        RefillsTipCard(
            icon = Icons.Default.AvTimer,
            title = "I'll tell you what's running low",
            body = "Keep an eye on the Overview screen — I'll flag items that are about to run out so you can add them here before it's too late.",
        )
    }
}

@Composable
private fun RefillsTipCard(icon: ImageVector, title: String, body: String) {
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
private fun RefillItemCard(
    item: ShoppingItemUiModel,
    onMarkAsPurchased: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = AdptTheme.colors
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 100.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var cardWidth by remember { mutableIntStateOf(0) }

    LaunchedEffect(item.entryId, item.status) { offsetX.snapTo(0f) }

    Box(modifier = Modifier.fillMaxWidth().onSizeChanged { cardWidth = it.width }) {
        // Background layers
        Box(modifier = Modifier.matchParentSize().clip(AdptShapes.card)) {
            // Right swipe — mark as purchased (green, only for pending)
            if (item.status == ShoppingListStatus.Pending) {
                Box(
                    modifier = Modifier.matchParentSize()
                        .graphicsLayer { alpha = (offsetX.value / thresholdPx).coerceIn(0f, 1f) }
                        .background(colors.ok),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    AdptIcon(
                        imageVector = Icons.Default.Check,
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
            // Left swipe — remove (red)
            Box(
                modifier = Modifier.matchParentSize()
                    .graphicsLayer { alpha = (-offsetX.value / thresholdPx).coerceIn(0f, 1f) }
                    .background(colors.critical),
                contentAlignment = Alignment.CenterEnd,
            ) {
                AdptIcon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(end = 24.dp).graphicsLayer {
                        val p = (-offsetX.value / thresholdPx).coerceIn(0f, 1f)
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
                .pointerInput(item.entryId, item.status) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value > thresholdPx && item.status == ShoppingListStatus.Pending -> {
                                        offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                        onMarkAsPurchased()
                                    }
                                    offsetX.value < -thresholdPx -> {
                                        offsetX.animateTo(
                                            -cardWidth.toFloat(),
                                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                                        )
                                        onRemove()
                                    }
                                    else -> offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val maxRight = if (item.status == ShoppingListStatus.Pending) cardWidth.toFloat() else 0f
                                offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-cardWidth.toFloat(), maxRight))
                            }
                        },
                    )
                },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AdptText(item.name, style = AdptTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    val (chipBg, chipFg) = when (item.status) {
                        ShoppingListStatus.Pending -> colors.warningContainer to colors.onWarningContainer
                        ShoppingListStatus.Purchased -> colors.criticalContainer to colors.onCriticalContainer
                    }
                    AdptChip(containerColor = chipBg) {
                        AdptText(item.status.name, style = AdptTheme.typography.labelSmall, color = chipFg)
                    }
                    if (item.status == ShoppingListStatus.Purchased) {
                        item.purchasedQuantity?.let { qty ->
                            Spacer(Modifier.height(2.dp))
                            AdptText("Qty: $qty", style = AdptTheme.typography.bodySmall, color = colors.onSurface.copy(alpha = 0.5f))
                        }
                        item.depletionLabel?.let { label ->
                            Spacer(Modifier.height(2.dp))
                            AdptText(label, style = AdptTheme.typography.bodySmall, color = colors.accent)
                        }
                    }
                }
            }
        }
    }
}
