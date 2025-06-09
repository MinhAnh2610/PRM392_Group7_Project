package com.tutorial.project.data.repository

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val auth: Auth) {
  suspend fun signUp(email: String, password: String) = withContext(Dispatchers.IO) {
    try {
      val result = auth.signUpWith(Email) {
        this.email = email
        this.password = password
      }
      Result.success(result)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun login(email: String, password: String) = withContext(Dispatchers.IO) {
    try {
      val result = auth.signInWith(Email) {
        this.email = email
        this.password = password
      }
      Result.success(result)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun isLoggedIn(): Boolean = auth.currentSessionOrNull() != null

  suspend fun logout() {
    auth.signOut()
  }

  fun getCurrentUserId(): String? {
    return auth.currentUserOrNull()?.id
  }
}