# Custom UI Design System — adpt

**Date:** 2026-04-06
**Status:** Approved

## Goal

Replace all Material3 UI components with a fully custom Compose Foundation–based design system. The new UI must feel warm, calm, and home-like — reinforcing the app's purpose of reducing the stress of keeping home stock up to date. No Material3 imports anywhere in the UI layer after migration.

---

## Design Decisions

| Dimension | Decision |
|---|---|
| Aesthetic | Minimal & Clean |
| Color palette | Terracotta & Cream |
| Shape language | Soft & Pill-like |
| Dark mode | Yes — both light and dark themes |
| Typography | System font (no custom font import) |

---

## Section 1 — Design Tokens

All tokens live in `androidApp/src/main/kotlin/com/adpt/app/ui/design/`.

### Colors — `AdptColors`

A data class with semantically named color slots. Two instances: `LightColors` and `DarkColors`.

```
background       // page/screen background
onBackground     // default text on background
surface          // card / sheet background
onSurface        // text on surface
surfaceMuted     // subtle dividers, placeholder areas
accent           // primary action color (terracotta)
onAccent         // text/icon on accent
accentMuted      // faint tint for selected/active states
critical         // severity: critical items
onCritical       // text/icon on critical
warning          // severity: high items
onWarning        // text/icon on warning
ok               // severity: normal / low items
onOk             // text/icon on ok
outline          // borders, subtle strokes
```

**Light palette values:**
```
background       #faf7f3
onBackground     #2a1f17
surface          #ffffff
onSurface        #2a1f17
surfaceMuted     #f0ebe3
accent           #b5613a
onAccent         #ffffff
accentMuted      rgba(181,97,58, 0.10)
critical         #b5613a
onCritical       #ffffff
warning          #c09030
onWarning        #ffffff
ok               #5a7a5c
onOk             #ffffff
outline          #e8e2d8
```

**Dark palette values:**
```
background       #1f1a17
onBackground     #ede6dc
surface          #2d2520
onSurface        #ede6dc
surfaceMuted     #3d2f28
accent           #e8a07a
onAccent         #1f1a17
accentMuted      rgba(232,160,122, 0.12)
critical         #e8a07a
onCritical       #1f1a17
warning          #d4a840
onWarning        #1f1a17
ok               #a3c4a6
onOk             #1f1a17
outline          rgba(237,230,220, 0.10)
```

Severity chip backgrounds (tinted, not solid):

| Severity | Light bg | Light fg | Dark bg | Dark fg |
|---|---|---|---|---|
| Critical | `#fce4d6` | `#8b3520` | `#4a2216` | `#f5c0a0` |
| High | `#fef2cc` | `#8a5e00` | `#3d3010` | `#f0d890` |

### Typography — `AdptTypography`

A data class of `TextStyle` values using the system font. All weights use `FontWeight` constants — no font file imports.

| Token | Size | Weight | Usage |
|---|---|---|---|
| `titleLarge` | 22sp | Bold 700 | Screen title in top bar |
| `titleMedium` | 16sp | SemiBold 600 | Card item name |
| `titleSmall` | 14sp | SemiBold 600 | Dialog titles, section headings |
| `bodyMedium` | 14sp | Normal 400 | Body text in cards |
| `bodySmall` | 12sp | Normal 400 | Subtitles, secondary info |
| `labelMedium` | 12sp | SemiBold 600 | Badges, chip labels, nav labels |
| `labelSmall` | 10sp | SemiBold 600 | Priority badges, small status labels |

### Shape — `AdptShapes`

```kotlin
object AdptShapes {
    val pill  = RoundedCornerShape(99.dp)   // chips, badges, buttons, FAB, icon buttons
    val card  = RoundedCornerShape(16.dp)   // cards, dialogs, summary chips
    val small = RoundedCornerShape(8.dp)    // text fields, dropdown menus, snackbars
}
```

### Spacing — `AdptSpacing`

```kotlin
object AdptSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
```

### Theme Provider — `AdptTheme`

A single composable wrapping four `CompositionLocal` providers:

```kotlin
@Composable
fun AdptTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
)
```

Access via object accessors:

```kotlin
object AdptTheme {
    val colors: AdptColors @Composable get() = LocalAdptColors.current
    val typography: AdptTypography @Composable get() = LocalAdptTypography.current
    val shapes: AdptShapes get() = AdptShapes
    val spacing: AdptSpacing get() = AdptSpacing
}
```

`MainActivity` wraps everything in `AdptTheme`. `MaterialTheme` is removed entirely.

---

## Section 2 — Component Library

All components live in `ui/design/components/`. Zero `androidx.compose.material3` imports. Uses only `androidx.compose.foundation`, `androidx.compose.animation`, and `androidx.compose.ui`.

### Atoms

**`AdptSurface`**
`Box` with `Modifier.background(color, shape).clip(shape)`. Replaces `Surface`.

**`AdptText`**
Thin wrapper over `BasicText` that resolves a `TextStyle` slot from `AdptTheme.typography`. Replaces `Text`.

**`AdptIcon`**
Wraps `androidx.compose.ui.graphics.vector.Image` (not Material's `Icon`). Accepts `ImageVector` + `tint` + `contentDescription`.

**`AdptIconButton`**
Clickable `Box` with `indication = rememberRipple()`, minimum 44×44dp touch target, pill shape. Replaces `IconButton`.

**`AdptButton`**
Filled pill button. Background = `AdptTheme.colors.accent`, text = `onAccent`. Replaces `Button`.

**`AdptOutlinedButton`**
Pill shape, `Modifier.border(1.dp, outline, pill)`, transparent background. Replaces `OutlinedButton`.

**`AdptTextButton`**
Transparent background, accent-colored label. Replaces `TextButton`.

**`AdptChip`**
Small pill (`AdptShapes.pill`) with `containerColor` + `contentColor` slots. Used for severity summary chips, "In list" badge, status badges.

**`AdptBadge`**
Pill or circular indicator. Used for the days-remaining ring on Overview (circular, border-only variant) and priority labels (filled pill variant).

**`AdptCheckbox`**
Custom `Canvas`-drawn animated checkbox. Animates the check stroke on toggle. Replaces `Checkbox`.

**`AdptProgressIndicator`**
Rotating arc drawn on `Canvas` using `InfiniteTransition`. Replaces `CircularProgressIndicator`.

### Molecules

**`AdptCard`**
`AdptSurface` with `AdptShapes.card` and `Modifier.shadow(elevation, AdptShapes.card)`. Replaces `Card` and `ElevatedCard`.

**`AdptTopBar`**
`Row` with `title: @Composable () -> Unit`, `navigationIcon`, and `actions` slots. Handles `WindowInsets.statusBars` padding. Replaces `TopAppBar`.

**`AdptFab`**
Pill-shaped (`AdptShapes.pill`) clickable container. Supports icon-only and icon+label variants. Background = `accent`. Replaces `FloatingActionButton`.

**`AdptNavBar`**
Floating pill nav bar. Keeps the current frosted-glass layered blur effect. Active item highlighted with `accentMuted` background + `accent` icon/label color. Replaces `NavigationBar` + `NavigationBarItem`.

**`AdptScaffold`**
Custom `Box`-based layout composable wiring `topBar`, `bottomBar`, `floatingActionButton`, `snackbarHost`, and `content` slots. Manages `WindowInsets` padding. Replaces `Scaffold`.

**`AdptSnackbarHost` / `AdptSnackbar`**
Custom snackbar with slide-up enter and fade-out exit animation. Shape = `AdptShapes.small`. Replaces `SnackbarHost` / `Snackbar`.

**`AdptDialog`**
Uses `androidx.compose.ui.window.Dialog`. Draws a card-shaped (`AdptShapes.card`) container with `title`, `content`, `confirmButton`, and `dismissButton` slots. Replaces `AlertDialog`.

**`AdptTextField`**
`BasicTextField` with animated floating label, filled or outlined variants, error state with message slot, and trailing icon slot. Uses `AdptShapes.small`. Replaces `OutlinedTextField` and `TextField`.

**`AdptDropdownMenu`**
`Popup`-based dropdown with enter/exit animation. `AdptShapes.small` container. Item composable accepts leading icon + text. Replaces `DropdownMenu` + `DropdownMenuItem`.

**`AdptExposedDropdown`**
Composes `AdptTextField` (read-only, trailing chevron) + `AdptDropdownMenu`. Replaces `ExposedDropdownMenuBox`.

---

## Section 3 — Migration Phases

### Phase 1 — Design System

Build all tokens and components in `ui/design/`. Nothing in existing screens changes. Material3 remains a dependency throughout this phase.

Deliverable: `AdptTheme {}` compiles and all custom components are functional. `MainActivity` is updated to use `AdptTheme` instead of the old `AdptTheme` (Material3 wrapper) — but existing screens still use Material3 components internally.

### Phase 2 — Screen Migration

Migrate screens in this order: **Overview → Shopping → Stock → Items**.

Each screen is rewritten to import only from `ui/design/`. After each screen is migrated, zero `androidx.compose.material3` imports remain in that file.

Items screen is last because it has the most components (dialogs, dropdowns, checkboxes, search field, selection strip).

### Phase 3 — Cleanup

- Remove from `androidApp/build.gradle.kts`: `compose.material3` only
- Keep `compose.material.icons` and `compose.material.icons.extended` — these provide `ImageVector` objects only, no Material3 components, so they are safe to retain and do not violate the "zero Material3" rule
- Delete `ui/theme/Color.kt` and `ui/theme/Theme.kt`
- Resolve any remaining compilation errors
- Run `./gradlew assembleDebug` to confirm clean build

---

## Files Created / Modified

**New:**
```
androidApp/src/main/kotlin/com/adpt/app/ui/design/
  AdptColors.kt
  AdptTypography.kt
  AdptShapes.kt
  AdptSpacing.kt
  AdptTheme.kt
  components/
    AdptSurface.kt
    AdptText.kt
    AdptIcon.kt
    AdptIconButton.kt
    AdptButton.kt
    AdptOutlinedButton.kt
    AdptTextButton.kt
    AdptChip.kt
    AdptBadge.kt
    AdptCheckbox.kt
    AdptProgressIndicator.kt
    AdptCard.kt
    AdptTopBar.kt
    AdptFab.kt
    AdptNavBar.kt
    AdptScaffold.kt
    AdptSnackbar.kt
    AdptDialog.kt
    AdptTextField.kt
    AdptDropdownMenu.kt
    AdptExposedDropdown.kt
```

**Modified:**
```
androidApp/src/main/kotlin/com/adpt/app/MainActivity.kt
androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewScreen.kt
androidApp/src/main/kotlin/com/adpt/app/ui/shopping/ShoppingScreen.kt
androidApp/src/main/kotlin/com/adpt/app/ui/stock/StockScreen.kt
androidApp/src/main/kotlin/com/adpt/app/ui/items/ItemsScreen.kt
androidApp/src/main/kotlin/com/adpt/app/navigation/AppNavigation.kt
androidApp/build.gradle.kts
```

**Deleted (Phase 3):**
```
androidApp/src/main/kotlin/com/adpt/app/ui/theme/Color.kt
androidApp/src/main/kotlin/com/adpt/app/ui/theme/Theme.kt
```
