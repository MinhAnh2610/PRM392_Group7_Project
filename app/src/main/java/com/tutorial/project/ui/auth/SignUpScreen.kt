package com.tutorial.project.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.dto.AuthResult
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.navigation.Screen
import com.tutorial.project.viewmodel.AuthViewModel
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth

@Composable
fun SignUpScreen(
  navController: NavController,
  onNavigateToLogin: () -> Unit
) {
  val viewModel: AuthViewModel = viewModel(
    factory = GenericViewModelFactory {
      val repository = AuthRepository(SupabaseClientProvider.client.auth)
      AuthViewModel(repository)
    }
  )

  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  val context = LocalContext.current

  val authResult by viewModel.signupResult.observeAsState()
  val navigateToDashboard by viewModel.navigateToDashboard.observeAsState(false)

  LaunchedEffect(authResult) {
    when (authResult) {
      is AuthResult.Success -> {
        val email = (authResult as AuthResult.Success).userEmail
        Toast.makeText(context, "Sign up successful! $email", Toast.LENGTH_SHORT).show()
      }
      is AuthResult.Error -> {
        val message = (authResult as AuthResult.Error).message
        Toast.makeText(context, "Sign up failed: $message", Toast.LENGTH_SHORT).show()
      }
      null -> {}
    }
  }

  LaunchedEffect(navigateToDashboard) {
    if (navigateToDashboard) {
      navController.navigate("dashboard") {
        popUpTo("sign_up") { inclusive = true } // prevent back navigation to login
      }
    }
  }

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Center
  ) {
    Text("Sign Up", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

    OutlinedTextField(
      value = email,
      onValueChange = { email = it },
      label = { Text("Email") },
      modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
      value = password,
      onValueChange = { password = it },
      label = { Text("Password") },
      visualTransformation = PasswordVisualTransformation(),
      modifier = Modifier.fillMaxWidth()
    )

    Button(
      onClick = { viewModel.signUp(email, password) },
      modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    ) {
      Text("Sign Up")
    }

    TextButton(
      onClick = onNavigateToLogin,
      modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
    ) {
      Text("Already have an account? Login")
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
      onClick = {
        // Navigate to Dashboard but clear Login from the back stack
        navController.navigate(Screen.Dashboard.route) {
          popUpTo(Screen.Login.route) { inclusive = true }
        }
      },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Continue as Guest")
    }
  }
}
