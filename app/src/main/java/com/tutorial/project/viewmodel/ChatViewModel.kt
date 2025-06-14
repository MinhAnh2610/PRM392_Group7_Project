// app/src/main/java/com/tutorial/project/viewmodel/ChatViewModel.kt
package com.tutorial.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.dto.UiState
import com.tutorial.project.data.model.Message
import com.tutorial.project.data.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel(
  private val chatRepository: ChatRepository,
  private val supabaseClient: SupabaseClient,
  private val storeOwnerId: String // Added
) : ViewModel() {
  private val _messagesState = MutableStateFlow<UiState<List<Message>>>(UiState.Loading)
  val messagesState: StateFlow<UiState<List<Message>>> = _messagesState.asStateFlow()

  private var realtimeListenerJob: Job? = null
  // 1. Create the channel immediately
  private val chatChannel: RealtimeChannel = supabaseClient.channel("realtime:public:messages")

  init {

    // 2. Load the initial message history
    loadMessageHistory()

    // 3. Start listening for real-time updates
    startListeningForNewMessages()
  }

  private fun loadMessageHistory() {
    viewModelScope.launch {
      _messagesState.value = UiState.Loading
      // Pass storeOwnerId to the repository
      chatRepository.getMessageHistory(storeOwnerId)
        .onSuccess { messages ->
          _messagesState.value = UiState.Success(messages)
        }
        .onFailure { error ->
          _messagesState.value = UiState.Error(error.message ?: "Failed to load messages")
        }
    }
  }

  fun sendMessage(content: String) {
    if (content.isBlank()) return

    // --- Optimistic Update ---
    val currentState = _messagesState.value
    if (currentState is UiState.Success) {
      val optimisticMessage = Message(
        id = null, // ID is null because it's not from the database yet
        sender_id = supabaseClient.auth.currentUserOrNull()?.id ?: "",
        receiver_id = storeOwnerId,
        content = content,
        created_at = null // Timestamp will be set by the database
      )
      // Immediately add the temporary message to the UI
      _messagesState.value = UiState.Success(currentState.data + optimisticMessage)
    }
    // --- End Optimistic Update ---


    viewModelScope.launch {
      // Send the message to the database in the background
      chatRepository.sendMessage(content, storeOwnerId)
        .onFailure { error ->
          // Optional: Handle send failure (e.g., remove the optimistic message and show an error)
          println("Error sending message: ${error.message}")
        }
    }
  }

  private fun startListeningForNewMessages() {
    realtimeListenerJob?.cancel()

    realtimeListenerJob = viewModelScope.launch {
      chatRepository.getNewMessagesFlow(chatChannel, storeOwnerId)
        .catch { e -> _messagesState.value = UiState.Error("Realtime error: ${e.message}") }
        .collect { newMessage ->
          val currentUiState = _messagesState.value
          if (currentUiState is UiState.Success) {
            val currentMessages = currentUiState.data.toMutableList()
            // Check if we have an optimistic version of this message (which has a null id)
            val optimisticMessageIndex = currentMessages.indexOfFirst {
              it.id == null && it.content == newMessage.content
            }

            if (optimisticMessageIndex != -1) {
              // If we find the optimistic message, replace it with the real one from the DB
              currentMessages[optimisticMessageIndex] = newMessage
            } else {
              // Otherwise, this is a new message from the other user, so just add it
              currentMessages.add(newMessage)
            }
            _messagesState.value = UiState.Success(currentMessages)
          }
        }
    }

    viewModelScope.launch {
      chatChannel.subscribe()
    }
  }

  override fun onCleared() {
    super.onCleared()
    realtimeListenerJob?.cancel()
    viewModelScope.launch {
      chatChannel.unsubscribe()
    }
  }
}