package com.tutorial.project.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.model.Product
import com.tutorial.project.data.repository.ProductRepository
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: ProductRepository) : ViewModel() {
  private val _products = MutableLiveData<List<Product>>()
  val products: LiveData<List<Product>> = _products

  private val _error = MutableLiveData<String?>()
  val error: LiveData<String?> = _error

  private val _isLoading = MutableLiveData<Boolean>()
  val isLoading: LiveData<Boolean> = _isLoading

  init {
    loadProducts()
  }

  fun loadProducts() {
    viewModelScope.launch {
      _isLoading.value = true
      val result = repository.fetchProducts()
      Log.e("SUPABASE.products", result.toString())
      result.onSuccess {
        _products.value = it
        _error.value = null
      }.onFailure {
        _error.value = it.message
      }
      _isLoading.value = false
    }
  }
}
