package com.jhonlauro.callamechanic.data.repository

import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.model.CreateAppointmentRequest
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
}