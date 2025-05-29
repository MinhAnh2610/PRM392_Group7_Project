package com.tutorial.project.data.model

import java.util.Date

data class Message(
  val id: Int,
  val sender_id: String,
  val receiver_id: String,
  val content: String,
  val sent_at: Date
)
