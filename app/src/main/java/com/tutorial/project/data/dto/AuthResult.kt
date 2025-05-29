package com.tutorial.project.data.dto

sealed class AuthResult {
  data class Success(val userEmail: String) : AuthResult()
  data class Error(val message: String) : AuthResult()
}
