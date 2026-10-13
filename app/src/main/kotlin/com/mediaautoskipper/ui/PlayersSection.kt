package com.mediaautoskipper.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

/**
 * The Rules tab's first two cards: Simplified Chinese detection (with the optional dislike option and the
 * warning that goes with it) and the monitored players with an "add installed app" list.
 */
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
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
        // Dislike writes to the user's account and makes skipping slower and less reliable (it waits before
        // acting and some players move on by themselves), so the warning sits above every dislike option.
        if (onSimplifiedDislikeChange != null) DislikeWarningCard()

        SectionCard(stringResource(R.string.rules_section_detection)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Dimens.touchTarget)
                    .toggleable(
                        value = simplifiedEnabled,
                        role = Role.Switch,
                        onValueChange = onSimplifiedChange,
                    )
                    .testTag(PlayerTags.SIMPLIFIED_SWITCH),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.md),
            ) {
                Text(stringResource(R.string.rule_simplified), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = simplifiedEnabled, onCheckedChange = null)
            }
            if (onSimplifiedDislikeChange != null) {
                DislikeOption(
                    checked = simplifiedDislike,
                    onCheckedChange = onSimplifiedDislikeChange,
                    enabled = simplifiedEnabled,
                    modifier = Modifier.testTag(PlayerTags.SIMPLIFIED_DISLIKE),
                )
            }
        }

        SectionCard(stringResource(R.string.players_title)) {
            monitored.forEach { p ->
                Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = Dimens.touchTarget), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    val removeLabel = stringResource(R.string.a11y_remove_player, p.label)
                    TextButton(
                        onClick = { onRemove(p.pkg) },
                        modifier = Modifier.semantics { contentDescription = removeLabel },
                    ) { Text(stringResource(R.string.action_remove)) }
                }
            }

            val addable = candidates.filter { c -> monitored.none { it.pkg == c.pkg } }
            if (addable.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    stringResource(R.string.players_add_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                addable.forEach { p ->
                    Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = Dimens.touchTarget), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        val addLabel = stringResource(R.string.a11y_add_player, p.label)
                        TextButton(
                            onClick = { onAdd(p.pkg) },
                            modifier = Modifier.semantics { contentDescription = addLabel },
                        ) { Text(stringResource(R.string.action_add)) }
                    }
                }
            }
        }
    }
}
