package com.jhonlauro.callamechanic.data.model

data class CreateAppointmentRequest(
    val serviceType: String,
    val vehicleInfo: String,
    val problemDescription: String,
    val scheduledDate: String
)