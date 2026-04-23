package com.jhonlauro.callamechanic.data.model

data class ApiMessageResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: Any?,
    val timestamp: String?
)