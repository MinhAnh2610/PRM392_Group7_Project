package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderItemsWithProductDetails(
  val id: Int,
  val order_id: Int,
  val product_id: Int,
  val quantity: Int,
  val price: Double,
  val product_name: String,
  val product_image_url: String?
)
