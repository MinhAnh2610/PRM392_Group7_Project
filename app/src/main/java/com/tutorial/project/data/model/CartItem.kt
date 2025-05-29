package com.tutorial.project.data.model

data class CartItem(
  val id: Int,
  val user_id: String,
  val user: Profile,
  val product_id: Int,
  val product: Product,
  val quantity: Int,
)
