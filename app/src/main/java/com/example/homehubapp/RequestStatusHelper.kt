package com.example.homehubapp

object RequestStatusHelper {
    fun nextStatus(current: String): String? = when (current) {
        "Pending" -> "In Progress"
        "In Progress" -> "Completed"
        else -> null
    }

    fun amountWhenCompleted(): Double = 450.0
}
