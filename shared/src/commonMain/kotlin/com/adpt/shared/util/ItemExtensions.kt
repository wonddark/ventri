package com.adpt.shared.util

import com.adpt.shared.db.Item
import com.adpt.shared.db.ItemQueries
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/**
 * Returns the estimated depletion date as epoch millis, or null if
 * [Item.lastPurchasedAt], [Item.purchasedQuantity] are missing, or
 * [Item.consumptionRate] is zero.
 */
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

fun Item.estimatedDepletionDate(): Long? {
    val purchasedAt = lastPurchasedAt ?: return null
    val quantity = purchasedQuantity ?: return null
    if (consumptionRate == 0.0) return null
    val daysUntilDepletion = quantity / consumptionRate
    return purchasedAt + (daysUntilDepletion * MILLIS_PER_DAY).toLong()
}
