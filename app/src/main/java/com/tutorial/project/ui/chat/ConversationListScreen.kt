// app/src/main/java/com/tutorial/project/ui/chat/ConversationsListScreen.kt
package com.tutorial.project.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        title = {
          Text(
            "Messages",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.headlineSmall
          )
        },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface
        )
      )
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .background(MaterialTheme.colorScheme.background)
    ) {
      when (val state = conversationsState) {
        is UiState.Loading -> {
          Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator(
              color = MaterialTheme.colorScheme.primary,
              strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "Loading conversations...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        is UiState.Error -> {
          Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "😔",
              fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "Something went wrong",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.error,
              fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = state.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        is UiState.Success -> {
          val conversations = state.data
          if (conversations.isEmpty()) {
            Column(
              modifier = Modifier.align(Alignment.Center),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "💬",
                fontSize = 64.sp
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "No conversations yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Start a conversation to see it here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
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
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(
      defaultElevation = 2.dp,
      pressedElevation = 4.dp
    ),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Avatar
      UserAvatar(
        username = conversation.other_username ?: "U",
        hasUnreadMessages = conversation.unread_count > 0
      )

      Spacer(modifier = Modifier.width(12.dp))

      // Content
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = conversation.other_username ?: "Unknown User",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )

          // Timestamp
          /*conversation.last_message_time?.let { timestamp ->
            Text(
              text = formatTimestamp(timestamp),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }*/
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Last message with sender indicator
          Text(
            text = buildString {
              /*if (conversation.last_message_from_me == true) {
                append("You: ")
              }*/
              append(conversation.last_message ?: "No messages yet")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (conversation.unread_count > 0) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (conversation.unread_count > 0) {
              FontWeight.Medium
            } else {
              FontWeight.Normal
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )

          // Unread count badge
          if (conversation.unread_count > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = CircleShape,
              modifier = Modifier.size(24.dp)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
              ) {
                Text(
                  text = if (conversation.unread_count > 99) "99+" else conversation.unread_count.toString(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun UserAvatar(
  username: String,
  hasUnreadMessages: Boolean = false
) {
  val avatarColor = if (hasUnreadMessages) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.primaryContainer
  }

  val textColor = if (hasUnreadMessages) {
    MaterialTheme.colorScheme.onPrimary
  } else {
    MaterialTheme.colorScheme.onPrimaryContainer
  }

  BadgedBox(
    badge = {
      if (hasUnreadMessages) {
        Badge(
          containerColor = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(12.dp)
        )
      }
    }
  ) {
    Surface(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape),
      color = avatarColor
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
      ) {
        if (username.isNotEmpty()) {
          Text(
            text = username.first().uppercase(),
            style = MaterialTheme.typography.headlineSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
          )
        } else {
          Icon(
            Icons.Default.Person,
            contentDescription = "User",
            tint = textColor,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  }
}

private fun formatTimestamp(timestamp: Long): String {
  val now = System.currentTimeMillis()
  val diff = now - timestamp

  return when {
    diff < TimeUnit.MINUTES.toMillis(1) -> "now"
    diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m"
    diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h"
    diff < TimeUnit.DAYS.toMillis(7) -> {
      val days = TimeUnit.MILLISECONDS.toDays(diff)
      "${days}d"
    }
    else -> {
      val date = Date(timestamp)
      SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
  }
}