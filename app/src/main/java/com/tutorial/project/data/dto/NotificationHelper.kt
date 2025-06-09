// app/src/main/java/com/tutorial/project/util/NotificationHelper.kt
package com.tutorial.project.data.dto

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.tutorial.project.MainActivity
import com.tutorial.project.MainActivity.Companion.CART_CHANNEL_ID
import com.tutorial.project.R
import com.tutorial.project.navigation.Screen

object NotificationHelper {

  fun showCartNotification(context: Context, itemCount: Int) {
    // This intent will be wrapped in a PendingIntent to be triggered when the notification is tapped.
    // We add an extra to tell MainActivity to navigate to the Cart screen.
    val resultIntent = Intent(context, MainActivity::class.java).apply {
      putExtra("NAVIGATE_TO", Screen.Cart.route)
    }

    // TaskStackBuilder creates a synthetic back stack, so pressing back from the
    // Cart screen will lead to the Dashboard, not exit the app.
    val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
      addNextIntentWithParentStack(resultIntent)
      getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    val builder = NotificationCompat.Builder(context, CART_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a proper cart icon if you have one
      .setContentTitle("You have items in your cart!")
      .setContentText("You have $itemCount item(s) waiting for you. Complete your purchase now.")
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(resultPendingIntent)
      .setAutoCancel(true) // Notification automatically removes itself when tapped

    if (ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    ) {
      with(NotificationManagerCompat.from(context)) {
        // notificationId is a unique int for each notification that you must define
        val notificationId = 1
        notify(notificationId, builder.build())
      }
    }
  }
}