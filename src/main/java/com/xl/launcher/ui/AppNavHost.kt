package com.xl.launcher.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost(startRoute: String = "home") {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startRoute, modifier = Modifier.fillMaxSize()) {
        composable("home") { HomeScreenNav(navController) }
        composable("assistant") { AssistantScreen() }
        composable("store") { StoreScreen() }
        composable("profiles") { ProfilesScreen() }
    }
}
