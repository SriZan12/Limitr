package com.khadka.limitr

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.khadka.limitr.services.notifications.NotificationPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NotificationPreferencesTest {
    @Test fun perPackageChoicesAreIndependentAndExplicitOffWinsOverLegacyOn() = runBlocking {
        val store = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
                transform(data.value).also { data.value = it }
        }
        val settings = NotificationPreferences(store)
        settings.set("first.app", true)
        settings.set("second.app", false)
        settings.inherit("second.app", true)
        settings.inherit("legacy.app", true)
        assertEquals(mapOf("first.app" to true, "second.app" to false, "legacy.app" to true), settings.choices.first())
        settings.set("first.app", false)
        assertFalse(settings.choices.first().getValue("first.app"))
        assertTrue(settings.choices.first().getValue("legacy.app"))
        settings.set("legacy.app", null)
        assertFalse(settings.choices.first().containsKey("legacy.app"))
    }
}
