package com.jhonlauro.callamechanic.data.model

data class Appointment(
    val id: Long,
    val client: AppointmentClient?,
    val mechanic: AppointmentMechanic?,
    val serviceType: String?,
    val vehicleInfo: String?,
    val problemDescription: String?,
    val scheduledDate: String?,
    val status: String?,
    val createdAt: String?
)

data class AppointmentClient(
    val id: Long?,
    val fullName: String?,
    val email: String?,
    val phoneNumber: String?
)

data class AppointmentMechanic(
    val id: Long?,
    val fullName: String?,
    val mechanicId: String?,
    val phoneNumber: String?
)