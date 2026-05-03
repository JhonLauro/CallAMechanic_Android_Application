package com.jhonlauro.callamechanic.ui.client

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Vehicle
import com.jhonlauro.callamechanic.data.model.VehicleRequest
import com.jhonlauro.callamechanic.data.repository.VehicleRepository
import com.jhonlauro.callamechanic.databinding.ActivityManageVehiclesBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.common.AppTransitions
import com.jhonlauro.callamechanic.ui.common.FormScrollHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageVehiclesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageVehiclesBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var adapter: VehicleAdapter

    private val vehicleTypes = listOf(
        "Select type...",
        "Sedan",
        "SUV",
        "Truck",
        "Van",
        "Motorcycle",
        "Hatchback",
        "Coupe",
        "Wagon"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageVehiclesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        vehicleRepository = VehicleRepository()
        adapter = VehicleAdapter(emptyList()) { vehicle -> confirmDelete(vehicle) }

        FormScrollHelper.enable(binding.root)
        binding.rvVehicles.layoutManager = LinearLayoutManager(this)
        binding.rvVehicles.adapter = adapter

        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            vehicleTypes
        )
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spVehicleType.adapter = typeAdapter

        binding.btnBackVehicles.setOnClickListener {
            finish()
            AppTransitions.close(this)
        }

        binding.btnToggleVehicleForm.setOnClickListener {
            binding.cardVehicleForm.visibility =
                if (binding.cardVehicleForm.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.btnSaveVehicle.setOnClickListener {
            createVehicle()
        }
    }

    override fun onResume() {
        super.onResume()
        loadVehicles()
    }

    private fun loadVehicles() {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            showError("No active session found")
            return
        }

        binding.progressBarVehicles.visibility = View.VISIBLE
        vehicleRepository.getVehicles(token).enqueue(object : Callback<ApiMessageResponse<List<Vehicle>>> {
            override fun onResponse(
                call: Call<ApiMessageResponse<List<Vehicle>>>,
                response: Response<ApiMessageResponse<List<Vehicle>>>
            ) {
                binding.progressBarVehicles.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    val vehicles = response.body()?.data ?: emptyList()
                    adapter.updateData(vehicles.sortedByDescending { it.id })
                    binding.tvVehiclesCount.text = vehicles.size.toString()
                    binding.tvVehiclesEmpty.visibility = if (vehicles.isEmpty()) View.VISIBLE else View.GONE
                    binding.tvVehicleError.visibility = View.GONE
                } else {
                    showError("Failed to load vehicles")
                }
            }

            override fun onFailure(call: Call<ApiMessageResponse<List<Vehicle>>>, t: Throwable) {
                binding.progressBarVehicles.visibility = View.GONE
                showError(t.message ?: "Something went wrong")
            }
        })
    }

    private fun createVehicle() {
        val make = binding.etMake.text.toString().trim()
        val model = binding.etModel.text.toString().trim()
        val year = binding.etYear.text.toString().trim()
        val plate = binding.etPlateNumber.text.toString().trim()
        val color = binding.etColor.text.toString().trim()
        val type = binding.spVehicleType.selectedItem?.toString().orEmpty()
        val notes = binding.etNotes.text.toString().trim()

        if (make.isEmpty() || model.isEmpty() || year.isEmpty() || plate.isEmpty() || color.isEmpty() || type == vehicleTypes.first()) {
            showError("Please complete all required vehicle details")
            return
        }

        if (!Regex("^\\d{4}$").matches(year)) {
            showError("Year must be a valid 4-digit year")
            return
        }

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            showError("No active session found")
            return
        }

        binding.progressBarVehicles.visibility = View.VISIBLE
        binding.btnSaveVehicle.isEnabled = false

        val request = VehicleRequest(make, model, year, plate, color, type, notes)
        vehicleRepository.createVehicle(token, request).enqueue(object : Callback<ApiMessageResponse<Vehicle>> {
            override fun onResponse(
                call: Call<ApiMessageResponse<Vehicle>>,
                response: Response<ApiMessageResponse<Vehicle>>
            ) {
                binding.progressBarVehicles.visibility = View.GONE
                binding.btnSaveVehicle.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true) {
                    clearForm()
                    binding.cardVehicleForm.visibility = View.GONE
                    Toast.makeText(this@ManageVehiclesActivity, "Vehicle registered", Toast.LENGTH_SHORT).show()
                    loadVehicles()
                } else {
                    showError("Failed to register vehicle")
                }
            }

            override fun onFailure(call: Call<ApiMessageResponse<Vehicle>>, t: Throwable) {
                binding.progressBarVehicles.visibility = View.GONE
                binding.btnSaveVehicle.isEnabled = true
                showError(t.message ?: "Failed to register vehicle")
            }
        })
    }

    private fun confirmDelete(vehicle: Vehicle) {
        AlertDialog.Builder(this)
            .setTitle("Delete Vehicle")
            .setMessage("Delete ${vehicle.displayName()}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> deleteVehicle(vehicle.id) }
            .show()
    }

    private fun deleteVehicle(vehicleId: Long) {
        val token = sessionManager.getToken() ?: return
        binding.progressBarVehicles.visibility = View.VISIBLE
        vehicleRepository.deleteVehicle(token, vehicleId).enqueue(object : Callback<ApiMessageResponse<Any>> {
            override fun onResponse(
                call: Call<ApiMessageResponse<Any>>,
                response: Response<ApiMessageResponse<Any>>
            ) {
                binding.progressBarVehicles.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@ManageVehiclesActivity, "Vehicle deleted", Toast.LENGTH_SHORT).show()
                    loadVehicles()
                } else {
                    showError("Failed to delete vehicle")
                }
            }

            override fun onFailure(call: Call<ApiMessageResponse<Any>>, t: Throwable) {
                binding.progressBarVehicles.visibility = View.GONE
                showError(t.message ?: "Failed to delete vehicle")
            }
        })
    }

    private fun clearForm() {
        binding.etMake.text?.clear()
        binding.etModel.text?.clear()
        binding.etYear.text?.clear()
        binding.etPlateNumber.text?.clear()
        binding.etColor.text?.clear()
        binding.etNotes.text?.clear()
        binding.spVehicleType.setSelection(0)
        binding.tvVehicleError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.tvVehicleError.text = message
        binding.tvVehicleError.visibility = View.VISIBLE
    }
}
