package com.mediaautoskipper.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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

/**
 * Home tab: brand header, the master switch as the main card, then the notification access status and
 * the battery hint. The whole switch row is one 64 dp touch target, so it is easy to hit in a car.
 */
@Composable
fun HomeContent(
    accessGranted: Boolean,
    enabled: Boolean,
    onOpenAccessSettings: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onOpenBatterySettings: () -> Unit = {},
) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(Dimens.md),
        verticalArrangement = Arrangement.spacedBy(Dimens.md),
    ) {
        BrandHeader()

        SectionCard(
            title = null,
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 64.dp)
                    .toggleable(
                        value = enabled,
                        enabled = accessGranted,
                        role = Role.Switch,
                        onValueChange = onToggle,
                    )
                    .testTag(Tags.ENABLE_SWITCH),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.md),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.switch_label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(if (enabled) R.string.status_on else R.string.status_off),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = null, enabled = accessGranted)
            }
            if (!accessGranted) {
                Text(
                    stringResource(R.string.switch_needs_access),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (enabled) {
                Text(stringResource(R.string.switch_on_reminder), style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (accessGranted) {
            SectionCard(title = null, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Image(
                        painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSecondaryContainer),
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        stringResource(R.string.status_access_granted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        } else {
            SectionCard(title = null, containerColor = MaterialTheme.colorScheme.errorContainer) {
                Text(
                    stringResource(R.string.status_access_missing_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    stringResource(R.string.status_access_missing_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Button(onClick = onOpenAccessSettings, modifier = Modifier.defaultMinSize(minHeight = Dimens.touchTarget)) {
                    Text(stringResource(R.string.action_open_settings))
                }
            }
        }

        SectionCard(title = null) {
            Text(stringResource(R.string.battery_hint), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onOpenBatterySettings, modifier = Modifier.defaultMinSize(minHeight = Dimens.touchTarget)) {
                Text(stringResource(R.string.action_open_battery_settings))
            }
        }
    }
}

/** App icon on its teal tile, the name and a one-line description. */
@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary),
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(56.dp))
        }
        Column {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
