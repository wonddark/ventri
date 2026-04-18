package com.ventri.app.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import com.ventri.shared.model.ThresholdConfig
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
        val USER_ID = stringPreferencesKey("user_id")
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

    val userId: StateFlow<String?> = dataStore.data
        .map { prefs -> prefs[Keys.USER_ID] }
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun getRegisteredUserId(): String? =
        dataStore.data.first()[Keys.USER_ID]

    suspend fun setUserId(id: String) {
        dataStore.edit { it[Keys.USER_ID] = id }
    }

    suspend fun clearUserId() {
        dataStore.edit { it.remove(Keys.USER_ID) }
    }
}
