package com.tutorial.project.data.repository

import com.tutorial.project.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class ProfileInsert(
    val id: String,
    val username: String,
    val phone: String
)

class ProfileRepository(private val client: SupabaseClient) {

    suspend fun createProfile(userId: String, username: String, phone: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profileData = ProfileInsert(
                id = userId,
                username = username,
                phone = phone
            )

            client
                .from("profiles")
                .insert(profileData)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(userId: String): Result<Profile?> = withContext(Dispatchers.IO) {
        try {
            val result = client
                .from("profiles")
                .select() {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<Profile>()

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(userId: String, username: String, phone: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updateData = mapOf(
                "username" to username,
                "phone" to phone
            )

            client
                .from("profiles")
                .update(updateData) {
                    filter {
                        eq("id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isUsernameAvailable(username: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val result = client
                .from("profiles")
                .select() {
                    filter {
                        eq("username", username)
                    }
                }
                .decodeSingleOrNull<Profile>()

            Result.success(result == null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
