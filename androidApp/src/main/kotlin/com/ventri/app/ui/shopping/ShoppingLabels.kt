package com.ventri.app.ui.shopping

sealed interface ShoppingDepletionLabel {
    object AlreadyDepleted : ShoppingDepletionLabel
    data class WillLast(val days: Long) : ShoppingDepletionLabel
}
