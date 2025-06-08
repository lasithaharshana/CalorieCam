package com.example.caloriecam.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val screens = listOf(
        Screen.Home,
        Screen.Camera
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar(modifier = modifier) {
        screens.forEach { screen ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = if (currentRoute == screen.route) {
                            screen.selectedIcon
                        } else {
                            screen.unselectedIcon
                        },
                        contentDescription = screen.title
                    )
                },
                label = { Text(text = screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination and avoid building up
                        // a large stack of destinations
                        popUpTo(navController.graph.startDestinationId)
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}