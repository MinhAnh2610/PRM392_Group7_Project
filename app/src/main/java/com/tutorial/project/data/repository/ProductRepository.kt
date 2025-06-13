package com.tutorial.project.data.repository

import com.tutorial.project.data.model.Product
import com.tutorial.project.data.model.ProductWithStoreInfo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class ProductRepository(private val client: SupabaseClient) {

  suspend fun fetchProducts(): Result<List<ProductWithStoreInfo>> = try {
    val result = client
      .from("products_with_store_info")
      .select()
      .decodeList<ProductWithStoreInfo>()

    Result.success(result)
  } catch (e: Exception) {
    Result.failure(e)
  }

  suspend fun getProductById(productId: Int): Result<ProductWithStoreInfo?> = try {
    val product = client
      .from("products_with_store_info")
      .select { filter { eq("id", productId) } } // Make sure value is String
      .decodeSingleOrNull<ProductWithStoreInfo>() // Or decodeList and take firstOrNull
    Result.success(product)
  } catch (e: Exception) {
    Result.failure(e)
  }
}
