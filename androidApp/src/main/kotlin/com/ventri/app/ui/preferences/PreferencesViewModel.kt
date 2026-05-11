package com.ventri.app.ui.preferences

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ventri.app.VentriApplication
import com.ventri.app.preferences.NotificationFrequency
import com.ventri.app.preferences.ThemeMode
import com.ventri.shared.model.ThresholdConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PreferencesUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val notificationFrequency: NotificationFrequency = NotificationFrequency.TwicePerDay,
    val thresholds: ThresholdConfig = ThresholdConfig(),
    val languageTag: String? = null, // null = follow system
)

class PreferencesViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as VentriApplication).prefs

    private val _languageTag = MutableStateFlow(readCurrentLanguageTag())

    val uiState: StateFlow<PreferencesUiState> = combine(
        prefs.themeMode,
        prefs.notificationFrequency,
        prefs.thresholdConfig,
        _languageTag,
    ) { theme, freq, thresholds, langTag ->
        PreferencesUiState(theme, freq, thresholds, langTag)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PreferencesUiState())

    fun setLanguage(tag: String?) {
        val locales = if (tag == null) LocaleListCompat.getEmptyLocaleList()
                      else LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(locales)
        // Activity recreates automatically; _languageTag updated optimistically so UI stays consistent
        _languageTag.value = tag
    }

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

    private fun readCurrentLanguageTag(): String? {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.toLanguageTags().isEmpty()) null else locales[0]?.language
    }
}
