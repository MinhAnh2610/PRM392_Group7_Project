package com.tutorial.project.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tutorial.project.ui.auth.LoginScreen
import com.tutorial.project.ui.auth.SignUpScreen
import com.tutorial.project.ui.dashboard.DashboardScreen
import com.tutorial.project.viewmodel.AuthViewModel

@Composable
fun MainNavHost() {
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = Screen.Login.route) {
    composable(Screen.Login.route) {
      LoginScreen(
        navController,
        onNavigateToSignUp = {
          navController.navigate(Screen.SignUp.route)
        }
      )
    }
    composable(Screen.SignUp.route) {
      SignUpScreen(
        navController,
        onNavigateToLogin = {
          navController.navigate(Screen.Login.route)
        }
      )
    }
    composable(Screen.Dashboard.route) {
      DashboardScreen(
        navController
      )
    }
  }
}