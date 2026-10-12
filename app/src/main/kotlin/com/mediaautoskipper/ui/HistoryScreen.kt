package com.mediaautoskipper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mediaautoskipper.MasApp
import com.mediaautoskipper.R
import com.mediaautoskipper.history.SkipRecord
import com.mediaautoskipper.settings.AppSettings
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

object HistoryTags {
    const val CLEAR = "history_clear"
    const val EMPTY = "history_empty"
    const val CONFIRM = "history_clear_confirm"
    const val CANCEL = "history_clear_cancel"
    fun chip(pkg: String) = "history_chip_$pkg"
    fun disliked(id: Long) = "history_disliked_$id"
}

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as MasApp
    val scope = rememberCoroutineScope()
    val records by app.historyDb.dao().observeAll().collectAsState(initial = emptyList())
    val settings by app.settingsStore.settings.collectAsState(initial = AppSettings())
    val players = settings.monitoredPackages.map { PlayerInfo(it, PlayerCatalog.label(context, it)) }
    val labels = (players + records.map { it.pkg }.distinct().map { PlayerInfo(it, PlayerCatalog.label(context, it)) })
        .associate { it.pkg to it.label }

    HistoryContent(
        records = records,
        players = players,
        playerLabel = { labels[it] ?: it },
        onClear = { scope.launch { app.historyDb.dao().clear() } },
    )
}

/**
 * Newest-first list of skips as cards. [players] is the monitored list reused as the filter; nothing
 * selected means every player is shown.
 */
@Composable
fun HistoryContent(
    records: List<SkipRecord>,
    players: List<PlayerInfo>,
    playerLabel: (String) -> String,
    onClear: () -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var confirming by remember { mutableStateOf(false) }

    val shown = records
        .filter { selected.isEmpty() || it.pkg in selected }
        .sortedWith(compareByDescending<SkipRecord> { it.timeMillis }.thenByDescending { it.id })
    val format = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = Dimens.md).padding(top = Dimens.md),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
            players.forEach { p ->
                FilterChip(
                    selected = p.pkg in selected,
                    onClick = { selected = if (p.pkg in selected) selected - p.pkg else selected + p.pkg },
                    label = { Text(p.label) },
                    modifier = Modifier.testTag(HistoryTags.chip(p.pkg)),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { confirming = true }, modifier = Modifier.testTag(HistoryTags.CLEAR)) {
                Text(stringResource(R.string.history_clear))
            }
        }

        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(Dimens.lg), contentAlignment = Alignment.TopCenter) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                    Text(
                        stringResource(R.string.history_empty),
                        Modifier.testTag(HistoryTags.EMPTY),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.history_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                items(shown, key = { it.id }) { r ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.cardRadius),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(Modifier.padding(Dimens.md), verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                            Text(r.title.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOf(r.artist, playerLabel(r.pkg)).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm), verticalAlignment = Alignment.CenterVertically) {
                                Pill(r.reason, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                if (r.disliked) {
                                    Pill(
                                        stringResource(R.string.history_disliked),
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                        MaterialTheme.colorScheme.onTertiaryContainer,
                                        Modifier.testTag(HistoryTags.disliked(r.id)),
                                    )
                                }
                            }
                            Text(
                                format.format(Date(r.timeMillis)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item { Box(Modifier.padding(bottom = Dimens.md)) }
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onClear()
                    },
                    modifier = Modifier.testTag(HistoryTags.CONFIRM),
                ) { Text(stringResource(R.string.history_clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }, modifier = Modifier.testTag(HistoryTags.CANCEL)) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** A small rounded label; its meaning is always in the text, never in the colour alone. */
@Composable
private fun Pill(text: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = container) {
        Text(
            text,
            Modifier.padding(horizontal = Dimens.sm, vertical = Dimens.xs),
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}
