package com.ventri.app.ui.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ventri.app.R
import com.ventri.app.ui.util.displayName
import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ventri.app.ui.components.AnimatedListItem
import com.ventri.app.ui.design.VentriShapes
import com.ventri.app.ui.design.VentriTheme
import com.ventri.app.ui.design.LocalBarsVisible
import com.ventri.app.ui.design.LocalNavBarHeight
import com.ventri.app.ui.design.components.VentriButton
import com.ventri.app.ui.design.components.VentriCard
import com.ventri.app.ui.design.components.VentriCheckbox
import com.ventri.app.ui.design.components.VentriChip
import com.ventri.app.ui.design.components.VentriClickableCard
import com.ventri.app.ui.design.components.VentriDialog
import com.ventri.app.ui.design.components.VentriDropdownMenu
import com.ventri.app.ui.design.components.VentriDropdownMenuItem
import com.ventri.app.ui.design.components.VentriExposedDropdown
import com.ventri.app.ui.design.components.VentriFab
import com.ventri.app.ui.design.components.VentriIcon
import com.ventri.app.ui.design.components.VentriIconButton
import com.ventri.app.ui.design.components.VentriOutlinedButton
import com.ventri.app.ui.design.components.VentriProgressIndicator
import com.ventri.app.ui.design.components.VentriSnackbarHost
import com.ventri.app.ui.design.components.VentriSurface
import com.ventri.app.ui.design.components.VentriText
import com.ventri.app.ui.design.components.VentriTextField
import com.ventri.app.ui.design.components.VentriTextButton
import com.ventri.app.ui.design.components.VentriTextFieldVariant
import com.ventri.app.ui.design.components.VentriTopBar
import com.ventri.app.ui.design.components.rememberVentriSnackbarHostState
import com.ventri.shared.model.ItemPriority
import com.ventri.shared.model.ItemUnit

@Composable
fun ItemsScreen(
    navController: NavController,
    viewModel: ItemsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTemplatePicker by rememberSaveable { mutableStateOf(viewModel.showAddOnStart && !viewModel.selectionMode) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var addFormPrefill by remember { mutableStateOf(ItemFormPrefill()) }
    var editingItem by remember { mutableStateOf<ItemUiModel?>(null) }
    var freePlanBannerDismissed by rememberSaveable { mutableStateOf(false) }
    val snackbarState = rememberVentriSnackbarHostState()
    val navBarHeight = LocalNavBarHeight.current
    val barsVisible = LocalBarsVisible.current
    val density = LocalDensity.current

    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    val context = LocalContext.current
    val errDuplicateName = stringResource(R.string.items_error_duplicate_name)
    val errLimitReached = stringResource(R.string.items_error_limit_reached, ItemsViewModel.FREE_ITEM_LIMIT)
    val snackAddedToShopping = stringResource(R.string.items_snackbar_added_to_shopping)
    val snackAlreadyInShopping = stringResource(R.string.items_snackbar_already_in_shopping)

    val addResultStrings = remember { MutableSharedFlow<String?>(extraBufferCapacity = 1) }
    val editResultStrings = remember { MutableSharedFlow<String?>(extraBufferCapacity = 1) }

    LaunchedEffect(viewModel.addItemResult) {
        viewModel.addItemResult.collect { error ->
            addResultStrings.emit(when (error) {
                null -> null
                InsertItemError.DuplicateName -> errDuplicateName
                is InsertItemError.LimitReached -> errLimitReached
            })
        }
    }

    LaunchedEffect(viewModel.editItemResult) {
        viewModel.editItemResult.collect { error ->
            editResultStrings.emit(when (error) {
                null -> null
                UpdateItemError.DuplicateName -> errDuplicateName
            })
        }
    }

    LaunchedEffect(viewModel.snackbarMessage) {
        viewModel.snackbarMessage.collect { event ->
            val msg = when (event) {
                ItemsSnackbarEvent.AddedToShopping -> snackAddedToShopping
                ItemsSnackbarEvent.AlreadyInShopping -> snackAlreadyInShopping
                is ItemsSnackbarEvent.SelectionFailed -> context.resources.getQuantityString(
                    R.plurals.items_snackbar_selection_failed, event.count, event.count
                )
            }
            snackbarState.showSnackbar(msg)
        }
    }

    LaunchedEffect(viewModel.navigationEvent) {
        viewModel.navigationEvent.collect { navController.popBackStack() }
    }

    if (showAddDialog) {
        ItemFormDialog(
            title = stringResource(R.string.items_add_title),
            confirmLabel = stringResource(R.string.items_add_confirm),
            prefill = addFormPrefill,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, unit, priority, rate ->
                viewModel.handleIntent(
                    ItemsIntent.AddItemConfirmed(
                        name,
                        unit,
                        priority,
                        rate
                    )
                )
            },
            resultFlow = addResultStrings,
            onSuccess = { showAddDialog = false },
        )
    }

    editingItem?.let { item ->
        ItemFormDialog(
            title = stringResource(R.string.items_edit_title),
            confirmLabel = stringResource(R.string.common_save),
            prefill = ItemFormPrefill(
                name = item.name,
                unit = item.unit,
                priority = item.priority,
                consumptionRate = item.consumptionRate,
            ),
            onDismiss = { editingItem = null },
            onConfirm = { name, unit, priority, rate ->
                viewModel.handleIntent(
                    ItemsIntent.EditItemConfirmed(
                        item.id,
                        name,
                        unit,
                        priority,
                        rate
                    )
                )
            },
            resultFlow = editResultStrings,
            onSuccess = { editingItem = null },
        )
    }

    // Bottom padding: in selection mode, reserve space for the SelectionActionStrip
    val selectionStripHeight = if (uiState.selectionMode) 80.dp else 0.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VentriTheme.colors.background)
    ) {
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topBarHeightDp),
                contentAlignment = Alignment.Center,
            ) { VentriProgressIndicator() }

            uiState.items.isEmpty() -> {
                val filtersActive =
                    uiState.isSearchActive || uiState.priorityFilter.isNotEmpty()
                if (filtersActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = topBarHeightDp,
                                bottom = navBarHeight
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        VentriText(
                            stringResource(R.string.items_no_match),
                            style = VentriTheme.typography.bodyMedium,
                            color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                        )
                    }
                } else {
                    ItemsEmptyState(
                        topPadding = topBarHeightDp,
                        bottomPadding = navBarHeight + 72.dp,
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = topBarHeightDp,
                    bottom = if (uiState.selectionMode) selectionStripHeight + 16.dp
                    else navBarHeight + 72.dp, // extra space for FAB when browsing
                    start = 16.dp,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!uiState.selectionMode) {
                    if (!uiState.isRegistered && !freePlanBannerDismissed) {
                        item(key = "free_plan_banner") {
                            FreePlanBanner(
                                onDismiss = { freePlanBannerDismissed = true },
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    item(key = "header") {
                        VentriText(
                            text = pluralStringResource(R.plurals.item_count, uiState.items.size, uiState.items.size),
                            style = VentriTheme.typography.bodySmall,
                            color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(
                                top = 8.dp,
                                bottom = 4.dp
                            ),
                        )
                    }
                }
                items(uiState.items, key = { it.id }) { item ->
                    AnimatedListItem(
                        index = uiState.items.indexOf(item),
                        animationKey = Pair(
                            uiState.sortOrder,
                            uiState.priorityFilter
                        ),
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

        // Pinned top bar overlay
        ItemsTopBar(
            uiState = uiState,
            onIntent = viewModel::handleIntent,
            modifier = Modifier.onSizeChanged { topBarHeightPx = it.height },
        )

        // FAB (only in browse mode)
        if (!uiState.selectionMode) {
            AnimatedVisibility(
                visible = barsVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = navBarHeight + 16.dp),
            ) {
                VentriFab(onClick = { showTemplatePicker = true }) {
                    VentriIcon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = VentriTheme.colors.onAccent
                    )
                }
            }
        }

        // SelectionActionStrip overlay (only in selection mode)
        AnimatedVisibility(
            visible = uiState.selectionMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SelectionActionStrip(
                selectedCount = uiState.selectedItemIds.size,
                onCancel = { viewModel.handleIntent(ItemsIntent.SelectionCancelled) },
                onConfirm = { viewModel.handleIntent(ItemsIntent.SelectionConfirmed) },
            )
        }

        // Snackbar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = navBarHeight + 8.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
        ) {
            VentriSnackbarHost(snackbarState)
        }
    }

    if (showTemplatePicker) {
        ItemTemplatePickerScreen(
            onTemplateSelected = { template ->
                addFormPrefill = template.toPrefill()
                showTemplatePicker = false
                showAddDialog = true
            },
            onStartFromScratch = {
                addFormPrefill = ItemFormPrefill()
                showTemplatePicker = false
                showAddDialog = true
            },
            onDismiss = { showTemplatePicker = false },
        )
    }
}

@Composable
private fun ItemsTopBar(
    uiState: ItemsUiState,
    onIntent: (ItemsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isSearchActive) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        VentriTopBar(
            title = {
                VentriTextField(
                    value = uiState.searchQuery,
                    onValueChange = { onIntent(ItemsIntent.SearchQueryChanged(it)) },
                    placeholder = stringResource(R.string.items_search_placeholder),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    variant = VentriTextFieldVariant.Transparent,
                    modifier = Modifier.focusRequester(focusRequester),
                )
            },
            navigationIcon = {
                VentriIconButton(onClick = { onIntent(ItemsIntent.SearchToggled) }) {
                    VentriIcon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.items_close_search_cd)
                    )
                }
            },
            modifier = modifier,
        )
    } else if (uiState.selectionMode) {
        VentriTopBar(
            title = {
                VentriText(
                    stringResource(R.string.items_add_to_shopping_title),
                    style = VentriTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                VentriIconButton(onClick = { onIntent(ItemsIntent.SelectionCancelled) }) {
                    VentriIcon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.items_cancel_selection_cd)
                    )
                }
            },
            actions = {
                VentriIconButton(onClick = { onIntent(ItemsIntent.SearchToggled) }) {
                    VentriIcon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.items_search_cd)
                    )
                }
            },
            modifier = modifier,
        )
    } else {
        var showSortMenu by remember { mutableStateOf(false) }
        var showFilterMenu by remember { mutableStateOf(false) }
        VentriTopBar(
            title = {
                VentriText(
                    stringResource(R.string.items_screen_title),
                    style = VentriTheme.typography.titleLarge
                )
            },
            actions = {
                VentriIconButton(onClick = { onIntent(ItemsIntent.SearchToggled) }) {
                    VentriIcon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.items_search_cd)
                    )
                }
                Box {
                    VentriIconButton(onClick = { showSortMenu = true }) {
                        VentriIcon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.items_sort_cd)
                        )
                    }
                    VentriDropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }) {
                        SortOrder.entries.forEach { order ->
                            val label = when (order) {
                                SortOrder.Priority -> stringResource(R.string.items_sort_priority)
                                SortOrder.NameAsc -> stringResource(R.string.items_sort_name_asc)
                                SortOrder.NameDesc -> stringResource(R.string.items_sort_name_desc)
                            }
                            VentriDropdownMenuItem(
                                text = {
                                    VentriText(
                                        text = label + if (uiState.sortOrder == order) " ✓" else "",
                                        color = if (uiState.sortOrder == order) VentriTheme.colors.accent
                                        else VentriTheme.colors.onSurface,
                                    )
                                },
                                onClick = {
                                    onIntent(
                                        ItemsIntent.SortOrderChanged(
                                            order
                                        )
                                    ); showSortMenu = false
                                },
                            )
                        }
                    }
                }
                Box {
                    VentriIconButton(onClick = { showFilterMenu = true }) {
                        VentriIcon(
                            Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.items_filter_priority_cd)
                        )
                    }
                    VentriDropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }) {
                        ItemPriority.entries.forEach { priority ->
                            VentriDropdownMenuItem(
                                text = { VentriText(priority.displayName()) },
                                onClick = {
                                    onIntent(
                                        ItemsIntent.PriorityFilterToggled(
                                            priority
                                        )
                                    )
                                },
                                leadingIcon = {
                                    VentriCheckbox(
                                        checked = priority in uiState.priorityFilter,
                                        onCheckedChange = {
                                            onIntent(
                                                ItemsIntent.PriorityFilterToggled(
                                                    priority
                                                )
                                            )
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            },
            modifier = modifier,
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
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        VentriDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                VentriText(
                    stringResource(R.string.items_remove_title),
                    style = VentriTheme.typography.titleSmall
                )
            },
            text = { VentriText(stringResource(R.string.items_remove_confirm, item.name)) },
            confirmButton = {
                VentriTextButton(onClick = {
                    onIntent(ItemsIntent.RemoveItem(item.id))
                    showDeleteConfirm = false
                }) { VentriText(stringResource(R.string.common_remove), color = VentriTheme.colors.critical) }
            },
            dismissButton = {
                VentriTextButton(onClick = { showDeleteConfirm = false }) {
                    VentriText(
                        stringResource(R.string.common_cancel),
                        color = VentriTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
        )
    }

    val cardContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                VentriCheckbox(checked = isSelected, onCheckedChange = null)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                VentriText(
                    item.name,
                    style = VentriTheme.typography.titleMedium
                )
                if (!selectionMode) {
                    Spacer(Modifier.height(2.dp))
                    VentriText(
                        if (item.consumptionRate != null)
                            stringResource(R.string.items_unit_rate, item.unit.displayName(), item.consumptionRate.toString())
                        else
                            item.unit.displayName(),
                        style = VentriTheme.typography.bodySmall,
                        color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
            PriorityBadge(priority = item.priority)
            if (!selectionMode) {
                Box {
                    VentriIconButton(onClick = { showMenu = true }) {
                        VentriIcon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.items_more_options_cd)
                        )
                    }
                    VentriDropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }) {
                        VentriDropdownMenuItem(
                            text = { VentriText(stringResource(R.string.common_edit)) },
                            onClick = { onEdit(); showMenu = false },
                        )
                        VentriDropdownMenuItem(
                            text = { VentriText(stringResource(R.string.common_remove)) },
                            onClick = {
                                showDeleteConfirm = true; showMenu = false
                            },
                        )
                        VentriDropdownMenuItem(
                            text = { VentriText(stringResource(R.string.items_add_to_shopping_menu)) },
                            onClick = {
                                onIntent(
                                    ItemsIntent.AddToShoppingList(
                                        item.id
                                    )
                                ); showMenu = false
                            },
                        )
                    }
                }
            }
        }
    }

    if (selectionMode) {
        VentriClickableCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onIntent(ItemsIntent.ToggleItemSelection(item.id)) },
        ) { cardContent() }
    } else {
        VentriCard(modifier = Modifier.fillMaxWidth()) { cardContent() }
    }
}

@Composable
private fun SelectionActionStrip(
    selectedCount: Int,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    VentriSurface(
        color = VentriTheme.colors.surface,
        shape = VentriShapes.small,
        modifier = Modifier.windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VentriOutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                VentriText(stringResource(R.string.common_cancel), color = VentriTheme.colors.onSurface)
            }
            VentriButton(
                onClick = onConfirm,
                enabled = selectedCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                VentriText(
                    text = pluralStringResource(R.plurals.items_add_count, selectedCount, selectedCount),
                    color = VentriTheme.colors.onAccent,
                )
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: ItemPriority) {
    val colors = VentriTheme.colors
    val (bg, fg) = when (priority) {
        ItemPriority.Highest -> colors.criticalContainer to colors.onCriticalContainer
        ItemPriority.High -> colors.warningContainer to colors.onWarningContainer
        ItemPriority.Normal -> colors.accentMuted to colors.accent
        ItemPriority.Low, ItemPriority.Lowest -> colors.surfaceMuted to colors.onSurface.copy(
            alpha = 0.5f
        )
    }
    VentriChip(containerColor = bg, modifier = Modifier.padding(end = 4.dp)) {
        VentriText(
            priority.displayName(),
            style = VentriTheme.typography.labelSmall,
            color = fg
        )
    }
}

@Composable
private fun FreePlanBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                VentriTheme.colors.accentMuted,
                shape = VentriShapes.small
            )
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VentriIcon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = VentriTheme.colors.accent.copy(alpha = 0.7f),
            modifier = Modifier.padding(end = 10.dp, top = 1.dp),
        )
        VentriText(
            text = stringResource(R.string.items_free_plan_text),
            style = VentriTheme.typography.bodySmall,
            color = VentriTheme.colors.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.weight(1f),
        )
        VentriIconButton(onClick = onDismiss) {
            VentriIcon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.items_free_plan_dismiss_cd),
                tint = VentriTheme.colors.onSurface.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun ItemsEmptyState(
    topPadding: Dp,
    bottomPadding: Dp,
) {
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
            imageVector = Icons.Default.Category,
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
            text = stringResource(R.string.items_empty_title),
            style = VentriTheme.typography.titleLarge,
            color = VentriTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        VentriText(
            text = stringResource(R.string.items_empty_body),
            style = VentriTheme.typography.bodyMedium,
            color = VentriTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(32.dp))
        ItemsTipCard(
            icon = Icons.Default.Speed,
            title = stringResource(R.string.items_onboarding_rate_title),
            body = stringResource(R.string.items_onboarding_rate_body),
        )
        Spacer(Modifier.height(12.dp))
        ItemsTipCard(
            icon = Icons.Default.Flag,
            title = stringResource(R.string.items_onboarding_priority_title),
            body = stringResource(R.string.items_onboarding_priority_body),
        )
        Spacer(Modifier.height(12.dp))
        ItemsTipCard(
            icon = Icons.Default.AddShoppingCart,
            title = stringResource(R.string.items_onboarding_shopping_title),
            body = stringResource(R.string.items_onboarding_shopping_body),
        )
    }
}

@Composable
private fun ItemsTipCard(
    icon: ImageVector,
    title: String,
    body: String,
) {
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
private fun ItemFormDialog(
    title: String,
    confirmLabel: String,
    prefill: ItemFormPrefill,
    onDismiss: () -> Unit,
    onConfirm: (name: String, unit: ItemUnit, priority: ItemPriority, rate: Double?) -> Unit,
    resultFlow: kotlinx.coroutines.flow.SharedFlow<String?>,
    onSuccess: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(prefill.name) }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUnit by rememberSaveable { mutableStateOf(prefill.unit) }
    var unitExpanded by remember { mutableStateOf(false) }
    var selectedPriority by rememberSaveable { mutableStateOf(prefill.priority) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var rateText by rememberSaveable { mutableStateOf(prefill.consumptionRate?.toString() ?: "") }
    var rateError by rememberSaveable { mutableStateOf<String?>(null) }
    var dontKnowRate by rememberSaveable { mutableStateOf(false) }

    val errNameRequired = stringResource(R.string.items_form_error_name_required)
    val errInvalidNumber = stringResource(R.string.items_form_error_invalid_number)
    val errGreaterThanZero = stringResource(R.string.items_form_error_greater_than_zero)

    LaunchedEffect(resultFlow) {
        resultFlow.collect { error ->
            if (error == null) onSuccess() else nameError = error
        }
    }

    fun validate(): Boolean {
        var valid = true
        if (name.isBlank()) {
            nameError = errNameRequired; valid = false
        }
        if (!dontKnowRate && rateText.isNotBlank()) {
            val rate = rateText.toDoubleOrNull()
            when {
                rate == null -> { rateError = errInvalidNumber; valid = false }
                rate <= 0.0 -> { rateError = errGreaterThanZero; valid = false }
            }
        }
        return valid
    }

    VentriDialog(
        onDismissRequest = onDismiss,
        title = {
            VentriText(
                title,
                style = VentriTheme.typography.titleSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VentriTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    placeholder = stringResource(R.string.items_form_name_placeholder),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError,
                    modifier = Modifier.fillMaxWidth(),
                )
                VentriExposedDropdown(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    selectedText = selectedUnit.displayName(),
                    label = stringResource(R.string.items_form_unit_label),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ItemUnit.entries.forEach { unit ->
                        VentriDropdownMenuItem(
                            text = { VentriText(unit.displayName()) },
                            onClick = {
                                selectedUnit = unit; unitExpanded = false
                            },
                            selected = unit == selectedUnit,
                        )
                    }
                }
                VentriExposedDropdown(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = it },
                    selectedText = selectedPriority.displayName(),
                    label = stringResource(R.string.items_form_priority_label),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ItemPriority.entries.forEach { priority ->
                        VentriDropdownMenuItem(
                            text = { VentriText(priority.displayName()) },
                            onClick = {
                                selectedPriority = priority; priorityExpanded =
                                false
                            },
                            selected = priority == selectedPriority,
                        )
                    }
                }
                VentriTextField(
                    value = if (dontKnowRate) "" else rateText,
                    onValueChange = { rateText = it; rateError = null },
                    placeholder = stringResource(R.string.items_form_rate_placeholder),
                    singleLine = true,
                    enabled = !dontKnowRate,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = rateError != null,
                    supportingText = rateError,
                    modifier = Modifier.fillMaxWidth(),
                )

                VentriCheckbox(
                    checked = dontKnowRate,
                    onCheckedChange = { dontKnowRate = it; rateError = null },
                    label = stringResource(R.string.items_form_dont_know_rate),
                )

                if (dontKnowRate) {
                    VentriText(
                        text = stringResource(R.string.items_form_rate_hint),
                        style = VentriTheme.typography.bodySmall,
                        color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        },
        confirmButton = {
            VentriTextButton(onClick = {
                val rate = if (dontKnowRate) null else rateText.toDoubleOrNull()
                if (validate()) onConfirm(
                    name.trim(),
                    selectedUnit,
                    selectedPriority,
                    rate
                )
            }) { VentriText(confirmLabel, color = VentriTheme.colors.accent) }
        },
        dismissButton = {
            VentriTextButton(onClick = onDismiss) {
                VentriText(
                    stringResource(R.string.common_cancel),
                    color = VentriTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        },
    )
}
