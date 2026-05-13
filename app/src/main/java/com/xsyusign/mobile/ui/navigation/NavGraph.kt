package com.xsyusign.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xsyusign.mobile.ui.screen.*

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object UserList : Screen("user_list")
    data object UserEdit : Screen("user_edit/{userId}") {
        fun createRoute(userId: Long?) = "user_edit/${userId ?: 0}"
    }
    data object SignLog : Screen("sign_log")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToUsers = { navController.navigate(Screen.UserList.route) },
                onNavigateToLogs = { navController.navigate(Screen.SignLog.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.UserList.route) {
            UserListScreen(
                onBack = { navController.popBackStack() },
                onEditUser = { userId ->
                    navController.navigate(Screen.UserEdit.createRoute(userId))
                }
            )
        }

        composable(
            route = Screen.UserEdit.route,
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
            UserEditScreen(
                userId = if (userId == 0L) null else userId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SignLog.route) {
            SignLogScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
