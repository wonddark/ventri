package com.ventri.app.ui.shopping

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ventri.app.R
import com.ventri.app.ui.util.displayName
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ventri.app.ui.components.AnimatedListItem
import com.ventri.app.ui.design.VentriShapes
import com.ventri.app.ui.design.VentriTheme
import com.ventri.app.ui.design.LocalBarsVisible
import com.ventri.app.ui.design.LocalNavBarHeight
import com.ventri.app.ui.design.components.VentriCard
import com.ventri.app.ui.design.components.VentriChip
import com.ventri.app.ui.design.components.VentriDialog
import com.ventri.app.ui.design.components.VentriFab
import com.ventri.app.ui.design.components.VentriIcon
import com.ventri.app.ui.design.components.VentriIconButton
import com.ventri.app.ui.design.components.VentriOutlinedButton
import com.ventri.app.ui.design.components.VentriProgressIndicator
import com.ventri.app.ui.design.components.VentriText
import com.ventri.app.ui.design.components.VentriTextButton
import com.ventri.app.ui.design.components.VentriTextField
import com.ventri.app.ui.design.components.VentriTopBar
import com.ventri.shared.model.ShoppingListStatus
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

    pendingError?.let { _ ->
        VentriDialog(
            onDismissRequest = { viewModel.clearPendingError() },
            title = {
                VentriText(
                    stringResource(R.string.shopping_error_title),
                    style = VentriTheme.typography.titleSmall
                )
            },
            text = { VentriText(stringResource(R.string.shopping_error_body)) },
            confirmButton = {
                VentriTextButton(onClick = { viewModel.clearPendingError() }) {
                    VentriText(stringResource(R.string.common_ok), color = VentriTheme.colors.accent)
                }
            },
        )
    }

    var showEmptyConfirm by remember { mutableStateOf(false) }
    var purchasingItem by remember { mutableStateOf<ShoppingItemUiModel?>(null) }

    if (showEmptyConfirm) {
        VentriDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = {
                VentriText(
                    stringResource(R.string.shopping_empty_list_title),
                    style = VentriTheme.typography.titleSmall
                )
            },
            text = { VentriText(stringResource(R.string.shopping_empty_list_confirm)) },
            confirmButton = {
                VentriTextButton(onClick = {
                    viewModel.handleIntent(ShoppingIntent.EmptyList)
                    showEmptyConfirm = false
                }) { VentriText(stringResource(R.string.shopping_empty_confirm_btn), color = VentriTheme.colors.accent) }
            },
            dismissButton = {
                VentriTextButton(onClick = { showEmptyConfirm = false }) {
                    VentriText(
                        stringResource(R.string.common_cancel),
                        color = VentriTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
        )
    }

    purchasingItem?.let { item ->
        var quantity by remember { mutableStateOf("") }
        var quantityError by remember { mutableStateOf<String?>(null) }
        val errRequired = stringResource(R.string.shopping_quantity_required)
        val errInvalid = stringResource(R.string.shopping_quantity_invalid)
        val errPositive = stringResource(R.string.shopping_quantity_must_be_positive)
        VentriDialog(
            onDismissRequest = { purchasingItem = null },
            title = {
                VentriText(
                    stringResource(R.string.shopping_mark_purchased_title),
                    style = VentriTheme.typography.titleSmall
                )
            },
            text = {
                Column {
                    VentriText(stringResource(R.string.shopping_mark_purchased_body, item.name))
                    Spacer(Modifier.height(8.dp))
                    VentriTextField(
                        value = quantity,
                        onValueChange = { quantity = it; quantityError = null },
                        label = stringResource(R.string.shopping_quantity_label, item.unit.displayName()),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = quantityError != null,
                        supportingText = quantityError,
                    )
                }
            },
            confirmButton = {
                VentriTextButton(onClick = {
                    val amount = quantity.toDoubleOrNull()
                    when {
                        quantity.isBlank() -> quantityError = errRequired
                        amount == null -> quantityError = errInvalid
                        amount <= 0.0 -> quantityError = errPositive
                        else -> {
                            viewModel.handleIntent(
                                ShoppingIntent.MarkAsPurchased(
                                    item.entryId,
                                    item.itemId,
                                    amount
                                )
                            )
                            purchasingItem = null
                        }
                    }
                }) { VentriText(stringResource(R.string.common_confirm), color = VentriTheme.colors.accent) }
            },
            dismissButton = {
                VentriTextButton(onClick = { purchasingItem = null }) {
                    VentriText(
                        stringResource(R.string.common_cancel),
                        color = VentriTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VentriTheme.colors.background)
    ) {
        when (val state = uiState) {
            ShoppingUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topBarHeightDp),
                contentAlignment = Alignment.Center,
            ) { VentriProgressIndicator() }

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
                        VentriText(
                            text = pluralStringResource(R.plurals.item_count, state.items.size, state.items.size),
                            style = VentriTheme.typography.bodySmall,
                            color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(
                                top = 8.dp,
                                bottom = 4.dp
                            ),
                        )
                    }
                    items(state.items, key = { it.entryId }) { item ->
                        AnimatedListItem(index = state.items.indexOf(item)) {
                            RefillItemCard(
                                item = item,
                                onMarkAsPurchased = { purchasingItem = item },
                                onRemove = {
                                    viewModel.handleIntent(
                                        ShoppingIntent.RemoveEntry(item.entryId)
                                    )
                                },
                            )
                        }
                    }
                    item(key = "clear") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            VentriOutlinedButton(onClick = {
                                viewModel.handleIntent(
                                    ShoppingIntent.ClearList
                                )
                            }) {
                                VentriText(
                                    stringResource(R.string.shopping_clear_purchased),
                                    color = VentriTheme.colors.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pinned top bar overlay
        VentriTopBar(
            title = {
                VentriText(
                    stringResource(R.string.shopping_title),
                    style = VentriTheme.typography.titleLarge
                )
            },
            actions = {
                VentriIconButton(onClick = { showEmptyConfirm = true }) {
                    VentriIcon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.shopping_empty_list_cd)
                    )
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
            VentriFab(onClick = { navController.navigate("items?selectionMode=true") }) {
                VentriIcon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = VentriTheme.colors.onAccent
                )
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
            .padding(
                top = topPadding,
                bottom = bottomPadding,
                start = 24.dp,
                end = 24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        VentriIcon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = VentriTheme.colors.accent,
            modifier = Modifier
                .background(
                    VentriTheme.colors.accentMuted,
                    shape = VentriShapes.pill
                )
                .padding(20.dp),
        )
        Spacer(Modifier.height(20.dp))
        VentriText(
            text = stringResource(R.string.shopping_empty_title),
            style = VentriTheme.typography.titleLarge,
            color = VentriTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        VentriText(
            text = stringResource(R.string.shopping_empty_body),
            style = VentriTheme.typography.bodyMedium,
            color = VentriTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(32.dp))
        RefillsTipCard(
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            title = stringResource(R.string.shopping_onboarding_add_title),
            body = stringResource(R.string.shopping_onboarding_add_body),
        )
        Spacer(Modifier.height(12.dp))
        RefillsTipCard(
            icon = Icons.Default.Check,
            title = stringResource(R.string.shopping_onboarding_log_title),
            body = stringResource(R.string.shopping_onboarding_log_body),
        )
        Spacer(Modifier.height(12.dp))
        RefillsTipCard(
            icon = Icons.Default.AvTimer,
            title = stringResource(R.string.shopping_onboarding_alert_title),
            body = stringResource(R.string.shopping_onboarding_alert_body),
        )
    }
}

@Composable
private fun RefillsTipCard(icon: ImageVector, title: String, body: String) {
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
                    .background(
                        VentriTheme.colors.accentMuted,
                        shape = VentriShapes.small
                    )
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
private fun ShoppingDepletionLabel.toText(): String = when (this) {
    ShoppingDepletionLabel.AlreadyDepleted -> stringResource(R.string.shopping_already_depleted)
    is ShoppingDepletionLabel.WillLast -> pluralStringResource(R.plurals.shopping_will_last, days.toInt(), days)
}

@Composable
private fun RefillItemCard(
    item: ShoppingItemUiModel,
    onMarkAsPurchased: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = VentriTheme.colors
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 100.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var cardWidth by remember { mutableIntStateOf(0) }

    LaunchedEffect(item.entryId, item.status) { offsetX.snapTo(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { cardWidth = it.width }) {
        // Background layers
        Box(modifier = Modifier
            .matchParentSize()
            .clip(VentriShapes.card)) {
            // Right swipe — mark as purchased (green, only for pending)
            if (item.status == ShoppingListStatus.Pending) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha =
                                (offsetX.value / thresholdPx).coerceIn(0f, 1f)
                        }
                        .background(colors.ok),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    VentriIcon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .graphicsLayer {
                                val p = (offsetX.value / thresholdPx).coerceIn(
                                    0f,
                                    1f
                                )
                                scaleX = 0.6f + 0.4f * p
                                scaleY = scaleX
                            },
                    )
                }
            }
            // Left swipe — remove (red)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = (-offsetX.value / thresholdPx).coerceIn(0f, 1f)
                    }
                    .background(colors.critical),
                contentAlignment = Alignment.CenterEnd,
            ) {
                VentriIcon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .graphicsLayer {
                            val p =
                                (-offsetX.value / thresholdPx).coerceIn(0f, 1f)
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
                .pointerInput(item.entryId, item.status) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value > thresholdPx && item.status == ShoppingListStatus.Pending -> {
                                        offsetX.animateTo(
                                            0f,
                                            spring(stiffness = Spring.StiffnessMedium)
                                        )
                                        onMarkAsPurchased()
                                    }

                                    offsetX.value < -thresholdPx -> {
                                        offsetX.animateTo(
                                            -cardWidth.toFloat(),
                                            spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            ),
                                        )
                                        onRemove()
                                    }

                                    else -> offsetX.animateTo(
                                        0f,
                                        spring(stiffness = Spring.StiffnessMedium)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    0f,
                                    spring(stiffness = Spring.StiffnessMedium)
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val maxRight =
                                    if (item.status == ShoppingListStatus.Pending) cardWidth.toFloat() else 0f
                                offsetX.snapTo(
                                    (offsetX.value + dragAmount).coerceIn(
                                        -cardWidth.toFloat(),
                                        maxRight
                                    )
                                )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VentriText(item.name, style = VentriTheme.typography.titleMedium)
                        val (chipBg, chipFg) = when (item.status) {
                            ShoppingListStatus.Pending -> colors.warningContainer to colors.onWarningContainer
                            ShoppingListStatus.Purchased -> colors.criticalContainer to colors.onCriticalContainer
                        }
                        VentriChip(containerColor = chipBg) {
                            VentriText(item.status.name, style = VentriTheme.typography.labelSmall, color = chipFg)
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (item.status == ShoppingListStatus.Purchased) {
                            item.purchasedQuantity?.let { qty ->
                                VentriText(
                                    "${qty.formatQuantity()} ${item.unit.displayName()}",
                                    style = VentriTheme.typography.bodySmall,
                                    color = colors.onSurface.copy(alpha = 0.5f),
                                )
                            }
                            item.depletionLabel?.let { label ->
                                VentriText(
                                    label.toText(),
                                    style = VentriTheme.typography.bodySmall,
                                    color = colors.accent.copy(alpha = 0.7f))
                            }
                        } else {
                            VentriText(
                                item.unit.displayName(),
                                style = VentriTheme.typography.bodySmall,
                                color = colors.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }

                }
            }
        }
    }
}

private fun Double.formatQuantity(): String =
    if (this % 1.0 == 0.0) toLong().toString() else "%.1f".format(this)
