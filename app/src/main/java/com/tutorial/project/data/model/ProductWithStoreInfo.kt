package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductWithStoreInfo(
  val id: Int,
  val store_id: Int,
  val name: String,
  val description: String?,
  val price: Double,
  val stock_quantity: Int,
  val image_url: String?,
  val category: String?,
  val store_name: String,
  val store_logo_url: String?,
  val store_owner_id: String
)
