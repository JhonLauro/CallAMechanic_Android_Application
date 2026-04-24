package com.jhonlauro.callamechanic.data.repository

import com.jhonlauro.callamechanic.data.model.AdminUserListData
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.model.AssignMechanicRequest
import com.jhonlauro.callamechanic.data.model.CreateMechanicRequest
import com.jhonlauro.callamechanic.data.model.CreateMechanicResponse
import com.jhonlauro.callamechanic.data.model.DeleteUserResponse
import com.jhonlauro.callamechanic.data.model.UpdateStatusRequest
import com.jhonlauro.callamechanic.data.remote.RetrofitClient
import retrofit2.Call

class AdminRepository {

    fun getUsers(token: String): Call<ApiMessageResponse<AdminUserListData>> {
        return RetrofitClient.apiService.getAdminUsers("Bearer $token")
    }

    fun createMechanic(
        token: String,
        request: CreateMechanicRequest
    ): Call<ApiMessageResponse<CreateMechanicResponse>> {
        return RetrofitClient.apiService.createMechanic("Bearer $token", request)
    }

    fun deleteUser(
        token: String,
        userId: Long
    ): Call<ApiMessageResponse<DeleteUserResponse>> {
        return RetrofitClient.apiService.deleteUser("Bearer $token", userId)
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

    fun assignMechanic(
        token: String,
        appointmentId: Long,
        mechanicId: Long
    ): Call<ApiMessageResponse<Appointment>> {
        return RetrofitClient.apiService.assignMechanic(
            "Bearer $token",
            appointmentId,
            AssignMechanicRequest(mechanicId)
        )
    }
}