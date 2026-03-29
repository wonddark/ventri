package com.adpt.shared.util

import com.adpt.shared.db.Item

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/**
 * Returns the estimated depletion date as epoch millis, or null if
 * [Item.lastPurchasedAt], [Item.purchasedQuantity] are missing, or
 * [Item.consumptionRate] is zero.
 */
fun Item.estimatedDepletionDate(): Long? {
    val purchasedAt = lastPurchasedAt ?: return null
    val quantity = purchasedQuantity ?: return null
    if (consumptionRate == 0.0) return null
    val daysUntilDepletion = quantity / consumptionRate
    return purchasedAt + (daysUntilDepletion * MILLIS_PER_DAY).toLong()
}
