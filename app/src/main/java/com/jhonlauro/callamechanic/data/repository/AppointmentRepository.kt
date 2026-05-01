package com.jhonlauro.callamechanic.data.repository

import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.model.CreateAppointmentRequest
import com.jhonlauro.callamechanic.data.model.UpdateStatusRequest
import com.jhonlauro.callamechanic.data.remote.RetrofitClient
import retrofit2.Call

class AppointmentRepository {

    fun getAppointments(token: String): Call<ApiMessageResponse<List<Appointment>>> {
        return RetrofitClient.apiService.getAppointments("Bearer $token")
    }

    fun createAppointment(
        token: String,
        request: CreateAppointmentRequest
    ): Call<ApiMessageResponse<Appointment>> {
        return RetrofitClient.apiService.createAppointment("Bearer $token", request)
    }

    fun updateAppointmentStatus(
        token: String,
        appointmentId: Long,
        status: String
    ): Call<ApiMessageResponse<Appointment>> {
        return RetrofitClient.apiService.updateAppointmentStatus(
            "Bearer $token",
            appointmentId,
            UpdateStatusRequest(status)
        )
    }

    fun claimAppointment(
        token: String,
        appointmentId: Long
    ): Call<ApiMessageResponse<Appointment>> {
        return RetrofitClient.apiService.claimAppointment("Bearer $token", appointmentId)
    }
}
