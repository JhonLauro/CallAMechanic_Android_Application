package com.jhonlauro.callamechanic.data.model

data class LoginResponse(
    val token: String,
    val user: User
)