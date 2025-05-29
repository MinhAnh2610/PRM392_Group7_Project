package com.tutorial.project.data.model

data class OrderItem(
  val id: Int,
  val order_id: Int,
  val order: Order,
  val product_id: Int,
  val product: Product,
  val quantity: Int,
  val price: Double
)
