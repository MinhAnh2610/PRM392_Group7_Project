package com.tutorial.project.data.model

import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Notification(
  val id: Int,
  val user_id: String,
  val message: String,
  val is_read: Boolean,
  val created_at: String,
)
