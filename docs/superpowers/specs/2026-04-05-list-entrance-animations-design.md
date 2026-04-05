# List Entrance Animations Design

**Date:** 2026-04-05

## Summary

Add staggered slide-from-bottom + fade-in animations to all list screens. Each item enters sequentially with a 50ms stagger delay. Animation re-plays on initial load and whenever the list content changes due to a bulk action (sort, filter, refresh) — but NOT on individual item add/remove, which will receive separate animation treatment later.

---

## Architecture

### New file: `ui/components/AnimatedListItem.kt`

A single reusable composable wraps each list card:

```kotlin
@Composable
fun AnimatedListItem(
    index: Int,
    animationKey: Any = Unit,
    content: @Composable () -> Unit,
)
```

- `var visible by remember(animationKey) { mutableStateOf(false) }` — resets to `false` whenever `animationKey` changes
- `LaunchedEffect(animationKey) { delay(index * 50L); visible = true }` — staggers each item's entry
- `AnimatedVisibility(visible, enter = fadeIn(tween(300)) + slideInVertically(tween(300, FastOutSlowInEasing)) { it / 3 })`
- Slide offset: 1/3 of item height upward
- Duration: 300ms per item, FastOutSlowInEasing

---

## Animation Keys Per Screen

| Screen | `animationKey` | Re-animates on |
|---|---|---|
| Items | `Pair(sortOrder, priorityFilter)` | Sort or filter change |
| Overview | `listVersion: Int` from `OverviewUiState.Success` | Refresh button tap |
| Stock | `Unit` | Initial load only |
| Shopping | `Unit` | Initial load only |

---

## Changes Required

### 1. New file — `AnimatedListItem.kt`
Create `androidApp/src/main/kotlin/com/adpt/app/ui/components/AnimatedListItem.kt` with the composable above.

### 2. `OverviewUiState` + `OverviewViewModel`
- Add `listVersion: Int = 0` to `OverviewUiState.Success`
- In `OverviewViewModel.refresh()`, increment `listVersion` when a new list is delivered

### 3. `OverviewScreen.kt`
Wrap `OverviewItemCard` in `AnimatedListItem(index, animationKey = state.listVersion)`.

### 4. `ItemsScreen.kt`
Wrap `ItemCard` in `AnimatedListItem(index, animationKey = Pair(uiState.sortOrder, uiState.priorityFilter))`.

### 5. `ShoppingScreen.kt`
Wrap `ShoppingItemCard` in `AnimatedListItem(index)` (default `Unit` key).

### 6. `StockScreen.kt`
Wrap `StockItemCard` in `AnimatedListItem(index)` (default `Unit` key).

---

## Out of Scope

- Individual item add/remove animations — separate design, to be done later
- Exit animations
- Any animation on the Loading → content transition (that's a screen-level concern)
