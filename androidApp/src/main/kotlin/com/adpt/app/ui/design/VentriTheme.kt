package com.ventri.app.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun VentriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalVentriColors provides if (darkTheme) DarkColors else LightColors,
        LocalVentriTypography provides DefaultTypography,
        content = content,
    )
}

object VentriTheme {
    val colors: VentriColors
        @Composable @ReadOnlyComposable
        get() = LocalVentriColors.current

    val typography: VentriTypography
        @Composable @ReadOnlyComposable
        get() = LocalVentriTypography.current

    val shapes: VentriShapes
        get() = VentriShapes

    val spacing: VentriSpacing
        get() = VentriSpacing
}
