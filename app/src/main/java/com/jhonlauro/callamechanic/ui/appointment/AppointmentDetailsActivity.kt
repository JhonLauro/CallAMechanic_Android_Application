package com.jhonlauro.callamechanic.ui.appointment

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

        binding.tvJobId.text = "#${intent.getLongExtra(EXTRA_ID, -1L)}"
        binding.tvStatus.text = formatStatus(intent.getStringExtra(EXTRA_STATUS))
        binding.tvClient.text = intent.getStringExtra(EXTRA_CLIENT) ?: "Unknown client"
        binding.tvContact.text = intent.getStringExtra(EXTRA_CONTACT) ?: "No contact"
        binding.tvVehicle.text = intent.getStringExtra(EXTRA_VEHICLE) ?: "No vehicle info"
        binding.tvServiceType.text = intent.getStringExtra(EXTRA_SERVICE_TYPE) ?: "-"
        binding.tvProblem.text = intent.getStringExtra(EXTRA_PROBLEM) ?: "No problem description"
        binding.tvSchedule.text = intent.getStringExtra(EXTRA_SCHEDULE) ?: "No schedule"
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
