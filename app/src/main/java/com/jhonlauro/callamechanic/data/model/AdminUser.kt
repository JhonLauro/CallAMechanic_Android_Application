package com.jhonlauro.callamechanic.data.model

data class AdminUser(
    val id: Long,
    val fullName: String?,
    val email: String?,
    val phoneNumber: String?,
    val role: String?,
    val mechanicId: String?,
    val isActive: Boolean?
)