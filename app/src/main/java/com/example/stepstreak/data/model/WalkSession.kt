package com.example.stepstreak.data.model

data class WalkSession(
    val user_id: String = "",
    val created_at: Long = System.currentTimeMillis()
)