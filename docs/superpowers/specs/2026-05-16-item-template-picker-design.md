# Item Template Picker — Design Spec

**Date:** 2026-05-16  
**Status:** Approved

## Overview

When the user taps the FAB on the Items screen, instead of opening the blank add-item form directly, a full-screen template picker overlay appears. The user picks a pre-made template (or starts from scratch), and the form opens pre-filled with the template values.

## Flow

```
FAB tap (or nav arg add=true)
  → ItemTemplatePickerScreen overlay
      → pick template → ItemFormDialog pre-filled with template values
      → "Start from scratch" → ItemFormDialog blank
      → back / system back → close picker, nothing opened
```

## Approach

State-based overlay inside `ItemsScreen`. No nav graph changes. Matches existing `ItemFormDialog` pattern (composable state, not ViewModel-driven).

## Data Model

### `ItemFormPrefill` (added to `ItemsViewModel.kt`)

```kotlin
data class ItemFormPrefill(
    val name: String = "",
    val unit: ItemUnit = ItemUnit.PIECE,
    val priority: ItemPriority = ItemPriority.Normal,
    val consumptionRate: Double? = null,
)
```

Replaces `initialItem: ItemUiModel?` parameter in `ItemFormDialog`. Edit path converts `ItemUiModel` to `ItemFormPrefill` inline at the call site.

### `ItemTemplate` + `ItemTemplateCategory` (new file `ItemTemplate.kt`)

```kotlin
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

fun ItemTemplate.toPrefill() = ItemFormPrefill(name, unit, priority, consumptionRate)
```

### Rate rule

`unit == ItemUnit.PIECE` → `consumptionRate = 1.0`. All other units → `consumptionRate = null`.

### Template list — 7 categories, 43 templates

**Food & Pantry** (12): Bread (piece), Eggs (piece), Rice (kg), Pasta (pack), Flour (kg), Sugar (kg), Salt (g), Cooking Oil (L), Coffee (g), Tea (pack), Honey (g), Canned Tomatoes (box)

**Dairy** (5): Milk (L), Butter (g), Cheese (g), Yogurt (piece), Cream (mL)

**Beverages** (5): Water (bottle), Juice (L), Soda (bottle), Beer (bottle), Wine (bottle)

**Fruits & Vegetables** (7): Apples (kg), Bananas (kg), Tomatoes (kg), Onions (kg), Potatoes (kg), Carrots (kg), Lemons (piece)

**Meat & Fish** (5): Chicken (kg), Beef (kg), Pork (kg), Fish (kg), Canned Tuna (box)

**Cleaning** (6): Dish Soap (mL), Laundry Detergent (g), All-Purpose Cleaner (mL), Bleach (mL), Trash Bags (pack), Sponges (piece)

**Personal Care** (8): Shampoo (mL), Conditioner (mL), Toothpaste (piece), Toothbrush (piece), Soap (piece), Toilet Paper (pack), Deodorant (piece), Razor (piece)

All priorities default to `ItemPriority.Normal`.

## `ItemTemplatePickerScreen` Composable (new file)

```kotlin
@Composable
fun ItemTemplatePickerScreen(
    onTemplateSelected: (ItemTemplate) -> Unit,
    onStartFromScratch: () -> Unit,
    onDismiss: () -> Unit,
)
```

**Structure:**
- `BackHandler(onBack = onDismiss)`
- `var searchQuery by rememberSaveable { mutableStateOf("") }`
- Full-size `Box` with `VentriTheme.colors.background`
- Pinned top bar overlay (same pattern as `ItemsTopBar` in `ItemsScreen`):
  - `VentriTopBar`: back icon → `onDismiss`, title = `stringResource(R.string.items_add_title)` ("Add Item")
  - Persistent `VentriTextField` search bar immediately below top bar
- `LazyColumn` with top padding = combined height of top bar + search bar:
  - Item 0: "Start from scratch" `VentriClickableCard` → `onStartFromScratch()`
  - Per category (filtered): section header label + template rows as `VentriClickableCard` → `onTemplateSelected(template)`
  - Search: `template.name.contains(query, ignoreCase = true)`. Category shown only if ≥ 1 template matches.
  - Empty state: `VentriText("No templates found")` when search produces zero results across all categories

## `ItemsScreen` Changes

**State:**
```kotlin
// Before
var showAddDialog by rememberSaveable { mutableStateOf(viewModel.showAddOnStart) }

// After
var showTemplatePicker by rememberSaveable { mutableStateOf(viewModel.showAddOnStart) }
var showAddDialog by rememberSaveable { mutableStateOf(false) }
var addFormPrefill by remember { mutableStateOf(ItemFormPrefill()) }
```

**FAB:** `onClick = { showTemplatePicker = true }`

**Picker overlay** (rendered after the main `Box` — last sibling draws on top in Compose):
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

**Add form call:** `prefill = addFormPrefill` (replaces `initialItem = null`)

**Edit form call:** `prefill = ItemFormPrefill(item.name, item.unit, item.priority, item.consumptionRate)` (replaces `initialItem = item`)

## `ItemFormDialog` Changes

- Parameter `initialItem: ItemUiModel?` → `prefill: ItemFormPrefill`
- Internal `rememberSaveable` initial values read from `prefill.*` instead of `initialItem?.*`

## New String Resources

| Key | Value |
|-----|-------|
| `items_template_picker_search_placeholder` | "Search templates..." |
| `items_template_start_from_scratch` | "Start from scratch" |
| `items_template_start_from_scratch_subtitle` | "Fill in all details manually" |
| `items_template_no_results` | "No templates found" |
| `items_template_back_cd` | "Back" |
| `items_template_category_food_pantry` | "Food & Pantry" |
| `items_template_category_dairy` | "Dairy" |
| `items_template_category_beverages` | "Beverages" |
| `items_template_category_fruits_vegetables` | "Fruits & Vegetables" |
| `items_template_category_meat_fish` | "Meat & Fish" |
| `items_template_category_cleaning` | "Cleaning" |
| `items_template_category_personal_care` | "Personal Care" |

## Files

| Action | Path |
|--------|------|
| NEW | `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemTemplate.kt` |
| NEW | `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemTemplatePickerScreen.kt` |
| MODIFY | `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemsViewModel.kt` |
| MODIFY | `androidApp/src/main/kotlin/com/ventri/app/ui/items/ItemsScreen.kt` |
| MODIFY | `androidApp/src/main/res/values/strings.xml` |

## Invariants

- V1: "Start from scratch" always visible regardless of search query
- V2: template picker only appears in browse mode (not selection mode)
- V3: `showAddOnStart` nav arg triggers picker, not form directly
- V4: `addFormPrefill` reset to `ItemFormPrefill()` on each "Start from scratch" to avoid stale prefill
