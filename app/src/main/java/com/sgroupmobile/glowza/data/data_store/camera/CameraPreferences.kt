package com.sgroupmobile.glowza.data.data_store.camera

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

//Create new a new property for Context
val Context.cameraDataStore by preferencesDataStore(
    name = "settings_camera"
)