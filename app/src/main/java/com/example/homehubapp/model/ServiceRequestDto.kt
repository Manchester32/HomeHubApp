package com.example.homehubapp.model

data class ServiceRequestDto(
    val id: Int,
    val serviceId: Int,
    val serviceName: String,
    val description: String,
    val serviceAddress: String,
    val preferredDate: String,
    val status: String,
    val amount: Double,
    val isPaid: Boolean
)
