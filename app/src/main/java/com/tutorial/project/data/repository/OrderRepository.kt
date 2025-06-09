package com.tutorial.project.data.repository

import com.tutorial.project.data.model.CartItemWithProductDetails
import com.tutorial.project.data.model.Order
import com.tutorial.project.data.model.OrderItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class OrderRepository (private val client: SupabaseClient, private val authRepository: AuthRepository) {

  // This operation should ideally be a transaction (e.g., in a Supabase Edge Function)
  // to ensure atomicity. Doing it client-side means handling partial failures.
  suspend fun createOrder(cartItems: List<CartItemWithProductDetails>, totalAmount: Double): Result<Order> {
    val userId = authRepository.getCurrentUserId()
      ?: return Result.failure(Exception("User not logged in"))

    return try {
      // 1. Create the Order
      val orderToInsert = Order(user_id = userId, total_amount = totalAmount, status = "pending")
      // Use returning = "representation" to get the inserted order back with its ID
      val createdOrderList = client.from("orders")
        .insert(orderToInsert) // Ensure correct enum
        .decodeList<Order>()

      if (createdOrderList.isEmpty()) {
        throw Exception("Failed to create order or get its representation back.")
      }
      val createdOrder = createdOrderList.first()


      // 2. Create OrderItems
      val orderItemsToInsert = cartItems.map { cartItem ->
        OrderItem(
          order_id = createdOrder.id!!, // Use the ID from the created order
          product_id = cartItem.product_id,
          quantity = cartItem.quantity,
          price = cartItem.product_price // Price at the time of purchase
        )
      }

      if (orderItemsToInsert.isNotEmpty()) {
        client.from("order_items").insert(orderItemsToInsert)
        // Add error checking for order_items insert if needed
      }

      // 3. Optionally, update product stock quantities (complex, better in backend/function)
      // for (item in cartItems) {
      //     client.postgrest.from("products")
      //         .update(mapOf("stock_quantity" to item.productStockQuantity - item.quantity))
      //         { filter("id", "eq", item.productId.toString()) }
      //         .execute()
      // }

      Result.success(createdOrder)
    } catch (e: Exception) {
      // Consider logic to handle partial success (e.g., order created but items failed)
      Result.failure(e)
    }
  }
}