package com.example.homehubapp.model

data class UpdateStatusRequest(
    val status: String,
    val amount: Double? = null
)
