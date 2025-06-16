package com.tutorial.project.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.ui.auth.LoginScreen
import com.tutorial.project.ui.auth.SignUpScreen
import com.tutorial.project.ui.cart.CartScreen
import com.tutorial.project.ui.chat.ChatScreen
import com.tutorial.project.ui.chat.ConversationsListScreen
import com.tutorial.project.ui.checkout.BillingScreen
import com.tutorial.project.ui.dashboard.DashboardScreen
import com.tutorial.project.ui.map.MapScreen
import com.tutorial.project.ui.order.OrderDetailScreen
import com.tutorial.project.ui.order.OrderHistoryScreen
import com.tutorial.project.ui.product.ProductDetailScreen
import io.github.jan.supabase.auth.auth
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun MainNavHost() {
  val navController = rememberNavController()

  // Dynamic start destination logic from 'Login-out-signup'
  val authRepository = AuthRepository(SupabaseClientProvider.client.auth)
  val startDestination = if (authRepository.isLoggedIn()) {
    Screen.Dashboard.route
  } else {
    Screen.Login.route
  }

  NavHost(navController = navController, startDestination = startDestination) {
    // --- Authentication Screens from 'Login-out-signup' ---
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
            // Pop SignUp from the back stack to prevent going back to it
            popUpTo(Screen.SignUp.route) { inclusive = true }
          }
        }
      )
    }

    // --- Feature Screens from 'main' ---
    composable(Screen.Dashboard.route) {
      DashboardScreen(navController = navController)
    }

    composable(
      route = Screen.ProductDetail.route,
      arguments = listOf(navArgument("productId") { type = NavType.IntType })
    ) { backStackEntry ->
      val productId = backStackEntry.arguments?.getInt("productId")
      requireNotNull(productId) { "Product ID is required" }
      ProductDetailScreen(navController, productId)
    }

    composable(Screen.Cart.route) {
      CartScreen(navController = navController)
    }

    composable(Screen.Billing.route) {
      BillingScreen(navController = navController)
    }

    composable(Screen.OrderHistory.route) {
      OrderHistoryScreen(navController = navController)
    }

    composable(
      route = Screen.OrderDetail.route,
      arguments = listOf(navArgument("orderId") { type = NavType.IntType })
    ) { backStackEntry ->
      val orderId = backStackEntry.arguments?.getInt("orderId")
      requireNotNull(orderId)
      OrderDetailScreen(navController = navController, orderId = orderId)
    }

    composable(Screen.ConversationsList.route) {
      ConversationsListScreen(navController = navController)
    }

    composable(
      route = Screen.Chat.route,
      arguments = listOf(
        navArgument("storeOwnerId") { type = NavType.StringType },
        navArgument("storeName") { type = NavType.StringType }
      )
    ) { backStackEntry ->
      val arguments = requireNotNull(backStackEntry.arguments)
      val storeOwnerId = requireNotNull(arguments.getString("storeOwnerId"))
      val storeName = requireNotNull(arguments.getString("storeName"))
      // Decode the store name in case it contains special characters
      val decodedStoreName = URLDecoder.decode(storeName, StandardCharsets.UTF_8.toString())

      ChatScreen(
        navController = navController,
        storeOwnerId = storeOwnerId,
        storeName = decodedStoreName
      )
    }

    composable(
      route = Screen.Map.route,
      arguments = listOf(
        navArgument("latitude") { type = NavType.FloatType }, // Pass lat/lon as float
        navArgument("longitude") { type = NavType.FloatType },
        navArgument("storeName") { type = NavType.StringType },
        navArgument("storeAddress") { type = NavType.StringType }
      )
    ) { backStackEntry ->
      val arguments = requireNotNull(backStackEntry.arguments)
      MapScreen(
        navController = navController,
        latitude = arguments.getFloat("latitude").toDouble(),
        longitude = arguments.getFloat("longitude").toDouble(),
        storeName = arguments.getString("storeName") ?: "Store Location",
        storeAddress = arguments.getString("storeAddress") ?: ""
      )
    }
  }
}