# Add-to-Shopping-List Redesign

**Date:** 2026-03-30
**Status:** Approved

## Overview

Replace the `ItemPickerDialog` modal in the Shopping screen with a navigation-based selection flow reusing the existing Items screen. Users can select any known items (and add new arbitrary ones) via a checkbox-driven list, then confirm to bulk-add all selections to the shopping list.

---

## Architecture & Data Flow

### Navigation

The `"items"` route gains an optional boolean nav argument:

```
"items?selectionMode=true"
```

Default: `false`. The Shopping screen's FAB navigates to this route instead of opening the picker dialog.

### ItemsViewModel

- Reads `selectionMode: Boolean` from `SavedStateHandle` at init.
- When `selectionMode=true`:
  - Combines the items Flow with the shopping list entries Flow to filter out already-listed items.
  - Tracks `selectedItemIds: Set<String>` in `ItemsUiState`.
- New intents:
  - `ToggleItemSelection(itemId: String)` — toggles membership in `selectedItemIds`
  - `SelectionConfirmed` — bulk-calls `db.addToShoppingList(id)` for each selected ID on IO, then emits a `NavigationEvent.PopBackStack` via `SharedFlow`
  - `SelectionCancelled` — emits `NavigationEvent.PopBackStack` immediately
- `AddItemConfirmed` in selection mode: inserts the item normally, then auto-adds the new item's ID to `selectedItemIds`.

### ShoppingViewModel

- Remove `availableItems` from `ShoppingUiState.Success`.
- Remove `AddItemConfirmed` intent and its handler.
- FAB `onClick` navigates to `"items?selectionMode=true"` via `NavController`.

---

## UI Components

### ItemsScreen — Selection Mode

| Element | Normal mode | Selection mode |
|---|---|---|
| Top bar title | `"Items"` | `"Add to shopping list"` |
| Search / sort / filter actions | Visible | Hidden |
| ItemCard leading slot | Empty | `Checkbox` (checked = selected) |
| ItemCard tap target | Opens menu | Toggles selection |
| More-vert menu | Visible | Hidden |
| FAB | Add new item | Add new item (auto-selects on success) |
| Bottom action strip | Not present | Cancel + Confirm (see below) |

**Bottom action strip:** A `Surface` with elevation docked above the `NavigationBar`. Contains two buttons split across full width:
- **Cancel** (outlined) — always enabled, calls `SelectionCancelled`
- **Confirm** (filled) — label `"Add N items"` where N = selection count; disabled when N = 0; calls `SelectionConfirmed`

### ShoppingScreen

- `ItemPickerDialog` composable removed entirely.
- FAB `onClick` navigates to `"items?selectionMode=true"`.
- `availableItems` removed from `ShoppingUiState.Success`.

---

## Error Handling & Edge Cases

| Case | Handling |
|---|---|
| `addToShoppingList` returns `AlreadyInList` | Silently skipped — already-listed items are filtered out at list-load time, so this should not occur |
| `addToShoppingList` returns `ItemNotFound` | Silently skipped |
| Confirm with empty selection | Impossible — Confirm button is disabled when `selectedItemIds` is empty |
| New item insert fails (duplicate name) | `ItemFormDialog` handles inline — no change |
| System back gesture | Treated as Cancel — pops back to Shopping with no changes |
| Rotation | ViewModel survives; `selectedItemIds` is preserved in `ItemsUiState` |
| Process death & restore | `selectionMode=true` restored via `SavedStateHandle`; `selectedItemIds` resets to empty — acceptable |
