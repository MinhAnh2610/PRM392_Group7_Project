package com.tutorial.project.data.model

import java.util.Date

data class Profile (
  val id: String,
  val username: String,
  val phone: String,
  val created_at: Date
)