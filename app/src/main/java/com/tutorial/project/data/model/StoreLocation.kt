package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StoreLocation(
  val id: Int,
  val name: String?,
  val latitude: Double,
  val longitude: Double,
  val address: String?
)
