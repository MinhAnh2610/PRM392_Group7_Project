package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
  val id: Int? = null,
  val name: String?,
  val description: String?,
  val price: Double?,
  val stockQuantity: Int? = null,
  val imageUrl: String? = null,
  val category: String?
)
