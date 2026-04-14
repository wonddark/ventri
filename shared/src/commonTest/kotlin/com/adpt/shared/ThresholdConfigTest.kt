package com.ventri.shared

import com.ventri.shared.model.ThresholdConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThresholdConfigTest {

    @Test
    fun defaultsAreCorrect() {
        val config = ThresholdConfig()
        assertEquals(1, config.criticalDays)
        assertEquals(2, config.highDays)
        assertEquals(3, config.normalDays)
    }

    @Test
    fun invariantHoldsForDefaults() {
        val config = ThresholdConfig()
        assertTrue(config.criticalDays < config.highDays)
        assertTrue(config.highDays < config.normalDays)
    }

    @Test
    fun customValuesAreStored() {
        val config = ThresholdConfig(criticalDays = 2, highDays = 5, normalDays = 10)
        assertEquals(2, config.criticalDays)
        assertEquals(5, config.highDays)
        assertEquals(10, config.normalDays)
    }
}
