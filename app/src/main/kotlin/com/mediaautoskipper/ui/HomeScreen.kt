package com.mediaautoskipper.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mediaautoskipper.MasApp
import com.mediaautoskipper.R
import com.mediaautoskipper.settings.EnableState
import kotlinx.coroutines.launch

object Tags {
    const val ENABLE_SWITCH = "enable_switch"
}

private fun hasNotificationAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/** Wires [HomeContent] to the real permission state and the settings store. */
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val store = (context.applicationContext as MasApp).settingsStore
    val scope = rememberCoroutineScope()

    var granted by remember { mutableStateOf(hasNotificationAccess(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = hasNotificationAccess(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val state by store.enableState.collectAsState(initial = EnableState(false, -1))
    val bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)

    HomeContent(
        accessGranted = granted,
        enabled = state.isActive(bootCount),
        onOpenAccessSettings = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
        onToggle = { checked -> scope.launch { store.setEnabled(checked, bootCount) } },
        onOpenBatterySettings = {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        },
    )
}

@Composable
fun HomeContent(
    accessGranted: Boolean,
    enabled: Boolean,
    onOpenAccessSettings: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onOpenBatterySettings: () -> Unit = {},
) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (accessGranted) {
            Text(stringResource(R.string.status_access_granted))
        } else {
            Text(stringResource(R.string.status_access_missing_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.status_access_missing_body))
            Button(onClick = onOpenAccessSettings) { Text(stringResource(R.string.action_open_settings)) }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(
                checked = enabled,
                enabled = accessGranted,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag(Tags.ENABLE_SWITCH),
            )
            Text(stringResource(R.string.switch_label))
        }
        if (!accessGranted) Text(stringResource(R.string.switch_needs_access))
        if (enabled) Text(stringResource(R.string.switch_on_reminder))

        Text(stringResource(R.string.battery_hint))
        OutlinedButton(onClick = onOpenBatterySettings) { Text(stringResource(R.string.action_open_battery_settings)) }
    }
}
