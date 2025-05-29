package com.tutorial.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.navigation.MainNavHost
import com.tutorial.project.ui.theme.ProjectTheme
import com.tutorial.project.viewmodel.AuthViewModel
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
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
}