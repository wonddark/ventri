# Preference Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a device-local preference screen (DataStore) that lets users configure the app theme, depletion notification frequency, and per-severity depletion day thresholds.

**Architecture:** A typed `AppPreferencesRepository` wraps DataStore and exposes `StateFlow`s for all three preference groups. `AdptApplication` holds the singleton and observes frequency changes to reschedule WorkManager. `deltaToSeverity` is refactored to accept a `ThresholdConfig` value object instead of hardcoded constants, fixing an existing severity bug in the process.

**Tech Stack:** Kotlin, Jetpack Compose + Material3, AndroidX DataStore Preferences (`1.1.4`), WorkManager, `kotlin("test")` for unit tests in the shared module.

---

## File Map

### New files
| File | Purpose |
|---|---|
| `shared/src/commonMain/kotlin/com/adpt/shared/model/ThresholdConfig.kt` | Depletion threshold value object |
| `shared/src/commonTest/kotlin/com/adpt/shared/ThresholdConfigTest.kt` | Unit tests for defaults and invariant |
| `shared/src/commonTest/kotlin/com/adpt/shared/DeltaToSeverityTest.kt` | Unit tests for `deltaToSeverity` |
| `androidApp/src/main/kotlin/com/adpt/app/preferences/ThemeMode.kt` | Theme enum |
| `androidApp/src/main/kotlin/com/adpt/app/preferences/NotificationFrequency.kt` | Frequency enum |
| `androidApp/src/main/kotlin/com/adpt/app/preferences/AppPreferencesRepository.kt` | DataStore wrapper with typed StateFlows |
| `androidApp/src/main/kotlin/com/adpt/app/ui/preferences/PreferencesViewModel.kt` | Preferences state and stepper logic |
| `androidApp/src/main/kotlin/com/adpt/app/ui/preferences/PreferencesScreen.kt` | Preferences UI |

### Modified files
| File | Change |
|---|---|
| `gradle/libs.versions.toml` | Add `datastore = "1.1.4"` version and library entry |
| `androidApp/build.gradle.kts` | Add `datastore-preferences` dependency |
| `shared/src/commonMain/kotlin/com/adpt/shared/util/ItemExtensions.kt` | Refactor `deltaToSeverity` to accept `ThresholdConfig`; fix Normal severity bug |
| `androidApp/src/main/kotlin/com/adpt/app/AdptApplication.kt` | Create `prefs` singleton; observe frequency for WorkManager |
| `androidApp/src/main/kotlin/com/adpt/app/MainActivity.kt` | Collect `themeMode`, pass resolved `isDark` to `AdptTheme` |
| `androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewViewModel.kt` | Collect `thresholdConfig`; pass to `deltaToSeverity` |
| `androidApp/src/main/kotlin/com/adpt/app/notifications/CriticalItemsWorker.kt` | Read `thresholdConfig` from repository |
| `androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewScreen.kt` | Add Settings gear icon to existing `TopAppBar` |
| `androidApp/src/main/kotlin/com/adpt/app/navigation/AppNavigation.kt` | Add `preferences` route; pass `onOpenSettings` to `OverviewScreen` |

---

## Task 1: Add DataStore Preferences dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `androidApp/build.gradle.kts`

- [ ] **Step 1: Add version and library entry to the version catalog**

In `gradle/libs.versions.toml`, add to `[versions]`:
```toml
datastore = "1.1.4"
```
Add to `[libraries]`:
```toml
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
```

- [ ] **Step 2: Add dependency to androidApp**

In `androidApp/build.gradle.kts`, inside the `dependencies { }` block after `implementation(libs.workmanager)`:
```kotlin
implementation(libs.datastore.preferences)
```

- [ ] **Step 3: Sync and verify build**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL (no DataStore-related errors).

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml androidApp/build.gradle.kts
git commit -m "build: add datastore-preferences dependency"
```

---

## Task 2: Add `ThresholdConfig` to the shared module

**Files:**
- Create: `shared/src/commonMain/kotlin/com/adpt/shared/model/ThresholdConfig.kt`
- Create: `shared/src/commonTest/kotlin/com/adpt/shared/ThresholdConfigTest.kt`

- [ ] **Step 1: Write the failing test**

Create `shared/src/commonTest/kotlin/com/adpt/shared/ThresholdConfigTest.kt`:
```kotlin
package com.adpt.shared

import com.adpt.shared.model.ThresholdConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThresholdConfigTest {

    @Test
    fun defaultsAreCorrect() {
        val config = ThresholdConfig()
        assertEquals(1, config.criticalDays)
        assertEquals(2, config.highDays)
        assertEquals(3, config.normalDays)
    }

    @Test
    fun invariantHoldsForDefaults() {
        val config = ThresholdConfig()
        assertTrue(config.criticalDays < config.highDays)
        assertTrue(config.highDays < config.normalDays)
    }

    @Test
    fun customValuesAreStored() {
        val config = ThresholdConfig(criticalDays = 2, highDays = 5, normalDays = 10)
        assertEquals(2, config.criticalDays)
        assertEquals(5, config.highDays)
        assertEquals(10, config.normalDays)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew :shared:allTests
```
Expected: FAIL — `ThresholdConfig` not found.

- [ ] **Step 3: Create `ThresholdConfig`**

Create `shared/src/commonMain/kotlin/com/adpt/shared/model/ThresholdConfig.kt`:
```kotlin
package com.adpt.shared.model

data class ThresholdConfig(
    val criticalDays: Int = 1,
    val highDays: Int = 2,
    val normalDays: Int = 3,
)
```

- [ ] **Step 4: Run to verify tests pass**

```bash
./gradlew :shared:allTests
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/adpt/shared/model/ThresholdConfig.kt \
        shared/src/commonTest/kotlin/com/adpt/shared/ThresholdConfigTest.kt
git commit -m "feat: add ThresholdConfig value class to shared model"
```

---

## Task 3: Refactor `deltaToSeverity` to accept `ThresholdConfig` and fix the Normal severity bug

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/adpt/shared/util/ItemExtensions.kt`
- Create: `shared/src/commonTest/kotlin/com/adpt/shared/DeltaToSeverityTest.kt`

**Bug:** The current implementation has two branches mapping to `Severity.High` — the 3-day band incorrectly returns `Severity.High` instead of `Severity.Normal`.

- [ ] **Step 1: Write the failing tests**

Create `shared/src/commonTest/kotlin/com/adpt/shared/DeltaToSeverityTest.kt`:
```kotlin
package com.adpt.shared

import com.adpt.shared.model.Severity
import com.adpt.shared.model.ThresholdConfig
import com.adpt.shared.util.deltaToSeverity
import kotlin.test.Test
import kotlin.test.assertEquals

class DeltaToSeverityTest {

    private val config = ThresholdConfig(criticalDays = 1, highDays = 2, normalDays = 3)
    private val ms = 24L * 60 * 60 * 1000L // one day in millis

    @Test
    fun criticalWhenDeltaIsExactlyOneCriticalDay() {
        assertEquals(Severity.Critical, deltaToSeverity(1 * ms, config))
    }

    @Test
    fun criticalWhenDeltaIsZero() {
        assertEquals(Severity.Critical, deltaToSeverity(0L, config))
    }

    @Test
    fun criticalWhenDeltaIsNegative() {
        assertEquals(Severity.Critical, deltaToSeverity(-1L, config))
    }

    @Test
    fun highWhenDeltaIsJustAboveCriticalThreshold() {
        assertEquals(Severity.High, deltaToSeverity(1 * ms + 1, config))
    }

    @Test
    fun highWhenDeltaIsExactlyTwoDays() {
        assertEquals(Severity.High, deltaToSeverity(2 * ms, config))
    }

    @Test
    fun normalWhenDeltaIsJustAboveHighThreshold() {
        assertEquals(Severity.Normal, deltaToSeverity(2 * ms + 1, config))
    }

    @Test
    fun normalWhenDeltaIsExactlyThreeDays() {
        assertEquals(Severity.Normal, deltaToSeverity(3 * ms, config))
    }

    @Test
    fun lowWhenDeltaIsAboveNormalThreshold() {
        assertEquals(Severity.Low, deltaToSeverity(3 * ms + 1, config))
    }

    @Test
    fun customThresholdsAreRespected() {
        val custom = ThresholdConfig(criticalDays = 3, highDays = 7, normalDays = 14)
        assertEquals(Severity.Critical, deltaToSeverity(3 * ms, custom))
        assertEquals(Severity.High, deltaToSeverity(4 * ms, custom))
        assertEquals(Severity.Normal, deltaToSeverity(8 * ms, custom))
        assertEquals(Severity.Low, deltaToSeverity(15 * ms, custom))
    }
}
```

- [ ] **Step 2: Run to verify they fail**

```bash
./gradlew :shared:allTests
```
Expected: FAIL — the `normalWhenDelta*` tests fail because the current code returns `Severity.High` for that range.

- [ ] **Step 3: Refactor `deltaToSeverity` in `ItemExtensions.kt`**

In `shared/src/commonMain/kotlin/com/adpt/shared/util/ItemExtensions.kt`:

Remove the three constant declarations at the top of the file:
```kotlin
private const val CRITICAL_THRESHOLD = 1 * MILLIS_PER_DAY
private const val WARNING_THRESHOLD = 2 * MILLIS_PER_DAY
private const val RECOMMENDED_THRESHOLD = 3 * MILLIS_PER_DAY
```

Replace the `deltaToSeverity` function with:
```kotlin
fun deltaToSeverity(delta: Long, config: ThresholdConfig): Severity {
    val criticalMs = config.criticalDays * MILLIS_PER_DAY
    val highMs = config.highDays * MILLIS_PER_DAY
    val normalMs = config.normalDays * MILLIS_PER_DAY
    return when {
        delta <= criticalMs -> Severity.Critical
        delta <= highMs -> Severity.High
        delta <= normalMs -> Severity.Normal
        else -> Severity.Low
    }
}
```

Add the `ThresholdConfig` import at the top of the file:
```kotlin
import com.adpt.shared.model.ThresholdConfig
```

- [ ] **Step 4: Fix the two call sites temporarily**

`CriticalItemsWorker.kt` line 36 — replace `deltaToSeverity(depletion - now)` with:
```kotlin
deltaToSeverity(depletion - now, ThresholdConfig())
```
Add import `import com.adpt.shared.model.ThresholdConfig`.

`OverviewViewModel.kt` line 77 — replace `deltaToSeverity(delta)` with:
```kotlin
deltaToSeverity(delta, ThresholdConfig())
```
Add import `import com.adpt.shared.model.ThresholdConfig`.

These use the default config for now; Tasks 7 and 8 will wire in the real values.

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :shared:allTests
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 6: Build the full project to verify no compile errors**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/adpt/shared/util/ItemExtensions.kt \
        shared/src/commonTest/kotlin/com/adpt/shared/DeltaToSeverityTest.kt \
        androidApp/src/main/kotlin/com/adpt/app/notifications/CriticalItemsWorker.kt \
        androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewViewModel.kt
git commit -m "fix: refactor deltaToSeverity to accept ThresholdConfig and fix Normal severity bug"
```

---

## Task 4: Create preference enums and `AppPreferencesRepository`

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/preferences/ThemeMode.kt`
- Create: `androidApp/src/main/kotlin/com/adpt/app/preferences/NotificationFrequency.kt`
- Create: `androidApp/src/main/kotlin/com/adpt/app/preferences/AppPreferencesRepository.kt`

- [ ] **Step 1: Create `ThemeMode` enum**

Create `androidApp/src/main/kotlin/com/adpt/app/preferences/ThemeMode.kt`:
```kotlin
package com.adpt.app.preferences

enum class ThemeMode { Light, Dark, System }
```

- [ ] **Step 2: Create `NotificationFrequency` enum**

Create `androidApp/src/main/kotlin/com/adpt/app/preferences/NotificationFrequency.kt`:
```kotlin
package com.adpt.app.preferences

enum class NotificationFrequency {
    OncePerDay,   // 24h WorkManager interval
    TwicePerDay,  // 12h WorkManager interval (previous default)
}
```

- [ ] **Step 3: Create `AppPreferencesRepository`**

Create `androidApp/src/main/kotlin/com/adpt/app/preferences/AppPreferencesRepository.kt`:
```kotlin
package com.adpt.app.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.adpt.shared.model.ThresholdConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AppPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATION_FREQUENCY = stringPreferencesKey("notification_frequency")
        val CRITICAL_DAYS = intPreferencesKey("critical_days")
        val HIGH_DAYS = intPreferencesKey("high_days")
        val NORMAL_DAYS = intPreferencesKey("normal_days")
    }

    val themeMode: StateFlow<ThemeMode> = dataStore.data
        .map { prefs ->
            prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.System
        }
        .stateIn(scope, SharingStarted.Eagerly, ThemeMode.System)

    val notificationFrequency: StateFlow<NotificationFrequency> = dataStore.data
        .map { prefs ->
            prefs[Keys.NOTIFICATION_FREQUENCY]
                ?.let { runCatching { NotificationFrequency.valueOf(it) }.getOrNull() }
                ?: NotificationFrequency.TwicePerDay
        }
        .stateIn(scope, SharingStarted.Eagerly, NotificationFrequency.TwicePerDay)

    val thresholdConfig: StateFlow<ThresholdConfig> = dataStore.data
        .map { prefs ->
            ThresholdConfig(
                criticalDays = prefs[Keys.CRITICAL_DAYS] ?: 1,
                highDays = prefs[Keys.HIGH_DAYS] ?: 2,
                normalDays = prefs[Keys.NORMAL_DAYS] ?: 3,
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, ThresholdConfig())

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setNotificationFrequency(freq: NotificationFrequency) {
        dataStore.edit { it[Keys.NOTIFICATION_FREQUENCY] = freq.name }
    }

    suspend fun setThresholdConfig(config: ThresholdConfig) {
        dataStore.edit { prefs ->
            prefs[Keys.CRITICAL_DAYS] = config.criticalDays
            prefs[Keys.HIGH_DAYS] = config.highDays
            prefs[Keys.NORMAL_DAYS] = config.normalDays
        }
    }
}
```

- [ ] **Step 4: Build to verify it compiles**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/preferences/
git commit -m "feat: add AppPreferencesRepository with DataStore backing"
```

---

## Task 5: Wire `AppPreferencesRepository` into `AdptApplication`

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/AdptApplication.kt`

- [ ] **Step 1: Rewrite `AdptApplication.kt`**

Replace the entire contents of `androidApp/src/main/kotlin/com/adpt/app/AdptApplication.kt`:
```kotlin
package com.adpt.app

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.adpt.app.notifications.CriticalItemsWorker
import com.adpt.app.notifications.NotificationHelper
import com.adpt.app.preferences.AppPreferencesRepository
import com.adpt.app.preferences.NotificationFrequency
import com.adpt.shared.db.AdptDatabase
import com.adpt.shared.db.DatabaseDriverFactory
import com.adpt.shared.db.createDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private val Application.dataStore by preferencesDataStore(name = "app_prefs")

class AdptApplication : Application() {

    lateinit var database: AdptDatabase
        private set

    lateinit var prefs: AppPreferencesRepository
        private set

    val pendingNavTarget = MutableStateFlow<String?>(null)
    val pendingShoppingError = MutableStateFlow<String?>(null)

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        database = createDatabase(DatabaseDriverFactory(this))
        prefs = AppPreferencesRepository(dataStore, applicationScope)
        NotificationHelper.createNotificationChannel(this)

        // Schedule immediately with whatever is stored, then reschedule on every change.
        scheduleStockCheck(prefs.notificationFrequency.value)
        applicationScope.launch {
            prefs.notificationFrequency.drop(1).collect { freq ->
                scheduleStockCheck(freq)
            }
        }
    }

    private fun scheduleStockCheck(frequency: NotificationFrequency) {
        val intervalHours = when (frequency) {
            NotificationFrequency.OncePerDay -> 24L
            NotificationFrequency.TwicePerDay -> 12L
        }
        val request = PeriodicWorkRequestBuilder<CriticalItemsWorker>(
            intervalHours, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CriticalItemsWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/AdptApplication.kt
git commit -m "feat: wire AppPreferencesRepository into AdptApplication and dynamic WorkManager scheduling"
```

---

## Task 6: Wire theme preference into `MainActivity`

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/MainActivity.kt`

- [ ] **Step 1: Update `MainActivity.kt`**

Replace the entire contents of `androidApp/src/main/kotlin/com/adpt/app/MainActivity.kt`:
```kotlin
package com.adpt.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adpt.app.navigation.AppNavigation
import com.adpt.app.preferences.ThemeMode
import com.adpt.app.ui.theme.AdptTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NAVIGATE_TO = "extra_navigate_to"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        requestNotificationPermissionIfNeeded()

        val app = application as AdptApplication
        setContent {
            val themeMode by app.prefs.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> isSystemInDarkTheme()
            }
            AdptTheme(darkTheme = isDark) {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.getStringExtra(EXTRA_NAVIGATE_TO)?.let { route ->
            (application as AdptApplication).pendingNavTarget.value = route
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0,
            )
        }
    }
}
```

- [ ] **Step 2: Build and verify**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/MainActivity.kt
git commit -m "feat: wire theme preference into MainActivity"
```

---

## Task 7: Update `OverviewViewModel` to use live `thresholdConfig`

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewViewModel.kt`

- [ ] **Step 1: Update `OverviewViewModel.kt`**

Replace the entire file contents:
```kotlin
package com.adpt.app.ui.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.adpt.app.AdptApplication
import com.adpt.shared.db.Item
import com.adpt.shared.model.AddToShoppingListResult
import com.adpt.shared.model.ItemPriority
import com.adpt.shared.model.Severity
import com.adpt.shared.model.ThresholdConfig
import com.adpt.shared.util.addToShoppingList
import com.adpt.shared.util.deltaToSeverity
import com.adpt.shared.util.estimatedDepletionDate
import com.adpt.shared.util.updateItemPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class OverviewItemUiModel(
    val id: String,
    val name: String,
    val severity: Severity,
    val deltaMillis: Long?,
    val isInShoppingList: Boolean,
)

sealed interface OverviewUiState {
    data object Loading : OverviewUiState
    data class Success(val items: List<OverviewItemUiModel>, val listVersion: Int = 0) : OverviewUiState
}

sealed interface OverviewIntent {
    data class AddToShoppingList(val itemId: String) : OverviewIntent
    data class IgnoreItem(val itemId: String) : OverviewIntent
}

class OverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as AdptApplication).database
    private val prefs = (application as AdptApplication).prefs

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

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
        prefs.thresholdConfig,
    ) { items, entries, now, version, thresholds ->
        val inShoppingList = entries.map { it.item_id }.toSet()
        val filtered = items.mapNotNull { item: Item ->
            if (item.priority == ItemPriority.Lowest) return@mapNotNull null
            val depletionDate = item.estimatedDepletionDate()
            if (depletionDate == null) {
                if (item.priority != ItemPriority.High && item.priority != ItemPriority.Highest) return@mapNotNull null
                return@mapNotNull OverviewItemUiModel(item.id, item.name, Severity.Critical, null, item.id in inShoppingList)
            }
            val delta = depletionDate - now
            val severity = deltaToSeverity(delta, thresholds)
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

    fun addAllToShoppingList(itemIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = itemIds.map { db.addToShoppingList(it) }
            val anySuccess = results.any { it is AddToShoppingListResult.Success }
            val anyError = results.any { it is AddToShoppingListResult.ItemNotFound }
            if (!anySuccess && anyError) {
                _errors.emit("Could not add items to the shopping list")
            }
        }
    }

    fun handleIntent(intent: OverviewIntent) {
        when (intent) {
            is OverviewIntent.AddToShoppingList -> viewModelScope.launch(Dispatchers.IO) {
                db.addToShoppingList(intent.itemId)
            }
            is OverviewIntent.IgnoreItem -> viewModelScope.launch(Dispatchers.IO) {
                db.itemQueries.updateItemPriority(intent.itemId, ItemPriority.Lowest)
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewViewModel.kt
git commit -m "feat: wire live thresholdConfig into OverviewViewModel"
```

---

## Task 8: Update `CriticalItemsWorker` to read `thresholdConfig` from repository

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/notifications/CriticalItemsWorker.kt`

- [ ] **Step 1: Update `CriticalItemsWorker.kt`**

Replace the entire file contents:
```kotlin
package com.adpt.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adpt.app.AdptApplication
import com.adpt.shared.model.ItemPriority
import com.adpt.shared.model.Severity
import com.adpt.shared.util.deltaToSeverity
import com.adpt.shared.util.estimatedDepletionDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class CriticalItemsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "critical_stock_check"
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as AdptApplication
        val db = app.database
        val thresholds = app.prefs.thresholdConfig.value
        val now = Clock.System.now().toEpochMilliseconds()

        val criticalItems = withContext(Dispatchers.IO) {
            db.itemQueries.selectAll().executeAsList().filter { item ->
                val depletion = item.estimatedDepletionDate()
                if (depletion == null) {
                    item.priority == ItemPriority.High || item.priority == ItemPriority.Highest
                } else {
                    deltaToSeverity(depletion - now, thresholds) == Severity.Critical
                }
            }
        }

        if (criticalItems.isNotEmpty()) {
            NotificationHelper.showCriticalNotification(applicationContext, criticalItems)
        }

        return Result.success()
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/notifications/CriticalItemsWorker.kt
git commit -m "feat: use live thresholdConfig in CriticalItemsWorker"
```

---

## Task 9: Create `PreferencesViewModel`

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/preferences/PreferencesViewModel.kt`

- [ ] **Step 1: Create `PreferencesViewModel.kt`**

Create `androidApp/src/main/kotlin/com/adpt/app/ui/preferences/PreferencesViewModel.kt`:
```kotlin
package com.adpt.app.ui.preferences

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adpt.app.AdptApplication
import com.adpt.app.preferences.NotificationFrequency
import com.adpt.app.preferences.ThemeMode
import com.adpt.shared.model.ThresholdConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PreferencesUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val notificationFrequency: NotificationFrequency = NotificationFrequency.TwicePerDay,
    val thresholds: ThresholdConfig = ThresholdConfig(),
)

class PreferencesViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as AdptApplication).prefs

    val uiState: StateFlow<PreferencesUiState> = combine(
        prefs.themeMode,
        prefs.notificationFrequency,
        prefs.thresholdConfig,
    ) { theme, freq, thresholds ->
        PreferencesUiState(theme, freq, thresholds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PreferencesUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setNotificationFrequency(freq: NotificationFrequency) {
        viewModelScope.launch { prefs.setNotificationFrequency(freq) }
    }

    fun incrementCritical() {
        val t = uiState.value.thresholds
        if (t.criticalDays + 1 >= t.highDays) return
        viewModelScope.launch { prefs.setThresholdConfig(t.copy(criticalDays = t.criticalDays + 1)) }
    }

    fun decrementCritical() {
        val t = uiState.value.thresholds
        if (t.criticalDays <= 1) return
        viewModelScope.launch { prefs.setThresholdConfig(t.copy(criticalDays = t.criticalDays - 1)) }
    }

    fun incrementHigh() {
        val t = uiState.value.thresholds
        if (t.highDays + 1 >= t.normalDays) return
        viewModelScope.launch { prefs.setThresholdConfig(t.copy(highDays = t.highDays + 1)) }
    }

    fun decrementHigh() {
        val t = uiState.value.thresholds
        if (t.highDays - 1 <= t.criticalDays) return
        viewModelScope.launch { prefs.setThresholdConfig(t.copy(highDays = t.highDays - 1)) }
    }

    fun incrementNormal() {
        val t = uiState.value.thresholds
        viewModelScope.launch { prefs.setThresholdConfig(t.copy(normalDays = t.normalDays + 1)) }
    }

    fun decrementNormal() {
        val t = uiState.value.thresholds
        if (t.normalDays - 1 <= t.highDays) return
        viewModelScope.launch { prefs.setThresholdConfig(t.copy(normalDays = t.normalDays - 1)) }
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/preferences/PreferencesViewModel.kt
git commit -m "feat: add PreferencesViewModel with stepper validation"
```

---

## Task 10: Create `PreferencesScreen`

**Files:**
- Create: `androidApp/src/main/kotlin/com/adpt/app/ui/preferences/PreferencesScreen.kt`

- [ ] **Step 1: Create `PreferencesScreen.kt`**

Create `androidApp/src/main/kotlin/com/adpt/app/ui/preferences/PreferencesScreen.kt`:
```kotlin
package com.adpt.app.ui.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adpt.app.preferences.NotificationFrequency
import com.adpt.app.preferences.ThemeMode
import com.adpt.shared.model.ThresholdConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: PreferencesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Preferences") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Appearance ──────────────────────────────────────────────────
            SectionHeader("Appearance")
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size,
                        ),
                        label = {
                            Text(
                                text = when (mode) {
                                    ThemeMode.Light -> "Light"
                                    ThemeMode.Dark -> "Dark"
                                    ThemeMode.System -> "System"
                                }
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ── Notifications ───────────────────────────────────────────────
            SectionHeader("Notifications")
            Text(
                text = "Depletion check frequency",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            NotificationFrequency.entries.forEach { freq ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = uiState.notificationFrequency == freq,
                        onClick = { viewModel.setNotificationFrequency(freq) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (freq) {
                            NotificationFrequency.OncePerDay -> "Once per day (24 h)"
                            NotificationFrequency.TwicePerDay -> "Twice per day (12 h)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // ── Depletion Thresholds ────────────────────────────────────────
            SectionHeader("Depletion Thresholds")
            Text(
                text = "Items are flagged when their estimated days remaining fall at or below each threshold.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            val t = uiState.thresholds
            ThresholdRow(
                label = "Critical",
                dotColor = MaterialTheme.colorScheme.error,
                days = t.criticalDays,
                onDecrement = { viewModel.decrementCritical() },
                onIncrement = { viewModel.incrementCritical() },
                decrementEnabled = t.criticalDays > 1,
                incrementEnabled = t.criticalDays + 1 < t.highDays,
            )
            ThresholdRow(
                label = "High",
                dotColor = MaterialTheme.colorScheme.tertiary,
                days = t.highDays,
                onDecrement = { viewModel.decrementHigh() },
                onIncrement = { viewModel.incrementHigh() },
                decrementEnabled = t.highDays - 1 > t.criticalDays,
                incrementEnabled = t.highDays + 1 < t.normalDays,
            )
            ThresholdRow(
                label = "Normal",
                dotColor = MaterialTheme.colorScheme.primary,
                days = t.normalDays,
                onDecrement = { viewModel.decrementNormal() },
                onIncrement = { viewModel.incrementNormal() },
                decrementEnabled = t.normalDays - 1 > t.highDays,
                incrementEnabled = true,
            )
            ThresholdRow(
                label = "Low",
                dotColor = MaterialTheme.colorScheme.secondary,
                days = t.normalDays + 1,
                onDecrement = {},
                onIncrement = {},
                decrementEnabled = false,
                incrementEnabled = false,
                readOnly = true,
                daysSuffix = "+",
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun ThresholdRow(
    label: String,
    dotColor: Color,
    days: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementEnabled: Boolean,
    incrementEnabled: Boolean,
    readOnly: Boolean = false,
    daysSuffix: String = "",
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Circle,
            contentDescription = null,
            tint = dotColor,
            modifier = Modifier.size(10.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = if (readOnly) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
        )
        if (readOnly) {
            Text(
                text = "${days}d$daysSuffix",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            IconButton(onClick = onDecrement, enabled = decrementEnabled) {
                Text(
                    text = "−",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (decrementEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
            Text(
                text = "${days}d",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(36.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onIncrement, enabled = incrementEnabled) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (incrementEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/ui/preferences/PreferencesScreen.kt
git commit -m "feat: add PreferencesScreen with theme, notification, and threshold sections"
```

---

## Task 11: Wire navigation — add `preferences` route and gear icon to Overview

**Files:**
- Modify: `androidApp/src/main/kotlin/com/adpt/app/navigation/AppNavigation.kt`
- Modify: `androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewScreen.kt`

- [ ] **Step 1: Add gear icon to `OverviewScreen`'s existing `TopAppBar`**

In `androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewScreen.kt`:

Change the `@Composable fun OverviewScreen` signature to accept a callback:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onOpenSettings: () -> Unit,
    viewModel: OverviewViewModel = viewModel(),
) {
```

In the `TopAppBar`'s `actions` block, add the settings icon **before** the existing Refresh button:
```kotlin
actions = {
    IconButton(onClick = onOpenSettings) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
        )
    }
    IconButton(onClick = { viewModel.refresh() }) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Refresh",
        )
    }
},
```

Add the import for `Settings` icon at the top:
```kotlin
import androidx.compose.material.icons.filled.Settings
```

- [ ] **Step 2: Add `preferences` route to `AppNavigation.kt`**

In `androidApp/src/main/kotlin/com/adpt/app/navigation/AppNavigation.kt`:

Add the import for `PreferencesScreen`:
```kotlin
import com.adpt.app.ui.preferences.PreferencesScreen
```

Inside the `NavHost` block, add after the existing composable declarations:
```kotlin
composable(Screen.Overview.route) {
    OverviewScreen(onOpenSettings = { navController.navigate("preferences") })
}
```

Replace the existing `composable(Screen.Overview.route) { OverviewScreen() }` line with the above.

Then add the preferences destination at the end of the `NavHost` block:
```kotlin
composable("preferences") {
    PreferencesScreen(onNavigateUp = { navController.navigateUp() })
}
```

- [ ] **Step 3: Build to verify**

```bash
./gradlew :androidApp:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run all shared tests to confirm nothing broke**

```bash
./gradlew :shared:allTests
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add androidApp/src/main/kotlin/com/adpt/app/navigation/AppNavigation.kt \
        androidApp/src/main/kotlin/com/adpt/app/ui/overview/OverviewScreen.kt
git commit -m "feat: add preferences navigation and settings gear icon to Overview"
```

---

## Manual Verification Checklist

After all tasks are complete, install the app on a device or emulator and verify:

- [ ] Tap the ⚙️ icon on the Overview screen → Preferences screen opens
- [ ] Back arrow returns to Overview
- [ ] **Theme:** Selecting Light/Dark/System immediately changes the app theme
- [ ] **Notification frequency:** Switching between Once/Twice per day persists after app restart (check WorkManager in Device File Explorer or logs)
- [ ] **Thresholds:** Tapping + increments the day count; tapping − decrements it
- [ ] **Stepper ordering enforced:** Cannot increment Critical beyond High-1, cannot decrement High below Critical+1, cannot decrement Normal below High+1
- [ ] **Critical minimum:** Cannot decrement Critical below 1
- [ ] **Low row** shows `Nd+` (one above Normal) and has no buttons
- [ ] Changing thresholds updates which items appear on the Overview screen (items that were Low become visible if thresholds are raised)
- [ ] All preferences survive app kill and restart
