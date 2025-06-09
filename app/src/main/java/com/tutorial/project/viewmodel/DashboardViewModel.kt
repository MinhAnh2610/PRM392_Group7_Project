package com.tutorial.project.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.dto.ToastEvent
import com.tutorial.project.data.model.CartItem
import com.tutorial.project.data.model.Product
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.ProductRepository
import kotlinx.coroutines.launch

class DashboardViewModel(
  private val productRepository: ProductRepository,
  private val cartRepository: CartRepository
) : ViewModel() {
  private val _products = MutableLiveData<List<Product>>()
  val products: LiveData<List<Product>> = _products

  private val _cartItems = MutableLiveData<List<CartItem>>()
  val cartItems: LiveData<List<CartItem>> = _cartItems

  private val _error = MutableLiveData<String?>()
  val error: LiveData<String?> = _error

  private val _toastEvent = MutableLiveData<ToastEvent?>()
  val toastEvent: LiveData<ToastEvent?> = _toastEvent

  private val _notificationRequest = MutableLiveData<Int?>()
  val notificationRequest: LiveData<Int?> = _notificationRequest

  init {
    loadProducts()
    loadCartItems()
  }

  fun loadCartItems() {
    viewModelScope.launch {
      val result = cartRepository.fetchCartItems()
      Log.e("SUPABASE.cartItems", result.toString())
      result.onSuccess {
        _cartItems.value = it
      }.onFailure {
        _error.value = it.message
      }
    }
  }

  private fun loadProducts() {
    viewModelScope.launch {
      val result = productRepository.fetchProducts()
      Log.e("SUPABASE.products", result.toString())
      result.onSuccess {
        _products.value = it
      }.onFailure {
        _error.value = it.message
      }
    }
  }

  fun addToCart(product: Product, quantity: Int) {
    if (quantity > product.stock_quantity) {
      _toastEvent.value = ToastEvent("Not enough stock available.")
      return
    }

    viewModelScope.launch {
      // For simplicity, we assume the user adds the full quantity.
      // A real app might check existing quantity in cart and add to it.
      cartRepository.addOrUpdateCartItem(product.id, quantity).fold(
        onSuccess = {
          _toastEvent.value = ToastEvent("Added to cart successfully!")
        },
        onFailure = {
          _toastEvent.value = ToastEvent("Error adding to cart: ${it.message}")
        }
      )
    }
  }

  fun checkForNotification() {
    viewModelScope.launch {
      cartRepository.getCartItemCount().onSuccess { count ->
        if (count > 0) {
          _notificationRequest.value = count.toInt()
        }
      }
    }
  }

  // New function to consume the event after notification is shown
  fun onNotificationShown() {
    _notificationRequest.value = null
  }

  /**
   * Call this function after the toast has been shown to prevent it from
   * re-appearing on configuration changes (e.g., screen rotation).
   */
  fun onToastShown() {
    _toastEvent.value = null
  }
}
