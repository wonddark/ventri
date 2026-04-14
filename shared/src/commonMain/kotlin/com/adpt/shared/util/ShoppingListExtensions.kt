package com.ventri.shared.util

import com.ventri.shared.db.VentriDatabase
import com.ventri.shared.model.AddToShoppingListResult
import com.ventri.shared.model.ShoppingListStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Marks the shopping list entry [entryId] as [ShoppingListStatus.Purchased] and records
 * the purchase on the associated item in a single transaction.
 * Returns true on success, false if the item could not be updated.
 */
fun VentriDatabase.markAsPurchased(
    entryId: String,
    itemId: String,
    amount: Double
): Boolean {
    var success = false
    var totalQuantity = amount
    transaction {
        val item = itemQueries.findById(itemId)
        if (item != null) {
            totalQuantity = (item.purchasedQuantity ?: 0.0) + amount
        }
        val itemUpdated = itemQueries.recordPurchase(itemId, totalQuantity)
        if (itemUpdated) {
            shoppingListEntryQueries.updateStatus(
                status = ShoppingListStatus.Purchased,
                id = entryId
            )
            success = true
        }

    }
    return success
}

/**
 * Deletes all entries from the shopping list regardless of status.
 */
fun VentriDatabase.emptyShoppingList() {
    shoppingListEntryQueries.deleteAll()
}

/**
 * Deletes all shopping list entries with [ShoppingListStatus.Purchased] status.
 */
fun VentriDatabase.clearPurchasedEntries() {
    shoppingListEntryQueries.deleteByStatus(ShoppingListStatus.Purchased)
}

/**
 * Deletes the shopping list entry with [entryId].
 * Returns true if the entry was found and deleted, false otherwise.
 */
fun VentriDatabase.removeShoppingListEntry(entryId: String): Boolean =
    shoppingListEntryQueries.delete(id = entryId).value > 0

/**
 * Creates a new [ShoppingListStatus.Pending] entry for the given [itemId].
 * Returns [AddToShoppingListResult.Success] with the generated entry id,
 * [AddToShoppingListResult.ItemNotFound] if no item with [itemId] exists, or
 * [AddToShoppingListResult.AlreadyInList] if the item is already in the shopping list.
 */
@OptIn(ExperimentalUuidApi::class)
fun VentriDatabase.addToShoppingList(itemId: String): AddToShoppingListResult {
    if (itemQueries.selectById(itemId).executeAsOneOrNull() == null) {
        return AddToShoppingListResult.ItemNotFound
    }
    if (shoppingListEntryQueries.selectByItemId(itemId)
            .executeAsOneOrNull() != null
    ) {
        return AddToShoppingListResult.AlreadyInList
    }
    val entryId = Uuid.random().toString()
    shoppingListEntryQueries.insert(
        id = entryId,
        item_id = itemId,
        status = ShoppingListStatus.Pending
    )
    return AddToShoppingListResult.Success(entryId)
}
