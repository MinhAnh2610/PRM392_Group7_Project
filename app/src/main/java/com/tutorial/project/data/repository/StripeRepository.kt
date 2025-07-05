// app/src/main/java/com/tutorial/project/data/repository/StripeRepository.kt
package com.tutorial.project.data.repository

import com.tutorial.project.data.model.CartItemWithProductDetails
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.Serializable

@Serializable
data class PaymentSheetRequest(val cartItems: List<CartItemWithProductDetails>, val userId: String)

@Serializable
data class PaymentSheetResponse(
  val paymentIntent: String,
  val ephemeralKey: String,
  val customer: String
)

class StripeRepository(private val client: SupabaseClient) {
  suspend fun getPaymentSheetDetails(items: List<CartItemWithProductDetails>): Result<PaymentSheetResponse> {
    return try {
      val userId = client.auth.currentUserOrNull()?.id
        ?: return Result.failure(Exception("User not logged in"))

      // The functions client automatically includes the Authorization header if the user is logged in.
      val response = client.functions.invoke(
        function = "create-payment-intent",
        body = PaymentSheetRequest(cartItems = items, userId = userId)
      )

      val decodedResponse = response.body<PaymentSheetResponse>()

      Result.success(decodedResponse)
    } catch (e: Exception) {
      println("StripeRepository Error: ${e.message}")
      Result.failure(e)
    }
  }
}