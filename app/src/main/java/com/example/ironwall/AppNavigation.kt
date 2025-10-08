package com.example.ironwall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ironwall.Screens.AddContactScreen
import com.example.ironwall.Screens.HomeScreen
import com.example.ironwall.Screens.LoginScreen
import com.example.ironwall.Screens.RegistrationScreen
import com.example.ironwall.Screens.SettingScreen
import com.example.ironwall.Screens.TokenEntryScreen
import io.ktor.client.HttpClient

// --------------------------
// Define all routes
// --------------------------
sealed class Screen(val route: String) {
    object Login : Screen("login_route")
    object Registration : Screen("registration_route")
    object TokenEntry : Screen("token_entry_route")
    object Home : Screen("home_route")
    object Setting : Screen("setting_route")
    object AddContact : Screen("add_contact_route")

    // Chat route with parameters
    object Chat : Screen("chat_route/{currentUserId}/{receiverId}/{receiverName}") {
        fun createRoute(currentUserId: String, receiverId: String, receiverName: String): String {
            return "chat_route/$currentUserId/$receiverId/$receiverName"
        }
    }
}

@Composable
fun AppNavigation(httpClient: HttpClient) {
    val navController = rememberNavController()
    val userViewModel: UserVM = viewModel()
    val users by userViewModel.users.collectAsState(initial = emptyList())

    // Decide start destination
    val startDestination = if (users.isNotEmpty()) {
        Screen.Login.route
    } else {
        Screen.Registration.route
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // Registration Screen
        composable(Screen.Registration.route) {
            RegistrationScreen(navController = navController, httpClient = httpClient)
        }

        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        // Token Entry (OTP Verification)
        composable(
            route = Screen.TokenEntry.route + "?totpSecret={totpSecret}",
            arguments = listOf(navArgument("totpSecret") {
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            })
        ) { backStackEntry ->
            TokenEntryScreen(
                navController = navController,
                totpSecret = backStackEntry.arguments?.getString("totpSecret")
            )
        }

        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        // Settings
        composable(Screen.Setting.route) {
            SettingScreen(navController = navController)
        }

        // Add Contact
        composable(Screen.AddContact.route) {
            val userViewModel: UserVM = viewModel()
            AddContactScreen(
                navController = navController,
                userVM = userViewModel,
                httpClient = httpClient
            )
        }


        // Chat Screen with arguments
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("currentUserId") { type = NavType.StringType },
                navArgument("receiverId") { type = NavType.StringType },
                navArgument("receiverName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val currentUserId = backStackEntry.arguments?.getString("currentUserId") ?: ""
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
            val receiverName = backStackEntry.arguments?.getString("receiverName") ?: ""

            ChatScreen(
                currentUserId = currentUserId,
                receiverId = receiverId,
                receiverName = receiverName,
                httpClient = httpClient
            )
        }
    }
}
