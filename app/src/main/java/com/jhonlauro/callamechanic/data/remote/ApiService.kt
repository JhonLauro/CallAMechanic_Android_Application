package com.jhonlauro.callamechanic.data.remote

import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.LoginRequest
import com.jhonlauro.callamechanic.data.model.LoginResponse
import com.jhonlauro.callamechanic.data.model.RegisterRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<ApiMessageResponse<LoginResponse>>

    @POST("auth/register")
    fun register(
        @Body request: RegisterRequest
    ): Call<ApiMessageResponse<LoginResponse>>
}