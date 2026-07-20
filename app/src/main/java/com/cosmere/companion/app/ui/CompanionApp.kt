package com.cosmere.companion.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cosmere.companion.app.ui.screens.CharactersScreen
import com.cosmere.companion.app.ui.screens.DiceScreen
import com.cosmere.companion.app.ui.screens.ReferenceScreen

private enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Characters("characters", "Characters", Icons.Filled.Groups),
    Dice("dice", "Dice", Icons.Filled.Casino),
    Reference("reference", "Reference", Icons.AutoMirrored.Filled.MenuBook),
}

@Composable
fun CompanionApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var referenceFocusKey by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
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
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Characters.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.Characters.route) {
                CharactersScreen(
                    onOpenReference = { key ->
                        referenceFocusKey = key
                        navController.navigate(TopLevelDestination.Reference.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(TopLevelDestination.Dice.route) { DiceScreen() }
            composable(TopLevelDestination.Reference.route) {
                ReferenceScreen(
                    focusKey = referenceFocusKey,
                    onFocusHandled = { referenceFocusKey = null },
                )
            }
        }
    }
}
