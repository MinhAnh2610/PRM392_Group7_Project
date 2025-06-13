// app/src/main/java/com/tutorial/project/ui/chat/ChatScreen.kt
package com.tutorial.project.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
  navController: NavController,
  StoreOwnerId: String,
  StoreName: String
) {
  var messageText by remember { mutableStateOf("") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(StoreName) },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    bottomBar = {
      Surface(tonalElevation = 4.dp) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextField(
            value = messageText,
            onValueChange = { messageText = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            colors = TextFieldDefaults.colors(
              focusedIndicatorColor = MaterialTheme.colorScheme.primary,
              unfocusedIndicatorColor = MaterialTheme.colorScheme.surface
            )
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(onClick = { /* TODO: Send message logic */ }) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
          }
        }
      }
    }
  ) { padding ->
    // This LazyColumn will hold the chat messages
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 8.dp),
      verticalArrangement = Arrangement.Bottom
    ) {
      // Placeholder for chat messages
      item {
        Text(
          "Chat history with $StoreName would appear here.",
          modifier = Modifier.padding(16.dp)
        )
      }
    }
  }
}