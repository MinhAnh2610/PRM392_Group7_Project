package com.tutorial.project.data.repository

import com.tutorial.project.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository(
  private val client: SupabaseClient,
  private val authRepository: AuthRepository // Injected from main
) {

  /**
   * Creates a new profile for the currently authenticated user.
   * This combines the creation logic from 'Login-out-signup' with the
   * authentication context from 'main'.
   */
  suspend fun createProfile(username: String, phone: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val userId = authRepository.getCurrentUserId()
        ?: return@withContext Result.failure(Exception("User not logged in"))

      // No need for a separate ProfileInsert class, we can create a map directly.
      val profileData = mapOf(
        "id" to userId,
        "username" to username,
        "phone" to phone
      )

      client.from("profiles").insert(profileData)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Fetches the profile for the currently authenticated user.
   * This is the improved 'getMyProfile' from the 'main' branch.
   */
  suspend fun getCurrentUserProfile(): Result<Profile?> = withContext(Dispatchers.IO) {
    try {
      val userId = authRepository.getCurrentUserId()
        ?: return@withContext Result.failure(Exception("User not logged in"))

      val profile = client.from("profiles")
        .select { filter { eq("id", userId) } }
        .decodeSingleOrNull<Profile>()

      Result.success(profile)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Updates the profile for the currently authenticated user.
   * This improved version from 'main' handles partial updates gracefully
   * and returns the updated profile, which is useful for the UI.
   */
  suspend fun updateProfile(username: String?, phone: String?): Result<Profile> = withContext(Dispatchers.IO) {
    try {
      val userId = authRepository.getCurrentUserId()
        ?: return@withContext Result.failure(Exception("User not logged in"))

      val updateData = mutableMapOf<String, Any>()
      username?.let { updateData["username"] = it }
      phone?.let { updateData["phone"] = it }

      if (updateData.isEmpty()) {
        return@withContext Result.failure(Exception("No data to update"))
      }

      val updatedProfile = client.from("profiles")
        .update(updateData) { filter { eq("id", userId) } }
        .decodeSingle<Profile>()

      Result.success(updatedProfile)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Checks if a username is already taken.
   * This is a useful utility function retained from the 'Login-out-signup' branch.
   */
  suspend fun isUsernameAvailable(username: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val result = client.from("profiles")
        .select() {
          filter { eq("username", username) }
          limit(1) // Optimization: we only need to know if one exists
        }
        .decodeSingleOrNull<Profile>()

      Result.success(result == null)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}