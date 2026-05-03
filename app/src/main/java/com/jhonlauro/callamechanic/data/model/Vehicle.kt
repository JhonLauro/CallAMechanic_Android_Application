package com.jhonlauro.callamechanic.data.model

data class Vehicle(
    val id: Long,
    val make: String?,
    val model: String?,
    val year: String?,
    val plateNumber: String?,
    val color: String?,
    val type: String?,
    val notes: String?,
    val recallStatus: String?
) {
    fun displayName(): String {
        val name = listOfNotNull(make, model, year)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val plate = plateNumber?.takeIf { it.isNotBlank() }
        return if (plate != null) "$name - $plate" else name.ifBlank { "Registered Vehicle" }
    }
}

