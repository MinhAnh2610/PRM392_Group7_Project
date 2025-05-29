package com.tutorial.project.data.model

import java.util.Date

data class Order(
  val id: Int,
  val user_id: String,
  val user: Profile,
  val total_amount: Double,
  val status: String,
  val created_at: Date
)
