package com.jhonlauro.callamechanic.data.model

data class CreateMechanicRequest(
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val password: String,
    val mechanicId: String
)