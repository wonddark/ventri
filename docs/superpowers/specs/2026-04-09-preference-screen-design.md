# Preference Screen Design

**Date:** 2026-04-09
**Status:** Approved

## Overview

Add a user-accessible preference screen to adpt that persists settings on-device (DataStore, not the SQLite database). Three preference groups: appearance (theme), notifications (depletion check frequency), and depletion severity thresholds.

---

## 1. Data Model

### `ThresholdConfig` (shared module)

```
shared/src/commonMain/kotlin/com/adpt/shared/model/ThresholdConfig.kt
```

```kotlin
data class ThresholdConfig(
    val criticalDays: Int = 1,
    val highDays: Int = 2,
    val normalDays: Int = 3,
    // Low is implicit: anything > normalDays days
)
```

Invariant enforced at all write sites: `criticalDays < highDays < normalDays`, all ≥ 1.

### `ThemeMode` (Android module)

```kotlin
enum class ThemeMode { Light, Dark, System }
```

### `NotificationFrequency` (Android module)

```kotlin
enum class NotificationFrequency { OncePerDay, TwicePerDay }
// OncePerDay  → 24h WorkManager interval
// TwicePerDay → 12h WorkManager interval (current default)
```

---

## 2. Storage: `AppPreferencesRepository`

```
androidApp/src/main/kotlin/com/adpt/app/preferences/AppPreferencesRepository.kt
```

Wraps `DataStore<Preferences>`. Exposes typed `StateFlow`s and suspend setters:

| Property | Type | Default |
|---|---|---|
| `themeMode` | `StateFlow<ThemeMode>` | `ThemeMode.System` |
| `notificationFrequency` | `StateFlow<NotificationFrequency>` | `NotificationFrequency.TwicePerDay` |
| `thresholdConfig` | `StateFlow<ThresholdConfig>` | `ThresholdConfig()` (1d/2d/3d) |

Setters: `setThemeMode`, `setNotificationFrequency`, `setThresholdConfig`.

`AdptApplication` constructs the singleton and exposes it as `val prefs: AppPreferencesRepository`.

---

## 3. Navigation & Entry Point

- Settings is **not** a bottom-nav tab.
- `OverviewScreen` gains a `TopAppBar` with a trailing ⚙️ `IconButton` that navigates to the `preferences` route.
- The `preferences` route is added to `AppNavigation` as a full screen (no bottom nav visible). The `TopAppBar` on `PreferencesScreen` has a back arrow.

---

## 4. Preference Screen UI

```
androidApp/src/main/kotlin/com/adpt/app/ui/preferences/PreferencesScreen.kt
androidApp/src/main/kotlin/com/adpt/app/ui/preferences/PreferencesViewModel.kt
```

Scrollable column with three labeled sections:

### Appearance
- `SegmentedButton` with three segments: **Light · Dark · System**
- Immediately applies theme on selection.

### Notifications
- `RadioButton` group: **Once per day (24h)** / **Twice per day (12h)**
- Immediately reschedules WorkManager on change.

### Depletion Thresholds
- Three stepper rows: Critical, High, Normal.
- Each row: `[−]  Nd  [+]` with a colored severity dot.
- Low row is read-only and shows `Nd+` where N = normalDays + 1.
- Stepper buttons disabled to enforce ordering:

| Button | Disabled when |
|---|---|
| Critical `+` | `criticalDays + 1 >= highDays` |
| High `−` | `highDays - 1 <= criticalDays` |
| High `+` | `highDays + 1 >= normalDays` |
| Normal `−` | `normalDays - 1 <= highDays` |
| Critical `−` | `criticalDays <= 1` |

Changes save immediately to DataStore on each tap.

---

## 5. System Wiring

### Theme

`MainActivity` collects `prefs.themeMode` as Compose state and resolves it before passing to `AdptTheme`:

```kotlin
val themeMode by app.prefs.themeMode.collectAsState()
val isDark = when (themeMode) {
    ThemeMode.Light  -> false
    ThemeMode.Dark   -> true
    ThemeMode.System -> isSystemInDarkTheme()
}
AdptTheme(darkTheme = isDark) { AppNavigation() }
```

### Notification Frequency

`AdptApplication.onCreate()` launches a coroutine that collects `prefs.notificationFrequency` and calls `scheduleStockCheck(frequency)` whenever it changes. `ExistingPeriodicWorkPolicy.UPDATE` replaces any existing schedule.

```
OncePerDay  → 24h
TwicePerDay → 12h
```

### Depletion Thresholds

`deltaToSeverity` in `ItemExtensions.kt` is refactored to:

```kotlin
fun deltaToSeverity(delta: Long, config: ThresholdConfig): Severity
```

**Bug fix:** the current implementation maps the 3-day band to `Severity.High` instead of `Severity.Normal`. This is corrected as part of this change.

Call sites updated:
- `CriticalItemsWorker.doWork()` reads `thresholdConfig` once at the start of each run.
- Any ViewModel calling `deltaToSeverity` collects `thresholdConfig` from `AdptApplication.prefs`.

The shared module remains Android-free: `ThresholdConfig` is a plain data class; `deltaToSeverity` is a pure function.

---

## 6. Files Changed

### New
| File | Purpose |
|---|---|
| `shared/.../model/ThresholdConfig.kt` | Threshold value class |
| `androidApp/.../preferences/AppPreferencesRepository.kt` | DataStore wrapper |
| `androidApp/.../ui/preferences/PreferencesScreen.kt` | Settings UI |
| `androidApp/.../ui/preferences/PreferencesViewModel.kt` | Preferences state & logic |

### Modified
| File | Change |
|---|---|
| `shared/.../util/ItemExtensions.kt` | `deltaToSeverity` takes `ThresholdConfig`; fix Normal severity bug |
| `androidApp/.../AdptApplication.kt` | Create `prefs` repository; observe frequency for WorkManager |
| `androidApp/.../MainActivity.kt` | Collect theme, pass to `AdptTheme` |
| `androidApp/.../navigation/AppNavigation.kt` | Add `preferences` route |
| `androidApp/.../ui/overview/OverviewScreen.kt` | Add TopAppBar with gear icon |
| `androidApp/.../notifications/CriticalItemsWorker.kt` | Read `thresholdConfig` from repository |
