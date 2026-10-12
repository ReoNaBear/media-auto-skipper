package com.mediaautoskipper.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class EnableStateTest {

    private fun store(scope: CoroutineScope) = SettingsStore(
        PreferenceDataStoreFactory.create(scope = scope) { File(createTempDirectory().toFile(), "e.preferences_pb") },
    )

    @Test
    fun offByDefault() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        assertFalse(store(scope).enableState.first().isActive(currentBootCount = 7))
        scope.cancel()
    }

    @Test
    fun activeAfterTurningOnInSameBoot() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val s = store(scope)
        s.setEnabled(true, currentBootCount = 7)
        assertTrue(s.enableState.first().isActive(currentBootCount = 7))
        scope.cancel()
    }

    @Test
    fun inactiveAfterBootCountChanges() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val s = store(scope)
        s.setEnabled(true, currentBootCount = 7)
        assertFalse(s.enableState.first().isActive(currentBootCount = 8))
        scope.cancel()
    }

    @Test
    fun turningOffDeactivates() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val s = store(scope)
        s.setEnabled(true, 7)
        s.setEnabled(false, 7)
        assertFalse(s.enableState.first().isActive(7))
        scope.cancel()
    }
}
