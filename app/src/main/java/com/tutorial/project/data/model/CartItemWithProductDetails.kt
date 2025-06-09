package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CartItemWithProductDetails(
  val cartItemId: Int,
  val userId: String,
  val productId: Int,
  var quantity: Int,
  val productName: String,
  val productPrice: Double,
  val productImageUrl: String?
)