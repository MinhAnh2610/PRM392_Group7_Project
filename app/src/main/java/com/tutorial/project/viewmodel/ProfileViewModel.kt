package com.tutorial.project.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.model.Profile
import com.tutorial.project.data.repository.ProfileRepository
import kotlinx.coroutines.launch

class ProfileViewModel(private val profileRepository: ProfileRepository) : ViewModel() {
  private val _profile = MutableLiveData<Profile?>()
  val profile: LiveData<Profile?> = _profile

  private val _error = MutableLiveData<String?>()
  val error: LiveData<String?> = _error

  private val _isLoading = MutableLiveData<Boolean>(false)
  val isLoading: LiveData<Boolean> = _isLoading

  fun fetchProfile() {
    viewModelScope.launch {
      _isLoading.value = true
      _error.value = null
      profileRepository.getCurrentUserProfile().fold(
        onSuccess = { _profile.value = it },
        onFailure = { _error.value = it.message }
      )
      _isLoading.value = false
    }
  }

  // Add methods for updating profile, tied to UI fields
}