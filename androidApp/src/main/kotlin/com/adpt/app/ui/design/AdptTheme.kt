package com.adpt.app.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun AdptTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAdptColors provides if (darkTheme) DarkColors else LightColors,
        LocalAdptTypography provides DefaultTypography,
        content = content,
    )
}

object AdptTheme {
    val colors: AdptColors
        @Composable @ReadOnlyComposable
        get() = LocalAdptColors.current

    val typography: AdptTypography
        @Composable @ReadOnlyComposable
        get() = LocalAdptTypography.current

    val shapes: AdptShapes
        get() = AdptShapes

    val spacing: AdptSpacing
        get() = AdptSpacing
}
