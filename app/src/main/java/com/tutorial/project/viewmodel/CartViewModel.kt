package com.tutorial.project.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.model.CartItemWithProductDetails
import com.tutorial.project.data.repository.CartRepository
import kotlinx.coroutines.launch

class CartViewModel(private val cartRepository: CartRepository) : ViewModel() {

  private val _cartItems = MutableLiveData<List<CartItemWithProductDetails>>(emptyList())
  val cartItems: LiveData<List<CartItemWithProductDetails>> = _cartItems

  private val _totalPrice = MutableLiveData<Double>(0.0)
  val totalPrice: LiveData<Double> = _totalPrice

  private val _isLoading = MutableLiveData<Boolean>(false)
  val isLoading: LiveData<Boolean> = _isLoading

  private val _error = MutableLiveData<String?>()
  val error: LiveData<String?> = _error

  fun loadCartItems() {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null
      val result = cartRepository.getCartItemsWithDetails().fold(
        onSuccess = { items ->
          _cartItems.value = items
          calculateTotal(items)
        },
        onFailure = { _error.value = it.message }
      )
      _isLoading.value = false
      Log.e("SUPABASE.cartItems", result.toString())
    }
  }

  private fun calculateTotal(items: List<CartItemWithProductDetails>) {
    _totalPrice.value = items.sumOf { it.product_price * it.quantity }
  }

  fun updateQuantity(cartItemId: Int, newQuantity: Int) {
    viewModelScope.launch {
      // Optimistically update UI or wait for success
      cartRepository.updateCartItemQuantity(cartItemId, newQuantity).fold(
        onSuccess = { loadCartItems() }, // Reload cart to reflect changes
        onFailure = { _error.value = "Failed to update quantity: ${it.message}" }
      )
    }
  }

  fun removeItem(cartItemId: Int) {
    viewModelScope.launch {
      cartRepository.removeCartItem(cartItemId).fold(
        onSuccess = { loadCartItems() }, // Reload cart
        onFailure = { _error.value = "Failed to remove item: ${it.message}" }
      )
    }
  }

  fun clearCart() {
    viewModelScope.launch {
      _isLoading.value = true
      cartRepository.clearCart().fold(
        onSuccess = {
          _cartItems.value = emptyList()
          _totalPrice.value = 0.0
        },
        onFailure = { _error.value = it.message }
      )
      _isLoading.value = false
    }
  }
}