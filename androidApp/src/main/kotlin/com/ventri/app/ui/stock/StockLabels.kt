package com.ventri.app.ui.stock

sealed interface StockDaysLabel {
    object TrackingUsage : StockDaysLabel
    object LessThanADay : StockDaysLabel
    data class Days(val count: Long) : StockDaysLabel
}
