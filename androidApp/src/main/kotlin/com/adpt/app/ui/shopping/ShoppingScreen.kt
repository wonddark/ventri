package com.adpt.app.ui.shopping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.adpt.app.ui.components.AnimatedListItem
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
            title = { AdptText("Could Not Update Shopping List", style = AdptTheme.typography.titleSmall) },
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
    var removingItem by remember { mutableStateOf<ShoppingItemUiModel?>(null) }

    if (showEmptyConfirm) {
        AdptDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { AdptText("Empty Shopping List", style = AdptTheme.typography.titleSmall) },
            text = { AdptText("Remove all items from the shopping list?") },
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

    removingItem?.let { item ->
        AdptDialog(
            onDismissRequest = { removingItem = null },
            title = { AdptText("Remove Item", style = AdptTheme.typography.titleSmall) },
            text = { AdptText("Remove ${item.name} from the shopping list?") },
            confirmButton = {
                AdptTextButton(onClick = {
                    viewModel.handleIntent(ShoppingIntent.RemoveEntry(item.entryId))
                    removingItem = null
                }) { AdptText("Remove", color = AdptTheme.colors.critical) }
            },
            dismissButton = {
                AdptTextButton(onClick = { removingItem = null }) {
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topBarHeightDp, bottom = navBarHeight),
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
                            ShoppingItemCard(
                                item = item,
                                onMarkAsPurchased = { purchasingItem = item },
                                onRemove = { removingItem = item },
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
            title = { AdptText("Shopping", style = AdptTheme.typography.titleLarge) },
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
private fun ShoppingItemCard(
    item: ShoppingItemUiModel,
    onMarkAsPurchased: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = AdptTheme.colors
    AdptCard(modifier = Modifier.fillMaxWidth()) {
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
            if (item.status == ShoppingListStatus.Pending) {
                AdptIconButton(onClick = onMarkAsPurchased) {
                    AdptIcon(Icons.Default.Check, contentDescription = "Mark as purchased")
                }
            }
            AdptIconButton(onClick = onRemove) {
                AdptIcon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}
