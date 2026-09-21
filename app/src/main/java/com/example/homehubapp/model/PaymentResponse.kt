package com.example.homehubapp.model

data class PaymentResponse(
    val message: String,
    val paymentId: Int,
    val amount: Double,
    val paymentMethod: String,
    val paymentStatus: String
)
