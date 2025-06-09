package com.tutorial.project.data.repository

import com.tutorial.project.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class ProfileRepository(private val client: SupabaseClient, private val authRepository: AuthRepository) {

  suspend fun getMyProfile(): Result<Profile?> {
    val userId = authRepository.getCurrentUserId()
      ?: return Result.failure(Exception("User not logged in"))
    return try {
      val profile = client.from("profiles")
        .select { filter { eq("id", userId) } }
        .decodeSingleOrNull<Profile>() // Assumes this extension exists
      Result.success(profile)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateProfile(username: String?, phone: String?): Result<Profile> {
    val userId = authRepository.getCurrentUserId()
      ?: return Result.failure(Exception("User not logged in"))
    return try {
      val updateData = mutableMapOf<String, Any?>()
      username?.let { updateData["username"] = it }
      phone?.let { updateData["phone"] = it }

      if (updateData.isEmpty()) return Result.failure(Exception("No data to update"))

      val updatedProfile = client.from("profiles")
        .update(updateData) { filter { eq("id", userId) } }
        .decodeSingle<Profile>() // Assumes this returns the updated row
      Result.success(updatedProfile)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}