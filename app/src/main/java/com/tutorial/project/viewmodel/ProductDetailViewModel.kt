/* app/src/main/java/com/tutorial/project/viewmodel/ProductDetailViewModel.kt */
package com.tutorial.project.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.dto.ToastEvent
import com.tutorial.project.data.model.ProductWithStoreInfo
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductDetailViewModel(
  private val productRepository: ProductRepository,
  private val cartRepository: CartRepository,
  private val authRepository: AuthRepository,
  private val productId: Int
) : ViewModel() {

  private val _product = MutableLiveData<ProductWithStoreInfo?>()
  val product: LiveData<ProductWithStoreInfo?> = _product

  private val _error = MutableLiveData<String?>()
  val error: LiveData<String?> = _error

  private val _isLoading = MutableLiveData(false)
  val isLoading: LiveData<Boolean> = _isLoading

  private val _toastEvent = MutableLiveData<ToastEvent?>()
  val toastEvent: LiveData<ToastEvent?> = _toastEvent

  private val _navigateToChat = MutableLiveData<Pair<String, String>?>(null)
  val navigateToChat: LiveData<Pair<String, String>?> = _navigateToChat

  init {
    loadProductDetails()
  }

  fun isUserLoggedIn(): Boolean = authRepository.isLoggedIn()

  private fun loadProductDetails() {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null
      productRepository.getProductById(productId).fold(
        onSuccess = { _product.value = it },
        onFailure = { _error.value = it.message }
      )
      _isLoading.value = false
    }
  }

  fun addToCart(quantity: Int) {
    val currentProduct = _product.value
    if (currentProduct == null) {
      _toastEvent.value = ToastEvent("Product not loaded yet.")
      return
    }
    if (quantity <= 0) {
      _toastEvent.value = ToastEvent("Please select a valid quantity.")
      return
    }
    if (quantity > currentProduct.stock_quantity) {
      _toastEvent.value = ToastEvent("Not enough stock available.")
      return
    }

    viewModelScope.launch {
      // For simplicity, we assume the user adds the full quantity.
      // A real app might check existing quantity in cart and add to it.
      cartRepository.addOrUpdateCartItem(currentProduct.id, quantity).fold(
        onSuccess = {
          _toastEvent.value = ToastEvent("Added to cart successfully!")
        },
        onFailure = {
          _toastEvent.value = ToastEvent("Error adding to cart: ${it.message}")
        }
      )
    }
  }

  fun onChatIconClick() {
    val productInfo = _product.value ?: return // Exit if product isn't loaded

    // Trigger navigation with owner ID and store name
    _navigateToChat.value = productInfo.store_owner_id to productInfo.store_name
  }

  fun onChatNavigated() {
    _navigateToChat.value = null
  }

  fun onToastShown() {
    _toastEvent.value = null
  }
}