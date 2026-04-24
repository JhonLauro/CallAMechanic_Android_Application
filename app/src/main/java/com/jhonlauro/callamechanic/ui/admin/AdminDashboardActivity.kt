package com.jhonlauro.callamechanic.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.repository.AppointmentRepository
import com.jhonlauro.callamechanic.databinding.ActivityAdminDashboardBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.auth.LoginActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var appointmentRepository: AppointmentRepository
    private lateinit var adapter: AdminAppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        appointmentRepository = AppointmentRepository()
        adapter = AdminAppointmentAdapter(emptyList())

        binding.rvAdminAppointments.layoutManager = LinearLayoutManager(this)
        binding.rvAdminAppointments.adapter = adapter

        binding.tvAdminWelcome.text = "Welcome, ${sessionManager.getFullName() ?: "Admin"}"

        binding.btnUserRegistry.setOnClickListener {
            startActivity(Intent(this, UserRegistryActivity::class.java))
        }

        binding.btnCreateMechanic.setOnClickListener {
            startActivity(Intent(this, CreateMechanicActivity::class.java))
        }

        binding.btnRefreshAdmin.setOnClickListener {
            loadAppointments()
        }

        binding.btnLogoutAdmin.setOnClickListener {
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
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
                        adapter.updateData(appointments)

                        binding.tvTotalJobs.text = appointments.size.toString()
                        binding.tvPendingJobs.text = appointments.count { it.status == "PENDING" }.toString()
                        binding.tvInProgressJobs.text = appointments.count { it.status == "IN_PROGRESS" }.toString()
                        binding.tvCompletedJobs.text = appointments.count { it.status == "COMPLETED" }.toString()

                        if (appointments.isEmpty()) {
                            binding.tvEmptyStateAdmin.visibility = View.VISIBLE
                            binding.tvEmptyStateAdmin.text = "No appointments found"
                        } else {
                            binding.tvEmptyStateAdmin.visibility = View.GONE
                        }
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
}