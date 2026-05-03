package com.jhonlauro.callamechanic.data.repository

import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Vehicle
import com.jhonlauro.callamechanic.data.model.VehicleRequest
import com.jhonlauro.callamechanic.data.remote.RetrofitClient
import retrofit2.Call

class VehicleRepository {

    fun getVehicles(token: String): Call<ApiMessageResponse<List<Vehicle>>> {
        return RetrofitClient.apiService.getVehicles("Bearer $token")
    }

    fun createVehicle(token: String, request: VehicleRequest): Call<ApiMessageResponse<Vehicle>> {
        return RetrofitClient.apiService.createVehicle("Bearer $token", request)
    }

    fun deleteVehicle(token: String, vehicleId: Long): Call<ApiMessageResponse<Any>> {
        return RetrofitClient.apiService.deleteVehicle("Bearer $token", vehicleId)
    }
}

