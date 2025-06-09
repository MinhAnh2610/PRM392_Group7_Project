package com.tutorial.project.data.repository

import com.tutorial.project.data.model.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.Returning // Import for Returning options
import io.github.jan.supabase.realtime.PostgresAction.Insert
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.JsonObject

class ChatRepository(
  private val client: SupabaseClient, // SupabaseClient has postgrest, auth, realtime plugins
  private val auth: Auth,             // Can also get from client.auth
  private val realtime: Realtime      // Can also get from client.realtime
) {
  // ***** IMPORTANT: Replace this with the actual UUID of your store's user account *****
  private val STORE_USER_ID = "YOUR_STORE_USER_UUID_HERE"

  private fun getCurrentUserId(): String? = auth.currentUserOrNull()?.id

  suspend fun sendMessage(content: String): Result<Message?> {
    return try {
      val senderId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
      if (STORE_USER_ID == "YOUR_STORE_USER_UUID_HERE") {
        return Result.failure(Exception("Store user ID not configured. Please set it in ChatRepository."))
      }

      // Create the message object using Kotlin property names
      val messageToSend = Message(
        sender_id = senderId,
        receiver_id = STORE_USER_ID,
        content = content,
        sent_at = "" // Database (e.g., default now()) will set this.
        // If you want it from client, generate an ISO string here.
      )

      // Use client.postgrest explicitly for clarity, or client.from() if preferred
      val insertedMessage = client.from("messages")
        .insert(messageToSend) // Crucial: get the full inserted row back
        .decodeSingle<Message>() // Decodes the response into your Message data class

      Result.success(insertedMessage)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getMessageHistory(): Result<List<Message>> {
    return try {
      val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
      if (STORE_USER_ID == "YOUR_STORE_USER_UUID_HERE") {
        return Result.failure(Exception("Store user ID not configured."))
      }

      val messages = client.from("messages")
        .select {
          filter {
            or {
              and { // Messages sent by user to store
                eq("sender_id", userId)
                eq("receiver_id", STORE_USER_ID)
              }
              and { // Messages sent by store to user
                eq("sender_id", STORE_USER_ID)
                eq("receiver_id", userId)
              }
            }
          }
          order("sent_at", Order.ASCENDING)
        }
        .decodeList<Message>()
      Result.success(messages)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /*

  @OptIn(ExperimentalCoroutinesApi::class)
  fun listenForNewMessages(): Flow<JsonObject> { // Corrected return type to Flow<Message>
    val userId = getCurrentUserId() ?: throw IllegalStateException("User not logged in for realtime chat")
    if (STORE_USER_ID == "YOUR_STORE_USER_UUID_HERE") {
      throw IllegalStateException("Store user ID not configured for realtime chat.")
    }

    // Channel name can be more specific or general based on your RLS and filtering needs
    val channel = realtime.channel("chat_messages_user_${userId}_store")

    // Listen to inserts on 'messages' table.
    // RLS policies on Supabase for 'messages' table should ensure user only gets relevant messages.
    // Example RLS for SELECT on 'messages': (auth.uid() = sender_id OR auth.uid() = receiver_id)
    // Example RLS for INSERT on 'messages': (auth.uid() = sender_id)
    return channel.postgresChangeFlow<Message>(schema = "public") { // Type parameter is Message
      table = "messages"
      // event = "INSERT" // You can explicitly listen for inserts if needed, default is "*"
    }.mapNotNull { postgresAction -> // The 'it' here is a PostgresAction
      when (postgresAction) {
        is Insert -> {
          val newMessageRecord = postgresAction.record // This is of type Message
          // Ensure the message is part of the current user <-> store conversation
          // Access properties using Kotlin names (senderId, receiverId)
          if ((newMessageRecord.send == userId && newMessageRecord.receiverId == STORE_USER_ID) ||
            (newMessageRecord.senderId == STORE_USER_ID && newMessageRecord.receiverId == userId)) {
            newMessageRecord // Return the Message object
          } else {
            null // Filter out irrelevant messages
          }
        }
        else -> null // We only care about new messages (inserts) for this specific logic
      }
    }
    // Remember: The ViewModel collecting this flow will need to call channel.subscribe()
    // and channel.unsubscribe() when done.
  }

   */
}