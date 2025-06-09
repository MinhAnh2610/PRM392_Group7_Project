package com.tutorial.project.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.model.CartItemWithProductDetails
import com.tutorial.project.data.model.Order
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.OrderRepository
import kotlinx.coroutines.launch

class BillingViewModel(
  private val orderRepository: OrderRepository,
  private val cartRepository: CartRepository // Inject to clear cart
) : ViewModel() {

  private val _orderResult = MutableLiveData<OrderProcessState>()
  val orderResult: LiveData<OrderProcessState> = _orderResult

  fun processOrder(cartItems: List<CartItemWithProductDetails>, totalAmount: Double) {
    if (cartItems.isEmpty()) {
      _orderResult.value = OrderProcessState.Error("Cart is empty.")
      return
    }

    viewModelScope.launch {
      _orderResult.value = OrderProcessState.Processing
      // MOCK PAYMENT STEP
      // In a real app, integrate with a payment gateway here.
      // For this project, we assume payment is successful.

      orderRepository.createOrder(cartItems, totalAmount).fold(
        onSuccess = { createdOrder ->
          _orderResult.value = OrderProcessState.Success(createdOrder)
          // Clear the cart after successful order
          cartRepository.clearCart().onFailure {
            // Log or handle error for cart clearing, but order is still successful
            println("Failed to clear cart after order: ${it.message}")
          }
        },
        onFailure = {
          _orderResult.value = OrderProcessState.Error(it.message ?: "Order creation failed")
        }
      )
    }
  }
}

sealed class OrderProcessState {
  object Processing : OrderProcessState()
  data class Success(val order: Order) : OrderProcessState()
  data class Error(val message: String) : OrderProcessState()
  object Idle : OrderProcessState() // Initial state
}