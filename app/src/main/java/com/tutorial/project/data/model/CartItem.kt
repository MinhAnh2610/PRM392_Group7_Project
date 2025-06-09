package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CartItem(
  val id: Int? = null,
  val user_id: String,
  val product_id: Int,
  val quantity: Int,
)
