package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CartItemWithProductDetails(
  val cart_item_id: Int,
  val user_id: String,
  val product_id: Int,
  var quantity: Int,
  val product_name: String,
  val product_price: Double,
  val product_image_url: String?
)