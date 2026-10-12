package com.mediaautoskipper

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mediaautoskipper.ui.HistoryScreen
import com.mediaautoskipper.ui.HomeScreen
import com.mediaautoskipper.ui.MasTheme
import com.mediaautoskipper.ui.SettingsScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MasTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                val tabs = listOf(
                    R.string.tab_home to R.drawable.ic_nav_home,
                    R.string.tab_rules to R.drawable.ic_nav_rules,
                    R.string.tab_history to R.drawable.ic_nav_history,
                )
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            tabs.forEachIndexed { i, (label, icon) ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    icon = { Icon(painterResource(icon), contentDescription = null) },
                                    label = { Text(stringResource(label)) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    Column(Modifier.padding(padding)) {
                        when (tab) {
                            0 -> HomeScreen()
                            1 -> SettingsScreen()
                            else -> HistoryScreen()
                        }
                    }
                }
            }
        }
    }
}
