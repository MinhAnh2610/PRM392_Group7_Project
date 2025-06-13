package com.tutorial.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Store (
  val id: Int,
  val name: String,
  val description: String,
  val logo_url: String,
  val owner_id: String,
  val created_at: String
)