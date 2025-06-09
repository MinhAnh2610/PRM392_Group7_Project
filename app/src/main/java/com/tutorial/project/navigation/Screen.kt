package com.tutorial.project.navigation

sealed class Screen(val route: String) {
  data object Login : Screen("login")
  data object SignUp : Screen("sign_up")
  data object Dashboard: Screen("dashboard")
  data object ProductDetail : Screen("product_detail_{productId}") {
    // This helper function builds the full route with the specific product ID
    fun createRoute(productId: Int) = "product_detail_$productId"
  }
  data object Cart : Screen("cart")
}