package com.jhonlauro.callamechanic.data.model

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val password: String
)