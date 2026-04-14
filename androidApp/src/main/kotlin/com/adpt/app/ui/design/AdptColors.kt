package com.ventri.app.ui.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class VentriColors(
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentMuted: Color,
    val critical: Color,
    val onCritical: Color,
    val warning: Color,
    val onWarning: Color,
    val ok: Color,
    val onOk: Color,
    val outline: Color,
    val criticalContainer: Color,
    val onCriticalContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

val LightColors = VentriColors(
    background        = Color(0xFFFAF7F3),
    onBackground      = Color(0xFF2A1F17),
    surface           = Color(0xFFFFFFFF),
    onSurface         = Color(0xFF2A1F17),
    surfaceMuted      = Color(0xFFF0EBE3),
    accent            = Color(0xFFB5613A),
    onAccent          = Color(0xFFFFFFFF),
    accentMuted       = Color(0x1AB5613A),
    critical          = Color(0xFFB5613A),
    onCritical        = Color(0xFFFFFFFF),
    warning           = Color(0xFFC09030),
    onWarning         = Color(0xFFFFFFFF),
    ok                = Color(0xFF5A7A5C),
    onOk              = Color(0xFFFFFFFF),
    outline           = Color(0xFFE8E2D8),
    criticalContainer = Color(0xFFFCE4D6),
    onCriticalContainer = Color(0xFF8B3520),
    warningContainer  = Color(0xFFFEF2CC),
    onWarningContainer = Color(0xFF8A5E00),
)

val DarkColors = VentriColors(
    background        = Color(0xFF1F1A17),
    onBackground      = Color(0xFFEDE6DC),
    surface           = Color(0xFF2D2520),
    onSurface         = Color(0xFFEDE6DC),
    surfaceMuted      = Color(0xFF3D2F28),
    accent            = Color(0xFFE8A07A),
    onAccent          = Color(0xFF1F1A17),
    accentMuted       = Color(0x1FE8A07A),
    critical          = Color(0xFFE8A07A),
    onCritical        = Color(0xFF1F1A17),
    warning           = Color(0xFFD4A840),
    onWarning         = Color(0xFF1F1A17),
    ok                = Color(0xFFA3C4A6),
    onOk              = Color(0xFF1F1A17),
    outline           = Color(0x1AEDE6DC),
    criticalContainer = Color(0xFF4A2216),
    onCriticalContainer = Color(0xFFF5C0A0),
    warningContainer  = Color(0xFF3D3010),
    onWarningContainer = Color(0xFFF0D890),
)

val LocalVentriColors = staticCompositionLocalOf<VentriColors> { LightColors }
