package com.tutorial.project.data.repository

import com.tutorial.project.data.model.Product
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class ProductRepository(private val client: SupabaseClient) {

  suspend fun fetchProducts(): Result<List<Product>> = try {
    val result = client
      .from("products")
      .select()
      .decodeList<Product>()

    Result.success(result)
  } catch (e: Exception) {
    Result.failure(e)
  }
}
