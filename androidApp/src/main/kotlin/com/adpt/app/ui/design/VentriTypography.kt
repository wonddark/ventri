package com.ventri.app.ui.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class VentriTypography(
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
)

val DefaultTypography = VentriTypography(
    titleLarge  = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall  = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium  = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall   = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    labelSmall  = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
)

val LocalVentriTypography = staticCompositionLocalOf<VentriTypography> { DefaultTypography }
