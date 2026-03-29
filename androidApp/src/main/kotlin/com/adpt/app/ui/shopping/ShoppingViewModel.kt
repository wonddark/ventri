package com.adpt.app.ui.shopping

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.adpt.app.AdptApplication
import com.adpt.shared.db.SelectAllWithItem
import com.adpt.shared.db.SelectAvailableForShopping
import com.adpt.shared.model.ShoppingListStatus
import com.adpt.shared.util.addToShoppingList
import com.adpt.shared.util.clearPurchasedEntries
import com.adpt.shared.util.emptyShoppingList
import com.adpt.shared.util.markAsPurchased
import com.adpt.shared.util.removeShoppingListEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

data class AvailableItemUiModel(val id: String, val name: String)

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
        val availableItems: List<AvailableItemUiModel>,
    ) : ShoppingUiState
}

sealed interface ShoppingIntent {
    data class MarkAsPurchased(val entryId: String, val itemId: String, val amount: Double) : ShoppingIntent
    data class RemoveEntry(val entryId: String) : ShoppingIntent
    data object AddItem : ShoppingIntent
    data class AddItemConfirmed(val itemId: String) : ShoppingIntent
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

    val uiState: StateFlow<ShoppingUiState> = combine(
        db.shoppingListEntryQueries.selectAllWithItem().asFlow().mapToList(Dispatchers.IO),
        db.itemQueries.selectAvailableForShopping().asFlow().mapToList(Dispatchers.IO),
    ) { rows: List<SelectAllWithItem>, available: List<SelectAvailableForShopping> ->
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
            },
            availableItems = available.map { AvailableItemUiModel(it.id, it.name) },
        )
    }.stateIn(
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
            is ShoppingIntent.AddItem -> Unit
            is ShoppingIntent.AddItemConfirmed -> viewModelScope.launch {
                withContext(Dispatchers.IO) { db.addToShoppingList(intent.itemId) }
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
