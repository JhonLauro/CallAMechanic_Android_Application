package com.jhonlauro.callamechanic.ui.appointment

import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.R
import com.jhonlauro.callamechanic.data.model.AdminUser
import com.jhonlauro.callamechanic.data.model.AdminUserListData
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.repository.AdminRepository
import com.jhonlauro.callamechanic.databinding.ActivityAppointmentDetailsBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.common.AppTransitions
import com.jhonlauro.callamechanic.ui.common.FriendlyError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AppointmentDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppointmentDetailsBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adminRepository: AdminRepository
    private var mechanics: List<AdminUser> = emptyList()
    private var selectedMechanicId: Long = -1L
    private var currentStatus: String? = null
    private var appointmentId: Long = -1L
    private var adminActionInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppointmentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)
        adminRepository = AdminRepository()

        binding.btnBack.setOnClickListener {
            finish()
            AppTransitions.close(this)
        }

        appointmentId = intent.getLongExtra(EXTRA_ID, -1L)
        currentStatus = intent.getStringExtra(EXTRA_STATUS)
        val vehicle = intent.getStringExtra(EXTRA_VEHICLE) ?: "No vehicle info"
        val serviceType = intent.getStringExtra(EXTRA_SERVICE_TYPE) ?: "-"
        val schedule = intent.getStringExtra(EXTRA_SCHEDULE) ?: "No schedule"
        selectedMechanicId = intent.getLongExtra(EXTRA_MECHANIC_ID, -1L)

        binding.tvJobId.text = "#$appointmentId"
        renderStatus(currentStatus)
        binding.tvHeroSummary.text = "$serviceType • $schedule"
        binding.tvClient.text = intent.getStringExtra(EXTRA_CLIENT) ?: "Unknown client"
        binding.tvContact.text = intent.getStringExtra(EXTRA_CONTACT) ?: "No contact"
        binding.tvVehicle.text = vehicle
        binding.tvServiceType.text = serviceType
        binding.tvProblem.text = intent.getStringExtra(EXTRA_PROBLEM) ?: "No problem description"
        binding.tvSchedule.text = schedule
        binding.tvMechanic.text = intent.getStringExtra(EXTRA_MECHANIC) ?: "Unassigned"

        if (sessionManager.getRole() == "ADMIN") {
            binding.adminControls.visibility = View.VISIBLE
            setupAdminActions()
            loadMechanics()
        }
    }

    private fun setupAdminActions() {
        binding.mechanicPicker.setOnClickListener {
            if (mechanics.isEmpty()) {
                showMessage("No mechanics available yet.")
            } else {
                showMechanicMenu()
            }
        }

        binding.btnAssignMechanic.setOnClickListener {
            val token = sessionManager.getToken()
            val mechanic = mechanics.firstOrNull { it.id == selectedMechanicId }

            if (token.isNullOrBlank()) {
                showMessage("Session expired. Please sign in again.")
                return@setOnClickListener
            }
            if (appointmentId <= 0L) {
                showMessage("Appointment details are incomplete. Please go back and try again.")
                return@setOnClickListener
            }
            if (mechanic == null) {
                showMessage("Please select a mechanic first.")
                return@setOnClickListener
            }

            setAdminControlsEnabled(false)
            adminRepository.assignMechanic(token, appointmentId, mechanic.id)
                .enqueue(object : Callback<ApiMessageResponse<Appointment>> {
                    override fun onResponse(
                        call: Call<ApiMessageResponse<Appointment>>,
                        response: Response<ApiMessageResponse<Appointment>>
                    ) {
                        setAdminControlsEnabled(true)
                        if (response.isSuccessful && response.body()?.success == true) {
                            val updated = response.body()?.data
                            selectedMechanicId = updated?.mechanic?.id ?: mechanic.id
                            binding.tvMechanic.text = updated?.mechanic?.fullName ?: mechanic.fullName ?: "Assigned"
                            preselectAssignedMechanic()
                            showMessage("Mechanic assigned successfully.")
                        } else {
                            showMessage(FriendlyError.fromResponse(response, "Failed to assign mechanic. Please try again."))
                        }
                    }

                    override fun onFailure(call: Call<ApiMessageResponse<Appointment>>, t: Throwable) {
                        setAdminControlsEnabled(true)
                        showMessage(FriendlyError.fromThrowable(t, "Failed to assign mechanic. Please try again."))
                    }
                })
        }

        binding.btnStatusPending.setOnClickListener { updateStatus("PENDING") }
        binding.btnStatusProgress.setOnClickListener { updateStatus("IN_PROGRESS") }
        binding.btnStatusFinished.setOnClickListener { updateStatus("FINISHED") }
        renderStatusButtons()
    }

    private fun loadMechanics() {
        val token = sessionManager.getToken()
        if (token.isNullOrBlank()) return

        adminRepository.getUsers(token).enqueue(object : Callback<ApiMessageResponse<AdminUserListData>> {
            override fun onResponse(
                call: Call<ApiMessageResponse<AdminUserListData>>,
                response: Response<ApiMessageResponse<AdminUserListData>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    mechanics = response.body()?.data?.users
                        ?.filter { it.role == "MECHANIC" }
                        ?.sortedBy { it.fullName ?: "" }
                        ?: emptyList()
                    renderMechanicPicker()
                } else {
                    showMessage(FriendlyError.fromResponse(response, "Failed to load mechanics."))
                }
            }

            override fun onFailure(call: Call<ApiMessageResponse<AdminUserListData>>, t: Throwable) {
                showMessage(FriendlyError.fromThrowable(t, "Failed to load mechanics."))
            }
        })
    }

    private fun renderMechanicPicker() {
        preselectAssignedMechanic()
        binding.tvSelectedMechanic.text = mechanics
            .firstOrNull { it.id == selectedMechanicId }
            ?.let { mechanicLabel(it) }
            ?: "Select a mechanic..."
        binding.btnAssignMechanic.text =
            if (selectedMechanicId > 0L) "Reassign Mechanic" else "Assign Mechanic"
    }

    private fun preselectAssignedMechanic() {
        if (selectedMechanicId <= 0L || mechanics.isEmpty()) return
        if (mechanics.none { it.id == selectedMechanicId }) {
            selectedMechanicId = -1L
        }
    }

    private fun updateStatus(status: String) {
        if (currentStatus == status || adminActionInFlight) return
        val token = sessionManager.getToken()
        if (token.isNullOrBlank()) {
            showMessage("Session expired. Please sign in again.")
            return
        }

        setAdminControlsEnabled(false)
        adminRepository.updateAppointmentStatus(token, appointmentId, status)
            .enqueue(object : Callback<ApiMessageResponse<Appointment>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<Appointment>>,
                    response: Response<ApiMessageResponse<Appointment>>
                ) {
                    setAdminControlsEnabled(true)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val updatedStatus = response.body()?.data?.status ?: status
                        currentStatus = updatedStatus
                        renderStatus(updatedStatus)
                        renderStatusButtons()
                        showMessage("Appointment status updated.")
                    } else {
                        showMessage(FriendlyError.fromResponse(response, "Failed to update status. Please try again."))
                    }
                }

                override fun onFailure(call: Call<ApiMessageResponse<Appointment>>, t: Throwable) {
                    setAdminControlsEnabled(true)
                    showMessage(FriendlyError.fromThrowable(t, "Failed to update status. Please try again."))
                }
            })
    }

    private fun setAdminControlsEnabled(enabled: Boolean) {
        adminActionInFlight = !enabled
        binding.btnAssignMechanic.alpha = if (enabled) 1f else 0.65f
        binding.mechanicPicker.isEnabled = enabled
        binding.btnAssignMechanic.isEnabled = enabled
        renderStatusButtons()
    }

    private fun renderStatus(status: String?) {
        binding.tvStatus.text = formatStatus(status)
        applyStatusStyle(status)
    }

    private fun renderStatusButtons() {
        renderStatusButton(binding.btnStatusPending, currentStatus == "PENDING")
        renderStatusButton(binding.btnStatusProgress, currentStatus == "IN_PROGRESS")
        renderStatusButton(binding.btnStatusFinished, isFinishedStatus(currentStatus))
    }

    private fun renderStatusButton(button: TextView, selected: Boolean) {
        button.setBackgroundResource(
            if (selected) R.drawable.bg_admin_status_active else R.drawable.bg_admin_status_idle
        )
        button.setTextColor(getColor(if (selected) R.color.cam_on_primary else R.color.cam_primary))
        button.alpha = if (adminActionInFlight) 0.65f else 1f
        button.isEnabled = !adminActionInFlight
    }

    private fun showMechanicMenu() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_admin_dropdown)
            elevation = dp(10).toFloat()
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        mechanics.forEach { mechanic ->
            container.addView(TextView(this).apply {
                text = mechanicLabel(mechanic)
                setTextColor(getColor(R.color.cam_text))
                textSize = 14f
                minHeight = dp(46)
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(14), 0, dp(14), 0)
                if (mechanic.id == selectedMechanicId) {
                    setBackgroundResource(R.drawable.bg_admin_dropdown_item_selected)
                    setTextColor(getColor(R.color.cam_primary))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
            })
        }

        val popup = PopupWindow(
            container,
            binding.mechanicPicker.width,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            elevation = dp(12).toFloat()
        }

        for (index in 0 until container.childCount) {
            container.getChildAt(index).setOnClickListener {
                val mechanic = mechanics[index]
                selectedMechanicId = mechanic.id
                binding.tvSelectedMechanic.text = mechanicLabel(mechanic)
                binding.btnAssignMechanic.text = "Reassign Mechanic"
                popup.dismiss()
            }
        }

        popup.showAsDropDown(binding.mechanicPicker, 0, dp(6))
    }

    private fun mechanicLabel(mechanic: AdminUser): String {
        val name = mechanic.fullName ?: "Mechanic"
        val code = mechanic.mechanicId ?: "No ID"
        return "$name ($code)"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun formatStatus(status: String?): String {
        return when (status) {
            "IN_PROGRESS" -> "In Progress"
            "FINISHED", "COMPLETED" -> "Finished"
            "CANCELLED" -> "Cancelled"
            else -> "Pending"
        }
    }

    private fun isFinishedStatus(status: String?): Boolean {
        return status == "FINISHED" || status == "COMPLETED"
    }

    private fun applyStatusStyle(status: String?) {
        when (status) {
            "IN_PROGRESS" -> {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_chip_progress)
                binding.tvStatus.setTextColor(getColor(R.color.cam_primary))
            }
            "FINISHED", "COMPLETED" -> {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_chip_finished)
                binding.tvStatus.setTextColor(getColor(R.color.cam_success))
            }
            "CANCELLED" -> {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_chip_cancelled)
                binding.tvStatus.setTextColor(getColor(R.color.cam_danger))
            }
            else -> {
                binding.tvStatus.setBackgroundResource(R.drawable.bg_chip_pending)
                binding.tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
            }
        }
    }

    companion object {
        const val EXTRA_ID = "appointment_id"
        const val EXTRA_STATUS = "appointment_status"
        const val EXTRA_CLIENT = "appointment_client"
        const val EXTRA_CONTACT = "appointment_contact"
        const val EXTRA_VEHICLE = "appointment_vehicle"
        const val EXTRA_SERVICE_TYPE = "appointment_service_type"
        const val EXTRA_PROBLEM = "appointment_problem"
        const val EXTRA_SCHEDULE = "appointment_schedule"
        const val EXTRA_MECHANIC = "appointment_mechanic"
        const val EXTRA_MECHANIC_ID = "appointment_mechanic_id"
    }
}
