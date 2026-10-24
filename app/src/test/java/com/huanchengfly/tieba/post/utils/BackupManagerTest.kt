package com.huanchengfly.tieba.post.utils

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupManagerTest {
    @Test
    fun exportsSettingsButNotDeviceIds() {
        val items = BackupManager.exportPreferences(
            preferencesOf(
                booleanPreferencesKey("auto_sign") to true,
                stringPreferencesKey("client_id") to "secret",
                intPreferencesKey("sign_day") to 3,
                stringSetPreferencesKey("tags") to setOf("a", "b"),
            )
        )
        assertEquals(listOf("auto_sign", "tags"), items.map { it.key })
        assertEquals(listOf("boolean", "string_set"), items.map { it.type })
    }

    @Test
    fun preferencesRoundTripThroughJson() {
        val source = preferencesOf(
            booleanPreferencesKey("auto_sign") to true,
            intPreferencesKey("radius") to 12,
            stringPreferencesKey("auto_sign_time") to "08:30",
            stringSetPreferencesKey("tags") to setOf("a", "b"),
        )
        val json = com.google.gson.Gson().toJson(
            BackupManager.Backup(preferences = BackupManager.exportPreferences(source))
        )
        assert(json.contains("\"format\":\"tiebalite-backup\""))
        val target = mutablePreferencesOf()
        BackupManager.parse(json).preferences!!.forEach { BackupManager.applyPreference(target, it) }
        assertEquals(source.asMap(), target.asMap())
    }

    @Test(expected = BackupManager.InvalidBackupException::class)
    fun rejectsOtherJson() {
        BackupManager.parse("""{"foo":1}""")
    }

    @Test(expected = BackupManager.InvalidBackupException::class)
    fun rejectsNewerFormat() {
        BackupManager.parse("""{"format":"tiebalite-backup","version":99}""")
    }

    @Test
    fun ignoresBadPreferenceValue() {
        val target = mutablePreferencesOf()
        BackupManager.applyPreference(
            target,
            BackupManager.PreferenceItem("radius", "int", com.google.gson.JsonPrimitive("abc"))
        )
        assertNull(target[intPreferencesKey("radius")])
    }
}
