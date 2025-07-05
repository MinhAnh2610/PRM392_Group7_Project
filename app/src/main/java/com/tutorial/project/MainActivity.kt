package com.tutorial.project

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.tutorial.project.navigation.MainNavHost
import com.tutorial.project.ui.theme.ProjectTheme
import com.stripe.android.PaymentConfiguration

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    createNotificationChannel()

    // Initialize Stripe
    PaymentConfiguration.init(
      applicationContext,
      "pk_test_51RbbUnD7p526E7XrIPR3qIifK3hU5u1d19oQaPuGGoIVEycHO9fsXu4bZW6ZEc8Fx6YIhXl2K06ug3bFcXVEwO0k00hKDqN9RM" // <-- REPLACE with your Publishable Key
    )

    enableEdgeToEdge()
    setContent {
      ProjectTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            MainNavHost()
          }
        }
      }
    }
  }

  private fun createNotificationChannel() {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    val name = "Cart Reminders"
    val descriptionText = "Notifications about items in your cart"
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel(CART_CHANNEL_ID, name, importance).apply {
      description = descriptionText
    }
    // Register the channel with the system
    val notificationManager: NotificationManager =
      getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
  }

  companion object {
    const val CART_CHANNEL_ID = "cart_channel"
  }
}