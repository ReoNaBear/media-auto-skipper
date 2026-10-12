package com.mediaautoskipper

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.mediaautoskipper.history.HistoryDatabase
import com.mediaautoskipper.settings.SettingsStore

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class MasApp : Application() {
    val settingsStore: SettingsStore by lazy { SettingsStore(settingsDataStore) }
    val historyDb: HistoryDatabase by lazy { HistoryDatabase.open(this) }
}
