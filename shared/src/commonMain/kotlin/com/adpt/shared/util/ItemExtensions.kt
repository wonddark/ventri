package com.adpt.shared.util

import com.adpt.shared.db.Item
import com.adpt.shared.db.ItemQueries
import com.adpt.shared.model.InsertItemResult
import com.adpt.shared.model.ItemPriority
import com.adpt.shared.model.ItemUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/**
 * Inserts a new item with the given [name] and [unit].
 * Returns [InsertItemResult.Success] with the generated id, or [InsertItemResult.DuplicateName]
 * if an item with that name already exists.
 */
@OptIn(ExperimentalUuidApi::class)
fun ItemQueries.insertItem(name: String, unit: ItemUnit): InsertItemResult {
    if (selectByName(name).executeAsOneOrNull() != null) return InsertItemResult.DuplicateName
    val id = Uuid.random().toString()
    insert(
        id = id,
        name = name,
        unit = unit,
        consumptionRate = 0.0,
        lastPurchasedAt = null,
        purchasedQuantity = null,
        isInStock = false,
        priority = ItemPriority.Normal
    )
    return InsertItemResult.Success(id)
}

/**
 * Updates [lastPurchasedAt] to today's date and [purchasedQuantity] to [purchasedAmount],
 * and sets [isInStock] to true.
 * Returns true if the item was found and updated, false if no item with [itemId] exists.
 */
fun ItemQueries.recordPurchase(itemId: String, purchasedAmount: Double): Boolean {
    val today = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .atStartOfDayIn(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
    return updateStock(
        lastPurchasedAt = today,
        purchasedQuantity = purchasedAmount,
        isInStock = true,
        id = itemId
    ).value > 0
}

/**
 * Returns all item ids sorted by how soon they will be depleted (soonest first).
 * Items with no estimable depletion date are excluded.
 * Items already past their depletion date appear at the top (negative delta).
 */
fun ItemQueries.itemsSortedByDepletion(): List<String> {
    val now = Clock.System.now().toEpochMilliseconds()
    return selectAll()
        .executeAsList()
        .mapNotNull { item ->
            val depletionDate = item.estimatedDepletionDate() ?: return@mapNotNull null
            item.id to (depletionDate - now)
        }
        .sortedBy { (_, delta) -> delta }
        .map { (id, _) -> id }
}

fun Item.estimatedDepletionDate(): Long? {
    val purchasedAt = lastPurchasedAt ?: return null
    val quantity = purchasedQuantity ?: return null
    if (consumptionRate == 0.0) return null
    val daysUntilDepletion = quantity / consumptionRate
    return purchasedAt + (daysUntilDepletion * MILLIS_PER_DAY).toLong()
}
