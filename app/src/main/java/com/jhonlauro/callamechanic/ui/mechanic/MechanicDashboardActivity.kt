package com.jhonlauro.callamechanic.ui.mechanic

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.model.User
import com.jhonlauro.callamechanic.data.repository.AppointmentRepository
import com.jhonlauro.callamechanic.data.repository.ProfileRepository
import com.jhonlauro.callamechanic.databinding.ActivityMechanicDashboardBinding
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

class MechanicDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMechanicDashboardBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var appointmentRepository: AppointmentRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var newRequestsAdapter: MechanicAppointmentAdapter
    private lateinit var activeJobsAdapter: MechanicAppointmentAdapter
    private lateinit var finishedJobsAdapter: MechanicAppointmentAdapter
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadAppointments(silent = true)
            refreshHandler.postDelayed(this, 30000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMechanicDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        appointmentRepository = AppointmentRepository()
        profileRepository = ProfileRepository()

        newRequestsAdapter = createAdapter(MechanicAppointmentAdapter.Mode.NEW_REQUESTS)
        activeJobsAdapter = createAdapter(MechanicAppointmentAdapter.Mode.ACTIVE_JOBS)
        finishedJobsAdapter = createAdapter(MechanicAppointmentAdapter.Mode.FINISHED_JOBS)

        binding.rvNewRequests.layoutManager = LinearLayoutManager(this)
        binding.rvNewRequests.adapter = newRequestsAdapter
        binding.rvNewRequests.setHasFixedSize(false)
        binding.rvActiveJobs.layoutManager = LinearLayoutManager(this)
        binding.rvActiveJobs.adapter = activeJobsAdapter
        binding.rvActiveJobs.setHasFixedSize(false)
        binding.rvFinishedJobs.layoutManager = LinearLayoutManager(this)
        binding.rvFinishedJobs.adapter = finishedJobsAdapter
        binding.rvFinishedJobs.setHasFixedSize(false)

        binding.tvMechanicWelcome.text = "Welcome, ${sessionManager.getFullName() ?: "Mechanic"}"
        binding.tvMechanicRole.text = sessionManager.getRole() ?: "MECHANIC"
        renderProfilePhoto()

        binding.btnMechanicProfile.setOnClickListener {
            showProfileMenu()
        }

        binding.btnRefreshMechanic.visibility = View.GONE

        binding.btnLogoutMechanic.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        renderProfilePhoto()
        loadProfile()
        loadAppointments()
    }

    private fun renderProfilePhoto() {
        ProfilePhotoRenderer.show(binding.btnMechanicProfile, sessionManager.getPhotoUrl())
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
                        binding.tvMechanicWelcome.text = "Welcome, ${user.fullName}"
                        renderProfilePhoto()
                    }
                }
            }

            override fun onFailure(call: Call<ApiMessageResponse<User>>, t: Throwable) = Unit
        })
    }

    override fun onStart() {
        super.onStart()
        refreshHandler.postDelayed(refreshRunnable, 30000)
    }

    override fun onStop() {
        refreshHandler.removeCallbacks(refreshRunnable)
        super.onStop()
    }

    private fun createAdapter(mode: MechanicAppointmentAdapter.Mode): MechanicAppointmentAdapter {
        return MechanicAppointmentAdapter(
            mode = mode,
            onClaim = { appointment -> claimAppointment(appointment.id) },
            onStart = { appointment -> updateAppointmentStatus(appointment.id, "IN_PROGRESS") },
            onFinish = { appointment -> updateAppointmentStatus(appointment.id, "FINISHED") },
            onView = { appointment -> openAppointmentDetails(appointment) }
        )
    }

    private fun loadAppointments(silent: Boolean = false) {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            if (!silent) showEmptyMessage("No active session found")
            return
        }

        if (!silent) {
            binding.progressBarMechanic.visibility = View.VISIBLE
            binding.tvMechanicEmptyState.visibility = View.GONE
        }

        appointmentRepository.getAppointments(token)
            .enqueue(object : Callback<ApiMessageResponse<List<Appointment>>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<List<Appointment>>>,
                    response: Response<ApiMessageResponse<List<Appointment>>>
                ) {
                    if (!silent) binding.progressBarMechanic.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        updateDashboard(response.body()?.data ?: emptyList())
                    } else if (!silent) {
                        showEmptyMessage("Failed to load mechanic dashboard")
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<List<Appointment>>>,
                    t: Throwable
                ) {
                    if (!silent) {
                        binding.progressBarMechanic.visibility = View.GONE
                        showEmptyMessage(t.message ?: "Something went wrong")
                    }
                }
            })
    }

    private fun updateDashboard(appointments: List<Appointment>) {
        val newRequests = appointments
            .filter { isUnassignedPending(it) }
            .sortedWith(newestAppointmentFirst())
        val assignedJobs = appointments.filter { isAssignedToCurrentMechanic(it) }
        val activeJobs = assignedJobs.filter {
            val status = normalizeStatus(it.status)
            status == "PENDING" || status == "IN_PROGRESS"
        }.sortedWith(newestAppointmentFirst())
        val finishedJobs = assignedJobs
            .filter { normalizeStatus(it.status) == "FINISHED" }
            .sortedWith(newestAppointmentFirst())
        val inProgressJobs = assignedJobs.filter { normalizeStatus(it.status) == "IN_PROGRESS" }

        newRequestsAdapter.updateData(newRequests)
        activeJobsAdapter.updateData(activeJobs)
        finishedJobsAdapter.updateData(finishedJobs)

        binding.tvNewJobs.text = newRequests.size.toString()
        binding.tvMyActiveJobs.text = activeJobs.size.toString()
        binding.tvMechanicInProgress.text = inProgressJobs.size.toString()
        binding.tvMechanicFinished.text = finishedJobs.size.toString()

        binding.tvNewRequestsCount.text = newRequests.size.toString()
        binding.tvActiveJobsCount.text = activeJobs.size.toString()
        binding.tvFinishedJobsCount.text = finishedJobs.size.toString()

        binding.tvNewRequestsEmpty.visibility = if (newRequests.isEmpty()) View.VISIBLE else View.GONE
        binding.tvActiveJobsEmpty.visibility = if (activeJobs.isEmpty()) View.VISIBLE else View.GONE
        binding.tvFinishedJobsEmpty.visibility = if (finishedJobs.isEmpty()) View.VISIBLE else View.GONE

        binding.tvMechanicEmptyState.visibility = View.GONE
    }

    private fun claimAppointment(appointmentId: Long) {
        val token = sessionManager.getToken() ?: return
        setBusy(true)
        appointmentRepository.claimAppointment(token, appointmentId)
            .enqueue(createActionCallback("Job claimed successfully."))
    }

    private fun updateAppointmentStatus(appointmentId: Long, status: String) {
        val token = sessionManager.getToken() ?: return
        setBusy(true)
        appointmentRepository.updateAppointmentStatus(token, appointmentId, status)
            .enqueue(createActionCallback("Job updated successfully."))
    }

    private fun createActionCallback(successMessage: String): Callback<ApiMessageResponse<Appointment>> {
        return object : Callback<ApiMessageResponse<Appointment>> {
            override fun onResponse(
                call: Call<ApiMessageResponse<Appointment>>,
                response: Response<ApiMessageResponse<Appointment>>
            ) {
                setBusy(false)
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@MechanicDashboardActivity, successMessage, Toast.LENGTH_SHORT).show()
                    loadAppointments()
                } else {
                    Toast.makeText(this@MechanicDashboardActivity, "Action failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiMessageResponse<Appointment>>, t: Throwable) {
                setBusy(false)
                Toast.makeText(this@MechanicDashboardActivity, t.message ?: "Action failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setBusy(isBusy: Boolean) {
        binding.progressBarMechanic.visibility = if (isBusy) View.VISIBLE else View.GONE
    }

    private fun showEmptyMessage(message: String) {
        binding.tvMechanicEmptyState.text = message
        binding.tvMechanicEmptyState.visibility = View.VISIBLE
    }

    private fun isUnassignedPending(appointment: Appointment): Boolean {
        return normalizeStatus(appointment.status) == "PENDING" && appointment.mechanic == null
    }

    private fun isAssignedToCurrentMechanic(appointment: Appointment): Boolean {
        val currentUserId = sessionManager.getUserId()
        val currentName = sessionManager.getFullName()?.lowercase()
        val mechanic = appointment.mechanic ?: return false

        return mechanic.id == currentUserId ||
            (!currentName.isNullOrBlank() && mechanic.fullName?.lowercase() == currentName)
    }

    private fun normalizeStatus(status: String?): String {
        return if (status == "COMPLETED") "FINISHED" else status ?: "PENDING"
    }

    private fun newestAppointmentFirst(): Comparator<Appointment> {
        return compareByDescending<Appointment> { it.scheduledDate ?: "" }
            .thenByDescending { it.id }
    }

    private fun showProfileMenu() {
        ProfileDropdown.show(
            anchor = binding.btnMechanicProfile,
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
            putExtra(AppointmentDetailsActivity.EXTRA_MECHANIC, appointment.mechanic?.fullName ?: sessionManager.getFullName())
        })
        AppTransitions.open(this)
    }
}
