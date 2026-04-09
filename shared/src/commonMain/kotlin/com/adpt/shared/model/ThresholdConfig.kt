package com.adpt.shared.model

data class ThresholdConfig(
    val criticalDays: Int = 1,
    val highDays: Int = 2,
    val normalDays: Int = 3,
)
