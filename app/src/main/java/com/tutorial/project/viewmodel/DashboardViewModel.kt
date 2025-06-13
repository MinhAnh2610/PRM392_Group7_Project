package com.tutorial.project.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.dto.ToastEvent
import com.tutorial.project.data.model.CartItemWithProductDetails
import com.tutorial.project.data.model.ProductWithStoreInfo
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.ProductRepository
import kotlinx.coroutines.launch

class DashboardViewModel(
  private val productRepository: ProductRepository,
  private val cartRepository: CartRepository
) : ViewModel() {
  // --- LiveData for UI State ---
  private val _products = MutableLiveData<List<ProductWithStoreInfo>>()
  val products: LiveData<List<ProductWithStoreInfo>> = _products

  private val _cartItems = MutableLiveData<List<CartItemWithProductDetails>>()
  val cartItems: LiveData<List<CartItemWithProductDetails>> = _cartItems

  private val _error = MutableLiveData<String?>()
  val error: LiveData<String?> = _error

  // Kept from 'Login-out-signup' for loading indicators
  private val _isLoading = MutableLiveData<Boolean>()
  val isLoading: LiveData<Boolean> = _isLoading

  // Kept from 'main' for user feedback
  private val _toastEvent = MutableLiveData<ToastEvent?>()
  val toastEvent: LiveData<ToastEvent?> = _toastEvent

  // Kept from 'main' for triggering local notifications
  private val _notificationRequest = MutableLiveData<Int?>()
  val notificationRequest: LiveData<Int?> = _notificationRequest

  // --- Data Loading ---
  init {
    loadProducts()
    loadCartItems()
  }

  fun loadProducts() {
    viewModelScope.launch {
      _isLoading.value = true
      // Correctly uses 'productRepository' and manages loading state
      val result = productRepository.fetchProducts()
      Log.d("DashboardViewModel", "Products Result: $result")
      result.onSuccess {
        _products.value = it
        _error.value = null
      }.onFailure {
        _error.value = it.message
      }
      _isLoading.value = false
    }
  }

  fun loadCartItems() {
    viewModelScope.launch {
      val result = cartRepository.getCartItemsWithDetails()
      Log.d("DashboardViewModel", "Cart Items Result: $result")
      result.onSuccess {
        _cartItems.value = it
      }.onFailure {
        _error.value = it.message
      }
    }
  }

  // --- UI Actions ---
  fun addToCart(product: ProductWithStoreInfo, quantity: Int) {
    // Use camelCase 'stockQuantity' from the merged Product model
    if (quantity > product.stock_quantity) {
      _toastEvent.value = ToastEvent("Not enough stock available.")
      return
    }

    viewModelScope.launch {
      cartRepository.addOrUpdateCartItem(product.id, quantity).fold(
        onSuccess = {
          _toastEvent.value = ToastEvent("Added to cart successfully!")
          // Refresh cart items to update the badge
          loadCartItems()
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

  // --- Event Consumption ---
  /**
   * Call this function after the toast has been shown to prevent it from
   * re-appearing on configuration changes.
   */
  fun onToastShown() {
    _toastEvent.value = null
  }

  /**
   * Call this after the notification is shown to consume the event.
   */
  fun onNotificationShown() {
    _notificationRequest.value = null
  }
}