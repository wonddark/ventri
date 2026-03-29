package com.adpt.shared

import kotlin.test.Test
import kotlin.test.assertNotNull

class PlatformTest {
    @Test
    fun platformNameIsNotNull() {
        assertNotNull(platformName())
    }
}
