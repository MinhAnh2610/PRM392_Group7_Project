package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
  val id: String,
  val username: String?,
  val phone: String?,
  val created_at: String? = null
)
