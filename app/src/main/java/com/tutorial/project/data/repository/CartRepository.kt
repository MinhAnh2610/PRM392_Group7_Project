// app/src/main/java/com/tutorial/project/data/repository/CartRepository.kt
package com.tutorial.project.data.repository

import com.tutorial.project.data.model.CartItem
import com.tutorial.project.data.model.CartItemWithProductDetails
import com.tutorial.project.data.model.Product
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count

class CartRepository(
  private val client: SupabaseClient,
  private val authRepository: AuthRepository
) {

  // IMPORTANT: For getCartItemsWithDetails, create a VIEW in Supabase for easier joins:
  // CREATE OR REPLACE VIEW cart_items_detailed AS
  // SELECT
  //     ci.id AS cart_item_id,
  //     ci.user_id,
  //     ci.product_id,
  //     ci.quantity,
  //     p.name AS product_name,
  //     p.price AS product_price,
  //     p.image_url AS product_image_url
  // FROM cart_items ci
  // JOIN products p ON ci.product_id = p.id;
  suspend fun getCartItemsWithDetails(): Result<List<CartItemWithProductDetails>> {
    val userId = authRepository.getCurrentUserId()
      ?: return Result.failure(Exception("User not logged in"))
    return try {
      val items = client.from("cart_items_detailed") // Use the view
        .select { filter { eq("user_id", userId) } }
        .decodeList<CartItemWithProductDetails>()
      Result.success(items)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun fetchCartItems(): Result<List<CartItem>> = try {
    val result = client
      .from("cart_items")
      .select()
      .decodeList<CartItem>()

    Result.success(result)
  } catch (e: Exception) {
    Result.failure(e)
  }

  suspend fun addOrUpdateCartItem(productId: Int, quantityToAdd: Int): Result<Unit> {
    val userId = authRepository.getCurrentUserId()
      ?: return Result.failure(Exception("User not logged in"))

    if (quantityToAdd <= 0) return Result.failure(Exception("Quantity must be positive"))

    return try {
      // 1. Check if the item already exists in the cart for the current user.
      val existingItem = client.from("cart_items")
        .select {
          filter {
            eq("user_id", userId)
            eq("product_id", productId)
          }
        }
        .decodeSingleOrNull<CartItem>()

      if (existingItem != null) {
        // 2. If it exists, calculate the new quantity and UPDATE the existing item.
        val newQuantity = existingItem.quantity + quantityToAdd
        client.from("cart_items")
          .update(mapOf("quantity" to newQuantity)) {
            filter { eq("id", existingItem.id!!) } // Update using the primary key
          }
      } else {
        // 3. If it does not exist, INSERT a new cart item.
        val newCartItem = CartItem(
          user_id = userId,
          product_id = productId,
          quantity = quantityToAdd
        )
        client.from("cart_items").insert(newCartItem)
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun removeCartItem(cartItemId: Int): Result<Unit> { // cartItemId is the PK of cart_items
    val userId = authRepository.getCurrentUserId() // Good for an RLS policy check
      ?: return Result.failure(Exception("User not logged in"))
    return try {
      client.from("cart_items")
        .delete {
          filter { eq("id", cartItemId) }
          filter { eq("user_id", userId) } // RLS also enforces this
        }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateCartItemQuantity(cartItemId: Int, newQuantity: Int): Result<Unit> {
    val userId = authRepository.getCurrentUserId()
      ?: return Result.failure(Exception("User not logged in"))
    if (newQuantity <= 0) return removeCartItem(cartItemId) // Or handle as error

    return try {
      client.from("cart_items")
        .update(mapOf("quantity" to newQuantity)) {
          filter { eq("id", cartItemId) }
          filter { eq("user_id", userId) } // Ensure user can only update their own items
        }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }


  suspend fun clearCart(): Result<Unit> {
    val userId = authRepository.getCurrentUserId()
      ?: return Result.failure(Exception("User not logged in"))
    return try {
      client.from("cart_items")
        .delete { filter { eq("user_id", userId) } }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getCartItemCount(): Result<Long> {
    val userId = authRepository.getCurrentUserId() ?: return Result.success(0L)
    return try {
      val response = client.from("cart_items").select {
        count(Count.EXACT)
        filter { eq("user_id", userId) }
      }
      Result.success(response.countOrNull() ?: 0L)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}