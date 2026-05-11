package com.ventri.app.ui.items

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ventri.app.VentriApplication
import com.ventri.shared.db.Item
import com.ventri.shared.db.SelectAllWithItem
import com.ventri.shared.model.AddToShoppingListResult
import com.ventri.shared.model.InsertItemResult
import com.ventri.shared.model.ItemPriority
import com.ventri.shared.model.ItemUnit
import com.ventri.shared.model.UpdateItemResult
import com.ventri.shared.util.addToShoppingList
import com.ventri.shared.util.deleteItem
import com.ventri.shared.util.insertItem
import com.ventri.shared.util.updateItem
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

enum class SortOrder {
    Priority,
    NameAsc,
    NameDesc,
}

data class ItemUiModel(
    val id: String,
    val name: String,
    val unit: ItemUnit,
    val consumptionRate: Double?,
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
    val isRegistered: Boolean = true, // default true to avoid banner flash while prefs load
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
        val consumptionRate: Double?,
    ) : ItemsIntent
    data class EditItem(val itemId: String) : ItemsIntent
    data class EditItemConfirmed(
        val id: String,
        val name: String,
        val unit: ItemUnit,
        val priority: ItemPriority,
        val consumptionRate: Double?,
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

    private val db = (application as VentriApplication).database
    private val prefs = (application as VentriApplication).prefs

    companion object {
        const val FREE_ITEM_LIMIT = 7
    }

    val selectionMode: Boolean = savedStateHandle.get<Boolean>("selectionMode") ?: false
    val showAddOnStart: Boolean = savedStateHandle.get<Boolean>("add") ?: false
    // Immutable: set once from nav arg, does not update reactively

    private val _addItemResult = MutableSharedFlow<InsertItemError?>()
    val addItemResult: SharedFlow<InsertItemError?> = _addItemResult.asSharedFlow()

    private val _editItemResult = MutableSharedFlow<UpdateItemError?>()
    val editItemResult: SharedFlow<UpdateItemError?> = _editItemResult.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<ItemsSnackbarEvent>()
    val snackbarMessage: SharedFlow<ItemsSnackbarEvent> = _snackbarMessage.asSharedFlow()

    private val _navigationEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
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
            .map { item -> ItemUiModel(item.id, item.name, item.unit, item.consumptionRate.takeIf { it > 0.0 }, item.priority) }
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
        flowOf(emptySet()) // completes immediately; combine retains the last emitted value
    }

    val uiState: StateFlow<ItemsUiState> = combine(
        baseFlow,
        shoppingListItemIdsFlow,
        _selectedItemIds,
        prefs.userId,
    ) { base, shoppingIds, selectedIds, userId ->
        base.copy(
            items = if (selectionMode) base.items.filter { it.id !in shoppingIds } else base.items,
            selectedItemIds = selectedIds,
            isRegistered = userId != null,
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
                val isRegistered = prefs.userId.value != null
                val result = withContext(Dispatchers.IO) {
                    db.itemQueries.insertItem(
                        name = intent.name,
                        unit = intent.unit,
                        priority = intent.priority,
                        consumptionRate = intent.consumptionRate ?: 0.0,
                        maxItems = if (isRegistered) null else FREE_ITEM_LIMIT,
                    )
                }
                when (result) {
                    is InsertItemResult.Success -> {
                        _addItemResult.emit(null)
                        if (selectionMode) {
                            _selectedItemIds.value = _selectedItemIds.value + result.id
                        }
                    }
                    InsertItemResult.DuplicateName -> _addItemResult.emit(InsertItemError.DuplicateName)
                    InsertItemResult.LimitReached -> _addItemResult.emit(InsertItemError.LimitReached(FREE_ITEM_LIMIT))
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
                        consumptionRate = intent.consumptionRate ?: 0.0,
                    )
                }
                when (result) {
                    UpdateItemResult.Success -> _editItemResult.emit(null)
                    UpdateItemResult.DuplicateName -> _editItemResult.emit(UpdateItemError.DuplicateName)
                }
            }
            is ItemsIntent.RemoveItem -> viewModelScope.launch {
                withContext(Dispatchers.IO) { db.itemQueries.deleteItem(intent.itemId) }
            }
            is ItemsIntent.AddToShoppingList -> viewModelScope.launch {
                val result = withContext(Dispatchers.IO) { db.addToShoppingList(intent.itemId) }
                when (result) {
                    is AddToShoppingListResult.Success -> _snackbarMessage.emit(ItemsSnackbarEvent.AddedToShopping)
                    AddToShoppingListResult.AlreadyInList -> _snackbarMessage.emit(ItemsSnackbarEvent.AlreadyInShopping)
                    AddToShoppingListResult.ItemNotFound -> return@launch
                }
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
                val notFound = withContext(Dispatchers.IO) {
                    ids.count { id ->
                        db.addToShoppingList(id) == AddToShoppingListResult.ItemNotFound
                    }
                }
                if (notFound > 0) _snackbarMessage.emit(ItemsSnackbarEvent.SelectionFailed(notFound))
                _navigationEvent.tryEmit(Unit)
            }
            is ItemsIntent.SelectionCancelled -> _navigationEvent.tryEmit(Unit)
        }
    }
}
