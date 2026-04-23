package com.jhonlauro.callamechanic.data.repository

import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.LoginRequest
import com.jhonlauro.callamechanic.data.model.LoginResponse
import com.jhonlauro.callamechanic.data.model.RegisterRequest
import com.jhonlauro.callamechanic.data.remote.RetrofitClient
import retrofit2.Call

class AuthRepository {

    fun login(request: LoginRequest): Call<ApiMessageResponse<LoginResponse>> {
        return RetrofitClient.apiService.login(request)
    }

    fun register(request: RegisterRequest): Call<ApiMessageResponse<LoginResponse>> {
        return RetrofitClient.apiService.register(request)
    }
}