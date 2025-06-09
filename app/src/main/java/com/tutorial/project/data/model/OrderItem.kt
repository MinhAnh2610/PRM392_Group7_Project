package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderItem(
  val id: Int? = null,
  val order_id: Int,
  val product_id: Int,
  val quantity: Int,
  val price: Double
)
