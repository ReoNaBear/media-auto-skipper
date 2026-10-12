package com.mediaautoskipper.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mediaautoskipper.R

data class PlayerInfo(val pkg: String, val label: String)

object PlayerTags {
    const val SIMPLIFIED_SWITCH = "simplified_switch"
    const val SIMPLIFIED_DISLIKE = "simplified_dislike"
}

/** Resolves display names and lists installed apps that look like media players. */
object PlayerCatalog {
    private val knownNames = mapOf(
        "com.google.android.apps.youtube.music" to "YouTube Music",
        "com.google.android.youtube" to "YouTube",
        "com.spotify.music" to "Spotify",
    )

    fun label(context: Context, pkg: String): String =
        knownNames[pkg] ?: try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkg
        }

    /** Apps that register for media buttons or expose a MediaBrowserService (see manifest `queries`). */
    fun installedMediaApps(context: Context): List<PlayerInfo> {
        val pm = context.packageManager
        val packages = buildSet {
            pm.queryBroadcastReceivers(Intent(Intent.ACTION_MEDIA_BUTTON), 0).forEach { add(it.activityInfo.packageName) }
            pm.queryIntentServices(Intent("android.media.browse.MediaBrowserService"), 0)
                .forEach { add(it.serviceInfo.packageName) }
        }
        return packages.filter { it != context.packageName }
            .map { PlayerInfo(it, label(context, it)) }
            .sortedBy { it.label.lowercase() }
    }
}

/** The Simplified Chinese toggle and the monitored-players list with an "add installed app" list. */
@Composable
fun RuleTogglesAndPlayers(
    simplifiedEnabled: Boolean,
    onSimplifiedChange: (Boolean) -> Unit,
    monitored: List<PlayerInfo>,
    candidates: List<PlayerInfo>,
    onRemove: (String) -> Unit,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
    simplifiedDislike: Boolean = false,
    /** Called with the new state of the Simplified Chinese "also mark as dislike" option; when null, it is not shown. */
    onSimplifiedDislikeChange: ((Boolean) -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(
                checked = simplifiedEnabled,
                onCheckedChange = onSimplifiedChange,
                modifier = Modifier.testTag(PlayerTags.SIMPLIFIED_SWITCH),
            )
            Text(stringResource(R.string.rule_simplified))
        }
        if (onSimplifiedDislikeChange != null) {
            DislikeOption(
                checked = simplifiedDislike,
                onCheckedChange = onSimplifiedDislikeChange,
                enabled = simplifiedEnabled,
                modifier = Modifier.testTag(PlayerTags.SIMPLIFIED_DISLIKE),
            )
            Text(stringResource(R.string.dislike_note), style = MaterialTheme.typography.bodySmall)
        }

        Text(stringResource(R.string.players_title), style = MaterialTheme.typography.titleMedium)
        monitored.forEach { p ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(p.label, Modifier.weight(1f))
                TextButton(onClick = { onRemove(p.pkg) }) { Text(stringResource(R.string.action_remove)) }
            }
        }

        val addable = candidates.filter { c -> monitored.none { it.pkg == c.pkg } }
        if (addable.isNotEmpty()) {
            Text(stringResource(R.string.players_add_title), style = MaterialTheme.typography.titleSmall)
            addable.forEach { p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.label, Modifier.weight(1f))
                    TextButton(onClick = { onAdd(p.pkg) }) { Text(stringResource(R.string.action_add)) }
                }
            }
        }
    }
}
