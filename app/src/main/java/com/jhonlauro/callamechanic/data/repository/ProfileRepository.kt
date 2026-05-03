package com.jhonlauro.callamechanic.data.repository

import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.ChangePasswordRequest
import com.jhonlauro.callamechanic.data.model.UpdateProfileRequest
import com.jhonlauro.callamechanic.data.model.UploadPhotoResponse
import com.jhonlauro.callamechanic.data.model.User
import com.jhonlauro.callamechanic.data.remote.RetrofitClient
import retrofit2.Call
import okhttp3.MultipartBody

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

    fun changePassword(
        token: String,
        request: ChangePasswordRequest
    ): Call<ApiMessageResponse<Any>> {
        return RetrofitClient.apiService.changePassword("Bearer $token", request)
    }

    fun uploadProfilePhoto(
        token: String,
        file: MultipartBody.Part
    ): Call<ApiMessageResponse<UploadPhotoResponse>> {
        return RetrofitClient.apiService.uploadProfilePhoto("Bearer $token", file)
    }
}
