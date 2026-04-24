package com.jhonlauro.callamechanic.ui.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.repository.AppointmentRepository
import com.jhonlauro.callamechanic.databinding.ActivityClientDashboardBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.auth.LoginActivity
import com.jhonlauro.callamechanic.ui.profile.ProfileActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ClientDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientDashboardBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var appointmentRepository: AppointmentRepository
    private lateinit var adapter: AppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        appointmentRepository = AppointmentRepository()

        adapter = AppointmentAdapter(emptyList())

        binding.rvAppointments.layoutManager = LinearLayoutManager(this)
        binding.rvAppointments.adapter = adapter

        binding.tvWelcome.text = "Welcome, ${sessionManager.getFullName() ?: "Client"}"

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        binding.btnBookAppointment.setOnClickListener {
            startActivity(Intent(this, BookAppointmentActivity::class.java))
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
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
                        val appointments = response.body()?.data ?: emptyList()
                        adapter.updateData(appointments)

                        val activeCount = appointments.count {
                            it.status == "PENDING" || it.status == "IN_PROGRESS"
                        }

                        val completedCount = appointments.count {
                            it.status == "COMPLETED"
                        }

                        binding.tvActiveServices.text = activeCount.toString()
                        binding.tvCompletedServices.text = completedCount.toString()
                        binding.tvRegisteredVehicles.text = "0"

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
}