package com.mediaautoskipper.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SettingsStoreTest {

    private fun open(file: File, scope: CoroutineScope) =
        SettingsStore(PreferenceDataStoreFactory.create(scope = scope) { file })

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Test
    fun firstRunHasDefaultsAndDefaultPlayers() = runBlocking {
        val dir = createTempDirectory().toFile()
        val scope = newScope()
        val s = open(File(dir, "a.preferences_pb"), scope).settings.first()
        assertEquals(listOf("合輯", "合辑", "精選", "精选", "Full Album", "Mix"), s.keywords)
        assertEquals(
            listOf("com.google.android.apps.youtube.music", "com.google.android.youtube", "com.spotify.music"),
            s.monitoredPackages,
        )
        assertTrue(s.simplifiedEnabled)
        scope.cancel()
    }

    @Test
    fun writtenSettingsSurviveReopeningTheStore() = runBlocking {
        val file = File(createTempDirectory().toFile(), "b.preferences_pb")
        val first = newScope()
        val store = open(file, first)
        store.setKeywords(listOf("DJ", "Nightcore"))
        store.setRegexes(listOf("""^\[Live\]"""))
        store.setSimplifiedEnabled(false)
        store.setMonitoredPackages(listOf("com.spotify.music"))
        first.coroutineContext[Job]!!.cancelAndJoin()

        val second = newScope()
        val s = open(file, second).settings.first()
        assertEquals(listOf("DJ", "Nightcore"), s.keywords)
        assertEquals(listOf("""^\[Live\]"""), s.regexes)
        assertEquals(false, s.simplifiedEnabled)
        assertEquals(listOf("com.spotify.music"), s.monitoredPackages)
        second.cancel()
    }

    @Test
    fun removingEveryDefaultKeywordStaysEmpty() = runBlocking {
        val scope = newScope()
        val store = open(File(createTempDirectory().toFile(), "c.preferences_pb"), scope)
        store.setKeywords(emptyList())
        assertEquals(emptyList(), store.settings.first().keywords)
        scope.cancel()
    }

    @Test
    fun neverWrittenDislikeRulesMeansEverythingSkipsOnly() = runBlocking {
        val scope = newScope()
        val store = open(File(createTempDirectory().toFile(), "d.preferences_pb"), scope)
        store.setKeywords(listOf("Mix"))
        assertEquals(emptySet(), store.settings.first().dislikeRules)
        scope.cancel()
    }

    @Test
    fun dislikeFlagsSurviveReopeningTheStore() = runBlocking {
        val file = File(createTempDirectory().toFile(), "e.preferences_pb")
        val first = newScope()
        val store = open(file, first)
        store.setDislike("keyword:Mix", true)
        store.setDislike(DislikeRules.SIMPLIFIED, true)
        store.setDislike("keyword:Mix", false)
        first.coroutineContext[Job]!!.cancelAndJoin()

        val second = newScope()
        assertEquals(setOf(DislikeRules.SIMPLIFIED), open(file, second).settings.first().dislikeRules)
        second.cancel()
    }

    @Test
    fun editingAKeywordMovesItsFlagAndDeletingDropsIt() = runBlocking {
        val scope = newScope()
        val store = open(File(createTempDirectory().toFile(), "f.preferences_pb"), scope)
        store.setKeywords(listOf("Mix", "DJ"))
        store.setDislike("keyword:Mix", true)

        store.setKeywords(listOf("DJ Mix", "DJ"))
        assertEquals(setOf("keyword:DJ Mix"), store.settings.first().dislikeRules)

        store.setKeywords(listOf("DJ"))
        assertEquals(emptySet(), store.settings.first().dislikeRules)

        store.setKeywords(listOf("DJ", "DJ Mix"))
        assertEquals(emptySet(), store.settings.first().dislikeRules)
        scope.cancel()
    }

    @Test
    fun regexFlagsFollowEditsToo() = runBlocking {
        val scope = newScope()
        val store = open(File(createTempDirectory().toFile(), "g.preferences_pb"), scope)
        store.setRegexes(listOf("^a"))
        store.setDislike("regex:^a", true)
        store.setRegexes(listOf("^b"))
        assertEquals(setOf("regex:^b"), store.settings.first().dislikeRules)
        scope.cancel()
    }
}
