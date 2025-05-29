package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
  val id:Int,
  val name: String,
  val description: String?,
  val price: Double,
  val stock_quantity: Int,
  val image_url: String,
  val category: String
)