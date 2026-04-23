package com.jhonlauro.callamechanic.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.LoginResponse
import com.jhonlauro.callamechanic.data.model.RegisterRequest
import com.jhonlauro.callamechanic.data.repository.AuthRepository
import com.jhonlauro.callamechanic.databinding.ActivityRegisterBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.admin.AdminDashboardActivity
import com.jhonlauro.callamechanic.ui.client.ClientDashboardActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository()
        sessionManager = SessionManager(this)

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.tvError.visibility = View.GONE

        if (fullName.isEmpty() || email.isEmpty() || phoneNumber.isEmpty() || password.isEmpty()) {
            binding.tvError.text = "All fields are required"
            binding.tvError.visibility = View.VISIBLE
            return
        }

        if (!email.contains("@")) {
            binding.tvError.text = "Invalid email"
            binding.tvError.visibility = View.VISIBLE
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        val request = RegisterRequest(fullName, email, phoneNumber, password)

        authRepository.register(request).enqueue(object : Callback<ApiMessageResponse<LoginResponse>> {
            override fun onResponse(
                call: Call<ApiMessageResponse<LoginResponse>>,
                response: Response<ApiMessageResponse<LoginResponse>>
            ) {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                    val registerData = response.body()!!.data!!

                    sessionManager.saveSession(
                        registerData.token,
                        registerData.user.role,
                        registerData.user.id,
                        registerData.user.fullName
                    )

                    if (registerData.user.role.uppercase() == "ADMIN") {
                        startActivity(Intent(this@RegisterActivity, AdminDashboardActivity::class.java))
                    } else {
                        startActivity(Intent(this@RegisterActivity, ClientDashboardActivity::class.java))
                    }
                    finishAffinity()
                } else {
                    binding.tvError.text = "Registration failed"
                    binding.tvError.visibility = View.VISIBLE
                }
            }

            override fun onFailure(
                call: Call<ApiMessageResponse<LoginResponse>>,
                t: Throwable
            ) {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
                binding.tvError.text = t.message ?: "Something went wrong"
                binding.tvError.visibility = View.VISIBLE
            }
        })
    }
}