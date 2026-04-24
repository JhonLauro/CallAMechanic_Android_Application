package com.jhonlauro.callamechanic.data.model

data class User(
    val id: Long,
    val fullName: String,
    val email: String?,
    val role: String,
    val mechanicId: String?,
    val adminId: String?,
    val phoneNumber: String?
)