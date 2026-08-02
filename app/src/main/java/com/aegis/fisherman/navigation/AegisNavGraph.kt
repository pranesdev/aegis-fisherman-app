package com.aegis.fisherman.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aegis.fisherman.ui.dashboard.DashboardScreen
import com.aegis.fisherman.ui.guide.GuideScreen
import com.aegis.fisherman.ui.history.TripHistoryScreen
import com.aegis.fisherman.ui.map.MapScreen
import com.aegis.fisherman.ui.settings.SettingsScreen
import com.aegis.fisherman.ui.sync.SyncScreen
import com.aegis.fisherman.ui.weather.WeatherScreen

private sealed class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Dashboard : Destination("dashboard", "Home", Icons.Filled.Dashboard)
    data object Map : Destination("map", "Map", Icons.Filled.Map)
    data object Weather : Destination("weather", "Weather", Icons.Filled.Cloud)
    data object Guide : Destination("guide", "Guide", Icons.AutoMirrored.Filled.MenuBook)
    data object Sync : Destination("sync", "Sync", Icons.Filled.Sync)
    data object History : Destination("history", "Log", Icons.Filled.History)
    data object Settings : Destination("settings", "Settings", Icons.Filled.Settings)
}

private val bottomNavItems = listOf(
    Destination.Dashboard, Destination.Map, Destination.Weather, Destination.Guide, Destination.Sync,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AegisNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = Color.Transparent, // Let the background gradient show through
        topBar = {
            TopAppBar(
                title = { 
                    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text("A", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
                        }
                        Text("AEGIS Fisherman", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.Black.copy(alpha = 0.3f)) {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                bottomNavItems.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.White.copy(alpha = 0.5f),
                            unselectedTextColor = Color.White.copy(alpha = 0.5f),
                            indicatorColor = Color.White.copy(alpha = 0.1f)
                        ),
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.aegis.fisherman.ui.theme.AegisColors.BackgroundGradient)
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Destination.Dashboard.route
            ) {
                composable(Destination.Dashboard.route) { DashboardScreen() }
                composable(Destination.Map.route) { MapScreen() }
                composable(Destination.Weather.route) { WeatherScreen() }
                composable(Destination.Guide.route) { GuideScreen() }
                composable(Destination.Sync.route) { SyncScreen() }
                composable(Destination.History.route) { TripHistoryScreen() }
                composable(Destination.Settings.route) { SettingsScreen() }
            }
        }
    }
}