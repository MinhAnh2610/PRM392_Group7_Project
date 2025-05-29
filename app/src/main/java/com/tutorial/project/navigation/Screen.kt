package com.tutorial.project.navigation

sealed class Screen(val route: String) {
  data object Login : Screen("login")
  data object SignUp : Screen("sign_up")
  data object Dashboard: Screen("dashboard")
}