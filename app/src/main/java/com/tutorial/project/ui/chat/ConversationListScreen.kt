// app/src/main/java/com/tutorial/project/ui/chat/ConversationsListScreen.kt
package com.tutorial.project.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.dto.UiState
import com.tutorial.project.data.model.Conversation
import com.tutorial.project.data.repository.ChatRepository
import com.tutorial.project.navigation.Screen
import com.tutorial.project.viewmodel.ConversationsViewModel
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsListScreen(navController: NavController) {
  val viewModel: ConversationsViewModel = viewModel(
    factory = GenericViewModelFactory {
      ConversationsViewModel(
        ChatRepository(
          client = SupabaseClientProvider.client,
          auth = SupabaseClientProvider.client.auth,
        )
      )
    }
  )

  val conversationsState by viewModel.conversationsState.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Messages") },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      when (val state = conversationsState) {
        is UiState.Loading -> {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        is UiState.Error -> {
          Text(
            text = state.message,
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.error
          )
        }
        is UiState.Success -> {
          val conversations = state.data
          if (conversations.isEmpty()) {
            Text(
              "You have no messages.",
              modifier = Modifier.align(Alignment.Center)
            )
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(vertical = 8.dp)
            ) {
              items(conversations, key = { it.other_user_id }) { conversation ->
                ConversationItem(
                  conversation = conversation,
                  onClick = {
                    navController.navigate(
                      Screen.Chat.createRoute(
                        storeOwnerId = conversation.other_user_id,
                        storeName = conversation.other_username ?: "User"
                      )
                    )
                  }
                )
                HorizontalDivider()
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ConversationItem(
  conversation: Conversation,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // You can add a user avatar here later
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = conversation.other_username ?: "Unknown User",
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodyLarge
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = conversation.last_message ?: "No messages yet",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}