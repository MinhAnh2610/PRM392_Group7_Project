package com.tutorial.project.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.ui.auth.LoginScreen
import com.tutorial.project.ui.auth.SignUpScreen
import com.tutorial.project.ui.dashboard.DashboardScreen
import io.github.jan.supabase.auth.auth


@Composable
fun MainNavHost() {
  val navController = rememberNavController()

  val authRepository = AuthRepository(SupabaseClientProvider.client.auth)
  val startDestination = if (authRepository.isLoggedIn()) {
    Screen.Dashboard.route
  } else {
    Screen.Login.route
  }

  NavHost(navController = navController, startDestination = startDestination) {
    composable(Screen.Login.route) {
      LoginScreen(
        navController = navController,
        onNavigateToSignUp = {
          navController.navigate(Screen.SignUp.route)
        }
      )
    }
    composable(Screen.SignUp.route) {
      SignUpScreen(
        navController = navController,
        onNavigateToLogin = {
          navController.navigate(Screen.Login.route) {
            popUpTo(Screen.SignUp.route) { inclusive = true }
          }
        }
      )
    }
    composable(Screen.Dashboard.route) {
      DashboardScreen(navController = navController)
    }
  }
}
