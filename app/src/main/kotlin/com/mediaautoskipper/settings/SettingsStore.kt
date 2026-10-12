package com.mediaautoskipper.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mediaautoskipper.filter.DefaultBlacklist
import com.mediaautoskipper.filter.FilterConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object DefaultPlayers {
    const val YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"
    const val YOUTUBE = "com.google.android.youtube"
    const val SPOTIFY = "com.spotify.music"
    val packages: List<String> = listOf(YOUTUBE_MUSIC, YOUTUBE, SPOTIFY)
}

data class AppSettings(
    val simplifiedEnabled: Boolean = true,
    val keywords: List<String> = DefaultBlacklist.keywords,
    val regexes: List<String> = emptyList(),
    val monitoredPackages: List<String> = DefaultPlayers.packages,
    /** Rule keys (see [DislikeRules]) whose matches also send the player's dislike action. Empty by default. */
    val dislikeRules: Set<String> = emptySet(),
) {
    val filterConfig: FilterConfig get() = FilterConfig(simplifiedEnabled, keywords, regexes)
}

/** Master switch: effective only while the device has not rebooted since it was turned on. */
data class EnableState(val enabled: Boolean, val bootCount: Int) {
    fun isActive(currentBootCount: Int): Boolean = enabled && bootCount == currentBootCount
}

/**
 * Persists user settings in a Preferences DataStore. A list key that was never written yields the
 * default list; once written, an empty list stays empty (the user removed every default entry).
 */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { p ->
        AppSettings(
            simplifiedEnabled = p[SIMPLIFIED] ?: true,
            keywords = p[KEYWORDS]?.let(::decode) ?: DefaultBlacklist.keywords,
            regexes = p[REGEXES]?.let(::decode) ?: emptyList(),
            monitoredPackages = p[PACKAGES]?.let(::decode) ?: DefaultPlayers.packages,
            dislikeRules = p[DISLIKE_RULES]?.let(::decode)?.toSet() ?: emptySet(),
        )
    }

    /** Stored switch state; use [EnableState.isActive] to learn whether the service may act. */
    val enableState: Flow<EnableState> = dataStore.data.map { p ->
        EnableState(enabled = p[ENABLED] ?: false, bootCount = p[ENABLED_BOOT_COUNT] ?: -1)
    }

    suspend fun setSimplifiedEnabled(enabled: Boolean) {
        dataStore.edit { it[SIMPLIFIED] = enabled }
    }

    /** Replaces the keyword list; dislike flags follow edited entries and are dropped for removed ones. */
    suspend fun setKeywords(keywords: List<String>) {
        dataStore.edit {
            val old = it[KEYWORDS]?.let(::decode) ?: DefaultBlacklist.keywords
            reconcileFlags(it, RuleKind.KEYWORD, old, keywords)
            it[KEYWORDS] = encode(keywords)
        }
    }

    /** Replaces the regex list; dislike flags follow edited entries and are dropped for removed ones. */
    suspend fun setRegexes(regexes: List<String>) {
        dataStore.edit {
            val old = it[REGEXES]?.let(::decode) ?: emptyList()
            reconcileFlags(it, RuleKind.REGEX, old, regexes)
            it[REGEXES] = encode(regexes)
        }
    }

    /** Turns the "also dislike" option of one rule (see [DislikeRules.key]) on or off. */
    suspend fun setDislike(ruleKey: String, enabled: Boolean) {
        dataStore.edit {
            val current = it[DISLIKE_RULES]?.let(::decode)?.toSet() ?: emptySet()
            it[DISLIKE_RULES] = encode((if (enabled) current + ruleKey else current - ruleKey).toList())
        }
    }

    private fun reconcileFlags(p: MutablePreferences, kind: RuleKind, old: List<String>, new: List<String>) {
        val flags = p[DISLIKE_RULES]?.let(::decode)?.toSet() ?: return
        p[DISLIKE_RULES] = encode(DislikeRules.reconcile(flags, kind, old, new).toList())
    }

    suspend fun setMonitoredPackages(packages: List<String>) {
        dataStore.edit { it[PACKAGES] = encode(packages) }
    }

    /** Records [currentBootCount] so that a later reboot invalidates the switch. */
    suspend fun setEnabled(enabled: Boolean, currentBootCount: Int) {
        dataStore.edit {
            it[ENABLED] = enabled
            it[ENABLED_BOOT_COUNT] = currentBootCount
        }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("master_enabled")
        val ENABLED_BOOT_COUNT = intPreferencesKey("master_enabled_boot_count")
        val SIMPLIFIED = booleanPreferencesKey("simplified_enabled")
        val KEYWORDS = stringPreferencesKey("keywords")
        val REGEXES = stringPreferencesKey("regexes")
        val PACKAGES = stringPreferencesKey("monitored_packages")
        val DISLIKE_RULES = stringPreferencesKey("dislike_rules")

        // One entry per line; entries never contain line breaks (they are flattened on write).
        fun encode(items: List<String>): String =
            items.joinToString("\n") { it.replace('\n', ' ').replace('\r', ' ') }

        fun decode(s: String): List<String> = if (s.isEmpty()) emptyList() else s.split("\n")
    }
}
