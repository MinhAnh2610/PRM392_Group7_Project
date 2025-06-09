package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
  val id: Int? = null,
  val sender_id: String,
  val receiver_id: String, // Could be a fixed store ID or dynamic
  val content: String,
  val sent_at: String? // Consider Instant
)
