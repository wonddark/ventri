package com.adpt.shared.util

import com.adpt.shared.db.AdptDatabase
import com.adpt.shared.model.AddToShoppingListResult
import com.adpt.shared.model.ShoppingListStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Marks the shopping list entry [entryId] as [ShoppingListStatus.Purchased] and records
 * the purchase on the associated item in a single transaction.
 * Returns true on success, false if the item could not be updated.
 */
fun AdptDatabase.markAsPurchased(entryId: String, itemId: String, amount: Double): Boolean {
    var success = false
    transaction {
        val itemUpdated = itemQueries.recordPurchase(itemId, amount)
        if (itemUpdated) {
            shoppingListEntryQueries.updateStatus(status = ShoppingListStatus.Purchased, id = entryId)
            success = true
        }
    }
    return success
}

/**
 * Deletes all entries from the shopping list regardless of status.
 */
fun AdptDatabase.emptyShoppingList() {
    shoppingListEntryQueries.deleteAll()
}

/**
 * Deletes all shopping list entries with [ShoppingListStatus.Purchased] status.
 */
fun AdptDatabase.clearPurchasedEntries() {
    shoppingListEntryQueries.deleteByStatus(ShoppingListStatus.Purchased)
}

/**
 * Deletes the shopping list entry with [entryId].
 * Returns true if the entry was found and deleted, false otherwise.
 */
fun AdptDatabase.removeShoppingListEntry(entryId: String): Boolean =
    shoppingListEntryQueries.delete(id = entryId).value > 0

/**
 * Creates a new [ShoppingListStatus.Pending] entry for the given [itemId].
 * Returns [AddToShoppingListResult.Success] with the generated entry id,
 * [AddToShoppingListResult.ItemNotFound] if no item with [itemId] exists, or
 * [AddToShoppingListResult.AlreadyInList] if the item is already in the shopping list.
 */
@OptIn(ExperimentalUuidApi::class)
fun AdptDatabase.addToShoppingList(itemId: String): AddToShoppingListResult {
    if (itemQueries.selectById(itemId).executeAsOneOrNull() == null) {
        return AddToShoppingListResult.ItemNotFound
    }
    if (shoppingListEntryQueries.selectByItemId(itemId).executeAsOneOrNull() != null) {
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
