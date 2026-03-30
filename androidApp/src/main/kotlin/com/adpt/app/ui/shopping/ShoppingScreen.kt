package com.adpt.app.ui.shopping

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.adpt.shared.model.ShoppingListStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    navController: NavController,
    viewModel: ShoppingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingError by viewModel.pendingError.collectAsStateWithLifecycle()

    pendingError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearPendingError() },
            title = { Text("Could Not Update Shopping List") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPendingError() }) { Text("OK") }
            },
        )
    }
    var showEmptyConfirm by remember { mutableStateOf(false) }
    var purchasingItem by remember { mutableStateOf<ShoppingItemUiModel?>(null) }
    var removingItem by remember { mutableStateOf<ShoppingItemUiModel?>(null) }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text("Empty Shopping List") },
            text = { Text("Remove all items from the shopping list?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.handleIntent(ShoppingIntent.EmptyList)
                    showEmptyConfirm = false
                }) { Text("Empty") }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) { Text("Cancel") }
            },
        )
    }

    removingItem?.let { item ->
        RemoveConfirmDialog(
            itemName = item.name,
            onDismiss = { removingItem = null },
            onConfirm = {
                viewModel.handleIntent(ShoppingIntent.RemoveEntry(item.entryId))
                removingItem = null
            },
        )
    }

    purchasingItem?.let { item ->
        PurchaseDialog(
            itemName = item.name,
            onDismiss = { purchasingItem = null },
            onConfirm = { amount ->
                viewModel.handleIntent(ShoppingIntent.MarkAsPurchased(item.entryId, item.itemId, amount))
                purchasingItem = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping") },
                actions = {
                    IconButton(onClick = { showEmptyConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Empty list")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("items?selectionMode=true") }) {
                Icon(Icons.Default.Add, contentDescription = "Add item to shopping list")
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            ShoppingUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is ShoppingUiState.Success -> if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
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
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.entryId }) { item ->
                        ShoppingItemCard(
                            item = item,
                            onMarkAsPurchased = { purchasingItem = item },
                            onRemove = { removingItem = item },
                        )
                    }
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.handleIntent(ShoppingIntent.ClearList) },
                            ) {
                                Text("Clear Purchased")
                            }
                        }
                    }
                }
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = item.status)
                if (item.status == ShoppingListStatus.Purchased) {
                    item.purchasedQuantity?.let { qty ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Qty: $qty",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item.depletionLabel?.let { label ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (item.status == ShoppingListStatus.Pending) {
                IconButton(onClick = onMarkAsPurchased) {
                    Icon(Icons.Default.Check, contentDescription = "Mark as purchased")
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ShoppingListStatus) {
    val bgColor = when (status) {
        ShoppingListStatus.Pending -> MaterialTheme.colorScheme.secondaryContainer
        ShoppingListStatus.Purchased -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (status) {
        ShoppingListStatus.Pending -> MaterialTheme.colorScheme.onSecondaryContainer
        ShoppingListStatus.Purchased -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(color = bgColor, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

@Composable
private fun RemoveConfirmDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Item") },
        text = { Text("Remove $itemName from the shopping list?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Remove") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun PurchaseDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit,
) {
    var quantity by remember { mutableStateOf("") }
    var quantityError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark as Purchased") },
        text = {
            Column {
                Text("How much $itemName did you buy?")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it; quantityError = null },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = quantityError != null,
                    supportingText = quantityError?.let { { Text(it) } },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = quantity.toDoubleOrNull()
                when {
                    quantity.isBlank() -> quantityError = "Quantity is required"
                    amount == null -> quantityError = "Enter a valid number"
                    amount <= 0.0 -> quantityError = "Must be greater than 0"
                    else -> onConfirm(amount)
                }
            }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
