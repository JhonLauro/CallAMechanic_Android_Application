package com.jhonlauro.callamechanic.data.remote

import com.jhonlauro.callamechanic.data.model.AdminUserListData
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.model.AssignMechanicRequest
import com.jhonlauro.callamechanic.data.model.CreateAppointmentRequest
import com.jhonlauro.callamechanic.data.model.CreateMechanicRequest
import com.jhonlauro.callamechanic.data.model.CreateMechanicResponse
import com.jhonlauro.callamechanic.data.model.DeleteUserResponse
import com.jhonlauro.callamechanic.data.model.LoginRequest
import com.jhonlauro.callamechanic.data.model.LoginResponse
import com.jhonlauro.callamechanic.data.model.RegisterRequest
import com.jhonlauro.callamechanic.data.model.UpdateProfileRequest
import com.jhonlauro.callamechanic.data.model.UpdateStatusRequest
import com.jhonlauro.callamechanic.data.model.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<ApiMessageResponse<LoginResponse>>

    @POST("auth/register")
    fun register(
        @Body request: RegisterRequest
    ): Call<ApiMessageResponse<LoginResponse>>

    @GET("appointments")
    fun getAppointments(
        @Header("Authorization") token: String
    ): Call<ApiMessageResponse<List<Appointment>>>

    @POST("appointments")
    fun createAppointment(
        @Header("Authorization") token: String,
        @Body request: CreateAppointmentRequest
    ): Call<ApiMessageResponse<Appointment>>

    @PATCH("appointments/{id}/status")
    fun updateAppointmentStatus(
        @Header("Authorization") token: String,
        @Path("id") appointmentId: Long,
        @Body request: UpdateStatusRequest
    ): Call<ApiMessageResponse<Appointment>>

    @PATCH("appointments/{id}/claim")
    fun claimAppointment(
        @Header("Authorization") token: String,
        @Path("id") appointmentId: Long
    ): Call<ApiMessageResponse<Appointment>>

    @GET("admin/users")
    fun getAdminUsers(
        @Header("Authorization") token: String
    ): Call<ApiMessageResponse<AdminUserListData>>

    @POST("admin/mechanics")
    fun createMechanic(
        @Header("Authorization") token: String,
        @Body request: CreateMechanicRequest
    ): Call<ApiMessageResponse<CreateMechanicResponse>>

    @DELETE("admin/users/{id}")
    fun deleteUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Long
    ): Call<ApiMessageResponse<DeleteUserResponse>>

    @PATCH("admin/appointments/{id}/assign-mechanic")
    fun assignMechanic(
        @Header("Authorization") token: String,
        @Path("id") appointmentId: Long,
        @Body request: AssignMechanicRequest
    ): Call<ApiMessageResponse<Appointment>>

    @GET("profile")
    fun getProfile(
        @Header("Authorization") token: String
    ): Call<ApiMessageResponse<User>>

    @retrofit2.http.PUT("profile")
    fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Call<ApiMessageResponse<User>>
}
