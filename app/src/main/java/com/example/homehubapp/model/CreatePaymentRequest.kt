package com.example.homehubapp.model

data class CreatePaymentRequest(
    val serviceRequestId: Int,
    val paymentMethod: String
)
