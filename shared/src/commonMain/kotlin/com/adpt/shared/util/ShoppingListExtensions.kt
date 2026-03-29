package com.adpt.shared.util

import com.adpt.shared.db.AdptDatabase
import com.adpt.shared.model.AddToShoppingListResult
import com.adpt.shared.model.ShoppingListStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
