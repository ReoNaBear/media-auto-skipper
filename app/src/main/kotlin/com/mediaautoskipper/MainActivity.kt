package com.mediaautoskipper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import com.mediaautoskipper.ui.HistoryScreen
import com.mediaautoskipper.ui.HomeScreen
import com.mediaautoskipper.ui.SettingsScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                val tabs = listOf(R.string.tab_home, R.string.tab_rules, R.string.tab_history)
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            tabs.forEachIndexed { i, label ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    icon = {},
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
