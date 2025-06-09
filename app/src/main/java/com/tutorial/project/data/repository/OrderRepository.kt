// app/src/main/java/com/tutorial/project/data/repository/OrderRepository.kt
package com.tutorial.project.data.repository

import com.tutorial.project.data.model.CartItemWithProductDetails
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OrderRepository(
  private val client: SupabaseClient,
  private val authRepository: AuthRepository
) {

  /**
   * Calls a Supabase RPC function to create an order, which handles stock checking,
   * order creation, order item creation, stock deduction, and cart clearing in a single transaction.
   */
  suspend fun createOrderAndProcessStock(cartItems: List<CartItemWithProductDetails>): Result<String> {
    if (authRepository.getCurrentUserId() == null) {
      return Result.failure(Exception("User not logged in"))
    }

    return try {
      // Construct the JSONB payload required by the PostgreSQL function
      val cartItemsJson = buildJsonArray {
        cartItems.forEach { item ->
          add(buildJsonObject {
            put("product_id", item.product_id)
            put("quantity", item.quantity)
            put("price", item.product_price) // Price at the time of purchase
          })
        }
      }

      // Call the RPC function
       client.postgrest.rpc(
        "create_order_and_deduct_stock",
        buildJsonObject {
          put("p_cart_items", cartItemsJson)
        }
      )

      // The function now also clears the cart, so we don't need to do it here.
      Result.success("Successfully created an order")

    } catch (e: Exception) {
      // The exception message from the database (e.g., "Not enough stock...") will be propagated here.
      Result.failure(e)
    }
  }
}