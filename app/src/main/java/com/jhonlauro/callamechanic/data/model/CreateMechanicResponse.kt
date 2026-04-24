package com.jhonlauro.callamechanic.data.model

data class CreateMechanicResponse(
    val message: String?,
    val mechanic: CreatedMechanicInfo?
)

data class CreatedMechanicInfo(
    val id: Long?,
    val fullName: String?,
    val email: String?,
    val mechanicId: String?
)