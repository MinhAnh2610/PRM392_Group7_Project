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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.dto.AuthResult
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.viewmodel.AuthViewModel
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth

@Composable
fun SignUpScreen(
  navController: NavController,
  onNavigateToLogin: () -> Unit,
) {
  val viewModel: AuthViewModel = viewModel(
    factory = GenericViewModelFactory {
      val repository = AuthRepository(SupabaseClientProvider.client.auth)
      AuthViewModel(repository)
    }
  )

  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var emailError by remember { mutableStateOf("") }
  var passwordError by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val authResult by viewModel.signupResult.observeAsState()
  val navigateToDashboard by viewModel.navigateToDashboard.observeAsState(false)

  // Validation function
  fun validateInputs(): Boolean {
    emailError = ""
    passwordError = ""

    if (email.isBlank()) {
      emailError = "Email is required"
      return false
    }
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      emailError = "Invalid email format"
      return false
    }
    if (password.isBlank()) {
      passwordError = "Password is required"
      return false
    }
    if (password.length < 6) {
      passwordError = "Password must be at least 6 characters"
      return false
    }
    return true
  }

  LaunchedEffect(authResult) {
    isLoading = false
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
        popUpTo("sign_up") { inclusive = true }
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.Center
  ) {
    Text("Sign Up", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

    OutlinedTextField(
      value = email,
      onValueChange = {
        email = it
        emailError = ""
      },
      label = { Text("Email") },
      isError = emailError.isNotEmpty(),
      modifier = Modifier.fillMaxWidth()
    )
    if (emailError.isNotEmpty()) {
      Text(
        text = emailError,
        color = Color.Red,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = password,
      onValueChange = {
        password = it
        passwordError = ""
      },
      label = { Text("Password") },
      visualTransformation = PasswordVisualTransformation(),
      isError = passwordError.isNotEmpty(),
      modifier = Modifier.fillMaxWidth()
    )
    if (passwordError.isNotEmpty()) {
      Text(
        text = passwordError,
        color = Color.Red,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
      )
    }

    Button(
      onClick = {
        if (validateInputs()) {
          isLoading = true
          viewModel.signUp(email, password)
        }
      },
      enabled = !isLoading,
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 16.dp)
    ) {
      if (isLoading) {
        Text("Signing up...")
      } else {
        Text("Sign Up")
      }
    }

    TextButton(
      onClick = onNavigateToLogin,
      modifier = Modifier
        .align(Alignment.End)
        .padding(top = 8.dp)
    ) {
      Text("Already have an account? Login")
    }
  }
}
