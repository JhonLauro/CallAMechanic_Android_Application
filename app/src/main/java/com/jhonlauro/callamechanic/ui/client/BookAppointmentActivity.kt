package com.jhonlauro.callamechanic.ui.client

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.model.CreateAppointmentRequest
import com.jhonlauro.callamechanic.data.model.Vehicle
import com.jhonlauro.callamechanic.data.repository.AppointmentRepository
import com.jhonlauro.callamechanic.data.repository.VehicleRepository
import com.jhonlauro.callamechanic.databinding.ActivityBookAppointmentBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.common.AppTransitions
import com.jhonlauro.callamechanic.ui.common.FormScrollHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookAppointmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookAppointmentBinding
    private lateinit var appointmentRepository: AppointmentRepository
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var sessionManager: SessionManager
    private var vehicles: List<Vehicle> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appointmentRepository = AppointmentRepository()
        vehicleRepository = VehicleRepository()
        sessionManager = SessionManager(this)
        FormScrollHelper.enable(binding.root)

        binding.btnSubmitAppointment.setOnClickListener {
            submitAppointment()
        }

        binding.btnCancel.setOnClickListener {
            finish()
            AppTransitions.close(this)
        }

        binding.etVehicleInfo.setOnClickListener {
            showVehiclePicker()
        }

        loadVehicles()
    }

    private fun loadVehicles() {
        val token = sessionManager.getToken() ?: return
        vehicleRepository.getVehicles(token)
            .enqueue(object : Callback<ApiMessageResponse<List<Vehicle>>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<List<Vehicle>>>,
                    response: Response<ApiMessageResponse<List<Vehicle>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        vehicles = response.body()?.data ?: emptyList()
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<List<Vehicle>>>,
                    t: Throwable
                ) {
                    vehicles = emptyList()
                }
            })
    }

    private fun showVehiclePicker() {
        if (vehicles.isEmpty()) return

        val labels = vehicles.map { it.displayName() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Vehicle")
            .setItems(labels) { _, which ->
                binding.etVehicleInfo.setText(labels[which])
            }
            .show()
    }

    private fun submitAppointment() {
        val serviceType = binding.etServiceType.text.toString().trim()
        val vehicleInfo = binding.etVehicleInfo.text.toString().trim()
        val problemDescription = binding.etProblemDescription.text.toString().trim()
        val scheduledDate = binding.etScheduledDate.text.toString().trim()

        binding.tvError.visibility = View.GONE

        if (serviceType.isEmpty() || vehicleInfo.isEmpty() || problemDescription.isEmpty() || scheduledDate.isEmpty()) {
            binding.tvError.text = "All fields are required"
            binding.tvError.visibility = View.VISIBLE
            return
        }

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            binding.tvError.text = "No active session found"
            binding.tvError.visibility = View.VISIBLE
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmitAppointment.isEnabled = false

        val request = CreateAppointmentRequest(
            serviceType = serviceType,
            vehicleInfo = vehicleInfo,
            problemDescription = problemDescription,
            scheduledDate = scheduledDate
        )

        appointmentRepository.createAppointment(token, request)
            .enqueue(object : Callback<ApiMessageResponse<Appointment>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<Appointment>>,
                    response: Response<ApiMessageResponse<Appointment>>
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitAppointment.isEnabled = true

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@BookAppointmentActivity,
                            "Appointment booked successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                        AppTransitions.close(this@BookAppointmentActivity)
                    } else {
                        binding.tvError.text = response.errorBody()?.string() ?: "Failed to book appointment"
                        binding.tvError.visibility = View.VISIBLE
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<Appointment>>,
                    t: Throwable
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitAppointment.isEnabled = true
                    binding.tvError.text = t.message ?: "Something went wrong"
                    binding.tvError.visibility = View.VISIBLE
                }
            })
    }
}
