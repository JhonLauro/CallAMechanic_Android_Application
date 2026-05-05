package com.jhonlauro.callamechanic.ui.appointment

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.R
import com.jhonlauro.callamechanic.databinding.ActivityAppointmentDetailsBinding
import com.jhonlauro.callamechanic.ui.common.AppTransitions

class AppointmentDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppointmentDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppointmentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
            AppTransitions.close(this)
        }

        val status = intent.getStringExtra(EXTRA_STATUS)
        val vehicle = intent.getStringExtra(EXTRA_VEHICLE) ?: "No vehicle info"
        val serviceType = intent.getStringExtra(EXTRA_SERVICE_TYPE) ?: "-"
        val schedule = intent.getStringExtra(EXTRA_SCHEDULE) ?: "No schedule"

        binding.tvJobId.text = "#${intent.getLongExtra(EXTRA_ID, -1L)}"
        binding.tvStatus.text = formatStatus(status)
        applyStatusStyle(status)
        binding.tvHeroSummary.text = "$serviceType • $schedule"
        binding.tvClient.text = intent.getStringExtra(EXTRA_CLIENT) ?: "Unknown client"
        binding.tvContact.text = intent.getStringExtra(EXTRA_CONTACT) ?: "No contact"
        binding.tvVehicle.text = vehicle
        binding.tvServiceType.text = serviceType
        binding.tvProblem.text = intent.getStringExtra(EXTRA_PROBLEM) ?: "No problem description"
        binding.tvSchedule.text = schedule
        binding.tvMechanic.text = intent.getStringExtra(EXTRA_MECHANIC) ?: "Unassigned"
    }

    private fun formatStatus(status: String?): String {
        return when (status) {
            "IN_PROGRESS" -> "In Progress"
            "FINISHED", "COMPLETED" -> "Finished"
            "CANCELLED" -> "Cancelled"
            else -> "Pending"
        }
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
    }
}
