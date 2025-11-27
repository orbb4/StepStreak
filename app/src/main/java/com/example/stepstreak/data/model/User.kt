package com.example.stepstreak.data.model

data class User(
    val streak: Int = 0,
    val steps_goal: Int = 5000,
    val email: String = "",
    val created_at: Long = System.currentTimeMillis()
)