package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Order(
  val id: Int? = null,
  val user_id: String?,
  val total_amount: Double,
  val status: String = "pending",
  val created_at: String? = null
)
