package com.tutorial.project.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.model.StoreLocation
import com.tutorial.project.data.repository.StoreLocationRepository
import kotlinx.coroutines.launch

class MapViewModel(private val storeLocationRepository: StoreLocationRepository) : ViewModel() {

  private val _locations = MutableLiveData<List<StoreLocation>>()
  val locations: LiveData<List<StoreLocation>> = _locations

  private val _isLoading = MutableLiveData<Boolean>(false)
  val isLoading: LiveData<Boolean> = _isLoading

  private val _error = MutableLiveData<String?>()
  val error: LiveData<String?> = _error

  init {
    fetchLocations()
  }

  fun fetchLocations() {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null
      storeLocationRepository.getStoreLocations().fold(
        onSuccess = { _locations.value = it },
        onFailure = { _error.value = it.message }
      )
      _isLoading.value = false
    }
  }
}