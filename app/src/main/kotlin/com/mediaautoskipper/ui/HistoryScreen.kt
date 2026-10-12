package com.mediaautoskipper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
 * Newest-first list of skips. [players] is the monitored list reused as the filter; nothing selected
 * means every player is shown.
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

    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            players.forEach { p ->
                FilterChip(
                    selected = p.pkg in selected,
                    onClick = { selected = if (p.pkg in selected) selected - p.pkg else selected + p.pkg },
                    label = { Text(p.label) },
                    modifier = Modifier.testTag(HistoryTags.chip(p.pkg)),
                )
            }
        }
        TextButton(onClick = { confirming = true }, modifier = Modifier.testTag(HistoryTags.CLEAR)) {
            Text(stringResource(R.string.history_clear))
        }

        if (shown.isEmpty()) {
            Text(stringResource(R.string.history_empty), Modifier.testTag(HistoryTags.EMPTY))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shown, key = { it.id }) { r ->
                    Column {
                        Text(r.title.ifBlank { "—" }, style = MaterialTheme.typography.titleSmall)
                        Text(listOf(r.artist, playerLabel(r.pkg)).filter { it.isNotBlank() }.joinToString(" · "))
                        Text(
                            format.format(Date(r.timeMillis)) + " · " + r.reason,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
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
