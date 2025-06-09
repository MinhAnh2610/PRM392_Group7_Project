package com.tutorial.project.data.repository

import com.tutorial.project.data.model.StoreLocation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class StoreLocationRepository (private val client: SupabaseClient) {

  suspend fun getStoreLocations(): Result<List<StoreLocation>> {
    return try {
      val locations = client.from("store_locations")
        .select()
        .decodeList<StoreLocation>()
      Result.success(locations)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}