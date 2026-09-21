package com.example.homehubapp.model

data class LoginResponse(
    val message: String,
    val userId: Int,
    val name: String,
    val email: String
)
