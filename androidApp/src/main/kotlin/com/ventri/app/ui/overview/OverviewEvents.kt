package com.ventri.app.ui.overview

sealed interface OverviewError {
    object AddToShoppingListFailed : OverviewError
}
