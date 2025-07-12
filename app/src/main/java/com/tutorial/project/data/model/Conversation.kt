package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
/*data class Conversation(
  val other_user_id: String,
  val other_username: String?,
  val last_message: String?,
  val last_message_at: String?
)*/

data class Conversation(
  val other_user_id: String,
  val other_username: String?,
  val last_message: String?,
//  val last_message_time: Long?,
//  val last_message_from_me: Boolean?,
  val unread_count: Int = 0
)
