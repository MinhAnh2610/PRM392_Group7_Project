package com.tutorial.project.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.dto.AuthResult
import com.tutorial.project.data.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
  private val _loginResult = MutableLiveData<AuthResult?>()
  val loginResult: LiveData<AuthResult?> = _loginResult

  private val _signupResult = MutableLiveData<AuthResult?>()
  val signupResult: LiveData<AuthResult?> = _signupResult

  private val _navigateToDashboard = MutableLiveData(false)
  val navigateToDashboard: LiveData<Boolean> = _navigateToDashboard

  fun login(email: String, password: String) {
    viewModelScope.launch {
      val result = repository.login(email, password)
      Log.e("SUPABASE.login", result.toString())
      _loginResult.value = result.fold(
        onSuccess = { AuthResult.Success(email) },
        onFailure = { AuthResult.Error(it.message ?: "Unknown error") }
      )
      if (result.isSuccess) {
        _navigateToDashboard.value = true
      }
    }
  }

  fun signUp(email: String, password: String) {
    viewModelScope.launch {
      val result = repository.signUp(email, password)
      Log.e("SUPABASE.sign_up", result.toString())
      _signupResult.value = result.fold(
        onSuccess = { AuthResult.Success(it?.email ?: "") },
        onFailure = { AuthResult.Error(it.message ?: "Unknown error") }
      )
      if (result.isSuccess) {
        _navigateToDashboard.value = true
      }
    }
  }

  suspend fun logout() {
    repository.logout()
  }

  fun isLoggedIn(): Boolean = repository.isLoggedIn()
}