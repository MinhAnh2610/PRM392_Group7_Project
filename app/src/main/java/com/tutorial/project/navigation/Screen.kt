package com.tutorial.project.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
  data object Login : Screen("login")
  data object SignUp : Screen("sign_up")
  data object Dashboard: Screen("dashboard")
  data object ProductDetail : Screen("product_detail/{productId}") {
    fun createRoute(productId: Int) = "product_detail/$productId"
  }
  data object Cart : Screen("cart")
  data object Billing : Screen("billing")
  data object OrderHistory : Screen("order_history")
  data object OrderDetail : Screen("order_detail/{orderId}") {
    fun createRoute(orderId: Int) = "order_detail/$orderId"
  }
  data object ConversationsList : Screen("conversations_list")
  data object Chat : Screen("chat/{storeOwnerId}/{storeName}") {
    fun createRoute(storeOwnerId: String, storeName: String): String {
      val encodedStoreName = URLEncoder.encode(storeName, StandardCharsets.UTF_8.toString())
      return "chat/$storeOwnerId/$encodedStoreName"
    }
  }
  data object Map : Screen("map/{latitude}/{longitude}/{storeName}/{storeAddress}") {
    fun createRoute(lat: Double, lon: Double, name: String, address: String): String {
      val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
      val encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8.toString())
      return "map/$lat/$lon/$encodedName/$encodedAddress"
    }
  }
}