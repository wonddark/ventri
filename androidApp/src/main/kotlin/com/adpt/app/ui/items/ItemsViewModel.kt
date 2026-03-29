package com.adpt.app.ui.items

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.adpt.app.AdptApplication
import com.adpt.shared.db.Item
import com.adpt.shared.model.ItemPriority
import com.adpt.shared.model.ItemUnit
import com.adpt.shared.model.InsertItemResult
import com.adpt.shared.model.UpdateItemResult
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
}

class ItemsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as AdptApplication).database

    // null = success; non-null = error message to show inline
    private val _addItemResult = MutableSharedFlow<String?>()
    val addItemResult: SharedFlow<String?> = _addItemResult.asSharedFlow()

    private val _editItemResult = MutableSharedFlow<String?>()
    val editItemResult: SharedFlow<String?> = _editItemResult.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.Priority)
    private val _priorityFilter = MutableStateFlow(emptySet<ItemPriority>())
    private val _isSearchActive = MutableStateFlow(false)

    val uiState: StateFlow<ItemsUiState> = combine(
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
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ItemsUiState(),
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
                    is InsertItemResult.Success -> _addItemResult.emit(null)
                    InsertItemResult.DuplicateName -> _addItemResult.emit("An item with this name already exists")
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
                    UpdateItemResult.DuplicateName -> _editItemResult.emit("An item with this name already exists")
                }
            }
            is ItemsIntent.RemoveItem -> Unit
            is ItemsIntent.AddToShoppingList -> Unit
        }
    }
}
