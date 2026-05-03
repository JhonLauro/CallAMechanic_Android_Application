package com.jhonlauro.callamechanic.data.model

data class VehicleRequest(
    val make: String,
    val model: String,
    val year: String,
    val plateNumber: String,
    val color: String,
    val type: String,
    val notes: String
)

