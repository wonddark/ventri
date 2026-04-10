package com.adpt.shared

import com.adpt.shared.model.Severity
import com.adpt.shared.model.ThresholdConfig
import com.adpt.shared.util.deltaToSeverity
import kotlin.test.Test
import kotlin.test.assertEquals

class DeltaToSeverityTest {

    private val config = ThresholdConfig(criticalDays = 1, highDays = 2, normalDays = 3)
    private val ms = 24L * 60 * 60 * 1000L // one day in millis

    @Test
    fun criticalWhenDeltaIsExactlyOneCriticalDay() {
        assertEquals(Severity.Critical, deltaToSeverity(1 * ms, config))
    }

    @Test
    fun criticalWhenDeltaIsZero() {
        assertEquals(Severity.Critical, deltaToSeverity(0L, config))
    }

    @Test
    fun criticalWhenDeltaIsNegative() {
        assertEquals(Severity.Critical, deltaToSeverity(-1L, config))
    }

    @Test
    fun highWhenDeltaIsJustAboveCriticalThreshold() {
        assertEquals(Severity.High, deltaToSeverity(1 * ms + 1, config))
    }

    @Test
    fun highWhenDeltaIsExactlyTwoDays() {
        assertEquals(Severity.High, deltaToSeverity(2 * ms, config))
    }

    @Test
    fun normalWhenDeltaIsJustAboveHighThreshold() {
        assertEquals(Severity.Normal, deltaToSeverity(2 * ms + 1, config))
    }

    @Test
    fun normalWhenDeltaIsExactlyThreeDays() {
        assertEquals(Severity.Normal, deltaToSeverity(3 * ms, config))
    }

    @Test
    fun lowWhenDeltaIsAboveNormalThreshold() {
        assertEquals(Severity.Low, deltaToSeverity(3 * ms + 1, config))
    }

    @Test
    fun customThresholdsAreRespected() {
        val custom = ThresholdConfig(criticalDays = 3, highDays = 7, normalDays = 14)
        assertEquals(Severity.Critical, deltaToSeverity(3 * ms, custom))
        assertEquals(Severity.High, deltaToSeverity(4 * ms, custom))
        assertEquals(Severity.Normal, deltaToSeverity(8 * ms, custom))
        assertEquals(Severity.Low, deltaToSeverity(15 * ms, custom))
    }
}
