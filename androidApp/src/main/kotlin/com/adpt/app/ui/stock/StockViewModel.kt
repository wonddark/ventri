package com.adpt.app.ui.stock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.adpt.app.AdptApplication
import com.adpt.shared.db.Item
import com.adpt.shared.model.ItemUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

data class StockItemUiModel(
    val id: String,
    val name: String,
    val unit: ItemUnit,
    val remainingQuantity: Double,
    val daysRemainingLabel: String,
)

sealed interface StockUiState {
    data object Loading : StockUiState
    data class Success(val items: List<StockItemUiModel>) : StockUiState
}

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as AdptApplication).database

    val uiState: StateFlow<StockUiState> = db.itemQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { items: List<Item> ->
            val now = Clock.System.now().toEpochMilliseconds()
            val inStock = items.mapNotNull { item -> item.toStockUiModel(now) }
                .sortedBy { it.name.lowercase() }
            StockUiState.Success(inStock)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StockUiState.Loading,
        )

    private fun Item.toStockUiModel(now: Long): StockItemUiModel? {
        val purchasedAt = lastPurchasedAt ?: return null
        val qty = purchasedQuantity ?: return null
        if (consumptionRate == 0.0) return null

        val millisSincePurchase = now - purchasedAt
        val remaining = qty - (consumptionRate * millisSincePurchase / MILLIS_PER_DAY)
        if (remaining <= 0) return null

        val depletionDate = purchasedAt + (qty / consumptionRate * MILLIS_PER_DAY).toLong()
        val delta = depletionDate - now
        val daysLeft = delta / MILLIS_PER_DAY

        val label = when {
            delta < MILLIS_PER_DAY -> "Less than a day remaining"
            daysLeft == 1L -> "1 day remaining"
            else -> "$daysLeft days remaining"
        }

        return StockItemUiModel(
            id = id,
            name = name,
            unit = unit,
            remainingQuantity = remaining,
            daysRemainingLabel = label,
        )
    }
}
