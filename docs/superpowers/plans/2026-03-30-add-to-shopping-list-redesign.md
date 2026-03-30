# Add-to-Shopping-List Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `ItemPickerDialog` modal in the Shopping screen with a navigation-based flow that reuses the Items screen in selection mode, allowing users to pick multiple known items (or add new ones) and bulk-add them to the shopping list.

**Architecture:** The `"items"` route gains an optional `selectionMode` boolean nav arg; `ItemsViewModel` reads it via `SavedStateHandle` and adds selection state + new intents; `ItemsScreen` renders checkboxes and a bottom action strip when in selection mode; `ShoppingScreen`'s FAB navigates to `"items?selectionMode=true"` instead of opening a dialog.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, SQLDelight, Kotlin Coroutines/Flow, AndroidViewModel + SavedStateHandle.

---

## File Map

| File | Change |
|---|---|
| `androidApp/.../ui/shopping/ShoppingViewModel.kt` | Remove `availableItems`, `AvailableItemUiModel`, `AddItem`/`AddItemConfirmed` intents; simplify `uiState` from `combine` to single-flow `map` |
| `androidApp/.../ui/shopping/ShoppingScreen.kt` | Accept `NavController` param; remove `ItemPickerDialog` and `showItemPicker`; FAB navigates to items |
| `androidApp/.../navigation/AppNavigation.kt` | Register `selectionMode` nav arg on items route; pass `navController` to `ShoppingScreen` and `ItemsScreen` |
| `androidApp/.../ui/items/ItemsViewModel.kt` | Add `SavedStateHandle`; add `selectionMode`, `selectedItemIds` to state; add `_navigationEvent`, `shoppingListItemIdsFlow`; restructure `uiState` to two-step combine; add `ToggleItemSelection`, `SelectionConfirmed`, `SelectionCancelled` intents; auto-select new items in selection mode |
| `androidApp/.../ui/items/ItemsScreen.kt` | Accept `NavController`; collect `navigationEvent`; update `ItemsTopBar` for selection mode; update `ItemCard` with checkbox and click-to-toggle; add `SelectionActionStrip` bottom bar |

---

## Task 1: Simplify ShoppingViewModel

Remove `availableItems`, `AvailableItemUiModel`, `AddItem`/`AddItemConfirmed` intents, and the `combine` with the available-items flow.

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/shopping/ShoppingViewModel.kt`

- [ ] **Step 1: Replace the file contents**

```kotlin
package com.adpt.app.ui.shopping

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.adpt.app.AdptApplication
import com.adpt.shared.db.SelectAllWithItem
import com.adpt.shared.model.ShoppingListStatus
import com.adpt.shared.util.clearPurchasedEntries
import com.adpt.shared.util.emptyShoppingList
import com.adpt.shared.util.markAsPurchased
import com.adpt.shared.util.removeShoppingListEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

data class ShoppingItemUiModel(
    val entryId: String,
    val itemId: String,
    val name: String,
    val status: ShoppingListStatus,
    val purchasedQuantity: Double?,
    val depletionLabel: String?,
)

sealed interface ShoppingUiState {
    data object Loading : ShoppingUiState
    data class Success(
        val items: List<ShoppingItemUiModel>,
    ) : ShoppingUiState
}

sealed interface ShoppingIntent {
    data class MarkAsPurchased(val entryId: String, val itemId: String, val amount: Double) : ShoppingIntent
    data class RemoveEntry(val entryId: String) : ShoppingIntent
    data object EmptyList : ShoppingIntent
    data object ClearList : ShoppingIntent
}

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as AdptApplication).database

    val pendingError: StateFlow<String?> =
        getApplication<AdptApplication>().pendingShoppingError.asStateFlow()

    fun clearPendingError() {
        getApplication<AdptApplication>().pendingShoppingError.value = null
    }

    val uiState: StateFlow<ShoppingUiState> =
        db.shoppingListEntryQueries.selectAllWithItem().asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows: List<SelectAllWithItem> ->
                val now = Clock.System.now().toEpochMilliseconds()
                ShoppingUiState.Success(
                    items = rows.map { row ->
                        ShoppingItemUiModel(
                            entryId = row.entryId,
                            itemId = row.item_id,
                            name = row.name,
                            status = row.status,
                            purchasedQuantity = row.purchasedQuantity,
                            depletionLabel = computeDepletionLabel(
                                consumptionRate = row.consumptionRate,
                                lastPurchasedAt = row.lastPurchasedAt,
                                purchasedQuantity = row.purchasedQuantity,
                                now = now,
                            ),
                        )
                    }
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ShoppingUiState.Loading,
            )

    fun handleIntent(intent: ShoppingIntent) {
        when (intent) {
            is ShoppingIntent.MarkAsPurchased -> viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    db.markAsPurchased(intent.entryId, intent.itemId, intent.amount)
                }
            }
            is ShoppingIntent.RemoveEntry -> viewModelScope.launch {
                withContext(Dispatchers.IO) { db.removeShoppingListEntry(intent.entryId) }
            }
            is ShoppingIntent.EmptyList -> viewModelScope.launch {
                withContext(Dispatchers.IO) { db.emptyShoppingList() }
            }
            is ShoppingIntent.ClearList -> viewModelScope.launch {
                withContext(Dispatchers.IO) { db.clearPurchasedEntries() }
            }
        }
    }

    private fun computeDepletionLabel(
        consumptionRate: Double,
        lastPurchasedAt: Long?,
        purchasedQuantity: Double?,
        now: Long,
    ): String? {
        if (consumptionRate == 0.0 || lastPurchasedAt == null || purchasedQuantity == null) return null
        val depletionDate = lastPurchasedAt + (purchasedQuantity / consumptionRate * MILLIS_PER_DAY).toLong()
        val delta = depletionDate - now
        val days = delta / MILLIS_PER_DAY
        return when {
            delta <= 0 -> "Already depleted"
            days == 1L -> "Will last 1 day"
            else -> "Will last $days days"
        }
    }
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew assembleDebug 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD FAILED` — `ShoppingScreen.kt` still references the removed `availableItems` and `AddItemConfirmed`. Proceed to Task 2.

---

## Task 2: Update ShoppingScreen

Remove `ItemPickerDialog`, `showItemPicker` state, and `availableItems` usage. Accept `NavController` and navigate on FAB tap.

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/shopping/ShoppingScreen.kt`

- [ ] **Step 1: Replace the file contents**

```kotlin
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
```

- [ ] **Step 2: Verify the Shopping side compiles**

```bash
./gradlew assembleDebug 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD FAILED` — `AppNavigation.kt` still calls the old `ShoppingScreen()` and `ItemsScreen()` without `navController`. Proceed to Task 3.

---

## Task 3: Update AppNavigation

Register the `selectionMode` nav argument on the items route and pass `navController` to both `ShoppingScreen` and `ItemsScreen`.

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/navigation/AppNavigation.kt`

- [ ] **Step 1: Replace the file contents**

```kotlin
package com.adpt.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adpt.app.AdptApplication
import com.adpt.app.ui.items.ItemsScreen
import com.adpt.app.ui.overview.OverviewScreen
import com.adpt.app.ui.shopping.ShoppingScreen
import com.adpt.app.ui.stock.StockScreen
import kotlinx.coroutines.flow.filterNotNull

private sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Overview : Screen("overview", "Overview", Icons.Default.Home)
    data object Shopping : Screen("shopping", "Shopping", Icons.Default.ShoppingCart)
    data object Stock : Screen("stock", "Stock", Icons.Default.Refresh)
    data object Items : Screen("items", "Items", Icons.Default.Menu)

    companion object {
        val tabs = listOf(Overview, Shopping, Stock, Items)
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as AdptApplication

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        app.pendingNavTarget.filterNotNull().collect { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            app.pendingNavTarget.value = null
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                Screen.tabs.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route ||
                            currentRoute == "${screen.route}?selectionMode={selectionMode}",
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Overview.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Overview.route) { OverviewScreen() }
            composable(Screen.Shopping.route) { ShoppingScreen(navController = navController) }
            composable(Screen.Stock.route) { StockScreen() }
            composable(
                route = "${Screen.Items.route}?selectionMode={selectionMode}",
                arguments = listOf(
                    navArgument("selectionMode") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                ),
            ) { ItemsScreen(navController = navController) }
        }
    }
}
```

- [ ] **Step 2: Verify the nav side compiles**

```bash
./gradlew assembleDebug 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD FAILED` — `ItemsScreen` still has no `navController` param and `ItemsViewModel` has no `SavedStateHandle`. Proceed to Task 4.

---

## Task 4: Update ItemsViewModel

Add `SavedStateHandle`, selection state, shopping-list filter flow, navigation event, and the three new intents. Restructure `uiState` to a two-step combine.

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/items/ItemsViewModel.kt`

- [ ] **Step 1: Replace the file contents**

```kotlin
package com.adpt.app.ui.items

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.adpt.app.AdptApplication
import com.adpt.shared.db.Item
import com.adpt.shared.db.SelectAllWithItem
import com.adpt.shared.model.AddToShoppingListResult
import com.adpt.shared.model.InsertItemResult
import com.adpt.shared.model.ItemPriority
import com.adpt.shared.model.ItemUnit
import com.adpt.shared.model.UpdateItemResult
import com.adpt.shared.util.addToShoppingList
import com.adpt.shared.util.deleteItem
import com.adpt.shared.util.insertItem
import com.adpt.shared.util.updateItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOrder(val label: String) {
    Priority("Priority"),
    NameAsc("Name A→Z"),
    NameDesc("Name Z→A"),
}

data class ItemUiModel(
    val id: String,
    val name: String,
    val unit: ItemUnit,
    val consumptionRate: Double,
    val priority: ItemPriority,
)

data class ItemsUiState(
    val isLoading: Boolean = true,
    val items: List<ItemUiModel> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.Priority,
    val priorityFilter: Set<ItemPriority> = emptySet(),
    val isSearchActive: Boolean = false,
    val selectionMode: Boolean = false,
    val selectedItemIds: Set<String> = emptySet(),
)

sealed interface ItemsIntent {
    data object SearchToggled : ItemsIntent
    data class SearchQueryChanged(val query: String) : ItemsIntent
    data class SortOrderChanged(val sortOrder: SortOrder) : ItemsIntent
    data class PriorityFilterToggled(val priority: ItemPriority) : ItemsIntent
    data object AddItem : ItemsIntent
    data class AddItemConfirmed(
        val name: String,
        val unit: ItemUnit,
        val priority: ItemPriority,
        val consumptionRate: Double,
    ) : ItemsIntent
    data class EditItem(val itemId: String) : ItemsIntent
    data class EditItemConfirmed(
        val id: String,
        val name: String,
        val unit: ItemUnit,
        val priority: ItemPriority,
        val consumptionRate: Double,
    ) : ItemsIntent
    data class RemoveItem(val itemId: String) : ItemsIntent
    data class AddToShoppingList(val itemId: String) : ItemsIntent
    data class ToggleItemSelection(val itemId: String) : ItemsIntent
    data object SelectionConfirmed : ItemsIntent
    data object SelectionCancelled : ItemsIntent
}

class ItemsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val db = (application as AdptApplication).database

    val selectionMode: Boolean = savedStateHandle.get<Boolean>("selectionMode") ?: false

    private val _addItemResult = MutableSharedFlow<String?>()
    val addItemResult: SharedFlow<String?> = _addItemResult.asSharedFlow()

    private val _editItemResult = MutableSharedFlow<String?>()
    val editItemResult: SharedFlow<String?> = _editItemResult.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _navigationEvent = MutableSharedFlow<Unit>()
    val navigationEvent: SharedFlow<Unit> = _navigationEvent.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.Priority)
    private val _priorityFilter = MutableStateFlow(emptySet<ItemPriority>())
    private val _isSearchActive = MutableStateFlow(false)
    private val _selectedItemIds = MutableStateFlow(emptySet<String>())

    // Step 1: filter, sort, map — same logic as before
    private val baseFlow = combine(
        db.itemQueries.selectAll().asFlow().mapToList(Dispatchers.IO),
        _searchQuery,
        _sortOrder,
        _priorityFilter,
        _isSearchActive,
    ) { items: List<Item>, query, sortOrder, priorityFilter, isSearchActive ->
        val filtered = items
            .filter { item ->
                (query.isBlank() || item.name.contains(query, ignoreCase = true)) &&
                    (priorityFilter.isEmpty() || item.priority in priorityFilter)
            }
            .let { list ->
                when (sortOrder) {
                    SortOrder.Priority -> list.sortedBy { it.priority.ordinal }
                    SortOrder.NameAsc -> list.sortedBy { it.name.lowercase() }
                    SortOrder.NameDesc -> list.sortedByDescending { it.name.lowercase() }
                }
            }
            .map { item -> ItemUiModel(item.id, item.name, item.unit, item.consumptionRate, item.priority) }
        ItemsUiState(
            isLoading = false,
            items = filtered,
            searchQuery = query,
            sortOrder = sortOrder,
            priorityFilter = priorityFilter,
            isSearchActive = isSearchActive,
            selectionMode = selectionMode,
        )
    }

    // Step 2: item IDs currently in the shopping list — used to exclude them in selection mode
    private val shoppingListItemIdsFlow = if (selectionMode) {
        db.shoppingListEntryQueries.selectAllWithItem().asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows: List<SelectAllWithItem> -> rows.map { it.item_id }.toSet() }
    } else {
        flowOf(emptySet())
    }

    val uiState: StateFlow<ItemsUiState> = combine(
        baseFlow,
        shoppingListItemIdsFlow,
        _selectedItemIds,
    ) { base, shoppingIds, selectedIds ->
        base.copy(
            items = if (selectionMode) base.items.filter { it.id !in shoppingIds } else base.items,
            selectedItemIds = selectedIds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ItemsUiState(selectionMode = selectionMode),
    )

    fun handleIntent(intent: ItemsIntent) {
        when (intent) {
            is ItemsIntent.SearchToggled -> {
                _isSearchActive.value = !_isSearchActive.value
                if (!_isSearchActive.value) _searchQuery.value = ""
            }
            is ItemsIntent.SearchQueryChanged -> _searchQuery.value = intent.query
            is ItemsIntent.SortOrderChanged -> _sortOrder.value = intent.sortOrder
            is ItemsIntent.PriorityFilterToggled -> {
                val current = _priorityFilter.value
                _priorityFilter.value = if (intent.priority in current) {
                    current - intent.priority
                } else {
                    current + intent.priority
                }
            }
            is ItemsIntent.AddItem -> Unit
            is ItemsIntent.AddItemConfirmed -> viewModelScope.launch {
                val result = withContext(Dispatchers.IO) {
                    db.itemQueries.insertItem(
                        name = intent.name,
                        unit = intent.unit,
                        priority = intent.priority,
                        consumptionRate = intent.consumptionRate,
                    )
                }
                when (result) {
                    is InsertItemResult.Success -> {
                        _addItemResult.emit(null)
                        if (selectionMode) {
                            _selectedItemIds.value = _selectedItemIds.value + result.id
                        }
                    }
                    InsertItemResult.DuplicateName ->
                        _addItemResult.emit("An item with this name already exists")
                }
            }
            is ItemsIntent.EditItem -> Unit
            is ItemsIntent.EditItemConfirmed -> viewModelScope.launch {
                val result = withContext(Dispatchers.IO) {
                    db.itemQueries.updateItem(
                        id = intent.id,
                        name = intent.name,
                        unit = intent.unit,
                        priority = intent.priority,
                        consumptionRate = intent.consumptionRate,
                    )
                }
                when (result) {
                    UpdateItemResult.Success -> _editItemResult.emit(null)
                    UpdateItemResult.DuplicateName ->
                        _editItemResult.emit("An item with this name already exists")
                }
            }
            is ItemsIntent.RemoveItem -> viewModelScope.launch {
                withContext(Dispatchers.IO) { db.itemQueries.deleteItem(intent.itemId) }
            }
            is ItemsIntent.AddToShoppingList -> viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { db.addToShoppingList(intent.itemId) }
                val message = when (result) {
                    is AddToShoppingListResult.Success -> "Added to shopping list"
                    AddToShoppingListResult.AlreadyInList -> "Already in shopping list"
                    AddToShoppingListResult.ItemNotFound -> return@launch
                }
                _snackbarMessage.emit(message)
            }
            is ItemsIntent.ToggleItemSelection -> {
                val current = _selectedItemIds.value
                _selectedItemIds.value = if (intent.itemId in current) {
                    current - intent.itemId
                } else {
                    current + intent.itemId
                }
            }
            is ItemsIntent.SelectionConfirmed -> viewModelScope.launch {
                val ids = _selectedItemIds.value
                withContext(Dispatchers.IO) {
                    ids.forEach { id -> db.addToShoppingList(id) }
                }
                _navigationEvent.emit(Unit)
            }
            is ItemsIntent.SelectionCancelled -> viewModelScope.launch {
                _navigationEvent.emit(Unit)
            }
        }
    }
}
```

- [ ] **Step 2: Verify the ViewModel compiles**

```bash
./gradlew assembleDebug 2>&1 | grep -E "error:|BUILD"
```
Expected: `BUILD FAILED` — `ItemsScreen` still has the old signature. Proceed to Task 5.

---

## Task 5: Update ItemsScreen

Accept `NavController`, collect `navigationEvent`, update `ItemsTopBar` for selection mode, add checkbox + click-to-toggle to `ItemCard`, and add the `SelectionActionStrip` bottom bar.

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/items/ItemsScreen.kt`

- [ ] **Step 1: Replace the file contents**

```kotlin
package com.adpt.app.ui.items

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.adpt.shared.model.ItemPriority
import com.adpt.shared.model.ItemUnit

@Composable
fun ItemsScreen(
    navController: NavController,
    viewModel: ItemsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItemUiModel?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { navController.popBackStack() }
    }

    if (showAddDialog) {
        ItemFormDialog(
            title = "Add Item",
            confirmLabel = "Add",
            initialItem = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, unit, priority, rate ->
                viewModel.handleIntent(ItemsIntent.AddItemConfirmed(name, unit, priority, rate))
            },
            resultFlow = viewModel.addItemResult,
            onSuccess = { showAddDialog = false },
        )
    }

    editingItem?.let { item ->
        ItemFormDialog(
            title = "Edit Item",
            confirmLabel = "Save",
            initialItem = item,
            onDismiss = { editingItem = null },
            onConfirm = { name, unit, priority, rate ->
                viewModel.handleIntent(ItemsIntent.EditItemConfirmed(item.id, name, unit, priority, rate))
            },
            resultFlow = viewModel.editItemResult,
            onSuccess = { editingItem = null },
        )
    }

    Scaffold(
        topBar = {
            ItemsTopBar(
                uiState = uiState,
                onIntent = viewModel::handleIntent,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        },
        bottomBar = {
            if (uiState.selectionMode) {
                SelectionActionStrip(
                    selectedCount = uiState.selectedItemIds.size,
                    onCancel = { viewModel.handleIntent(ItemsIntent.SelectionCancelled) },
                    onConfirm = { viewModel.handleIntent(ItemsIntent.SelectionConfirmed) },
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Nothing here to show",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        selectionMode = uiState.selectionMode,
                        isSelected = item.id in uiState.selectedItemIds,
                        onEdit = { editingItem = item },
                        onIntent = viewModel::handleIntent,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemsTopBar(
    uiState: ItemsUiState,
    onIntent: (ItemsIntent) -> Unit,
) {
    if (uiState.selectionMode) {
        TopAppBar(title = { Text("Add to shopping list") })
        return
    }

    if (uiState.isSearchActive) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        TopAppBar(
            navigationIcon = {
                IconButton(onClick = { onIntent(ItemsIntent.SearchToggled) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
                }
            },
            title = {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { onIntent(ItemsIntent.SearchQueryChanged(it)) },
                    placeholder = { Text("Search items…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { /* handled reactively */ }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            },
        )
    } else {
        var showSortMenu by remember { mutableStateOf(false) }
        var showFilterMenu by remember { mutableStateOf(false) }

        TopAppBar(
            title = { Text("Items") },
            actions = {
                IconButton(onClick = { onIntent(ItemsIntent.SearchToggled) }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(order.label)
                                        if (uiState.sortOrder == order) {
                                            Text(
                                                text = " ✓",
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onIntent(ItemsIntent.SortOrderChanged(order))
                                    showSortMenu = false
                                },
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter by priority")
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                    ) {
                        ItemPriority.entries.forEach { priority ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = priority in uiState.priorityFilter,
                                            onCheckedChange = { onIntent(ItemsIntent.PriorityFilterToggled(priority)) },
                                        )
                                        Text(priority.name)
                                    }
                                },
                                onClick = { onIntent(ItemsIntent.PriorityFilterToggled(priority)) },
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun ItemCard(
    item: ItemUiModel,
    selectionMode: Boolean,
    isSelected: Boolean,
    onEdit: () -> Unit,
    onIntent: (ItemsIntent) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val cardContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onIntent(ItemsIntent.ToggleItemSelection(item.id)) },
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.unit.name} · ${item.consumptionRate}/day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PriorityBadge(priority = item.priority)
            if (!selectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { onEdit(); showMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove") },
                            onClick = { onIntent(ItemsIntent.RemoveItem(item.id)); showMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Shopping List") },
                            onClick = { onIntent(ItemsIntent.AddToShoppingList(item.id)); showMenu = false },
                        )
                    }
                }
            }
        }
    }

    if (selectionMode) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onIntent(ItemsIntent.ToggleItemSelection(item.id)) },
        ) { cardContent() }
    } else {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) { cardContent() }
    }
}

@Composable
private fun SelectionActionStrip(
    selectedCount: Int,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onConfirm,
                enabled = selectedCount > 0,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (selectedCount == 1) "Add 1 item" else "Add $selectedCount items")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemFormDialog(
    title: String,
    confirmLabel: String,
    initialItem: ItemUiModel?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, unit: ItemUnit, priority: ItemPriority, rate: Double) -> Unit,
    resultFlow: kotlinx.coroutines.flow.SharedFlow<String?>,
    onSuccess: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialItem?.name ?: "") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUnit by rememberSaveable { mutableStateOf(initialItem?.unit ?: ItemUnit.PIECE) }
    var unitExpanded by remember { mutableStateOf(false) }
    var selectedPriority by rememberSaveable { mutableStateOf(initialItem?.priority ?: ItemPriority.Normal) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var rateText by rememberSaveable { mutableStateOf(initialItem?.consumptionRate?.toString() ?: "") }
    var rateError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        resultFlow.collect { error ->
            if (error == null) onSuccess() else nameError = error
        }
    }

    fun validate(): Boolean {
        var valid = true
        if (name.isBlank()) {
            nameError = "Name is required"
            valid = false
        }
        val rate = rateText.toDoubleOrNull()
        when {
            rateText.isBlank() -> { rateError = "Consumption rate is required"; valid = false }
            rate == null -> { rateError = "Enter a valid number"; valid = false }
            rate <= 0.0 -> { rateError = "Must be greater than 0"; valid = false }
        }
        return valid
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedUnit.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false },
                    ) {
                        ItemUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = { selectedUnit = unit; unitExpanded = false },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedPriority.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false },
                    ) {
                        ItemPriority.entries.forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority.name) },
                                onClick = { selectedPriority = priority; priorityExpanded = false },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it; rateError = null },
                    label = { Text("Consumption rate / day") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = rateError != null,
                    supportingText = rateError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (validate()) {
                    onConfirm(name.trim(), selectedUnit, selectedPriority, rateText.toDouble())
                }
            }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun PriorityBadge(priority: ItemPriority) {
    val bgColor = when (priority) {
        ItemPriority.Highest -> MaterialTheme.colorScheme.errorContainer
        ItemPriority.High -> MaterialTheme.colorScheme.tertiaryContainer
        ItemPriority.Normal -> MaterialTheme.colorScheme.secondaryContainer
        ItemPriority.Low -> MaterialTheme.colorScheme.surfaceVariant
        ItemPriority.Lowest -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (priority) {
        ItemPriority.Highest -> MaterialTheme.colorScheme.onErrorContainer
        ItemPriority.High -> MaterialTheme.colorScheme.onTertiaryContainer
        ItemPriority.Normal -> MaterialTheme.colorScheme.onSecondaryContainer
        ItemPriority.Low -> MaterialTheme.colorScheme.onSurfaceVariant
        ItemPriority.Lowest -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = Modifier.padding(end = 4.dp),
    ) {
        Text(
            text = priority.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}
```

- [ ] **Step 2: Full build — must pass green**

```bash
./gradlew assembleDebug 2>&1 | tail -5
```
Expected: `BUILD SUCCESSFUL`

---

## Task 6: Commit

- [ ] **Step 1: Stage all changed files**

```bash
git add \
  androidApp/src/main/kotlin/com/adpt/app/ui/shopping/ShoppingViewModel.kt \
  androidApp/src/main/kotlin/com/adpt/app/ui/shopping/ShoppingScreen.kt \
  androidApp/src/main/kotlin/com/adpt/app/navigation/AppNavigation.kt \
  androidApp/src/main/kotlin/com/adpt/app/ui/items/ItemsViewModel.kt \
  androidApp/src/main/kotlin/com/adpt/app/ui/items/ItemsScreen.kt
```

- [ ] **Step 2: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat: replace item picker dialog with selection mode on Items screen

- ShoppingScreen FAB navigates to items?selectionMode=true
- ItemsScreen renders checkboxes and a bottom action strip in selection mode
- Already-listed items are filtered out in selection mode
- New items added in selection mode are auto-selected
- Confirm bulk-adds all selected items; Cancel pops back without changes

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```
