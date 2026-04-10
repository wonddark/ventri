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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.adpt.app.ui.components.AnimatedListItem
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.components.AdptButton
import com.adpt.app.ui.design.components.AdptCard
import com.adpt.app.ui.design.components.AdptCheckbox
import com.adpt.app.ui.design.components.AdptChip
import com.adpt.app.ui.design.components.AdptClickableCard
import com.adpt.app.ui.design.components.AdptDialog
import com.adpt.app.ui.design.components.AdptDropdownMenu
import com.adpt.app.ui.design.components.AdptDropdownMenuItem
import com.adpt.app.ui.design.components.AdptExposedDropdown
import com.adpt.app.ui.design.components.AdptFab
import com.adpt.app.ui.design.components.AdptIcon
import com.adpt.app.ui.design.components.AdptIconButton
import com.adpt.app.ui.design.components.AdptOutlinedButton
import com.adpt.app.ui.design.components.AdptProgressIndicator
import com.adpt.app.ui.design.components.AdptScaffold
import com.adpt.app.ui.design.components.AdptSnackbarHost
import com.adpt.app.ui.design.components.AdptSurface
import com.adpt.app.ui.design.components.AdptText
import com.adpt.app.ui.design.components.AdptTextField
import com.adpt.app.ui.design.components.AdptTextButton
import com.adpt.app.ui.design.components.AdptTextFieldVariant
import com.adpt.app.ui.design.components.AdptTopBar
import com.adpt.app.ui.design.components.rememberAdptSnackbarHostState
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
    val snackbarState = rememberAdptSnackbarHostState()

    LaunchedEffect(viewModel.snackbarMessage) {
        viewModel.snackbarMessage.collect { snackbarState.showSnackbar(it) }
    }

    LaunchedEffect(viewModel.navigationEvent) {
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

    AdptScaffold(
        topBar = {
            ItemsTopBar(uiState = uiState, onIntent = viewModel::handleIntent)
        },
        snackbarHost = { AdptSnackbarHost(snackbarState) },
        floatingActionButton = {
            AdptFab(onClick = { showAddDialog = true }) {
                AdptIcon(Icons.Default.Add, contentDescription = null, tint = AdptTheme.colors.onAccent)
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
            ) { AdptProgressIndicator() }

            uiState.items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                AdptText(
                    "Nothing here to show",
                    style = AdptTheme.typography.bodyMedium,
                    color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    AnimatedListItem(
                        index = uiState.items.indexOf(item),
                        animationKey = Pair(uiState.sortOrder, uiState.priorityFilter),
                    ) {
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
}

@Composable
private fun ItemsTopBar(uiState: ItemsUiState, onIntent: (ItemsIntent) -> Unit) {
    if (uiState.selectionMode) {
        AdptTopBar(title = { AdptText("Add to shopping list", style = AdptTheme.typography.titleLarge) })
    } else if (uiState.isSearchActive) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        AdptTopBar(
            title = {
                AdptTextField(
                    value = uiState.searchQuery,
                    onValueChange = { onIntent(ItemsIntent.SearchQueryChanged(it)) },
                    placeholder = "Search items…",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    variant = AdptTextFieldVariant.Transparent,
                    modifier = Modifier.focusRequester(focusRequester),
                )
            },
            navigationIcon = {
                AdptIconButton(onClick = { onIntent(ItemsIntent.SearchToggled) }) {
                    AdptIcon(Icons.Default.ArrowBack, contentDescription = "Close search")
                }
            },
        )
    } else {
        var showSortMenu by remember { mutableStateOf(false) }
        var showFilterMenu by remember { mutableStateOf(false) }
        AdptTopBar(
            title = { AdptText("Items", style = AdptTheme.typography.titleLarge) },
            actions = {
                AdptIconButton(onClick = { onIntent(ItemsIntent.SearchToggled) }) {
                    AdptIcon(Icons.Default.Search, contentDescription = "Search")
                }
                Box {
                    AdptIconButton(onClick = { showSortMenu = true }) {
                        AdptIcon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    AdptDropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        SortOrder.entries.forEach { order ->
                            AdptDropdownMenuItem(
                                text = {
                                    AdptText(
                                        text = order.label + if (uiState.sortOrder == order) " ✓" else "",
                                        color = if (uiState.sortOrder == order) AdptTheme.colors.accent
                                                else AdptTheme.colors.onSurface,
                                    )
                                },
                                onClick = { onIntent(ItemsIntent.SortOrderChanged(order)); showSortMenu = false },
                            )
                        }
                    }
                }
                Box {
                    AdptIconButton(onClick = { showFilterMenu = true }) {
                        AdptIcon(Icons.Default.FilterList, contentDescription = "Filter by priority")
                    }
                    AdptDropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        ItemPriority.entries.forEach { priority ->
                            AdptDropdownMenuItem(
                                text = { AdptText(priority.name) },
                                onClick = { onIntent(ItemsIntent.PriorityFilterToggled(priority)) },
                                leadingIcon = {
                                    AdptCheckbox(
                                        checked = priority in uiState.priorityFilter,
                                        onCheckedChange = { onIntent(ItemsIntent.PriorityFilterToggled(priority)) },
                                    )
                                },
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                AdptCheckbox(checked = isSelected, onCheckedChange = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                AdptText(item.name, style = AdptTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                AdptText(
                    "${item.unit.name} · ${item.consumptionRate}/day",
                    style = AdptTheme.typography.bodySmall,
                    color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
            PriorityBadge(priority = item.priority)
            if (!selectionMode) {
                Box {
                    AdptIconButton(onClick = { showMenu = true }) {
                        AdptIcon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    AdptDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        AdptDropdownMenuItem(
                            text = { AdptText("Edit") },
                            onClick = { onEdit(); showMenu = false },
                        )
                        AdptDropdownMenuItem(
                            text = { AdptText("Remove") },
                            onClick = { onIntent(ItemsIntent.RemoveItem(item.id)); showMenu = false },
                        )
                        AdptDropdownMenuItem(
                            text = { AdptText("Add to Shopping List") },
                            onClick = { onIntent(ItemsIntent.AddToShoppingList(item.id)); showMenu = false },
                        )
                    }
                }
            }
        }
    }

    if (selectionMode) {
        AdptClickableCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onIntent(ItemsIntent.ToggleItemSelection(item.id)) },
        ) { cardContent() }
    } else {
        AdptCard(modifier = Modifier.fillMaxWidth()) { cardContent() }
    }
}

@Composable
private fun SelectionActionStrip(selectedCount: Int, onCancel: () -> Unit, onConfirm: () -> Unit) {
    AdptSurface(
        color = AdptTheme.colors.surface,
        shape = AdptShapes.small,
        modifier = Modifier.windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AdptOutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                AdptText("Cancel", color = AdptTheme.colors.onSurface)
            }
            AdptButton(onClick = onConfirm, enabled = selectedCount > 0, modifier = Modifier.weight(1f)) {
                AdptText(
                    text = if (selectedCount == 1) "Add 1 item" else "Add $selectedCount items",
                    color = AdptTheme.colors.onAccent,
                )
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: ItemPriority) {
    val colors = AdptTheme.colors
    val (bg, fg) = when (priority) {
        ItemPriority.Highest -> colors.criticalContainer to colors.onCriticalContainer
        ItemPriority.High -> colors.warningContainer to colors.onWarningContainer
        ItemPriority.Normal -> colors.accentMuted to colors.accent
        ItemPriority.Low, ItemPriority.Lowest -> colors.surfaceMuted to colors.onSurface.copy(alpha = 0.5f)
    }
    AdptChip(containerColor = bg, modifier = Modifier.padding(end = 4.dp)) {
        AdptText(priority.name, style = AdptTheme.typography.labelSmall, color = fg)
    }
}

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
        resultFlow.collect { error -> if (error == null) onSuccess() else nameError = error }
    }

    fun validate(): Boolean {
        var valid = true
        if (name.isBlank()) { nameError = "Name is required"; valid = false }
        val rate = rateText.toDoubleOrNull()
        when {
            rateText.isBlank() -> { rateError = "Consumption rate is required"; valid = false }
            rate == null -> { rateError = "Enter a valid number"; valid = false }
            rate <= 0.0 -> { rateError = "Must be greater than 0"; valid = false }
        }
        return valid
    }

    AdptDialog(
        onDismissRequest = onDismiss,
        title = { AdptText(title, style = AdptTheme.typography.titleSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdptTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = "Name",
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError,
                    modifier = Modifier.fillMaxWidth(),
                )
                AdptExposedDropdown(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    selectedText = selectedUnit.name,
                    label = "Unit",
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ItemUnit.entries.forEach { unit ->
                        AdptDropdownMenuItem(
                            text = { AdptText(unit.name) },
                            onClick = { selectedUnit = unit; unitExpanded = false },
                        )
                    }
                }
                AdptExposedDropdown(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = it },
                    selectedText = selectedPriority.name,
                    label = "Priority",
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ItemPriority.entries.forEach { priority ->
                        AdptDropdownMenuItem(
                            text = { AdptText(priority.name) },
                            onClick = { selectedPriority = priority; priorityExpanded = false },
                        )
                    }
                }
                AdptTextField(
                    value = rateText,
                    onValueChange = { rateText = it; rateError = null },
                    label = "Consumption rate / day",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = rateError != null,
                    supportingText = rateError,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            AdptTextButton(onClick = {
                if (validate()) onConfirm(name.trim(), selectedUnit, selectedPriority, rateText.toDouble())
            }) { AdptText(confirmLabel, color = AdptTheme.colors.accent) }
        },
        dismissButton = {
            AdptTextButton(onClick = onDismiss) {
                AdptText("Cancel", color = AdptTheme.colors.onSurface.copy(alpha = 0.6f))
            }
        },
    )
}
