// app/src/main/java/com/tutorial/project/viewmodel/ConversationsViewModel.kt
package com.tutorial.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.dto.UiState
import com.tutorial.project.data.model.Conversation
import com.tutorial.project.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationsViewModel(private val chatRepository: ChatRepository) : ViewModel() {

  private val _conversationsState = MutableStateFlow<UiState<List<Conversation>>>(UiState.Loading)
  val conversationsState = _conversationsState.asStateFlow()

  init {
    loadConversations()
  }

  fun loadConversations() {
    viewModelScope.launch {
      _conversationsState.value = UiState.Loading
      chatRepository.getConversations()
        .onSuccess { conversations ->
          _conversationsState.value = UiState.Success(conversations)
        }
        .onFailure { error ->
          _conversationsState.value = UiState.Error(error.message ?: "Failed to load conversations")
        }
    }
  }
}