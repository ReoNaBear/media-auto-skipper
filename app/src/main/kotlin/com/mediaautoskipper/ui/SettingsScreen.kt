package com.mediaautoskipper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mediaautoskipper.MasApp
import com.mediaautoskipper.settings.AppSettings
import com.mediaautoskipper.settings.DislikeRules
import com.mediaautoskipper.settings.RuleKind
import kotlinx.coroutines.launch

/** Rules tab: dislike warning, detection, monitored players, keywords, regular expressions and language, as cards. */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val store = (context.applicationContext as MasApp).settingsStore
    val scope = rememberCoroutineScope()
    val settings by store.settings.collectAsState(initial = AppSettings())
    val candidates = remember { PlayerCatalog.installedMediaApps(context) }
    val monitored = settings.monitoredPackages.map { PlayerInfo(it, PlayerCatalog.label(context, it)) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(Dimens.md),
        verticalArrangement = Arrangement.spacedBy(Dimens.md),
    ) {
        RuleTogglesAndPlayers(
            simplifiedEnabled = settings.simplifiedEnabled,
            onSimplifiedChange = { scope.launch { store.setSimplifiedEnabled(it) } },
            monitored = monitored,
            candidates = candidates,
            onRemove = { pkg -> scope.launch { store.setMonitoredPackages(settings.monitoredPackages - pkg) } },
            onAdd = { pkg -> scope.launch { store.setMonitoredPackages(settings.monitoredPackages + pkg) } },
            simplifiedDislike = DislikeRules.SIMPLIFIED in settings.dislikeRules,
            onSimplifiedDislikeChange = { scope.launch { store.setDislike(DislikeRules.SIMPLIFIED, it) } },
        )
        RuleSection(
            RuleKind.KEYWORD,
            settings.keywords,
            onChange = { scope.launch { store.setKeywords(it) } },
            dislikeKeys = settings.dislikeRules,
            onDislikeChange = { key, on -> scope.launch { store.setDislike(key, on) } },
        )
        RuleSection(
            RuleKind.REGEX,
            settings.regexes,
            onChange = { scope.launch { store.setRegexes(it) } },
            dislikeKeys = settings.dislikeRules,
            onDislikeChange = { key, on -> scope.launch { store.setDislike(key, on) } },
        )
        LanguageSection()
    }
}
