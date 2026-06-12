package com.hibol.miette.soi.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun MainBottomBar(currentRoute: String?, navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.HOME,
            onClick = {
                if (currentRoute != Routes.HOME) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.AutoStories, contentDescription = "Journal") },
            label = { Text("Journal") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.PARTS,
            onClick = {
                if (currentRoute != Routes.PARTS) {
                    navController.navigate(Routes.PARTS) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.Groups, contentDescription = "Parties") },
            label = { Text("Parties") }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.EXPLORATION,
            onClick = {
                if (currentRoute != Routes.EXPLORATION) {
                    navController.navigate(Routes.EXPLORATION) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Default.Insights, contentDescription = "Exploration") },
            label = { Text("Explorer") }
        )
    }
}
