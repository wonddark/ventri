# Ventri

A personal inventory management app for Android that tracks household consumables, predicts depletion dates, and generates shopping lists automatically.

## Goal

Ventri helps you stay on top of household stock levels. You log items with a consumption rate and last purchase details; the app estimates when each item will run out and surfaces critical items on an overview dashboard. When stock runs low you can push items directly to a built-in shopping list and optionally receive background notifications for critical items.

## Architecture

The project follows a **Kotlin Multiplatform** layout with two Gradle modules:

```
ventri/
├── shared/          # KMP library — domain models, database schema, business logic
│   └── src/
│       ├── commonMain/  # Platform-agnostic code (models, SQLDelight queries, util extensions)
│       └── androidMain/ # Android-specific database driver wiring
└── androidApp/      # Android application module
    └── src/main/
        ├── ui/          # Compose screens + ViewModels (MVI-style)
        │   ├── overview/    # Dashboard — items sorted by depletion severity
        │   ├── items/       # Full item CRUD
        │   ├── stock/       # Log a new purchase / update stock
        │   ├── shopping/    # Shopping list management
        │   └── preferences/ # Theme, notification settings
        ├── ui/design/   # Custom design system (VentriTheme, tokens, component wrappers)
        ├── notifications/   # WorkManager worker + BroadcastReceiver for alerts
        └── preferences/     # DataStore-backed app preferences repository
```

### Key patterns

- **MVI** — each screen has a sealed `UiState`, a sealed `Intent`, and a `ViewModel` that exposes a `StateFlow<UiState>`.
- **SQLDelight** — type-safe SQL queries defined in `.sq` files; reactive streams via `asFlow()` + coroutines extensions.
- **Custom design system** — all Material 3 components are wrapped in `Ventri*` counterparts so the visual language can evolve independently of the upstream library.

## Main dependencies

| Dependency | Purpose |
|---|---|
| Kotlin Multiplatform 2.3 | Shared module targeting Android |
| Jetpack Compose (BOM 2026.03) | Declarative UI |
| SQLDelight 2.3 | Type-safe local database (SQLite) |
| AndroidX Navigation Compose | Type-safe screen routing |
| WorkManager | Background notifications for critical items |
| DataStore Preferences | Persistent user preferences |
| kotlinx-coroutines 1.10 | Async / reactive streams |
| kotlinx-datetime 0.6 | Platform-agnostic date/time arithmetic |

## Requirements

- Android API 26+ (minSdk 26)
- Android Studio Meerkat or later (AGP 8.13)

## Build

```bash
./gradlew :androidApp:assembleDebug
```
