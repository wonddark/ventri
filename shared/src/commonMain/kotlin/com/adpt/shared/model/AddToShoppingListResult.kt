package com.adpt.shared.model

sealed class AddToShoppingListResult {
    data class Success(val entryId: String) : AddToShoppingListResult()
    data object AlreadyInList : AddToShoppingListResult()
    data object ItemNotFound : AddToShoppingListResult()
}
