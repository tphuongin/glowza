package com.sgroupmobile.glowza.data.data_store.setting

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import com.sgroupmobile.glowza.R
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Khởi tạo DataStore cho Settings
val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val LANGUAGE = stringPreferencesKey("app_language")
        val DARK_MODE = intPreferencesKey("dark_mode")
        val LAST_TAB = intPreferencesKey("last_tab")
        val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
    }
    val isFirstRun: Flow<Boolean> = context.settingsDataStore.data.map { it[IS_FIRST_RUN] ?: true }

    suspend fun setFirstRunComplete() {
        context.settingsDataStore.edit { it[IS_FIRST_RUN] = false }
    }
    // Đọc Ngôn ngữ (Mặc định: Tiếng Việt)
    val language: Flow<String> = context.settingsDataStore.data.map { it[LANGUAGE] ?: "vi" }

    // Đọc Chế độ tối (Mặc định: Theo hệ thống)
    val darkMode: Flow<Int> = context.settingsDataStore.data.map {
        it[DARK_MODE] ?: 2 // 2 tương ứng với MODE_NIGHT_FOLLOW_SYSTEM
    }
    val lastTab: Flow<Int> = context.settingsDataStore.data.map {
        it[LAST_TAB] ?: R.id.nav_home // Mặc định về Home nếu chưa lưu gì
    }

    // Đọc Tab cuối cùng (Để không bị bay về Home)

    suspend fun setLanguage(lang: String) {
        context.settingsDataStore.edit { it[LANGUAGE] = lang }
    }

    suspend fun setDarkMode(mode: Int) {
        context.settingsDataStore.edit { it[DARK_MODE] = mode }
    }

    suspend fun setLastTab(tabId: Int) {
        context.settingsDataStore.edit { it[LAST_TAB] = tabId }
    }
}