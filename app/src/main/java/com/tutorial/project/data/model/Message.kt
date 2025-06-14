package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
  val id: Int? = null,
  val sender_id: String,
  val receiver_id: String, // Could be a fixed store ID or dynamic
  val content: String,
  val created_at: String? = null // Consider Instant
)
