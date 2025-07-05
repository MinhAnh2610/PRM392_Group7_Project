// app/src/main/java/com/tutorial/project/viewmodel/BillingViewModel.kt
package com.tutorial.project.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.PaymentSheet
import com.tutorial.project.data.model.CartItemWithProductDetails
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.OrderRepository
import com.tutorial.project.data.repository.StripeRepository
import kotlinx.coroutines.launch

sealed class PaymentStatus {
  data object Idle : PaymentStatus()
  data object Processing : PaymentStatus()
  data class ReadyToPay(val customerConfig: PaymentSheet.CustomerConfiguration, val clientSecret: String) : PaymentStatus()
  data class Success(val message: String) : PaymentStatus()
  data class Error(val message: String) : PaymentStatus()
}

class BillingViewModel(
  private val orderRepository: OrderRepository,
  private val cartRepository: CartRepository,
  private val stripeRepository: StripeRepository
) : ViewModel() {

  private val _paymentStatus = MutableLiveData<PaymentStatus>(PaymentStatus.Idle)
  val paymentStatus: LiveData<PaymentStatus> = _paymentStatus

  private val _cartItems = MutableLiveData<List<CartItemWithProductDetails>>(emptyList())
  val cartItems: LiveData<List<CartItemWithProductDetails>> = _cartItems

  private val _totalPrice = MutableLiveData(0.0)
  val totalPrice: LiveData<Double> = _totalPrice

  private val _clientSecret = MutableLiveData<String?>(null)
  val clientSecret: LiveData<String?> = _clientSecret

  init {
    loadCartAndPreparePayment()
  }

  private fun loadCartAndPreparePayment() {
    viewModelScope.launch {
      _paymentStatus.value = PaymentStatus.Processing
      cartRepository.getCartItemsWithDetails().fold(
        onSuccess = { items ->
          if (items.isEmpty()) {
            _paymentStatus.value = PaymentStatus.Error("Your cart is empty.")
            return@launch
          }
          _cartItems.value = items
          _totalPrice.value = items.sumOf { it.product_price * it.quantity }
          preparePaymentSheet(items)
        },
        onFailure = {
          _paymentStatus.value = PaymentStatus.Error(it.message ?: "Failed to load cart.")
        }
      )
    }
  }

  private fun preparePaymentSheet(items: List<CartItemWithProductDetails>) {
    viewModelScope.launch {
      stripeRepository.getPaymentSheetDetails(items).fold(
        onSuccess = { response ->
          val customerConfig = PaymentSheet.CustomerConfiguration(
            id = response.customer,
            ephemeralKeySecret = response.ephemeralKey
          )
          _paymentStatus.value = PaymentStatus.ReadyToPay(customerConfig, response.paymentIntent)
        },
        onFailure = { error ->
          _paymentStatus.value = PaymentStatus.Error(error.message ?: "Could not initiate payment.")
        }
      )
    }
  }

  fun onPaymentSuccess() {
    viewModelScope.launch {
      val items = _cartItems.value
      if (items.isNullOrEmpty()) {
        _paymentStatus.value = PaymentStatus.Error("Cannot create order, cart is empty.")
        return@launch
      }

      orderRepository.createOrderAndProcessStock(items).fold(
        onSuccess = {
          _paymentStatus.value = PaymentStatus.Success("Payment successful! Order created.")
        },
        onFailure = {
          // The payment succeeded, but order creation failed. This is a critical state.
          // In a real app, you would log this for manual intervention.
          _paymentStatus.value = PaymentStatus.Error("Payment was successful, but failed to create your order. Please contact support.")
        }
      )
    }
  }

  fun onPaymentError(message: String) {
    _paymentStatus.value = PaymentStatus.Error(message)
  }
}