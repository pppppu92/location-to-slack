package com.example.location_to_slack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.location_to_slack.data.AppDatabase
import com.example.location_to_slack.data.CheckpointRepository
import com.example.location_to_slack.ui.CheckpointViewModel
import com.example.location_to_slack.ui.MainScreen
import com.example.location_to_slack.ui.SettingsScreen
import com.example.location_to_slack.ui.theme.LocationtoslackTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CheckpointViewModel by viewModels {
        viewModelFactory {
            initializer {
                val dao = AppDatabase.getDatabase(applicationContext).checkpointDao()
                CheckpointViewModel(application, CheckpointRepository(dao))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LocationtoslackTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            listOf(
                                Triple("main", "メイン", Icons.Default.Home),
                                Triple("settings", "設定", Icons.Default.Settings)
                            ).forEach { (route, label, icon) ->
                                NavigationBarItem(
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) },
                                    selected = currentRoute == route,
                                    onClick = {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("main") { MainScreen(viewModel = viewModel) }
                        composable("settings") { SettingsScreen(viewModel = viewModel) }
                    }
                }
            }
        }
    }
}
