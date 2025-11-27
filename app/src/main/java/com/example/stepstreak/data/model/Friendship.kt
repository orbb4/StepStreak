package com.example.stepstreak.data.model

data class Friendship(
    val friend1: String = "",
    val friend2: String = "",
    val status: String = "pending", // accepted / pending / blocked
    val friendly_streak: Int = 0,
    val created_at: Long = System.currentTimeMillis()
)
