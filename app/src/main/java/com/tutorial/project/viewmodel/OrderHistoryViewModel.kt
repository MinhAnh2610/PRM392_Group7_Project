// app/src/main/java/com/tutorial/project/viewmodel/OrderHistoryViewModel.kt
package com.tutorial.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.model.Order
import com.tutorial.project.data.repository.OrderRepository
import com.tutorial.project.data.dto.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderHistoryViewModel(private val orderRepository: OrderRepository) : ViewModel() {
  private val _ordersState = MutableStateFlow<UiState<List<Order>>>(UiState.Loading)
  val ordersState = _ordersState.asStateFlow()

  init {
    fetchOrderHistory()
  }

  fun fetchOrderHistory() {
    viewModelScope.launch {
      _ordersState.value = UiState.Loading
      orderRepository.fetchUserOrders().fold(
        onSuccess = { _ordersState.value = UiState.Success(it) },
        onFailure = { _ordersState.value = UiState.Error(it.message ?: "Unknown error") }
      )
    }
  }
}