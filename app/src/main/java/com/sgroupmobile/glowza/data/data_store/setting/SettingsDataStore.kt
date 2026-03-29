package com.sgroupmobile.glowza.data.data_store.setting

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val DARK_MODE = intPreferencesKey("dark_mode")
        val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
    }

    // --- READ DATA ---

    val isFirstRun: Flow<Boolean> = context.settingsDataStore.data.map {
        it[PreferencesKeys.IS_FIRST_RUN] ?: true
    }
    val mode: Flow<Int> = context.settingsDataStore.data.map {
        it[PreferencesKeys.DARK_MODE] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    suspend fun setFirstRunComplete() {
        context.settingsDataStore.edit { it[PreferencesKeys.IS_FIRST_RUN] = false }
    }
    val language: Flow<String> = context.settingsDataStore.data.map {
        it[PreferencesKeys.LANGUAGE] ?: "vi"
    }

    val isDarkMode: Flow<Boolean> = context.settingsDataStore.data.map {
        val savedMode = it[PreferencesKeys.DARK_MODE] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        if (savedMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            val currentMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        } else {
            savedMode == AppCompatDelegate.MODE_NIGHT_YES
        }
    }

    suspend fun setLanguage(lang: String) {
        context.settingsDataStore.edit { it[PreferencesKeys.LANGUAGE] = lang }
    }

    suspend fun setDarkMode(mode: Int) {
        context.settingsDataStore.edit { it[PreferencesKeys.DARK_MODE] = mode }
    }
}
