// app/src/main/java/com/tutorial/project/ui/chat/ChatScreen.kt
package com.tutorial.project.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.dto.UiState
import com.tutorial.project.data.model.Message
import com.tutorial.project.data.repository.ChatRepository
import com.tutorial.project.viewmodel.ChatViewModel
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
  navController: NavController,
  storeOwnerId: String,
  storeName: String
) {
  val viewModel: ChatViewModel = viewModel(
    key = "chat/$storeOwnerId", // Ensure a unique ViewModel instance per chat
    factory = GenericViewModelFactory {
      ChatViewModel(
        chatRepository = ChatRepository(
          client = SupabaseClientProvider.client,
          auth = SupabaseClientProvider.client.auth,
        ),
        supabaseClient = SupabaseClientProvider.client,
        storeOwnerId = storeOwnerId // Pass the ID to the ViewModel
      )
    }
  )

  val messagesState by viewModel.messagesState.collectAsState()
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Chat with $storeName") }, // Dynamic title
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    bottomBar = {
      MessageInput(onSendMessage = { viewModel.sendMessage(it) })
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      when (val state = messagesState) {
        is UiState.Loading -> {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        is UiState.Error -> {
          Text(
            text = "Error: ${state.message}",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.align(Alignment.Center)
          )
        }
        is UiState.Success -> {
          val messages = state.data
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(messages, key = { it.id ?: it.created_at.toString() }) { message ->
              MessageBubble(
                message = message,
                isFromCurrentUser = message.sender_id == SupabaseClientProvider.client.auth.currentUserOrNull()?.id
              )
            }
          }
          // Scroll to the bottom when new messages arrive
          LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
              coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
              }
            }
          }
        }
      }
    }
  }
}


/*@Composable
fun MessageBubble(message: Message, isFromCurrentUser: Boolean) {
  val alignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
  val backgroundColor = if (isFromCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(
        start = if (isFromCurrentUser) 48.dp else 0.dp,
        end = if (isFromCurrentUser) 0.dp else 48.dp
      ),
    contentAlignment = alignment
  ) {
    Text(
      text = message.content,
      modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .background(backgroundColor)
        .padding(12.dp)
    )
  }
}*/
@Composable
fun MessageBubble(message: Message, isFromCurrentUser: Boolean) {
  val alignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
  val backgroundColor = if (isFromCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
  val textColor = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

  val senderLabel = if (isFromCurrentUser) "You" else "Store"

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(
        start = if (isFromCurrentUser) 48.dp else 0.dp,
        end = if (isFromCurrentUser) 0.dp else 48.dp
      ),
    contentAlignment = alignment
  ) {
    Column(
      modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .background(backgroundColor)
        .padding(12.dp)
    ) {
      Text(
        text = senderLabel,
        style = MaterialTheme.typography.labelSmall,
        color = textColor
      )

      Text(
        text = message.content,
        color = textColor,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
      )

      Text(
        text = formatTimestamp(message.created_at),
        style = MaterialTheme.typography.labelSmall,
        color = textColor.copy(alpha = 0.7f)
      )
    }
  }
}

fun formatTimestamp(timestamp: String?): String {
  return try {
    val dateTime = OffsetDateTime.parse(timestamp)
    dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
  } catch (e: Exception) {
    ""
  }
}

@Composable
fun MessageInput(onSendMessage: (String) -> Unit) {
  var text by remember { mutableStateOf(TextFieldValue("")) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(8.dp)
  ) {
    Row(
      modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.weight(1f),
        placeholder = { Text("Type a message...") },
        maxLines = 5
      )
      IconButton(
        onClick = {
          if (text.text.isNotBlank()) {
            onSendMessage(text.text)
            text = TextFieldValue("")
          }
        },
        enabled = text.text.isNotBlank()
      ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
      }
    }
  }
}