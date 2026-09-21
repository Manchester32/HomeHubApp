package com.example.homehubapp.model

data class CreateServiceRequest(
    val userId: Int,
    val serviceId: Int,
    val description: String,
    val serviceAddress: String,
    val preferredDate: String
)
