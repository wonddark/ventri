package com.adpt.app.ui.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.adpt.app.AdptApplication
import com.adpt.shared.db.Item
import com.adpt.shared.model.ItemPriority
import com.adpt.shared.model.Severity
import com.adpt.shared.util.addToShoppingList
import com.adpt.shared.util.deltaToSeverity
import com.adpt.shared.util.estimatedDepletionDate
import com.adpt.shared.util.updateItemPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class OverviewItemUiModel(
    val id: String,
    val name: String,
    val severity: Severity,
    val deltaMillis: Long,
)

sealed interface OverviewUiState {
    data object Loading : OverviewUiState
    data class Success(val items: List<OverviewItemUiModel>) : OverviewUiState
}

sealed interface OverviewIntent {
    data class AddToShoppingList(val itemId: String) : OverviewIntent
    data class IgnoreItem(val itemId: String) : OverviewIntent
}

class OverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as AdptApplication).database

    val uiState: StateFlow<OverviewUiState> = db.itemQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { items: List<Item> ->
            val now = Clock.System.now().toEpochMilliseconds()
            val filtered = items.mapNotNull { item: Item ->
                if (item.priority == ItemPriority.Lowest) return@mapNotNull null
                val depletionDate = item.estimatedDepletionDate() ?: return@mapNotNull null
                val delta = depletionDate - now
                val severity = deltaToSeverity(delta)
                if (severity == Severity.Good) return@mapNotNull null
                OverviewItemUiModel(item.id, item.name, severity, delta)
            }.sortedBy { it.deltaMillis }
            OverviewUiState.Success(filtered)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OverviewUiState.Loading,
        )

    fun handleIntent(intent: OverviewIntent) {
        when (intent) {
            is OverviewIntent.AddToShoppingList -> viewModelScope.launch(Dispatchers.IO) {
                db.addToShoppingList(intent.itemId)
            }
            is OverviewIntent.IgnoreItem -> viewModelScope.launch(Dispatchers.IO) {
                db.itemQueries.updateItemPriority(intent.itemId, ItemPriority.Lowest)
            }
        }
    }
}
