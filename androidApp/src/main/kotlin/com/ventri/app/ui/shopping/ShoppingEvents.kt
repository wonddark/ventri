package com.ventri.app.ui.shopping

sealed interface ShoppingError {
    data class AddFailed(val cause: String?) : ShoppingError
}
