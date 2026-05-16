# Item Template Picker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When the FAB is tapped on the Items screen, show a full-screen template picker (categorized, searchable) before the add-item form, so users can pick a pre-made template or start from scratch.

**Architecture:** State-based overlay inside `ItemsScreen` — no nav graph changes. `ItemTemplatePickerScreen` is a plain `@Composable` rendered as the last sibling of the main `Box`, making it appear on top. `ItemFormPrefill` replaces `initialItem: ItemUiModel?` in `ItemFormDialog` so both template-based and blank forms use the same path.

**Tech Stack:** Kotlin, Jetpack Compose, Material Icons Extended, JUnit4 (added), existing Ventri design system components (`VentriTopBar`, `VentriTextField`, `VentriClickableCard`, etc.)

---

## File Map

| Action | Path |
|--------|------|
| NEW | `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemTemplate.kt` |
| NEW | `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemTemplatePickerScreen.kt` |
| NEW | `androidApp/src/test/kotlin/com/ventri/app/ui/items/ItemTemplateTest.kt` |
| MODIFY | `androidApp/build.gradle.kts` — add `testImplementation` for JUnit4 |
| MODIFY | `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemsViewModel.kt` — add `ItemFormPrefill` |
| MODIFY | `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemsScreen.kt` — picker state, `ItemFormDialog` refactor, FAB, overlay |
| MODIFY | `androidApp/src/main/res/values/strings.xml` — 12 new strings |

---

## Task 1: Add string resources

**Files:**
- Modify: `androidApp/src/main/res/values/strings.xml:215` (before `</resources>`)

- [ ] **Step 1: Add strings**

Open `androidApp/src/main/res/values/strings.xml` and insert before the closing `</resources>` tag (currently line 215):

```xml
    <!-- Template picker -->
    <string name="items_template_picker_search_placeholder">Search templates…</string>
    <string name="items_template_start_from_scratch">Start from scratch</string>
    <string name="items_template_start_from_scratch_subtitle">Fill in all details manually</string>
    <string name="items_template_no_results">No templates found</string>
    <string name="items_template_back_cd">Back</string>
    <string name="items_template_category_food_pantry">Food &amp; Pantry</string>
    <string name="items_template_category_dairy">Dairy</string>
    <string name="items_template_category_beverages">Beverages</string>
    <string name="items_template_category_fruits_vegetables">Fruits &amp; Vegetables</string>
    <string name="items_template_category_meat_fish">Meat &amp; Fish</string>
    <string name="items_template_category_cleaning">Cleaning</string>
    <string name="items_template_category_personal_care">Personal Care</string>
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/res/values/strings.xml
git commit -m "feat(items): add string resources for template picker"
```

---

## Task 2: Add `ItemFormPrefill` + test infrastructure

**Files:**
- Modify: `androidApp/build.gradle.kts`
- Modify: `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemsViewModel.kt`
- Create: `androidApp/src/test/kotlin/com/ventri/app/ui/items/ItemTemplateTest.kt` (start with empty class, tests added in Task 3)

- [ ] **Step 1: Add JUnit4 test dependency**

In `androidApp/build.gradle.kts`, add inside the `dependencies { }` block:

```kotlin
testImplementation("junit:junit:4.13.2")
```

- [ ] **Step 2: Add `ItemFormPrefill` to `ItemsViewModel.kt`**

Open `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemsViewModel.kt`.

After the `ItemUiModel` data class (currently ends around line 47), add:

```kotlin
data class ItemFormPrefill(
    val name: String = "",
    val unit: ItemUnit = ItemUnit.PIECE,
    val priority: ItemPriority = ItemPriority.Normal,
    val consumptionRate: Double? = null,
)
```

- [ ] **Step 3: Create test directory and empty test class**

Create directories `androidApp/src/test/kotlin/com/ventri/app/ui/items/` and create `ItemTemplateTest.kt`:

```kotlin
package com.ventri.app.ui.items

class ItemTemplateTest
```

- [ ] **Step 4: Verify build**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add androidApp/build.gradle.kts \
        androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemsViewModel.kt \
        androidApp/src/test/kotlin/com/ventri/app/ui/items/ItemTemplateTest.kt
git commit -m "feat(items): add ItemFormPrefill data class and test infrastructure"
```

---

## Task 3: Create `ItemTemplate.kt` with data model and template list

**Files:**
- Create: `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemTemplate.kt`
- Modify: `androidApp/src/test/kotlin/com/ventri/app/ui/items/ItemTemplateTest.kt`

The rate rule: `unit == ItemUnit.PIECE → consumptionRate = 1.0`, all other units → `consumptionRate = null`. This is enforced by a private `template()` builder function.

- [ ] **Step 1: Write failing tests**

Replace the contents of `ItemTemplateTest.kt`:

```kotlin
package com.ventri.app.ui.items

import com.ventri.shared.model.ItemPriority
import com.ventri.shared.model.ItemUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItemTemplateTest {

    @Test
    fun `all PIECE unit templates have consumptionRate of 1 0`() {
        itemTemplateCategories
            .flatMap { it.templates }
            .filter { it.unit == ItemUnit.PIECE }
            .forEach { template ->
                assertEquals(
                    "Template '${template.name}' has PIECE unit but consumptionRate != 1.0",
                    1.0,
                    template.consumptionRate,
                )
            }
    }

    @Test
    fun `all non-PIECE unit templates have null consumptionRate`() {
        itemTemplateCategories
            .flatMap { it.templates }
            .filter { it.unit != ItemUnit.PIECE }
            .forEach { template ->
                assertNull(
                    "Template '${template.name}' has non-PIECE unit but consumptionRate is not null",
                    template.consumptionRate,
                )
            }
    }

    @Test
    fun `template list has 7 categories and 43 templates`() {
        assertEquals(7, itemTemplateCategories.size)
        assertEquals(43, itemTemplateCategories.sumOf { it.templates.size })
    }

    @Test
    fun `toPrefill maps all fields correctly`() {
        val template = ItemTemplate(
            name = "Rice",
            unit = ItemUnit.KG,
            priority = ItemPriority.Normal,
            consumptionRate = null,
        )
        val prefill = template.toPrefill()
        assertEquals("Rice", prefill.name)
        assertEquals(ItemUnit.KG, prefill.unit)
        assertEquals(ItemPriority.Normal, prefill.priority)
        assertNull(prefill.consumptionRate)
    }

    @Test
    fun `toPrefill preserves consumptionRate for PIECE templates`() {
        val template = ItemTemplate(
            name = "Bread",
            unit = ItemUnit.PIECE,
            priority = ItemPriority.Normal,
            consumptionRate = 1.0,
        )
        val prefill = template.toPrefill()
        assertEquals(1.0, prefill.consumptionRate)
    }
}
```

- [ ] **Step 2: Run tests — expect compilation failure (symbol not found)**

```bash
./gradlew :androidApp:test --tests "com.ventri.app.ui.items.ItemTemplateTest"
```

Expected: `FAILED` — `Unresolved reference: itemTemplateCategories`

- [ ] **Step 3: Create `ItemTemplate.kt`**

Create `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemTemplate.kt`:

```kotlin
package com.ventri.app.ui.items

import androidx.annotation.StringRes
import com.ventri.app.R
import com.ventri.shared.model.ItemPriority
import com.ventri.shared.model.ItemUnit

data class ItemTemplate(
    val name: String,
    val unit: ItemUnit,
    val priority: ItemPriority,
    val consumptionRate: Double?,
)

data class ItemTemplateCategory(
    @StringRes val nameRes: Int,
    val templates: List<ItemTemplate>,
)

fun ItemTemplate.toPrefill() = ItemFormPrefill(
    name = name,
    unit = unit,
    priority = priority,
    consumptionRate = consumptionRate,
)

private fun template(
    name: String,
    unit: ItemUnit,
    priority: ItemPriority = ItemPriority.Normal,
): ItemTemplate = ItemTemplate(
    name = name,
    unit = unit,
    priority = priority,
    consumptionRate = if (unit == ItemUnit.PIECE) 1.0 else null,
)

val itemTemplateCategories: List<ItemTemplateCategory> = listOf(
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_food_pantry,
        templates = listOf(
            template("Bread", ItemUnit.PIECE),
            template("Eggs", ItemUnit.PIECE),
            template("Rice", ItemUnit.KG),
            template("Pasta", ItemUnit.PACK),
            template("Flour", ItemUnit.KG),
            template("Sugar", ItemUnit.KG),
            template("Salt", ItemUnit.G),
            template("Cooking Oil", ItemUnit.L),
            template("Coffee", ItemUnit.G),
            template("Tea", ItemUnit.PACK),
            template("Honey", ItemUnit.G),
            template("Canned Tomatoes", ItemUnit.BOX),
        ),
    ),
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_dairy,
        templates = listOf(
            template("Milk", ItemUnit.L),
            template("Butter", ItemUnit.G),
            template("Cheese", ItemUnit.G),
            template("Yogurt", ItemUnit.PIECE),
            template("Cream", ItemUnit.ML),
        ),
    ),
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_beverages,
        templates = listOf(
            template("Water", ItemUnit.BOTTLE),
            template("Juice", ItemUnit.L),
            template("Soda", ItemUnit.BOTTLE),
            template("Beer", ItemUnit.BOTTLE),
            template("Wine", ItemUnit.BOTTLE),
        ),
    ),
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_fruits_vegetables,
        templates = listOf(
            template("Apples", ItemUnit.KG),
            template("Bananas", ItemUnit.KG),
            template("Tomatoes", ItemUnit.KG),
            template("Onions", ItemUnit.KG),
            template("Potatoes", ItemUnit.KG),
            template("Carrots", ItemUnit.KG),
            template("Lemons", ItemUnit.PIECE),
        ),
    ),
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_meat_fish,
        templates = listOf(
            template("Chicken", ItemUnit.KG),
            template("Beef", ItemUnit.KG),
            template("Pork", ItemUnit.KG),
            template("Fish", ItemUnit.KG),
            template("Canned Tuna", ItemUnit.BOX),
        ),
    ),
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_cleaning,
        templates = listOf(
            template("Dish Soap", ItemUnit.ML),
            template("Laundry Detergent", ItemUnit.G),
            template("All-Purpose Cleaner", ItemUnit.ML),
            template("Bleach", ItemUnit.ML),
            template("Trash Bags", ItemUnit.PACK),
            template("Sponges", ItemUnit.PIECE),
        ),
    ),
    ItemTemplateCategory(
        nameRes = R.string.items_template_category_personal_care,
        templates = listOf(
            template("Shampoo", ItemUnit.ML),
            template("Conditioner", ItemUnit.ML),
            template("Toothpaste", ItemUnit.PIECE),
            template("Toothbrush", ItemUnit.PIECE),
            template("Soap", ItemUnit.PIECE),
            template("Toilet Paper", ItemUnit.PACK),
            template("Deodorant", ItemUnit.PIECE),
            template("Razor", ItemUnit.PIECE),
        ),
    ),
)
```

- [ ] **Step 4: Run tests — expect pass**

```bash
./gradlew :androidApp:test --tests "com.ventri.app.ui.items.ItemTemplateTest"
```

Expected: `BUILD SUCCESSFUL` — 5 tests, 0 failures

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemTemplate.kt \
        androidApp/src/test/kotlin/com/ventri/app/ui/items/ItemTemplateTest.kt
git commit -m "feat(items): add ItemTemplate data model and categorized template list"
```

---

## Task 4: Refactor `ItemFormDialog` to use `ItemFormPrefill`

**Files:**
- Modify: `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemsScreen.kt`

This is a pure refactor — no behaviour change. The `ItemFormDialog` private function currently takes `initialItem: ItemUiModel?`. Replace it with `prefill: ItemFormPrefill`. Both call sites (add and edit) are updated.

- [ ] **Step 1: Update `ItemFormDialog` signature and internal state**

In `ItemsScreen.kt`, find the `ItemFormDialog` function (starts around line 814). Change its signature and the four `rememberSaveable` lines that read from `initialItem`:

```kotlin
// BEFORE signature:
private fun ItemFormDialog(
    title: String,
    confirmLabel: String,
    initialItem: ItemUiModel?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, unit: ItemUnit, priority: ItemPriority, rate: Double?) -> Unit,
    resultFlow: kotlinx.coroutines.flow.SharedFlow<String?>,
    onSuccess: () -> Unit,
)

// AFTER signature:
private fun ItemFormDialog(
    title: String,
    confirmLabel: String,
    prefill: ItemFormPrefill,
    onDismiss: () -> Unit,
    onConfirm: (name: String, unit: ItemUnit, priority: ItemPriority, rate: Double?) -> Unit,
    resultFlow: kotlinx.coroutines.flow.SharedFlow<String?>,
    onSuccess: () -> Unit,
)
```

Replace the four state declarations inside the function body:

```kotlin
// BEFORE:
var name by rememberSaveable { mutableStateOf(initialItem?.name ?: "") }
var selectedUnit by rememberSaveable { mutableStateOf(initialItem?.unit ?: ItemUnit.PIECE) }
var selectedPriority by rememberSaveable { mutableStateOf(initialItem?.priority ?: ItemPriority.Normal) }
var rateText by rememberSaveable { mutableStateOf(initialItem?.consumptionRate?.toString() ?: "") }

// AFTER:
var name by rememberSaveable { mutableStateOf(prefill.name) }
var selectedUnit by rememberSaveable { mutableStateOf(prefill.unit) }
var selectedPriority by rememberSaveable { mutableStateOf(prefill.priority) }
var rateText by rememberSaveable { mutableStateOf(prefill.consumptionRate?.toString() ?: "") }
```

- [ ] **Step 2: Update add-form call site**

Find the `if (showAddDialog)` block (around line 163). Change the `ItemFormDialog` call:

```kotlin
// BEFORE:
ItemFormDialog(
    title = stringResource(R.string.items_add_title),
    confirmLabel = stringResource(R.string.items_add_confirm),
    initialItem = null,
    ...
)

// AFTER:
ItemFormDialog(
    title = stringResource(R.string.items_add_title),
    confirmLabel = stringResource(R.string.items_add_confirm),
    prefill = ItemFormPrefill(),
    ...
)
```

- [ ] **Step 3: Update edit-form call site**

Find the `editingItem?.let { item ->` block (around line 184). Change the `ItemFormDialog` call:

```kotlin
// BEFORE:
ItemFormDialog(
    title = stringResource(R.string.items_edit_title),
    confirmLabel = stringResource(R.string.common_save),
    initialItem = item,
    ...
)

// AFTER:
ItemFormDialog(
    title = stringResource(R.string.items_edit_title),
    confirmLabel = stringResource(R.string.common_save),
    prefill = ItemFormPrefill(
        name = item.name,
        unit = item.unit,
        priority = item.priority,
        consumptionRate = item.consumptionRate,
    ),
    ...
)
```

- [ ] **Step 4: Verify build compiles**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemsScreen.kt
git commit -m "refactor(items): replace initialItem with ItemFormPrefill in ItemFormDialog"
```

---

## Task 5: Create `ItemTemplatePickerScreen`

**Files:**
- Create: `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemTemplatePickerScreen.kt`

The picker is a full-screen composable with:
- A pinned overlay column (top bar + search field) whose height is measured with `onSizeChanged`
- A `LazyColumn` below it with "Start from scratch" card, then filtered category sections
- `BackHandler` wired to `onDismiss`
- Search filters `template.name` case-insensitively; category only shown if ≥ 1 match

- [ ] **Step 1: Create the file**

Create `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemTemplatePickerScreen.kt`:

```kotlin
package com.ventri.app.ui.items

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ventri.app.R
import com.ventri.app.ui.design.VentriTheme
import com.ventri.app.ui.design.components.VentriClickableCard
import com.ventri.app.ui.design.components.VentriIcon
import com.ventri.app.ui.design.components.VentriIconButton
import com.ventri.app.ui.design.components.VentriText
import com.ventri.app.ui.design.components.VentriTextField
import com.ventri.app.ui.design.components.VentriTopBar
import com.ventri.app.ui.util.displayName

@Composable
fun ItemTemplatePickerScreen(
    onTemplateSelected: (ItemTemplate) -> Unit,
    onStartFromScratch: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val density = LocalDensity.current
    var topAreaHeightPx by remember { mutableIntStateOf(0) }
    val topAreaHeightDp = with(density) { topAreaHeightPx.toDp() }

    val filteredCategories by remember {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                itemTemplateCategories
            } else {
                itemTemplateCategories.mapNotNull { category ->
                    val matched = category.templates.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }
                    if (matched.isEmpty()) null else category.copy(templates = matched)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VentriTheme.colors.background)
    ) {
        // LazyColumn always rendered so "Start from scratch" is always visible (V1 invariant)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topAreaHeightDp + 8.dp,
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp,
            ),
        ) {
            item(key = "start_from_scratch") {
                StartFromScratchCard(
                    onClick = onStartFromScratch,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (filteredCategories.isEmpty()) {
                item(key = "no_results") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        VentriText(
                            text = stringResource(R.string.items_template_no_results),
                            style = VentriTheme.typography.bodyMedium,
                            color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            } else {
                filteredCategories.forEach { category ->
                    item(key = "header_${category.nameRes}") {
                        VentriText(
                            text = stringResource(category.nameRes),
                            style = VentriTheme.typography.labelSmall,
                            color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(
                        items = category.templates,
                        key = { "${category.nameRes}_${it.name}" },
                    ) { template ->
                        TemplateRow(
                            template = template,
                            onClick = { onTemplateSelected(template) },
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            }
        }

        // Pinned top area: top bar + search field
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VentriTheme.colors.background)
                .onSizeChanged { topAreaHeightPx = it.height },
        ) {
            VentriTopBar(
                title = {
                    VentriText(
                        text = stringResource(R.string.items_add_title),
                        style = VentriTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    VentriIconButton(onClick = onDismiss) {
                        VentriIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.items_template_back_cd),
                        )
                    }
                },
            )
            VentriTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.items_template_picker_search_placeholder),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun StartFromScratchCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VentriClickableCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VentriIcon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = VentriTheme.colors.accent,
            )
            Column {
                VentriText(
                    text = stringResource(R.string.items_template_start_from_scratch),
                    style = VentriTheme.typography.titleSmall,
                )
                VentriText(
                    text = stringResource(R.string.items_template_start_from_scratch_subtitle),
                    style = VentriTheme.typography.bodySmall,
                    color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun TemplateRow(
    template: ItemTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VentriClickableCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                VentriText(
                    text = template.name,
                    style = VentriTheme.typography.titleSmall,
                )
                VentriText(
                    text = if (template.consumptionRate != null) {
                        stringResource(
                            R.string.items_unit_rate,
                            template.unit.displayName(),
                            template.consumptionRate.toString(),
                        )
                    } else {
                        template.unit.displayName()
                    },
                    style = VentriTheme.typography.bodySmall,
                    color = VentriTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. If `Icons.Default.Edit` is unresolved, substitute `Icons.Default.Create`.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemTemplatePickerScreen.kt
git commit -m "feat(items): add ItemTemplatePickerScreen composable"
```

---

## Task 6: Integrate template picker into `ItemsScreen`

**Files:**
- Modify: `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemsScreen.kt`

Three changes: (1) replace `showAddDialog` initial state with `showTemplatePicker`, (2) wire FAB to open picker, (3) render `ItemTemplatePickerScreen` as the last sibling after the main `Box` so it appears on top.

- [ ] **Step 1: Replace state declarations**

In `ItemsScreen`, find (around line 107):

```kotlin
var showAddDialog by rememberSaveable { mutableStateOf(viewModel.showAddOnStart) }
```

Replace with:

```kotlin
var showTemplatePicker by rememberSaveable { mutableStateOf(viewModel.showAddOnStart) }
var showAddDialog by rememberSaveable { mutableStateOf(false) }
var addFormPrefill by remember { mutableStateOf(ItemFormPrefill()) }
```

- [ ] **Step 2: Update the add-form call site to pass `addFormPrefill`**

The `if (showAddDialog)` block (around line 163) currently passes `prefill = ItemFormPrefill()` (set in Task 4). Change it to pass the state variable:

```kotlin
if (showAddDialog) {
    ItemFormDialog(
        title = stringResource(R.string.items_add_title),
        confirmLabel = stringResource(R.string.items_add_confirm),
        prefill = addFormPrefill,          // ← was ItemFormPrefill()
        onDismiss = { showAddDialog = false },
        onConfirm = { name, unit, priority, rate ->
            viewModel.handleIntent(
                ItemsIntent.AddItemConfirmed(name, unit, priority, rate)
            )
        },
        resultFlow = addResultStrings,
        onSuccess = { showAddDialog = false },
    )
}
```

- [ ] **Step 3: Update FAB onClick**

Find the `VentriFab` (around line 318):

```kotlin
// BEFORE:
VentriFab(onClick = { showAddDialog = true }) {

// AFTER:
VentriFab(onClick = { showTemplatePicker = true }) {
```

- [ ] **Step 4: Add picker overlay after the main `Box`**

The main content `Box` starts with `Box(modifier = Modifier.fillMaxSize().background(...))` (around line 209) and ends before the closing brace of `ItemsScreen`. Add the picker overlay **after** that closing brace (as the last statement in `ItemsScreen`):

```kotlin
    if (showTemplatePicker) {
        ItemTemplatePickerScreen(
            onTemplateSelected = { template ->
                addFormPrefill = template.toPrefill()
                showTemplatePicker = false
                showAddDialog = true
            },
            onStartFromScratch = {
                addFormPrefill = ItemFormPrefill()
                showTemplatePicker = false
                showAddDialog = true
            },
            onDismiss = { showTemplatePicker = false },
        )
    }
```

This must be the **last** composable in `ItemsScreen`'s body so it renders on top of the main `Box` (Compose draws siblings in declaration order, last on top).

- [ ] **Step 5: Verify build and run tests**

```bash
./gradlew :androidApp:assembleDebug :androidApp:test
```

Expected: `BUILD SUCCESSFUL` — all tests pass.

- [ ] **Step 6: Manual smoke test on device/emulator**

1. Launch app → navigate to Items tab
2. Tap FAB → template picker appears (not the blank form)
3. Scroll through categories — confirm all 7 categories visible
4. Type "milk" in search → only Dairy / Milk row shown
5. Clear search → all categories restored
6. Tap "Start from scratch" → blank add-item form opens
7. Tap a template (e.g. "Rice") → form opens with name "Rice", unit kg, no rate
8. Tap a PIECE template (e.g. "Bread") → form opens with name "Bread", unit piece, rate "1.0"
9. Press system back while picker open → picker closes, Items screen intact
10. Navigate away and back → picker does not reappear (state not persisted across nav)

- [ ] **Step 7: Commit**

```bash
git add androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemsScreen.kt
git commit -m "feat(items): integrate template picker — FAB now opens picker before add form"
```
