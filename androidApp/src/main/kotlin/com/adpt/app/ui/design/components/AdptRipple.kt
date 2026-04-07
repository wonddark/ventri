package com.adpt.app.ui.design.components

import androidx.compose.foundation.Indication
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Wraps [rememberRipple] with suppressed deprecation to provide ripple indication without
 * depending on Material3. The deprecated API is still functional; this wrapper isolates the
 * suppression to one place. Will be replaced with a proper IndicationNodeFactory when the
 * material-ripple library stabilises its standalone API.
 */
@Suppress("DEPRECATION_ERROR")
@Composable
internal fun ripple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified,
): Indication = rememberRipple(bounded = bounded, radius = radius, color = color)
