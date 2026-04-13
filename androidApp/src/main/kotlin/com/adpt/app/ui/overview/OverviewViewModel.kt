package com.adpt.app.ui.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.adpt.app.AdptApplication
import com.adpt.shared.db.Item
import com.adpt.shared.model.AddToShoppingListResult
import com.adpt.shared.model.ItemPriority
import com.adpt.shared.model.Severity
import com.adpt.shared.model.ThresholdConfig
import com.adpt.shared.util.addToShoppingList
import com.adpt.shared.util.deltaToSeverity
import com.adpt.shared.util.estimatedDepletionDate
import com.adpt.shared.util.updateItemPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class OverviewItemUiModel(
    val id: String,
    val name: String,
    val severity: Severity,
    val deltaMillis: Long?, // null means not in stock
    val isInShoppingList: Boolean,
)

sealed interface OverviewUiState {
    data object Loading : OverviewUiState
    data class Success(
        val items: List<OverviewItemUiModel>,
        val criticalCount: Int,
        val highCount: Int,
        val hasAnyItems: Boolean,
        val severityFilter: Severity?,
        val listVersion: Int = 0,
    ) : OverviewUiState
}

sealed interface OverviewIntent {
    data class AddToShoppingList(val itemId: String) : OverviewIntent
    data class IgnoreItem(val itemId: String) : OverviewIntent
    data class ToggleSeverityFilter(val severity: Severity) : OverviewIntent
}

class OverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as AdptApplication).database
    private val prefs = (application as AdptApplication).prefs

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val clockSignal = MutableStateFlow(Clock.System.now().toEpochMilliseconds())
    private val refreshVersion = MutableStateFlow(0)
    private val _severityFilter = MutableStateFlow<Severity?>(null)

    fun refresh() {
        clockSignal.value = Clock.System.now().toEpochMilliseconds()
        refreshVersion.value++
    }

    // Produces the full (unfiltered) item list with unfiltered counts.
    private val baseItems: Flow<OverviewUiState.Success> = combine(
        db.itemQueries.selectAll().asFlow().mapToList(Dispatchers.IO),
        db.shoppingListEntryQueries.selectAll().asFlow().mapToList(Dispatchers.IO),
        clockSignal,
        refreshVersion,
        prefs.thresholdConfig,
    ) { items, entries, now, version, thresholds ->
        val inShoppingList = entries.map { it.item_id }.toSet()
        val allItems = items.mapNotNull { item: Item ->
            if (item.priority == ItemPriority.Lowest) return@mapNotNull null
            val depletionDate = item.estimatedDepletionDate()
            if (depletionDate == null) {
                if (item.priority != ItemPriority.High && item.priority != ItemPriority.Highest) return@mapNotNull null
                return@mapNotNull OverviewItemUiModel(item.id, item.name, Severity.Critical, null, item.id in inShoppingList)
            }
            val delta = depletionDate - now
            val severity = deltaToSeverity(delta, thresholds)
            if (severity == Severity.Low) return@mapNotNull null
            OverviewItemUiModel(item.id, item.name, severity, delta, item.id in inShoppingList)
        }.sortedWith(compareBy(nullsFirst()) { it.deltaMillis })
        OverviewUiState.Success(
            items = allItems,
            criticalCount = allItems.count { it.severity == Severity.Critical },
            highCount = allItems.count { it.severity == Severity.High },
            hasAnyItems = items.isNotEmpty(),
            severityFilter = null,
            listVersion = version,
        )
    }

    val uiState: StateFlow<OverviewUiState> = combine(
        baseItems,
        _severityFilter,
    ) { base, filter ->
        base.copy(
            items = if (filter != null) base.items.filter { it.severity == filter } else base.items,
            severityFilter = filter,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OverviewUiState.Loading,
    )

    fun addAllToShoppingList(itemIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = itemIds.map { db.addToShoppingList(it) }
            val anySuccess = results.any { it is AddToShoppingListResult.Success }
            val anyError = results.any { it is AddToShoppingListResult.ItemNotFound }
            if (!anySuccess && anyError) {
                _errors.emit("Could not add items to the shopping list")
            }
        }
    }

    fun handleIntent(intent: OverviewIntent) {
        when (intent) {
            is OverviewIntent.AddToShoppingList -> viewModelScope.launch(Dispatchers.IO) {
                db.addToShoppingList(intent.itemId)
            }
            is OverviewIntent.IgnoreItem -> viewModelScope.launch(Dispatchers.IO) {
                db.itemQueries.updateItemPriority(intent.itemId, ItemPriority.Lowest)
            }
            is OverviewIntent.ToggleSeverityFilter -> {
                _severityFilter.value =
                    if (_severityFilter.value == intent.severity) null else intent.severity
            }
        }
    }
}
