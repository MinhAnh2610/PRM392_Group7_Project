// app/src/main/java/com/tutorial/project/data/repository/ChatRepository.kt
package com.tutorial.project.data.repository

import com.tutorial.project.data.model.Conversation
import com.tutorial.project.data.model.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.buildJsonObject

class ChatRepository(
  private val client: SupabaseClient,
  private val auth: Auth,
) {
  private fun getCurrentUserId(): String? = auth.currentUserOrNull()?.id

  suspend fun getConversations(): Result<List<Conversation>> {
    return try {
      val result = client.postgrest.rpc("get_conversations", buildJsonObject { })
        .decodeList<Conversation>()
      Result.success(result)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun sendMessage(content: String, receiverId: String): Result<Message> {
    return try {
      val senderId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

      val messageToSend = Message(
        sender_id = senderId,
        receiver_id = receiverId,
        content = content
      )

      val insertedMessage = client.from("messages")
        .insert(messageToSend)
        .decodeSingle<Message>()

      Result.success(insertedMessage)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getMessageHistory(otherUserId: String): Result<List<Message>> {
    return try {
      val userId = getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))

      val messages = client.from("messages")
        .select {
          filter {
            or {
              and {
                eq("sender_id", userId)
                eq("receiver_id", otherUserId)
              }
              and {
                eq("sender_id", otherUserId)
                eq("receiver_id", userId)
              }
            }
          }
          order("created_at", Order.ASCENDING) // Order by creation time
        }
        .decodeList<Message>()
      Result.success(messages)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun getNewMessagesFlow(channel: RealtimeChannel, otherUserId: String): Flow<Message> {
    val userId = getCurrentUserId() ?: throw IllegalStateException("User not logged in for realtime chat")

    return channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
      table = "messages"
    }.mapNotNull { insertAction ->
      val newMessage = insertAction.decodeRecord<Message>()

      val isFromOtherUser = newMessage.sender_id == otherUserId && newMessage.receiver_id == userId
      val isFromCurrentUser = newMessage.sender_id == userId && newMessage.receiver_id == otherUserId

      if (isFromOtherUser || isFromCurrentUser) {
        newMessage
      } else {
        null
      }
    }
  }
}