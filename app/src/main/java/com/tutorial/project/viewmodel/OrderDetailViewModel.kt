// app/src/main/java/com/tutorial/project/viewmodel/OrderDetailViewModel.kt
package com.tutorial.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.model.OrderItemsWithProductDetails
import com.tutorial.project.data.repository.OrderRepository
import com.tutorial.project.data.dto.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderDetailViewModel(
  private val orderRepository: OrderRepository,
  private val orderId: Int
) : ViewModel() {
  private val _orderDetailState =
    MutableStateFlow<UiState<List<OrderItemsWithProductDetails>>>(UiState.Loading)
  val orderDetailState = _orderDetailState.asStateFlow()

  init {
    fetchOrderDetails()
  }

  fun fetchOrderDetails() {
    viewModelScope.launch {
      _orderDetailState.value = UiState.Loading
      orderRepository.getOrderDetails(orderId).fold(
        onSuccess = { _orderDetailState.value = UiState.Success(it) },
        onFailure = { _orderDetailState.value = UiState.Error(it.message ?: "Unknown error") }
      )
    }
  }
}