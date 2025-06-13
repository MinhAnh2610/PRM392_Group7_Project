// app/src/main/java/com/tutorial/project/ui/map/MapScreen.kt
package com.tutorial.project.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
  navController: NavController,
  latitude: Double,
  longitude: Double,
  storeName: String,
  storeAddress: String
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(storeName) },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = Alignment.Center
    ) {
      MapboxMap(
        Modifier.fillMaxSize(),
        mapViewportState = rememberMapViewportState {
          setCameraOptions {
            zoom(2.0)
            center(Point.fromLngLat(longitude, latitude))
            pitch(0.0)
            bearing(0.0)
          }
        },
      )
    }
  }
}