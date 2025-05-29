package com.tutorial.project.data.model

import java.util.Date

data class Notification(
  val id: Int,
  val user_id: String,
  val user: Profile,
  val message: String,
  val is_read: Boolean,
  val created_at: Date
)
