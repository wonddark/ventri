package com.ventri.app.ui.items

sealed interface InsertItemError {
    object DuplicateName : InsertItemError
    data class LimitReached(val limit: Int) : InsertItemError
}

sealed interface UpdateItemError {
    object DuplicateName : UpdateItemError
}

sealed interface ItemsSnackbarEvent {
    object AddedToShopping : ItemsSnackbarEvent
    object AlreadyInShopping : ItemsSnackbarEvent
    data class SelectionFailed(val count: Int) : ItemsSnackbarEvent
}
