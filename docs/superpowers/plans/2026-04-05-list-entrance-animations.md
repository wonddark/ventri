# List Entrance Animations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add staggered slide-from-bottom + fade-in entrance animations to all four list screens, re-triggering on initial load and bulk list changes (sort/filter/refresh).

**Architecture:** A single reusable `AnimatedListItem` composable wraps each card. It resets and restarts its stagger animation whenever `animationKey` changes. Each screen provides an appropriate key: sort+filter state for Items, a `listVersion` counter for Overview, and `Unit` (animate once) for Stock and Shopping.

**Tech Stack:** Jetpack Compose `AnimatedVisibility`, `fadeIn`, `slideInVertically`, `tween`, `FastOutSlowInEasing`

---

## File Map

| Action | File |
|---|---|
| **Create** | `androidApp/src/main/kotlin/com/adpt/app/ui/components/AnimatedListItem.kt` |
| **Modify** | `androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewViewModel.kt` |
| **Modify** | `androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewScreen.kt` |
| **Modify** | `androidApp/src/main/kotlin/com/adpt/app/ui/items/ItemsScreen.kt` |
| **Modify** | `androidApp/src/main/kotlin/com/adpt/app/ui/shopping/ShoppingScreen.kt` |
| **Modify** | `androidApp/src/main/kotlin/com/adpt/app/ui/stock/StockScreen.kt` |

---

## Task 1: Create `AnimatedListItem` composable

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/components/AnimatedListItem.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.adpt.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun AnimatedListItem(
    index: Int,
    animationKey: Any = Unit,
    content: @Composable () -> Unit,
) {
    var visible by remember(animationKey) { mutableStateOf(false) }
    LaunchedEffect(animationKey) {
        delay(index * 50L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) +
            slideInVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 3 },
            ),
    ) {
        content()
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/components/AnimatedListItem.kt
git commit -m "feat: add AnimatedListItem composable for staggered list entrance"
```

---

## Task 2: Add `listVersion` to Overview state and ViewModel

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewViewModel.kt`

The `OverviewUiState.Success` needs a `listVersion: Int` that increments each time `refresh()` is called. The ViewModel tracks this with a separate `MutableStateFlow<Int>`.

- [ ] **Step 1: Update `OverviewUiState.Success` to include `listVersion`**

In `OverviewViewModel.kt`, change the sealed interface:

```kotlin
sealed interface OverviewUiState {
    data object Loading : OverviewUiState
    data class Success(val items: List<OverviewItemUiModel>, val listVersion: Int = 0) : OverviewUiState
}
```

- [ ] **Step 2: Add `refreshVersion` flow and update `refresh()` and `uiState`**

Replace the existing `clockSignal`, `refresh()`, and `uiState` with the following (keep all other methods unchanged):

```kotlin
private val clockSignal = MutableStateFlow(Clock.System.now().toEpochMilliseconds())
private val refreshVersion = MutableStateFlow(0)

fun refresh() {
    clockSignal.value = Clock.System.now().toEpochMilliseconds()
    refreshVersion.value++
}

val uiState: StateFlow<OverviewUiState> = combine(
    db.itemQueries.selectAll().asFlow().mapToList(Dispatchers.IO),
    db.shoppingListEntryQueries.selectAll().asFlow().mapToList(Dispatchers.IO),
    clockSignal,
    refreshVersion,
) { items, entries, now, version ->
    val inShoppingList = entries.map { it.item_id }.toSet()
    val filtered = items.mapNotNull { item: Item ->
        if (item.priority == ItemPriority.Lowest) return@mapNotNull null
        val depletionDate = item.estimatedDepletionDate()
        if (depletionDate == null) {
            if (item.priority != ItemPriority.High && item.priority != ItemPriority.Highest) return@mapNotNull null
            return@mapNotNull OverviewItemUiModel(item.id, item.name, Severity.Critical, null, item.id in inShoppingList)
        }
        val delta = depletionDate - now
        val severity = deltaToSeverity(delta)
        if (severity == Severity.Low) return@mapNotNull null
        OverviewItemUiModel(item.id, item.name, severity, delta, item.id in inShoppingList)
    }.sortedWith(compareBy(nullsFirst()) { it.deltaMillis })
    OverviewUiState.Success(filtered, listVersion = version)
}
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OverviewUiState.Loading,
    )
```

- [ ] **Step 3: Build to verify it compiles**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewViewModel.kt
git commit -m "feat: add listVersion to OverviewUiState for animation re-trigger on refresh"
```

---

## Task 3: Animate Overview list

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewScreen.kt`

- [ ] **Step 1: Add the import**

At the top of `OverviewScreen.kt`, add:

```kotlin
import com.adpt.app.ui.components.AnimatedListItem
```

- [ ] **Step 2: Wrap `OverviewItemCard` in `AnimatedListItem`**

Locate the `items(state.items, key = { it.id })` block (around line 167) and update it:

```kotlin
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
```

- [ ] **Step 3: Build to verify it compiles**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewScreen.kt
git commit -m "feat: animate overview list items with staggered entrance"
```

---

## Task 4: Animate Items list

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/items/ItemsScreen.kt`

The Items screen has sort and filter in `uiState`. The `animationKey` is `Pair(sortOrder, priorityFilter)` so the animation replays whenever either changes.

- [ ] **Step 1: Add the import**

At the top of `ItemsScreen.kt`, add:

```kotlin
import com.adpt.app.ui.components.AnimatedListItem
```

- [ ] **Step 2: Wrap `ItemCard` in `AnimatedListItem`**

Locate the `items(uiState.items, key = { it.id })` block (around line 191) and update it:

```kotlin
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
```

- [ ] **Step 3: Build to verify it compiles**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/items/ItemsScreen.kt
git commit -m "feat: animate items list with staggered entrance, re-trigger on sort/filter"
```

---

## Task 5: Animate Shopping list

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/shopping/ShoppingScreen.kt`

No sort/filter on this screen — animation plays once on initial load (`Unit` key).

- [ ] **Step 1: Add the import**

At the top of `ShoppingScreen.kt`, add:

```kotlin
import com.adpt.app.ui.components.AnimatedListItem
```

- [ ] **Step 2: Wrap `ShoppingItemCard` in `AnimatedListItem`**

Locate the `items(state.items, key = { it.entryId })` block (around line 174) and update it:

```kotlin
items(state.items, key = { it.entryId }) { item ->
    AnimatedListItem(index = state.items.indexOf(item)) {
        ShoppingItemCard(
            item = item,
            onMarkAsPurchased = { purchasingItem = item },
            onRemove = { removingItem = item },
        )
    }
}
```

- [ ] **Step 3: Build to verify it compiles**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/shopping/ShoppingScreen.kt
git commit -m "feat: animate shopping list items with staggered entrance"
```

---

## Task 6: Animate Stock list

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/stock/StockScreen.kt`

No sort/filter on this screen — animation plays once on initial load (`Unit` key).

- [ ] **Step 1: Add the import**

At the top of `StockScreen.kt`, add:

```kotlin
import com.adpt.app.ui.components.AnimatedListItem
```

- [ ] **Step 2: Wrap `StockItemCard` in `AnimatedListItem`**

Locate the `items(state.items, key = { it.id })` block (around line 91) and update it:

```kotlin
items(state.items, key = { it.id }) { item ->
    AnimatedListItem(index = state.items.indexOf(item)) {
        StockItemCard(
            item = item,
            onMarkDepleted = { depletingItem = item },
        )
    }
}
```

- [ ] **Step 3: Build to verify it compiles**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/stock/StockScreen.kt
git commit -m "feat: animate stock list items with staggered entrance"
```
