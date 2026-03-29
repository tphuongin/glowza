package com.sgroupmobile.glowza.data.data_store.setting

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore by preferencesDataStore(
    name = "settings_prefs"
)