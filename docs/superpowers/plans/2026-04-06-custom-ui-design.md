# Custom UI Design System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all Material3 UI components with a fully custom Compose Foundation–based design system using a Terracotta & Cream palette with soft pill-like shapes.

**Architecture:** Component-first: build design tokens and all custom components in `ui/design/` first (Phase 1), then migrate each screen to use only those components (Phase 2), then remove the Material3 dependency (Phase 3). Material3 stays in `build.gradle` until Phase 3 so the app stays buildable throughout.

**Tech Stack:** Kotlin, Jetpack Compose Foundation, `androidx.compose.ui`, `androidx.compose.animation`, `androidx.compose.material.ripple` (for ripple indication)

---

## File Map

**Created (Phase 1):**
```
androidApp/src/main/kotlin/com/adpt/app/ui/design/
  AdptColors.kt           — light/dark color tokens + LocalAdptColors
  AdptTypography.kt       — text style tokens + LocalAdptTypography
  AdptShapes.kt           — shape constants (pill/card/small)
  AdptSpacing.kt          — spacing constants (xs/sm/md/lg/xl)
  AdptTheme.kt            — CompositionLocal provider + AdptTheme object
  components/
    AdptSurface.kt        — Box with background+clip; replaces Surface
    AdptText.kt           — BasicText wrapper; replaces Text
    AdptIcon.kt           — Image(vectorPainter); replaces Icon
    AdptIconButton.kt     — clickable pill Box; replaces IconButton
    AdptButton.kt         — filled pill button; replaces Button
    AdptOutlinedButton.kt — bordered pill button; replaces OutlinedButton
    AdptTextButton.kt     — text-only button; replaces TextButton
    AdptChip.kt           — pill label with color slots; replaces chip/badge Surfaces
    AdptBadge.kt          — circular border-only or filled pill; replaces custom badges
    AdptCheckbox.kt       — Canvas-drawn animated checkbox; replaces Checkbox
    AdptProgressIndicator.kt — Canvas spinner; replaces CircularProgressIndicator
    AdptCard.kt           — rounded surface with shadow; replaces Card/ElevatedCard
    AdptTopBar.kt         — Row with title/nav/actions slots; replaces TopAppBar
    AdptScaffold.kt       — SubcomposeLayout layout; replaces Scaffold
    AdptFab.kt            — pill FAB; replaces FloatingActionButton
    AdptNavBar.kt         — floating frosted-glass nav bar; replaces NavigationBar
    AdptSnackbar.kt       — slide-up snackbar + host; replaces SnackbarHost/Snackbar
    AdptDialog.kt         — Dialog-window card; replaces AlertDialog
    AdptTextField.kt      — BasicTextField with animated label; replaces TextField/OutlinedTextField
    AdptDropdownMenu.kt   — Popup-based menu; replaces DropdownMenu/DropdownMenuItem
    AdptExposedDropdown.kt — TextField + dropdown combo; replaces ExposedDropdownMenuBox
```

**Modified (Phase 2):**
```
androidApp/src/main/kotlin/com/adpt/app/MainActivity.kt
androidApp/src/main/kotlin/com/adpt/app/navigation/AppNavigation.kt
androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewScreen.kt
androidApp/src/main/kotlin/com/adpt/app/ui/shopping/ShoppingScreen.kt
androidApp/src/main/kotlin/com/adpt/app/ui/stock/StockScreen.kt
androidApp/src/main/kotlin/com/adpt/app/ui/items/ItemsScreen.kt
```

**Modified (Phase 3):**
```
androidApp/build.gradle.kts
gradle/libs.versions.toml
```

**Deleted (Phase 3):**
```
androidApp/src/main/kotlin/com/adpt/app/ui/theme/Color.kt
androidApp/src/main/kotlin/com/adpt/app/ui/theme/Theme.kt
```

---

## Phase 1 — Design System

### Task 1: Color Tokens

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/AdptColors.kt`

- [ ] **Step 1: Create AdptColors.kt**

```kotlin
package com.adpt.app.ui.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AdptColors(
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

val LightColors = AdptColors(
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

val DarkColors = AdptColors(
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

val LocalAdptColors = staticCompositionLocalOf<AdptColors> { LightColors }
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/AdptColors.kt
git commit -m "feat: add AdptColors design tokens"
```

---

### Task 2: Typography, Shape, and Spacing Tokens

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/AdptTypography.kt`
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/AdptShapes.kt`
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/AdptSpacing.kt`

- [ ] **Step 1: Create AdptTypography.kt**

```kotlin
package com.adpt.app.ui.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class AdptTypography(
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
)

val DefaultTypography = AdptTypography(
    titleLarge  = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall  = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium  = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall   = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    labelSmall  = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
)

val LocalAdptTypography = staticCompositionLocalOf<AdptTypography> { DefaultTypography }
```

- [ ] **Step 2: Create AdptShapes.kt**

```kotlin
package com.adpt.app.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object AdptShapes {
    val pill  = RoundedCornerShape(99.dp)
    val card  = RoundedCornerShape(16.dp)
    val small = RoundedCornerShape(8.dp)
}
```

- [ ] **Step 3: Create AdptSpacing.kt**

```kotlin
package com.adpt.app.ui.design

import androidx.compose.ui.unit.dp

object AdptSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/AdptTypography.kt \
        androidApp/src/main/kotlin/com/adpt/app/ui/design/AdptShapes.kt \
        androidApp/src/main/kotlin/com/adpt/app/ui/design/AdptSpacing.kt
git commit -m "feat: add typography, shape, and spacing tokens"
```

---

### Task 3: AdptTheme Provider

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/AdptTheme.kt`

- [ ] **Step 1: Create AdptTheme.kt**

```kotlin
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
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/AdptTheme.kt
git commit -m "feat: add AdptTheme composable and object accessor"
```

---

### Task 4: AdptSurface and AdptText

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptSurface.kt`
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptText.kt`

- [ ] **Step 1: Create AdptSurface.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptSurface(
    modifier: Modifier = Modifier,
    color: Color = AdptTheme.colors.surface,
    shape: Shape = AdptShapes.card,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color = color, shape = shape),
        content = content,
    )
}
```

- [ ] **Step 2: Create AdptText.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AdptTheme.typography.bodyMedium,
    color: Color = AdptTheme.colors.onSurface,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = overflow,
    )
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptSurface.kt \
        androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptText.kt
git commit -m "feat: add AdptSurface and AdptText atoms"
```

---

### Task 5: AdptIcon and AdptIconButton

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptIcon.kt`
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptIconButton.kt`

- [ ] **Step 1: Create AdptIcon.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = AdptTheme.colors.onSurface,
) {
    val painter = rememberVectorPainter(image = imageVector)
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(24.dp),
        colorFilter = ColorFilter.tint(tint),
        contentScale = ContentScale.Fit,
    )
}
```

- [ ] **Step 2: Create AdptIconButton.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun AdptIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 24.dp),
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptIcon.kt \
        androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptIconButton.kt
git commit -m "feat: add AdptIcon and AdptIconButton atoms"
```

---

### Task 6: Button Variants

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptButton.kt`

- [ ] **Step 1: Create AdptButton.kt** (contains all three button variants)

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = AdptTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(AdptShapes.pill)
            .background(
                color = if (enabled) colors.accent else colors.surfaceMuted,
                shape = AdptShapes.pill,
            )
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun AdptOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = AdptTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(AdptShapes.pill)
            .border(1.dp, if (enabled) colors.outline else colors.surfaceMuted, AdptShapes.pill)
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun AdptTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(AdptShapes.pill)
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptButton.kt
git commit -m "feat: add AdptButton, AdptOutlinedButton, AdptTextButton atoms"
```

---

### Task 7: AdptChip and AdptBadge

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptChip.kt`
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptBadge.kt`

- [ ] **Step 1: Create AdptChip.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

/**
 * Pill-shaped label with configurable background and content.
 * Used for severity summary chips, status labels, and "In list" badge.
 */
@Composable
fun AdptChip(
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(AdptShapes.pill)
            .background(color = containerColor, shape = AdptShapes.pill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
```

- [ ] **Step 2: Create AdptBadge.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Circular border-only badge for the "days remaining" indicator on OverviewScreen.
 */
@Composable
fun AdptCircleBadge(
    borderColor: Color,
    size: Dp = 48.dp,
    strokeWidth: Dp = 2.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .border(strokeWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptChip.kt \
        androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptBadge.kt
git commit -m "feat: add AdptChip and AdptBadge atoms"
```

---

### Task 8: AdptCheckbox

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptCheckbox.kt`

- [ ] **Step 1: Create AdptCheckbox.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = AdptTheme.colors
    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "checkProgress",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier = if (onCheckedChange != null) {
        Modifier
            .semantics { role = Role.Checkbox }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 18.dp),
                onClick = { onCheckedChange(!checked) },
            )
    } else Modifier

    Canvas(
        modifier = modifier
            .size(20.dp)
            .then(clickModifier),
    ) {
        val strokeWidth = 1.5.dp.toPx()
        val cornerRadius = CornerRadius(4.dp.toPx())
        val boxColor = if (checked) colors.accent else Color.Transparent
        val borderColor = if (checked) colors.accent else colors.outline

        drawRoundRect(color = boxColor, cornerRadius = cornerRadius)
        drawRoundRect(
            color = borderColor,
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth),
        )

        if (checkProgress > 0f) {
            val checkPath = Path().apply {
                moveTo(size.width * 0.20f, size.height * 0.50f)
                lineTo(size.width * 0.42f, size.height * 0.72f)
                lineTo(size.width * 0.80f, size.height * 0.28f)
            }
            val pathMeasure = PathMeasure().apply { setPath(checkPath, forceClosed = false) }
            val animatedPath = Path()
            pathMeasure.getSegment(
                startDistance = 0f,
                stopDistance = pathMeasure.length * checkProgress,
                destination = animatedPath,
                startWithMoveTo = true,
            )
            drawPath(
                path = animatedPath,
                color = colors.onAccent,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptCheckbox.kt
git commit -m "feat: add AdptCheckbox with animated Canvas drawing"
```

---

### Task 9: AdptProgressIndicator

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptProgressIndicator.kt`

- [ ] **Step 1: Create AdptProgressIndicator.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = AdptTheme.colors.accent,
    strokeWidth: Dp = 3.dp,
    size: Dp = 40.dp,
) {
    val transition = rememberInfiniteTransition(label = "progress")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
        ),
        label = "rotation",
    )

    val sweepAngle by transition.animateFloat(
        initialValue = 30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                30f at 0 using LinearEasing
                270f at 600 using LinearEasing
                30f at 1200 using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    Canvas(modifier = modifier.size(size)) {
        rotate(rotation) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptProgressIndicator.kt
git commit -m "feat: add AdptProgressIndicator Canvas spinner"
```

---

### Task 10: AdptCard

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptCard.kt`

- [ ] **Step 1: Create AdptCard.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.ripple.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = AdptShapes.card)
            .clip(AdptShapes.card)
            .background(color = AdptTheme.colors.surface, shape = AdptShapes.card),
        content = content,
    )
}

@Composable
fun AdptClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = AdptShapes.card)
            .clip(AdptShapes.card)
            .background(color = AdptTheme.colors.surface, shape = AdptShapes.card)
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
        content = content,
    )
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptCard.kt
git commit -m "feat: add AdptCard and AdptClickableCard"
```

---

### Task 11: AdptTopBar

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptTopBar.kt`

- [ ] **Step 1: Create AdptTopBar.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AdptTheme.colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                navigationIcon()
            } else {
                Spacer(modifier = Modifier.padding(start = 12.dp))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                title()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions()
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptTopBar.kt
git commit -m "feat: add AdptTopBar"
```

---

### Task 12: AdptScaffold

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptScaffold.kt`

- [ ] **Step 1: Create AdptScaffold.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

private enum class ScaffoldSlot { TopBar, BottomBar, Fab, Snackbar, Content }

@Composable
fun AdptScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)

        val topBarPlaceables = subcompose(ScaffoldSlot.TopBar, topBar).map { it.measure(loose) }
        val topBarHeight = topBarPlaceables.maxOfOrNull { it.height } ?: 0

        val bottomBarPlaceables = subcompose(ScaffoldSlot.BottomBar, bottomBar).map { it.measure(loose) }
        val bottomBarHeight = bottomBarPlaceables.maxOfOrNull { it.height } ?: 0

        val fabPlaceables = subcompose(ScaffoldSlot.Fab, floatingActionButton).map { it.measure(loose) }
        val fabHeight = fabPlaceables.maxOfOrNull { it.height } ?: 0
        val fabWidth = fabPlaceables.maxOfOrNull { it.width } ?: 0

        val snackbarPlaceables = subcompose(ScaffoldSlot.Snackbar, snackbarHost).map { it.measure(loose) }
        val snackbarHeight = snackbarPlaceables.maxOfOrNull { it.height } ?: 0
        val snackbarWidth = snackbarPlaceables.maxOfOrNull { it.width } ?: 0

        val contentPadding = PaddingValues(
            top = topBarHeight.toDp(),
            bottom = bottomBarHeight.toDp(),
        )
        val contentPlaceables = subcompose(ScaffoldSlot.Content) { content(contentPadding) }
            .map { it.measure(constraints.copy(minHeight = 0)) }

        val w = constraints.maxWidth
        val h = constraints.maxHeight

        layout(w, h) {
            contentPlaceables.forEach { it.placeRelative(0, 0) }
            topBarPlaceables.forEach { it.placeRelative(0, 0) }
            bottomBarPlaceables.forEach { it.placeRelative(0, h - bottomBarHeight) }

            val fabSpacing = 16.dp.roundToPx()
            fabPlaceables.forEach {
                it.placeRelative(
                    x = w - fabWidth - fabSpacing,
                    y = h - bottomBarHeight - fabHeight - fabSpacing,
                )
            }

            val snackbarSpacing = 8.dp.roundToPx()
            snackbarPlaceables.forEach {
                it.placeRelative(
                    x = (w - snackbarWidth) / 2,
                    y = h - bottomBarHeight - snackbarHeight - snackbarSpacing,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptScaffold.kt
git commit -m "feat: add AdptScaffold with SubcomposeLayout"
```

---

### Task 13: AdptFab

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptFab.kt`

- [ ] **Step 1: Create AdptFab.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = AdptTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = AdptShapes.pill)
            .clip(AdptShapes.pill)
            .background(color = colors.accent, shape = AdptShapes.pill)
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptFab.kt
git commit -m "feat: add AdptFab"
```

---

### Task 14: AdptNavBar

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptNavBar.kt`

- [ ] **Step 1: Create AdptNavBar.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

data class AdptNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun AdptNavBar(
    items: List<AdptNavItem>,
    currentRoute: String?,
    onItemSelected: (AdptNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AdptTheme.colors
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = navBottomPadding)
            .shadow(elevation = 8.dp, shape = AdptShapes.pill)
    ) {
        // Frosted glass background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = 0.99f }
                .blur(radius = 15.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    color = colors.surface.copy(alpha = 0.75f),
                    shape = AdptShapes.pill,
                )
        )
        // Nav items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AdptShapes.pill)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                AdptNavBarItem(
                    item = item,
                    selected = selected,
                    onSelected = { onItemSelected(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AdptNavBarItem(
    item: AdptNavItem,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AdptTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .background(
                color = if (selected) colors.accentMuted else Color.Transparent,
                shape = AdptShapes.pill,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onSelected,
            )
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AdptIcon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (selected) colors.accent else colors.onSurface.copy(alpha = 0.5f),
            )
            AdptText(
                text = item.label,
                style = AdptTheme.typography.labelSmall,
                color = if (selected) colors.accent else colors.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptNavBar.kt
git commit -m "feat: add AdptNavBar with frosted-glass effect"
```

---

### Task 15: AdptSnackbar

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptSnackbar.kt`

- [ ] **Step 1: Create AdptSnackbar.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme
import kotlinx.coroutines.delay

class AdptSnackbarHostState {
    var message by mutableStateOf<String?>(null)
        private set

    suspend fun showSnackbar(message: String) {
        this.message = message
        delay(3000)
        this.message = null
    }

    fun dismiss() { message = null }
}

@Composable
fun rememberAdptSnackbarHostState() = remember { AdptSnackbarHostState() }

@Composable
fun AdptSnackbarHost(hostState: AdptSnackbarHostState) {
    AnimatedVisibility(
        visible = hostState.message != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
    ) {
        hostState.message?.let { msg ->
            Box(
                modifier = Modifier
                    .shadow(elevation = 4.dp, shape = AdptShapes.small)
                    .clip(AdptShapes.small)
                    .background(color = AdptTheme.colors.onBackground, shape = AdptShapes.small)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                AdptText(
                    text = msg,
                    style = AdptTheme.typography.bodySmall,
                    color = AdptTheme.colors.background,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptSnackbar.kt
git commit -m "feat: add AdptSnackbar and AdptSnackbarHost"
```

---

### Task 16: AdptDialog

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptDialog.kt`

- [ ] **Step 1: Create AdptDialog.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .clip(AdptShapes.card)
                .background(color = AdptTheme.colors.surface, shape = AdptShapes.card)
                .padding(24.dp),
        ) {
            title()
            Spacer(modifier = Modifier.height(8.dp))
            text()
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptDialog.kt
git commit -m "feat: add AdptDialog"
```

---

### Task 17: AdptTextField

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptTextField.kt`

- [ ] **Step 1: Create AdptTextField.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

enum class AdptTextFieldVariant { Outlined, Transparent }

@Composable
fun AdptTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = false,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    variant: AdptTextFieldVariant = AdptTextFieldVariant.Outlined,
) {
    val colors = AdptTheme.colors
    val typography = AdptTheme.typography
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hasContent = value.isNotEmpty()

    val labelScale by animateFloatAsState(
        targetValue = if (focused || hasContent) 0.75f else 1f,
        animationSpec = tween(150),
        label = "labelScale",
    )
    val labelOffsetY by animateFloatAsState(
        targetValue = if (focused || hasContent) -22f else 0f,
        animationSpec = tween(150),
        label = "labelOffsetY",
    )

    val borderColor = when {
        isError -> colors.critical
        focused -> colors.accent
        else -> colors.outline
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (variant == AdptTextFieldVariant.Outlined) {
                        Modifier
                            .clip(AdptShapes.small)
                            .border(1.dp, borderColor, AdptShapes.small)
                    } else Modifier
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) { leadingIcon() }
                }
                Box(modifier = Modifier.weight(1f)) {
                    // Floating label
                    if (label != null) {
                        AdptText(
                            text = label,
                            style = typography.bodyMedium.copy(
                                fontSize = (typography.bodyMedium.fontSize.value * labelScale).sp,
                                color = when {
                                    isError -> colors.critical
                                    focused -> colors.accent
                                    else -> colors.onSurface.copy(alpha = 0.6f)
                                },
                            ),
                            modifier = Modifier
                                .padding(top = if (label != null && variant == AdptTextFieldVariant.Outlined) 18.dp else 0.dp)
                                .then(
                                    if (focused || hasContent)
                                        Modifier.padding(top = 0.dp)
                                    else Modifier
                                ),
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = if (label != null) 24.dp else 14.dp,
                                bottom = 10.dp,
                            ),
                        textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                        cursorBrush = SolidColor(colors.accent),
                        singleLine = singleLine,
                        readOnly = readOnly,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        visualTransformation = visualTransformation,
                        interactionSource = interactionSource,
                        decorationBox = { innerTextField ->
                            Box {
                                if (value.isEmpty() && placeholder != null && (label == null || focused)) {
                                    AdptText(
                                        text = placeholder,
                                        style = typography.bodyMedium,
                                        color = colors.onSurface.copy(alpha = 0.4f),
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
                if (trailingIcon != null) {
                    Box(modifier = Modifier.padding(start = 8.dp)) { trailingIcon() }
                }
            }
        }
        if (supportingText != null) {
            AdptText(
                text = supportingText,
                style = typography.labelSmall,
                color = if (isError) colors.critical else colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptTextField.kt
git commit -m "feat: add AdptTextField with animated floating label"
```

---

### Task 18: AdptDropdownMenu

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptDropdownMenu.kt`

- [ ] **Step 1: Create AdptDropdownMenu.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.ripple.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (expanded) {
        Popup(
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(100)) + scaleIn(tween(100), initialScale = 0.95f),
                exit = fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.95f),
            ) {
                Column(
                    modifier = modifier
                        .widthIn(min = 160.dp)
                        .shadow(elevation = 4.dp, shape = AdptShapes.small)
                        .clip(AdptShapes.small)
                        .background(color = AdptTheme.colors.surface, shape = AdptShapes.small),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun AdptDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (leadingIcon != null) {
            Box(modifier = Modifier.padding(end = 8.dp)) { leadingIcon() }
        }
        text()
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptDropdownMenu.kt
git commit -m "feat: add AdptDropdownMenu and AdptDropdownMenuItem"
```

---

### Task 19: AdptExposedDropdown

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptExposedDropdown.kt`

- [ ] **Step 1: Create AdptExposedDropdown.kt**

```kotlin
package com.adpt.app.ui.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptExposedDropdown(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedText: String,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        AdptTextField(
            value = selectedText,
            onValueChange = {},
            label = label,
            readOnly = true,
            trailingIcon = {
                AdptIconButton(onClick = { onExpandedChange(!expanded) }) {
                    AdptIcon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = AdptTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth(),
        )
        AdptDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/design/components/AdptExposedDropdown.kt
git commit -m "feat: add AdptExposedDropdown"
```

---

### Task 20: Wire AdptTheme into MainActivity

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/MainActivity.kt`

- [ ] **Step 1: Replace old `AdptTheme` import with the new one in MainActivity.kt**

Change the import from `com.adpt.app.ui.theme.AdptTheme` to `com.adpt.app.ui.design.AdptTheme`.

The `setContent { }` block stays exactly the same. Only the import changes:

```kotlin
// Remove:
import com.adpt.app.ui.theme.AdptTheme
// Add:
import com.adpt.app.ui.design.AdptTheme
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL. The app will look unchanged because screens still use Material3 internally — the new `AdptTheme` simply replaces the old Material3 wrapper at the root level.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/MainActivity.kt
git commit -m "feat: wire AdptTheme into MainActivity"
```

---

## Phase 2 — Screen Migration

### Task 21: Migrate OverviewScreen

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewScreen.kt`

- [ ] **Step 1: Rewrite OverviewScreen.kt**

Replace the file contents entirely. No `androidx.compose.material3` imports.

```kotlin
package com.adpt.app.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adpt.app.ui.components.AnimatedListItem
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.components.AdptBadge
import com.adpt.app.ui.design.components.AdptCard
import com.adpt.app.ui.design.components.AdptChip
import com.adpt.app.ui.design.components.AdptCircleBadge
import com.adpt.app.ui.design.components.AdptFab
import com.adpt.app.ui.design.components.AdptIcon
import com.adpt.app.ui.design.components.AdptIconButton
import com.adpt.app.ui.design.components.AdptProgressIndicator
import com.adpt.app.ui.design.components.AdptScaffold
import com.adpt.app.ui.design.components.AdptSnackbarHost
import com.adpt.app.ui.design.components.AdptText
import com.adpt.app.ui.design.components.AdptTopBar
import com.adpt.app.ui.design.components.rememberAdptSnackbarHostState
import com.adpt.shared.model.Severity
import kotlin.math.abs

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

@Composable
fun OverviewScreen(viewModel: OverviewViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = rememberAdptSnackbarHostState()

    LaunchedEffect(Unit) {
        viewModel.errors.collect { snackbarState.showSnackbar(it) }
    }

    val successItems = (uiState as? OverviewUiState.Success)?.items

    AdptScaffold(
        topBar = {
            AdptTopBar(
                title = {
                    AdptText("Overview", style = AdptTheme.typography.titleLarge)
                },
                actions = {
                    AdptIconButton(onClick = { viewModel.refresh() }) {
                        AdptIcon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        snackbarHost = { AdptSnackbarHost(snackbarState) },
        floatingActionButton = {
            if (!successItems.isNullOrEmpty()) {
                AdptFab(onClick = { viewModel.addAllToShoppingList(successItems.map { it.id }) }) {
                    AdptIcon(
                        Icons.Default.AddShoppingCart,
                        contentDescription = null,
                        tint = AdptTheme.colors.onAccent,
                    )
                    AdptText(
                        "Add all to list",
                        style = AdptTheme.typography.labelMedium,
                        color = AdptTheme.colors.onAccent,
                    )
                }
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            OverviewUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { AdptProgressIndicator() }

            is OverviewUiState.Success -> if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    AdptText(
                        "Nothing to show here",
                        style = AdptTheme.typography.bodyMedium,
                        color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                    )
                }
            } else {
                val criticalCount = state.items.count { it.severity == Severity.Critical }
                val highCount = state.items.count { it.severity == Severity.High }
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SummaryChip(
                            count = criticalCount,
                            label = "Critical",
                            containerColor = AdptTheme.colors.criticalContainer,
                            contentColor = AdptTheme.colors.onCriticalContainer,
                            modifier = Modifier.weight(1f),
                        )
                        SummaryChip(
                            count = highCount,
                            label = "High",
                            containerColor = AdptTheme.colors.warningContainer,
                            contentColor = AdptTheme.colors.onWarningContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            AnimatedListItem(
                                index = state.items.indexOf(item),
                                animationKey = state.listVersion,
                            ) {
                                OverviewItemCard(
                                    item = item,
                                    onAddToShoppingList = {
                                        viewModel.handleIntent(OverviewIntent.AddToShoppingList(item.id))
                                    },
                                    onIgnore = {
                                        viewModel.handleIntent(OverviewIntent.IgnoreItem(item.id))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    count: Int,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    AdptSurface(
        modifier = modifier,
        color = containerColor,
        shape = AdptTheme.shapes.card,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AdptText(count.toString(), style = AdptTheme.typography.titleLarge, color = contentColor)
            AdptText(label, style = AdptTheme.typography.labelMedium, color = contentColor)
        }
    }
}

@Composable
private fun OverviewItemCard(
    item: OverviewItemUiModel,
    onAddToShoppingList: () -> Unit,
    onIgnore: () -> Unit,
) {
    val colors = AdptTheme.colors
    val accentColor = when (item.severity) {
        Severity.Critical -> colors.critical
        Severity.High -> colors.warning
        Severity.Normal, Severity.Low -> colors.ok
    }

    AdptCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdptCircleBadge(borderColor = accentColor) {
                val label = when {
                    item.deltaMillis == null -> "--"
                    item.deltaMillis <= 0 -> "0d"
                    else -> "${item.deltaMillis / MILLIS_PER_DAY}d"
                }
                AdptText(label, style = AdptTheme.typography.labelMedium, color = accentColor)
            }
            Column(modifier = Modifier.weight(1f)) {
                AdptText(item.name, style = AdptTheme.typography.titleMedium)
                AdptText(
                    text = item.deltaMillis?.toDaysText() ?: "Not in stock",
                    style = AdptTheme.typography.bodySmall,
                    color = colors.onSurface.copy(alpha = 0.5f),
                )
            }
            if (item.isInShoppingList) {
                AdptChip(containerColor = colors.criticalContainer) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AdptIcon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = colors.onCriticalContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        AdptText(
                            "In list",
                            style = AdptTheme.typography.labelSmall,
                            color = colors.onCriticalContainer,
                        )
                    }
                }
            } else {
                AdptIconButton(onClick = onAddToShoppingList) {
                    AdptIcon(Icons.Default.ShoppingCart, contentDescription = "Add to shopping list")
                }
            }
            AdptIconButton(onClick = onIgnore) {
                AdptIcon(Icons.Default.Close, contentDescription = "Ignore item")
            }
        }
    }
}

private fun Long.toDaysText(): String {
    val days = abs(this / MILLIS_PER_DAY)
    return when {
        this <= 0 && days == 0L -> "Depleting today"
        this <= 0 -> "$days day(s) overdue"
        this < MILLIS_PER_DAY -> "Less than a day remaining"
        days == 1L -> "1 day remaining"
        else -> "$days days remaining"
    }
}
```

Note: `AdptSurface` is used inside `SummaryChip` — add the import:
```kotlin
import com.adpt.app.ui.design.components.AdptSurface
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL. Zero `androidx.compose.material3` imports in `OverviewScreen.kt`.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewScreen.kt
git commit -m "feat: migrate OverviewScreen to custom design system"
```

---

### Task 22: Migrate ShoppingScreen

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/shopping/ShoppingScreen.kt`

- [ ] **Step 1: Rewrite ShoppingScreen.kt**

Replace the file contents entirely. No `androidx.compose.material3` imports.

```kotlin
package com.adpt.app.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.adpt.app.ui.components.AnimatedListItem
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.components.AdptCard
import com.adpt.app.ui.design.components.AdptChip
import com.adpt.app.ui.design.components.AdptDialog
import com.adpt.app.ui.design.components.AdptFab
import com.adpt.app.ui.design.components.AdptIcon
import com.adpt.app.ui.design.components.AdptIconButton
import com.adpt.app.ui.design.components.AdptOutlinedButton
import com.adpt.app.ui.design.components.AdptProgressIndicator
import com.adpt.app.ui.design.components.AdptScaffold
import com.adpt.app.ui.design.components.AdptText
import com.adpt.app.ui.design.components.AdptTextField
import com.adpt.app.ui.design.components.AdptTextButton
import com.adpt.app.ui.design.components.AdptTopBar
import com.adpt.shared.model.ShoppingListStatus

@Composable
fun ShoppingScreen(
    navController: NavController,
    viewModel: ShoppingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingError by viewModel.pendingError.collectAsStateWithLifecycle()

    pendingError?.let { error ->
        AdptDialog(
            onDismissRequest = { viewModel.clearPendingError() },
            title = { AdptText("Could Not Update Shopping List", style = AdptTheme.typography.titleSmall) },
            text = { AdptText(error) },
            confirmButton = {
                AdptTextButton(onClick = { viewModel.clearPendingError() }) {
                    AdptText("OK", color = AdptTheme.colors.accent)
                }
            },
        )
    }

    var showEmptyConfirm by remember { mutableStateOf(false) }
    var purchasingItem by remember { mutableStateOf<ShoppingItemUiModel?>(null) }
    var removingItem by remember { mutableStateOf<ShoppingItemUiModel?>(null) }

    if (showEmptyConfirm) {
        AdptDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { AdptText("Empty Shopping List", style = AdptTheme.typography.titleSmall) },
            text = { AdptText("Remove all items from the shopping list?") },
            confirmButton = {
                AdptTextButton(onClick = {
                    viewModel.handleIntent(ShoppingIntent.EmptyList)
                    showEmptyConfirm = false
                }) { AdptText("Empty", color = AdptTheme.colors.accent) }
            },
            dismissButton = {
                AdptTextButton(onClick = { showEmptyConfirm = false }) {
                    AdptText("Cancel", color = AdptTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            },
        )
    }

    removingItem?.let { item ->
        AdptDialog(
            onDismissRequest = { removingItem = null },
            title = { AdptText("Remove Item", style = AdptTheme.typography.titleSmall) },
            text = { AdptText("Remove ${item.name} from the shopping list?") },
            confirmButton = {
                AdptTextButton(onClick = {
                    viewModel.handleIntent(ShoppingIntent.RemoveEntry(item.entryId))
                    removingItem = null
                }) { AdptText("Remove", color = AdptTheme.colors.critical) }
            },
            dismissButton = {
                AdptTextButton(onClick = { removingItem = null }) {
                    AdptText("Cancel", color = AdptTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            },
        )
    }

    purchasingItem?.let { item ->
        var quantity by remember { mutableStateOf("") }
        var quantityError by remember { mutableStateOf<String?>(null) }
        AdptDialog(
            onDismissRequest = { purchasingItem = null },
            title = { AdptText("Mark as Purchased", style = AdptTheme.typography.titleSmall) },
            text = {
                Column {
                    AdptText("How much ${item.name} did you buy?")
                    Spacer(Modifier.height(8.dp))
                    AdptTextField(
                        value = quantity,
                        onValueChange = { quantity = it; quantityError = null },
                        label = "Quantity",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = quantityError != null,
                        supportingText = quantityError,
                    )
                }
            },
            confirmButton = {
                AdptTextButton(onClick = {
                    val amount = quantity.toDoubleOrNull()
                    when {
                        quantity.isBlank() -> quantityError = "Quantity is required"
                        amount == null -> quantityError = "Enter a valid number"
                        amount <= 0.0 -> quantityError = "Must be greater than 0"
                        else -> {
                            viewModel.handleIntent(ShoppingIntent.MarkAsPurchased(item.entryId, item.itemId, amount))
                            purchasingItem = null
                        }
                    }
                }) { AdptText("Confirm", color = AdptTheme.colors.accent) }
            },
            dismissButton = {
                AdptTextButton(onClick = { purchasingItem = null }) {
                    AdptText("Cancel", color = AdptTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            },
        )
    }

    AdptScaffold(
        topBar = {
            AdptTopBar(
                title = { AdptText("Shopping", style = AdptTheme.typography.titleLarge) },
                actions = {
                    AdptIconButton(onClick = { showEmptyConfirm = true }) {
                        AdptIcon(Icons.Default.Delete, contentDescription = "Empty list")
                    }
                },
            )
        },
        floatingActionButton = {
            AdptFab(onClick = { navController.navigate("items?selectionMode=true") }) {
                AdptIcon(Icons.Default.Add, contentDescription = null, tint = AdptTheme.colors.onAccent)
            }
        },
    ) { innerPadding ->
        when (val state = uiState) {
            ShoppingUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { AdptProgressIndicator() }

            is ShoppingUiState.Success -> if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    AdptText(
                        "Nothing here to show",
                        style = AdptTheme.typography.bodyMedium,
                        color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.entryId }) { item ->
                        AnimatedListItem(index = state.items.indexOf(item)) {
                            ShoppingItemCard(
                                item = item,
                                onMarkAsPurchased = { purchasingItem = item },
                                onRemove = { removingItem = item },
                            )
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AdptOutlinedButton(onClick = { viewModel.handleIntent(ShoppingIntent.ClearList) }) {
                                AdptText("Clear Purchased", color = AdptTheme.colors.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemCard(
    item: ShoppingItemUiModel,
    onMarkAsPurchased: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = AdptTheme.colors
    AdptCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AdptText(item.name, style = AdptTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                val (chipBg, chipFg) = when (item.status) {
                    ShoppingListStatus.Pending -> colors.warningContainer to colors.onWarningContainer
                    ShoppingListStatus.Purchased -> colors.criticalContainer to colors.onCriticalContainer
                }
                AdptChip(containerColor = chipBg) {
                    AdptText(item.status.name, style = AdptTheme.typography.labelSmall, color = chipFg)
                }
                if (item.status == ShoppingListStatus.Purchased) {
                    item.purchasedQuantity?.let { qty ->
                        Spacer(Modifier.height(2.dp))
                        AdptText("Qty: $qty", style = AdptTheme.typography.bodySmall, color = colors.onSurface.copy(alpha = 0.5f))
                    }
                    item.depletionLabel?.let { label ->
                        Spacer(Modifier.height(2.dp))
                        AdptText(label, style = AdptTheme.typography.bodySmall, color = colors.accent)
                    }
                }
            }
            if (item.status == ShoppingListStatus.Pending) {
                AdptIconButton(onClick = onMarkAsPurchased) {
                    AdptIcon(Icons.Default.Check, contentDescription = "Mark as purchased")
                }
            }
            AdptIconButton(onClick = onRemove) {
                AdptIcon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL. Zero `androidx.compose.material3` imports in `ShoppingScreen.kt`.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/shopping/ShoppingScreen.kt
git commit -m "feat: migrate ShoppingScreen to custom design system"
```

---

### Task 23: Migrate StockScreen

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/stock/StockScreen.kt`

- [ ] **Step 1: Rewrite StockScreen.kt**

```kotlin
package com.adpt.app.ui.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adpt.app.ui.components.AnimatedListItem
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.components.AdptCard
import com.adpt.app.ui.design.components.AdptDialog
import com.adpt.app.ui.design.components.AdptIcon
import com.adpt.app.ui.design.components.AdptIconButton
import com.adpt.app.ui.design.components.AdptProgressIndicator
import com.adpt.app.ui.design.components.AdptScaffold
import com.adpt.app.ui.design.components.AdptText
import com.adpt.app.ui.design.components.AdptTextButton
import com.adpt.app.ui.design.components.AdptTopBar

@Composable
fun StockScreen(viewModel: StockViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var depletingItem by remember { mutableStateOf<StockItemUiModel?>(null) }

    depletingItem?.let { item ->
        AdptDialog(
            onDismissRequest = { depletingItem = null },
            title = { AdptText("Update consumption rate?", style = AdptTheme.typography.titleSmall) },
            text = { AdptText("Would you like to recalculate the consumption rate based on actual usage since the last purchase?") },
            confirmButton = {
                AdptTextButton(onClick = {
                    viewModel.markDepleted(item.id, updateRate = true)
                    depletingItem = null
                }) { AdptText("Yes", color = AdptTheme.colors.accent) }
            },
            dismissButton = {
                AdptTextButton(onClick = {
                    viewModel.markDepleted(item.id, updateRate = false)
                    depletingItem = null
                }) { AdptText("No", color = AdptTheme.colors.onSurface.copy(alpha = 0.6f)) }
            },
        )
    }

    AdptScaffold(
        topBar = { AdptTopBar(title = { AdptText("Stock", style = AdptTheme.typography.titleLarge) }) },
    ) { innerPadding ->
        when (val state = uiState) {
            StockUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { AdptProgressIndicator() }

            is StockUiState.Success -> if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    AdptText(
                        "Nothing here to show",
                        style = AdptTheme.typography.bodyMedium,
                        color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        AnimatedListItem(index = state.items.indexOf(item)) {
                            StockItemCard(item = item, onMarkDepleted = { depletingItem = item })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockItemCard(item: StockItemUiModel, onMarkDepleted: () -> Unit) {
    AdptCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AdptText(item.name, style = AdptTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                AdptText(
                    text = item.daysRemainingLabel,
                    style = AdptTheme.typography.bodySmall,
                    color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
            AdptText(
                text = "${item.remainingQuantity.formatQuantity()} ${item.unit.name}",
                style = AdptTheme.typography.bodyMedium,
                color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
            )
            AdptIconButton(onClick = onMarkDepleted) {
                AdptIcon(
                    imageVector = Icons.Outlined.RemoveShoppingCart,
                    contentDescription = "Mark as depleted",
                    tint = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun Double.formatQuantity(): String =
    if (this % 1.0 == 0.0) toLong().toString() else "%.1f".format(this)
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL. Zero `androidx.compose.material3` imports in `StockScreen.kt`.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/stock/StockScreen.kt
git commit -m "feat: migrate StockScreen to custom design system"
```

---

### Task 24: Migrate ItemsScreen

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/items/ItemsScreen.kt`

- [ ] **Step 1: Rewrite ItemsScreen.kt**

```kotlin
package com.adpt.app.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.adpt.app.ui.components.AnimatedListItem
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.components.AdptButton
import com.adpt.app.ui.design.components.AdptCard
import com.adpt.app.ui.design.components.AdptCheckbox
import com.adpt.app.ui.design.components.AdptChip
import com.adpt.app.ui.design.components.AdptClickableCard
import com.adpt.app.ui.design.components.AdptDialog
import com.adpt.app.ui.design.components.AdptDropdownMenu
import com.adpt.app.ui.design.components.AdptDropdownMenuItem
import com.adpt.app.ui.design.components.AdptExposedDropdown
import com.adpt.app.ui.design.components.AdptFab
import com.adpt.app.ui.design.components.AdptIcon
import com.adpt.app.ui.design.components.AdptIconButton
import com.adpt.app.ui.design.components.AdptOutlinedButton
import com.adpt.app.ui.design.components.AdptProgressIndicator
import com.adpt.app.ui.design.components.AdptScaffold
import com.adpt.app.ui.design.components.AdptSnackbarHost
import com.adpt.app.ui.design.components.AdptSurface
import com.adpt.app.ui.design.components.AdptText
import com.adpt.app.ui.design.components.AdptTextField
import com.adpt.app.ui.design.components.AdptTextButton
import com.adpt.app.ui.design.components.AdptTextFieldVariant
import com.adpt.app.ui.design.components.AdptTopBar
import com.adpt.app.ui.design.components.rememberAdptSnackbarHostState
import com.adpt.shared.model.ItemPriority
import com.adpt.shared.model.ItemUnit

@Composable
fun ItemsScreen(
    navController: NavController,
    viewModel: ItemsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItemUiModel?>(null) }
    val snackbarState = rememberAdptSnackbarHostState()

    LaunchedEffect(viewModel.snackbarMessage) {
        viewModel.snackbarMessage.collect { snackbarState.showSnackbar(it) }
    }

    LaunchedEffect(viewModel.navigationEvent) {
        viewModel.navigationEvent.collect { navController.popBackStack() }
    }

    if (showAddDialog) {
        ItemFormDialog(
            title = "Add Item",
            confirmLabel = "Add",
            initialItem = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, unit, priority, rate ->
                viewModel.handleIntent(ItemsIntent.AddItemConfirmed(name, unit, priority, rate))
            },
            resultFlow = viewModel.addItemResult,
            onSuccess = { showAddDialog = false },
        )
    }

    editingItem?.let { item ->
        ItemFormDialog(
            title = "Edit Item",
            confirmLabel = "Save",
            initialItem = item,
            onDismiss = { editingItem = null },
            onConfirm = { name, unit, priority, rate ->
                viewModel.handleIntent(ItemsIntent.EditItemConfirmed(item.id, name, unit, priority, rate))
            },
            resultFlow = viewModel.editItemResult,
            onSuccess = { editingItem = null },
        )
    }

    AdptScaffold(
        topBar = {
            ItemsTopBar(uiState = uiState, onIntent = viewModel::handleIntent)
        },
        snackbarHost = { AdptSnackbarHost(snackbarState) },
        floatingActionButton = {
            AdptFab(onClick = { showAddDialog = true }) {
                AdptIcon(Icons.Default.Add, contentDescription = null, tint = AdptTheme.colors.onAccent)
            }
        },
        bottomBar = {
            if (uiState.selectionMode) {
                SelectionActionStrip(
                    selectedCount = uiState.selectedItemIds.size,
                    onCancel = { viewModel.handleIntent(ItemsIntent.SelectionCancelled) },
                    onConfirm = { viewModel.handleIntent(ItemsIntent.SelectionConfirmed) },
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { AdptProgressIndicator() }

            uiState.items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                AdptText(
                    "Nothing here to show",
                    style = AdptTheme.typography.bodyMedium,
                    color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    AnimatedListItem(
                        index = uiState.items.indexOf(item),
                        animationKey = Pair(uiState.sortOrder, uiState.priorityFilter),
                    ) {
                        ItemCard(
                            item = item,
                            selectionMode = uiState.selectionMode,
                            isSelected = item.id in uiState.selectedItemIds,
                            onEdit = { editingItem = item },
                            onIntent = viewModel::handleIntent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemsTopBar(uiState: ItemsUiState, onIntent: (ItemsIntent) -> Unit) {
    if (uiState.selectionMode) {
        AdptTopBar(title = { AdptText("Add to shopping list", style = AdptTheme.typography.titleLarge) })
    } else if (uiState.isSearchActive) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        AdptTopBar(
            title = {
                AdptTextField(
                    value = uiState.searchQuery,
                    onValueChange = { onIntent(ItemsIntent.SearchQueryChanged(it)) },
                    placeholder = "Search items…",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    variant = AdptTextFieldVariant.Transparent,
                    modifier = Modifier.focusRequester(focusRequester),
                )
            },
            navigationIcon = {
                AdptIconButton(onClick = { onIntent(ItemsIntent.SearchToggled) }) {
                    AdptIcon(Icons.Default.ArrowBack, contentDescription = "Close search")
                }
            },
        )
    } else {
        var showSortMenu by remember { mutableStateOf(false) }
        var showFilterMenu by remember { mutableStateOf(false) }
        AdptTopBar(
            title = { AdptText("Items", style = AdptTheme.typography.titleLarge) },
            actions = {
                AdptIconButton(onClick = { onIntent(ItemsIntent.SearchToggled) }) {
                    AdptIcon(Icons.Default.Search, contentDescription = "Search")
                }
                Box {
                    AdptIconButton(onClick = { showSortMenu = true }) {
                        AdptIcon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    AdptDropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        SortOrder.entries.forEach { order ->
                            AdptDropdownMenuItem(
                                text = {
                                    AdptText(
                                        text = order.label + if (uiState.sortOrder == order) " ✓" else "",
                                        color = if (uiState.sortOrder == order) AdptTheme.colors.accent
                                                else AdptTheme.colors.onSurface,
                                    )
                                },
                                onClick = { onIntent(ItemsIntent.SortOrderChanged(order)); showSortMenu = false },
                            )
                        }
                    }
                }
                Box {
                    AdptIconButton(onClick = { showFilterMenu = true }) {
                        AdptIcon(Icons.Default.FilterList, contentDescription = "Filter by priority")
                    }
                    AdptDropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        ItemPriority.entries.forEach { priority ->
                            AdptDropdownMenuItem(
                                text = { AdptText(priority.name) },
                                onClick = { onIntent(ItemsIntent.PriorityFilterToggled(priority)) },
                                leadingIcon = {
                                    AdptCheckbox(
                                        checked = priority in uiState.priorityFilter,
                                        onCheckedChange = { onIntent(ItemsIntent.PriorityFilterToggled(priority)) },
                                    )
                                },
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun ItemCard(
    item: ItemUiModel,
    selectionMode: Boolean,
    isSelected: Boolean,
    onEdit: () -> Unit,
    onIntent: (ItemsIntent) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val cardContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                AdptCheckbox(checked = isSelected, onCheckedChange = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                AdptText(item.name, style = AdptTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                AdptText(
                    "${item.unit.name} · ${item.consumptionRate}/day",
                    style = AdptTheme.typography.bodySmall,
                    color = AdptTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
            PriorityBadge(priority = item.priority)
            if (!selectionMode) {
                Box {
                    AdptIconButton(onClick = { showMenu = true }) {
                        AdptIcon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    AdptDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        AdptDropdownMenuItem(
                            text = { AdptText("Edit") },
                            onClick = { onEdit(); showMenu = false },
                        )
                        AdptDropdownMenuItem(
                            text = { AdptText("Remove") },
                            onClick = { onIntent(ItemsIntent.RemoveItem(item.id)); showMenu = false },
                        )
                        AdptDropdownMenuItem(
                            text = { AdptText("Add to Shopping List") },
                            onClick = { onIntent(ItemsIntent.AddToShoppingList(item.id)); showMenu = false },
                        )
                    }
                }
            }
        }
    }

    if (selectionMode) {
        AdptClickableCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onIntent(ItemsIntent.ToggleItemSelection(item.id)) },
        ) { cardContent() }
    } else {
        AdptCard(modifier = Modifier.fillMaxWidth()) { cardContent() }
    }
}

@Composable
private fun SelectionActionStrip(selectedCount: Int, onCancel: () -> Unit, onConfirm: () -> Unit) {
    AdptSurface(
        color = AdptTheme.colors.surface,
        shape = AdptShapes.small,
        modifier = Modifier.windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AdptOutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                AdptText("Cancel", color = AdptTheme.colors.onSurface)
            }
            AdptButton(onClick = onConfirm, enabled = selectedCount > 0, modifier = Modifier.weight(1f)) {
                AdptText(
                    text = if (selectedCount == 1) "Add 1 item" else "Add $selectedCount items",
                    color = AdptTheme.colors.onAccent,
                )
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: ItemPriority) {
    val colors = AdptTheme.colors
    val (bg, fg) = when (priority) {
        ItemPriority.Highest -> colors.criticalContainer to colors.onCriticalContainer
        ItemPriority.High -> colors.warningContainer to colors.onWarningContainer
        ItemPriority.Normal -> colors.accentMuted to colors.accent
        ItemPriority.Low, ItemPriority.Lowest -> colors.surfaceMuted to colors.onSurface.copy(alpha = 0.5f)
    }
    AdptChip(containerColor = bg, modifier = Modifier.padding(end = 4.dp)) {
        AdptText(priority.name, style = AdptTheme.typography.labelSmall, color = fg)
    }
}

@Composable
private fun ItemFormDialog(
    title: String,
    confirmLabel: String,
    initialItem: ItemUiModel?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, unit: ItemUnit, priority: ItemPriority, rate: Double) -> Unit,
    resultFlow: kotlinx.coroutines.flow.SharedFlow<String?>,
    onSuccess: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialItem?.name ?: "") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUnit by rememberSaveable { mutableStateOf(initialItem?.unit ?: ItemUnit.PIECE) }
    var unitExpanded by remember { mutableStateOf(false) }
    var selectedPriority by rememberSaveable { mutableStateOf(initialItem?.priority ?: ItemPriority.Normal) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var rateText by rememberSaveable { mutableStateOf(initialItem?.consumptionRate?.toString() ?: "") }
    var rateError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        resultFlow.collect { error -> if (error == null) onSuccess() else nameError = error }
    }

    fun validate(): Boolean {
        var valid = true
        if (name.isBlank()) { nameError = "Name is required"; valid = false }
        val rate = rateText.toDoubleOrNull()
        when {
            rateText.isBlank() -> { rateError = "Consumption rate is required"; valid = false }
            rate == null -> { rateError = "Enter a valid number"; valid = false }
            rate <= 0.0 -> { rateError = "Must be greater than 0"; valid = false }
        }
        return valid
    }

    AdptDialog(
        onDismissRequest = onDismiss,
        title = { AdptText(title, style = AdptTheme.typography.titleSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdptTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = "Name",
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError,
                    modifier = Modifier.fillMaxWidth(),
                )
                AdptExposedDropdown(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    selectedText = selectedUnit.name,
                    label = "Unit",
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ItemUnit.entries.forEach { unit ->
                        AdptDropdownMenuItem(
                            text = { AdptText(unit.name) },
                            onClick = { selectedUnit = unit; unitExpanded = false },
                        )
                    }
                }
                AdptExposedDropdown(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = it },
                    selectedText = selectedPriority.name,
                    label = "Priority",
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ItemPriority.entries.forEach { priority ->
                        AdptDropdownMenuItem(
                            text = { AdptText(priority.name) },
                            onClick = { selectedPriority = priority; priorityExpanded = false },
                        )
                    }
                }
                AdptTextField(
                    value = rateText,
                    onValueChange = { rateText = it; rateError = null },
                    label = "Consumption rate / day",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = rateError != null,
                    supportingText = rateError,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            AdptTextButton(onClick = {
                if (validate()) onConfirm(name.trim(), selectedUnit, selectedPriority, rateText.toDouble())
            }) { AdptText(confirmLabel, color = AdptTheme.colors.accent) }
        },
        dismissButton = {
            AdptTextButton(onClick = onDismiss) {
                AdptText("Cancel", color = AdptTheme.colors.onSurface.copy(alpha = 0.6f))
            }
        },
    )
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL. Zero `androidx.compose.material3` imports in `ItemsScreen.kt`.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/items/ItemsScreen.kt
git commit -m "feat: migrate ItemsScreen to custom design system"
```

---

### Task 25: Migrate AppNavigation

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/navigation/AppNavigation.kt`

- [ ] **Step 1: Rewrite AppNavigation.kt**

```kotlin
package com.adpt.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adpt.app.AdptApplication
import com.adpt.app.ui.design.AdptTheme
import com.adpt.app.ui.design.components.AdptNavBar
import com.adpt.app.ui.design.components.AdptNavItem
import com.adpt.app.ui.items.ItemsScreen
import com.adpt.app.ui.overview.OverviewScreen
import com.adpt.app.ui.shopping.ShoppingScreen
import com.adpt.app.ui.stock.StockScreen
import kotlinx.coroutines.flow.filterNotNull

private val navItems = listOf(
    AdptNavItem("overview", "Overview", Icons.Default.AvTimer),
    AdptNavItem("shopping", "Shopping", Icons.Default.ShoppingCart),
    AdptNavItem("stock", "Stock", Icons.Default.Inventory),
    AdptNavItem("items", "Items", Icons.Default.Category),
)

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as AdptApplication
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        app.pendingNavTarget.filterNotNull().collect { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            app.pendingNavTarget.value = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AdptTheme.colors.background),
    ) {
        NavHost(
            navController = navController,
            startDestination = "overview",
            modifier = Modifier.fillMaxSize(),
        ) {
            composable("overview") { OverviewScreen() }
            composable("shopping") { ShoppingScreen(navController = navController) }
            composable("stock") { StockScreen() }
            composable(
                route = "items?selectionMode={selectionMode}",
                arguments = listOf(
                    navArgument("selectionMode") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                ),
            ) { ItemsScreen(navController = navController) }
        }

        AdptNavBar(
            items = navItems,
            currentRoute = navItems.firstOrNull { route ->
                currentRoute == route.route ||
                        (route.route == "items" && currentRoute?.startsWith("items?") == true)
            }?.route,
            onItemSelected = { item ->
                navController.navigate(item.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
```

- [ ] **Step 2: Verify build**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL. Zero `androidx.compose.material3` imports in `AppNavigation.kt`.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/navigation/AppNavigation.kt
git commit -m "feat: migrate AppNavigation to custom design system"
```

---

## Phase 3 — Cleanup

### Task 26: Remove Material3 Dependency

**Files:**
- Modify: `androidApp/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Delete: `androidApp/src/main/kotlin/com/adpt/app/ui/theme/Color.kt`
- Delete: `androidApp/src/main/kotlin/com/adpt/app/ui/theme/Theme.kt`

- [ ] **Step 1: Add material-ripple to libs.versions.toml**

In `gradle/libs.versions.toml`, add inside `[libraries]`:

```toml
compose-material-ripple = { group = "androidx.compose.material", name = "material-ripple" }
```

- [ ] **Step 2: Update androidApp/build.gradle.kts**

Remove `libs.compose.material3` and add `libs.compose.material.ripple`:

```kotlin
// REMOVE this line:
implementation(libs.compose.material3)

// ADD this line (keep all others):
implementation(libs.compose.material.ripple)
```

The full dependencies block after the change:

```kotlin
dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.datetime)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.ripple)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.activity)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.lifecycle.runtime)
    implementation(libs.navigation.compose)
    implementation(libs.workmanager)

    debugImplementation(libs.compose.ui.tooling)
}
```

- [ ] **Step 3: Delete old theme files**

```bash
rm androidApp/src/main/kotlin/com/adpt/app/ui/theme/Color.kt
rm androidApp/src/main/kotlin/com/adpt/app/ui/theme/Theme.kt
```

- [ ] **Step 4: Final build verification**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL with zero Material3 compile-time dependency.

- [ ] **Step 5: Commit**

```bash
git add androidApp/build.gradle.kts gradle/libs.versions.toml
git rm androidApp/src/main/kotlin/com/adpt/app/ui/theme/Color.kt \
       androidApp/src/main/kotlin/com/adpt/app/ui/theme/Theme.kt
git commit -m "feat: remove Material3 dependency, complete custom design system migration"
```
