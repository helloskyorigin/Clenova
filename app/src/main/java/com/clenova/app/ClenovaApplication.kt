package com.clenova.app

import android.app.Application
import com.clenova.app.core.storage.DeviceStorageProvider
import com.clenova.app.data.preferences.DataStorePreferencesRepository
import com.clenova.app.data.storage.LocalStorageRepository

/** Process-scoped dependencies shared by UI and future background workers. */
class ClenovaApplication : Application() {
    val preferencesRepository by lazy { DataStorePreferencesRepository(applicationContext) }
    val storageRepository by lazy { LocalStorageRepository(DeviceStorageProvider(applicationContext)) }
}
