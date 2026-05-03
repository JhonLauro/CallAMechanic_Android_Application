package com.jhonlauro.callamechanic.ui.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.model.User
import com.jhonlauro.callamechanic.data.model.Vehicle
import com.jhonlauro.callamechanic.data.repository.AppointmentRepository
import com.jhonlauro.callamechanic.data.repository.ProfileRepository
import com.jhonlauro.callamechanic.data.repository.VehicleRepository
import com.jhonlauro.callamechanic.databinding.ActivityClientDashboardBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.appointment.AppointmentDetailsActivity
import com.jhonlauro.callamechanic.ui.auth.LoginActivity
import com.jhonlauro.callamechanic.ui.common.AppTransitions
import com.jhonlauro.callamechanic.ui.common.ProfileDropdown
import com.jhonlauro.callamechanic.ui.common.ProfilePhotoRenderer
import com.jhonlauro.callamechanic.ui.profile.ProfileActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ClientDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientDashboardBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var appointmentRepository: AppointmentRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var adapter: AppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        appointmentRepository = AppointmentRepository()
        profileRepository = ProfileRepository()
        vehicleRepository = VehicleRepository()

        adapter = AppointmentAdapter(emptyList()) { appointment ->
            openAppointmentDetails(appointment)
        }

        binding.rvAppointments.layoutManager = LinearLayoutManager(this)
        binding.rvAppointments.adapter = adapter
        binding.rvAppointments.setHasFixedSize(false)

        binding.tvWelcome.text = "Welcome, ${sessionManager.getFullName() ?: "Client"}"
        renderProfilePhoto()

        binding.btnBookAppointment.setOnClickListener {
            startActivity(Intent(this, BookAppointmentActivity::class.java))
            AppTransitions.open(this)
        }

        binding.btnManageVehicles.setOnClickListener {
            startActivity(Intent(this, ManageVehiclesActivity::class.java))
            AppTransitions.open(this)
        }

        binding.btnProfile.setOnClickListener {
            showProfileMenu()
        }

        binding.btnLogout.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        renderProfilePhoto()
        loadProfile()
        loadAppointments()
        loadVehicles()
    }

    private fun renderProfilePhoto() {
        ProfilePhotoRenderer.show(binding.btnProfile, sessionManager.getPhotoUrl())
    }

    private fun loadProfile() {
        val token = sessionManager.getToken() ?: return
        profileRepository.getProfile(token).enqueue(object : Callback<ApiMessageResponse<User>> {
            override fun onResponse(
                call: Call<ApiMessageResponse<User>>,
                response: Response<ApiMessageResponse<User>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { user ->
                        sessionManager.updateProfileInfo(user.fullName, user.phoneNumber, user.photoUrl)
                        binding.tvWelcome.text = "Welcome, ${user.fullName}"
                        renderProfilePhoto()
                    }
                }
            }

            override fun onFailure(call: Call<ApiMessageResponse<User>>, t: Throwable) = Unit
        })
    }

    private fun loadAppointments() {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = "No active session found"
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        appointmentRepository.getAppointments(token)
            .enqueue(object : Callback<ApiMessageResponse<List<Appointment>>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<List<Appointment>>>,
                    response: Response<ApiMessageResponse<List<Appointment>>>
                ) {
                    binding.progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        val appointments = (response.body()?.data ?: emptyList())
                            .sortedWith(newestAppointmentFirst())
                        adapter.updateData(appointments)

                        val activeCount = appointments.count {
                            it.status == "PENDING" || it.status == "IN_PROGRESS"
                        }

                        val completedCount = appointments.count {
                            it.status == "COMPLETED" || it.status == "FINISHED"
                        }

                        binding.tvActiveServices.text = activeCount.toString()
                        binding.tvCompletedServices.text = completedCount.toString()
                        if (appointments.isEmpty()) {
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.tvEmptyState.text = "No service history yet. Book your first appointment!"
                        } else {
                            binding.tvEmptyState.visibility = View.GONE
                        }
                    } else {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.tvEmptyState.text = "Failed to load appointments"
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<List<Appointment>>>,
                    t: Throwable
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = t.message ?: "Something went wrong"
                }
            })
    }

    private fun newestAppointmentFirst(): Comparator<Appointment> {
        return compareByDescending<Appointment> { it.scheduledDate ?: "" }
            .thenByDescending { it.id }
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
                        binding.tvRegisteredVehicles.text =
                            (response.body()?.data ?: emptyList()).size.toString()
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<List<Vehicle>>>,
                    t: Throwable
                ) {
                    binding.tvRegisteredVehicles.text = "0"
                }
            })
    }

    private fun showProfileMenu() {
        ProfileDropdown.show(
            anchor = binding.btnProfile,
            onViewProfile = {
                startActivity(Intent(this, ProfileActivity::class.java))
                AppTransitions.open(this)
            },
            onSignOut = { signOut() }
        )
    }

    private fun signOut() {
        sessionManager.clearSession()
        startActivity(Intent(this, LoginActivity::class.java))
        AppTransitions.fade(this)
        finishAffinity()
    }

    private fun openAppointmentDetails(appointment: Appointment) {
        startActivity(Intent(this, AppointmentDetailsActivity::class.java).apply {
            putExtra(AppointmentDetailsActivity.EXTRA_ID, appointment.id)
            putExtra(AppointmentDetailsActivity.EXTRA_STATUS, appointment.status)
            putExtra(AppointmentDetailsActivity.EXTRA_CLIENT, appointment.client?.fullName ?: sessionManager.getFullName())
            putExtra(AppointmentDetailsActivity.EXTRA_CONTACT, appointment.client?.phoneNumber ?: sessionManager.getPhoneNumber())
            putExtra(AppointmentDetailsActivity.EXTRA_VEHICLE, appointment.vehicleInfo)
            putExtra(AppointmentDetailsActivity.EXTRA_SERVICE_TYPE, appointment.serviceType)
            putExtra(AppointmentDetailsActivity.EXTRA_PROBLEM, appointment.problemDescription)
            putExtra(AppointmentDetailsActivity.EXTRA_SCHEDULE, appointment.scheduledDate)
            putExtra(AppointmentDetailsActivity.EXTRA_MECHANIC, appointment.mechanic?.fullName ?: "Awaiting Assignment")
        })
        AppTransitions.open(this)
    }
}
