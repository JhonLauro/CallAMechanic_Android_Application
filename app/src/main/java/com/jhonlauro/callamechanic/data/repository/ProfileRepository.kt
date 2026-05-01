package com.jhonlauro.callamechanic.data.repository

import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.UpdateProfileRequest
import com.jhonlauro.callamechanic.data.model.User
import com.jhonlauro.callamechanic.data.remote.RetrofitClient
import retrofit2.Call

class ProfileRepository {

    fun getProfile(token: String): Call<ApiMessageResponse<User>> {
        return RetrofitClient.apiService.getProfile("Bearer $token")
    }

    fun updateProfile(
        token: String,
        request: UpdateProfileRequest
    ): Call<ApiMessageResponse<User>> {
        return RetrofitClient.apiService.updateProfile("Bearer $token", request)
    }
}
