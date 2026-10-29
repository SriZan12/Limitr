package com.khadka.limitr.services.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Package-keyed choices survive expiry and are independent of Android's notification access. */
@Singleton
class NotificationPreferences @Inject constructor(private val store: DataStore<Preferences>) {
    private val prefix = "hide_notifications:"
    val choices = store.data.map { values ->
        values.asMap().mapNotNull { (key, value) ->
            if (key.name.startsWith(prefix) && value is Boolean) key.name.removePrefix(prefix) to value else null
        }.toMap()
    }

    suspend fun set(packageName: String, enabled: Boolean?) {
        val key = booleanPreferencesKey(prefix + packageName)
        store.edit { if (enabled == null) it.remove(key) else it[key] = enabled }
    }

    suspend fun inherit(packageName: String, enabled: Boolean) {
        val key = booleanPreferencesKey(prefix + packageName)
        store.edit { if (it[key] == null) it[key] = enabled }
    }
}
