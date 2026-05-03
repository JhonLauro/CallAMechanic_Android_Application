package com.jhonlauro.callamechanic.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.model.User
import com.jhonlauro.callamechanic.data.repository.AppointmentRepository
import com.jhonlauro.callamechanic.data.repository.ProfileRepository
import com.jhonlauro.callamechanic.databinding.ActivityAdminDashboardBinding
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

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var appointmentRepository: AppointmentRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var activeAdapter: AdminAppointmentAdapter
    private lateinit var finishedAdapter: AdminAppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        appointmentRepository = AppointmentRepository()
        profileRepository = ProfileRepository()
        activeAdapter = AdminAppointmentAdapter(emptyList()) { appointment ->
            openAppointmentDetails(appointment)
        }
        finishedAdapter = AdminAppointmentAdapter(emptyList()) { appointment ->
            openAppointmentDetails(appointment)
        }

        binding.rvAdminAppointments.layoutManager = LinearLayoutManager(this)
        binding.rvAdminAppointments.adapter = activeAdapter
        binding.rvAdminAppointments.setHasFixedSize(false)
        binding.rvAdminFinishedAppointments.layoutManager = LinearLayoutManager(this)
        binding.rvAdminFinishedAppointments.adapter = finishedAdapter
        binding.rvAdminFinishedAppointments.setHasFixedSize(false)

        binding.tvAdminWelcome.text = "Welcome, ${sessionManager.getFullName() ?: "Admin"}"
        binding.tvRole.text = sessionManager.getRole() ?: "ADMIN"
        renderProfilePhoto()

        binding.btnUserRegistry.setOnClickListener {
            startActivity(Intent(this, UserRegistryActivity::class.java))
            AppTransitions.open(this)
        }

        binding.btnCreateMechanic.setOnClickListener {
            startActivity(Intent(this, CreateMechanicActivity::class.java))
            AppTransitions.open(this)
        }

        binding.btnRefreshAdmin.setOnClickListener {
            loadAppointments()
        }

        binding.btnOpenProfile.setOnClickListener {
            showProfileMenu()
        }

        binding.btnLogoutAdmin.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        renderProfilePhoto()
        loadProfile()
        loadAppointments()
    }

    private fun renderProfilePhoto() {
        ProfilePhotoRenderer.show(binding.btnOpenProfile, sessionManager.getPhotoUrl())
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
                        binding.tvAdminWelcome.text = "Welcome, ${user.fullName}"
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
            binding.tvEmptyStateAdmin.visibility = View.VISIBLE
            binding.tvEmptyStateAdmin.text = "No active session found"
            return
        }

        binding.progressBarAdmin.visibility = View.VISIBLE
        binding.tvEmptyStateAdmin.visibility = View.GONE

        appointmentRepository.getAppointments(token)
            .enqueue(object : Callback<ApiMessageResponse<List<Appointment>>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<List<Appointment>>>,
                    response: Response<ApiMessageResponse<List<Appointment>>>
                ) {
                    binding.progressBarAdmin.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        val appointments = response.body()?.data ?: emptyList()
                        val activeAppointments = appointments
                            .filter { !isFinishedStatus(it.status) }
                            .sortedWith(newestAppointmentFirst())
                        val finishedAppointments = appointments
                            .filter { isFinishedStatus(it.status) }
                            .sortedWith(newestAppointmentFirst())

                        activeAdapter.updateData(activeAppointments)
                        finishedAdapter.updateData(finishedAppointments)

                        binding.tvTotalJobs.text = appointments.size.toString()
                        binding.tvPendingJobs.text = appointments.count { it.status == "PENDING" }.toString()
                        binding.tvInProgressJobs.text = appointments.count { it.status == "IN_PROGRESS" }.toString()
                        binding.tvCompletedJobs.text = finishedAppointments.size.toString()
                        binding.tvActiveServiceCount.text = activeAppointments.size.toString()
                        binding.tvFinishedServiceCount.text = finishedAppointments.size.toString()

                        if (activeAppointments.isEmpty()) {
                            binding.tvEmptyStateAdmin.visibility = View.VISIBLE
                            binding.tvEmptyStateAdmin.text = "No active appointments found"
                        } else {
                            binding.tvEmptyStateAdmin.visibility = View.GONE
                        }

                        binding.tvFinishedEmptyStateAdmin.visibility =
                            if (finishedAppointments.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        binding.tvEmptyStateAdmin.visibility = View.VISIBLE
                        binding.tvEmptyStateAdmin.text = "Failed to load appointments"
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<List<Appointment>>>,
                    t: Throwable
                ) {
                    binding.progressBarAdmin.visibility = View.GONE
                    binding.tvEmptyStateAdmin.visibility = View.VISIBLE
                    binding.tvEmptyStateAdmin.text = t.message ?: "Something went wrong"
                }
            })
    }

    private fun isFinishedStatus(status: String?): Boolean {
        return status == "FINISHED" || status == "COMPLETED"
    }

    private fun newestAppointmentFirst(): Comparator<Appointment> {
        return compareByDescending<Appointment> { it.scheduledDate ?: "" }
            .thenByDescending { it.id }
    }

    private fun showProfileMenu() {
        ProfileDropdown.show(
            anchor = binding.btnOpenProfile,
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
            putExtra(AppointmentDetailsActivity.EXTRA_CLIENT, appointment.client?.fullName)
            putExtra(AppointmentDetailsActivity.EXTRA_CONTACT, appointment.client?.phoneNumber)
            putExtra(AppointmentDetailsActivity.EXTRA_VEHICLE, appointment.vehicleInfo)
            putExtra(AppointmentDetailsActivity.EXTRA_SERVICE_TYPE, appointment.serviceType)
            putExtra(AppointmentDetailsActivity.EXTRA_PROBLEM, appointment.problemDescription)
            putExtra(AppointmentDetailsActivity.EXTRA_SCHEDULE, appointment.scheduledDate)
            putExtra(AppointmentDetailsActivity.EXTRA_MECHANIC, appointment.mechanic?.fullName ?: "Unassigned")
        })
        AppTransitions.open(this)
    }
}
