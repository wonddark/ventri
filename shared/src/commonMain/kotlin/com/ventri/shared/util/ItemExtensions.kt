package com.ventri.shared.util

import com.ventri.shared.db.Item
import com.ventri.shared.db.ItemQueries
import com.ventri.shared.model.InsertItemResult
import com.ventri.shared.model.ItemPriority
import com.ventri.shared.model.ItemUnit
import com.ventri.shared.model.Severity
import com.ventri.shared.model.ThresholdConfig
import com.ventri.shared.model.UpdateItemResult
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/**
 * Returns the item with [id], or null if no such item exists.
 */
fun ItemQueries.findById(id: String): Item? = selectById(id).executeAsOneOrNull()

/**
 * Updates the priority of the item with [itemId] to [priority].
 * Returns true if the item was found and updated, false if no item with [itemId] exists.
 */
fun ItemQueries.updateItemPriority(itemId: String, priority: ItemPriority): Boolean =
    updatePriority(priority = priority, id = itemId).value > 0

/**
 * Returns the [Severity] for a given [delta] (estimatedDepletionDate - now, in millis)
 * using the provided [ThresholdConfig].
 * Critical: delta <= criticalDays (including negative/already depleted)
 * High:     delta <= highDays
 * Normal:   delta <= normalDays
 * Low:      otherwise
 */
fun deltaToSeverity(delta: Long, config: ThresholdConfig): Severity {
    val criticalMs = config.criticalDays * MILLIS_PER_DAY
    val highMs = config.highDays * MILLIS_PER_DAY
    val normalMs = config.normalDays * MILLIS_PER_DAY
    return when {
        delta <= criticalMs -> Severity.Critical
        delta <= highMs -> Severity.High
        delta <= normalMs -> Severity.Normal
        else -> Severity.Low
    }
}

/**
 * Inserts a new item with the given [name] and [unit].
 * Returns [InsertItemResult.Success] with the generated id, or [InsertItemResult.DuplicateName]
 * if an item with that name already exists.
 */
@OptIn(ExperimentalUuidApi::class)
fun ItemQueries.insertItem(
    name: String,
    unit: ItemUnit,
    priority: ItemPriority = ItemPriority.Normal,
    consumptionRate: Double = 0.0,
): InsertItemResult {
    if (selectByName(name).executeAsOneOrNull() != null) return InsertItemResult.DuplicateName
    val id = Uuid.random().toString()
    insert(
        id = id,
        name = name,
        unit = unit,
        consumptionRate = consumptionRate,
        lastPurchasedAt = null,
        purchasedQuantity = null,
        isInStock = false,
        priority = priority,
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

/**
 * Deletes the item with [itemId] from the database.
 * Returns true if the item was found and deleted, false if no item with [itemId] exists.
 */
fun ItemQueries.deleteItem(itemId: String): Boolean =
    delete(id = itemId).value > 0

/**
 * Updates the editable fields (name, unit, priority, consumptionRate) of the item with [id].
 * Returns [UpdateItemResult.DuplicateName] if another item already uses [name],
 * or [UpdateItemResult.Success] otherwise.
 */
fun ItemQueries.updateItem(
    id: String,
    name: String,
    unit: ItemUnit,
    priority: ItemPriority,
    consumptionRate: Double,
): UpdateItemResult {
    val existing = selectByName(name).executeAsOneOrNull()
    if (existing != null && existing.id != id) return UpdateItemResult.DuplicateName
    updateDetails(name = name, unit = unit, consumptionRate = consumptionRate, priority = priority, id = id)
    return UpdateItemResult.Success
}

fun Item.estimatedDepletionDate(): Long? {
    val purchasedAt = lastPurchasedAt ?: return null
    val quantity = purchasedQuantity ?: return null
    if (consumptionRate == 0.0) return null
    val daysUntilDepletion = quantity / consumptionRate
    return purchasedAt + (daysUntilDepletion * MILLIS_PER_DAY).toLong()
}
