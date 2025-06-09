package com.tutorial.project.viewmodel

import androidx.compose.runtime.Recomposer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorial.project.data.model.Message
import com.tutorial.project.data.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
  private val chatRepository: ChatRepository,
  private val supabaseClient: SupabaseClient // For channel management
) : ViewModel() {
  /*
    private val _messages = MutableStateFlow<UiState<List<Message>>>(Recomposer.State.Idle)
    val messages: StateFlow<UiState<List<Message>>> = _messages

    private val _sendMessageState = MutableStateFlow<UiState<Message?>>(Recomposer.State.Idle)
    val sendMessageState: StateFlow<UiState<Message?>> = _sendMessageState

    private var realtimeListenerJob: Job? = null
    private var chatChannelInstance: RealtimeChannel? = null

      init {
        loadMessageHistory()
        startListeningForNewMessages()
      }



      fun loadMessageHistory() {
        viewModelScope.launch {
          _messages.value = UiState.
          chatRepository.getMessageHistory()
            .onSuccess {
              _messages.value = UiState.Success(it)
            }
            .onFailure {
              _messages.value = UiState.Error(it.message ?: "Failed to load messages")
            }
        }
      }

      fun sendMessage(content: String) {
        viewModelScope.launch {
          _sendMessageState.value = UiState.Loading
          chatRepository.sendMessage(content)
            .onSuccess {
              _sendMessageState.value = UiState.Success(it)
              // New message will arrive via realtime listener, or you can manually add it here
              // For simplicity, relying on realtime for updates to the list.
              // If not using realtime for own messages, add 'it' to the _messages list.
            }
            .onFailure {
              _sendMessageState.value = UiState.Error(it.message ?: "Failed to send message")
            }
        }
      }

      private fun startListeningForNewMessages() {
        realtimeListenerJob?.cancel() // Cancel previous job if any
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return

        // Construct channel name as used in repository or a general one if RLS handles filtering
        chatChannelInstance =
          supabaseClient.realtime.channel("chat_messages_for_user_${userId}_with_store")


        realtimeListenerJob = viewModelScope.launch {
          try {
            // Subscribe before collecting
            if (chatChannelInstance?.status?.value != RealtimeChannel.Status.SUBSCRIBED) {
              chatChannelInstance?.subscribe()?.join() // Wait for subscription
            }

            chatRepository.listenForNewMessages().collect { newMessage ->
              val currentMessages = (_messages.value as? UiState.Success)?.data ?: emptyList()
              _messages.value = UiState.Success(currentMessages + newMessage)
            }
          } catch (e: Exception) {
            _messages.value = UiState.Error("Realtime connection error: ${e.message}")
          }
        }
      }



  override fun onCleared() {
    super.onCleared()
    realtimeListenerJob?.cancel()
    viewModelScope.launch { // Unsubscribe in a coroutine
      chatChannelInstance?.unsubscribe()
    }
  }
   */
}