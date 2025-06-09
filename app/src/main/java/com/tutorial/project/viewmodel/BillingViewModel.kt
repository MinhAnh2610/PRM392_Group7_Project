// app/src/main/java/com/tutorial/project/viewmodel/BillingViewModel.kt
package com.tutorial.project.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.model.CartItemWithProductDetails
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.OrderRepository
import kotlinx.coroutines.launch

class BillingViewModel(
  private val orderRepository: OrderRepository,
  private val cartRepository: CartRepository
) : ViewModel() {

  private val _orderProcessState = MutableLiveData<OrderProcessState>(OrderProcessState.Idle)
  val orderProcessState: LiveData<OrderProcessState> = _orderProcessState

  private val _cartItems = MutableLiveData<List<CartItemWithProductDetails>>(emptyList())
  val cartItems: LiveData<List<CartItemWithProductDetails>> = _cartItems

  private val _totalPrice = MutableLiveData(0.0)
  val totalPrice: LiveData<Double> = _totalPrice

  init {
    loadOrderSummary()
  }

  private fun loadOrderSummary() {
    viewModelScope.launch {
      _orderProcessState.value = OrderProcessState.Processing
      cartRepository.getCartItemsWithDetails().fold(
        onSuccess = { items ->
          _cartItems.value = items
          _totalPrice.value = items.sumOf { it.product_price * it.quantity }
          _orderProcessState.value = OrderProcessState.Idle // Ready to proceed
        },
        onFailure = {
          _orderProcessState.value = OrderProcessState.Error(it.message ?: "Failed to load cart.")
        }
      )
    }
  }

  fun placeOrder() {
    val itemsToOrder = _cartItems.value
    val totalAmount = _totalPrice.value

    if (itemsToOrder.isNullOrEmpty() || totalAmount == null || totalAmount <= 0) {
      _orderProcessState.value = OrderProcessState.Error("Cannot place an empty order.")
      return
    }

    viewModelScope.launch {
      _orderProcessState.value = OrderProcessState.Processing

      // Call the new transactional function in the repository
      orderRepository.createOrderAndProcessStock(itemsToOrder).fold(
        onSuccess = { result ->
          // The RPC function now handles cart clearing, so we just report success.
          _orderProcessState.value = OrderProcessState.Success(result)
        },
        onFailure = { orderError ->
          // This will now catch stock errors like "Not enough stock for product ID 5"
          val errorMessage = orderError.message ?: "Order creation failed"
          Log.e("SUPABASE.orders", errorMessage)
          _orderProcessState.value = OrderProcessState.Error(errorMessage)
        }
      )
    }
  }

  // Call this to reset the state after navigating away from the screen
  fun onOrderProcessed() {
    _orderProcessState.value = OrderProcessState.Idle
  }
}

sealed class OrderProcessState {
  data object Processing : OrderProcessState()
  data class Success(val message: String) : OrderProcessState()
  data class Error(val message: String) : OrderProcessState()
  data object Idle : OrderProcessState()
}