package com.jhonlauro.callamechanic.ui.client

import android.app.AlertDialog
import android.content.Intent
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
import com.jhonlauro.callamechanic.ui.common.FriendlyError
import com.jhonlauro.callamechanic.ui.common.clearFieldErrorOnInput
import com.jhonlauro.callamechanic.ui.common.clearFieldErrors
import com.jhonlauro.callamechanic.ui.common.showFieldError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookAppointmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookAppointmentBinding
    private lateinit var appointmentRepository: AppointmentRepository
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var sessionManager: SessionManager
    private var vehicles: List<Vehicle> = emptyList()
    private var selectedVehicleInfo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appointmentRepository = AppointmentRepository()
        vehicleRepository = VehicleRepository()
        sessionManager = SessionManager(this)
        FormScrollHelper.enable(binding.root)
        clearFieldErrorOnInput(binding.etServiceType, binding.etVehicleInfo, binding.etProblemDescription, binding.etScheduledDate)

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
        binding.etVehicleInfo.apply {
            isFocusable = false
            isCursorVisible = false
            keyListener = null
            hint = "Loading registered vehicles..."
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
                        selectedVehicleInfo = null
                        binding.etVehicleInfo.text?.clear()
                        binding.etVehicleInfo.hint = if (vehicles.isEmpty()) {
                            "Register a vehicle before booking"
                        } else {
                            "Tap to select a registered vehicle"
                        }
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<List<Vehicle>>>,
                    t: Throwable
                ) {
                    vehicles = emptyList()
                    selectedVehicleInfo = null
                    binding.etVehicleInfo.text?.clear()
                    binding.etVehicleInfo.hint = "Register a vehicle before booking"
                }
            })
    }

    private fun showVehiclePicker() {
        binding.tvError.visibility = View.GONE
        clearFieldErrors(binding.etVehicleInfo)

        if (vehicles.isEmpty()) {
            showFieldError(binding.etVehicleInfo, "Register a vehicle before booking an appointment.")
            showRegisterVehiclePrompt()
            return
        }

        val labels = vehicles.map { it.displayName() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Vehicle")
            .setItems(labels) { _, which ->
                selectedVehicleInfo = labels[which]
                binding.etVehicleInfo.setText(labels[which])
                clearFieldErrors(binding.etVehicleInfo)
            }
            .show()
    }

    private fun submitAppointment() {
        val serviceType = binding.etServiceType.text.toString().trim()
        val vehicleInfo = selectedVehicleInfo.orEmpty()
        val problemDescription = binding.etProblemDescription.text.toString().trim()
        val scheduledDate = binding.etScheduledDate.text.toString().trim()

        binding.tvError.visibility = View.GONE
        clearFieldErrors(binding.etServiceType, binding.etVehicleInfo, binding.etProblemDescription, binding.etScheduledDate)

        if (serviceType.isEmpty()) {
            showFieldError(binding.etServiceType, "Service type is required.")
            return
        }

        if (vehicles.isEmpty()) {
            showFieldError(binding.etVehicleInfo, "Register a vehicle before booking an appointment.")
            showRegisterVehiclePrompt()
            return
        }

        if (vehicleInfo.isEmpty()) {
            showFieldError(binding.etVehicleInfo, "Select one of your registered vehicles.")
            return
        }

        if (problemDescription.isEmpty()) {
            showFieldError(binding.etProblemDescription, "Problem description is required.")
            return
        }

        if (scheduledDate.isEmpty()) {
            showFieldError(binding.etScheduledDate, "Date is required.")
            return
        }

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            binding.tvError.text = "Session expired. Please sign in again."
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
                        binding.tvError.text = FriendlyError.fromResponse(response, "Validation failed. Please review your input.")
                        binding.tvError.visibility = View.VISIBLE
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<Appointment>>,
                    t: Throwable
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitAppointment.isEnabled = true
                    binding.tvError.text = FriendlyError.fromThrowable(t, "Request failed. Please try again.")
                    binding.tvError.visibility = View.VISIBLE
                }
            })
    }

    private fun showRegisterVehiclePrompt() {
        AlertDialog.Builder(this)
            .setTitle("Vehicle required")
            .setMessage("Please register one of your vehicles before booking an appointment.")
            .setPositiveButton("Manage Vehicles") { _, _ ->
                startActivity(Intent(this, ManageVehiclesActivity::class.java))
                AppTransitions.open(this)
            }
            .setNegativeButton("Not now", null)
            .show()
    }
}
