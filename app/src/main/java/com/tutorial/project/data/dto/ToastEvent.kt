package com.tutorial.project.data.dto

data class ToastEvent(val message: String, private val id: Long = System.nanoTime())
