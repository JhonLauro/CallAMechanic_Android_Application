package com.jhonlauro.callamechanic.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.LoginRequest
import com.jhonlauro.callamechanic.data.model.LoginResponse
import com.jhonlauro.callamechanic.data.repository.AuthRepository
import com.jhonlauro.callamechanic.databinding.ActivityLoginBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.admin.AdminDashboardActivity
import com.jhonlauro.callamechanic.ui.client.ClientDashboardActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository()
        sessionManager = SessionManager(this)

        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser() {
        val identifier = binding.etIdentifier.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.tvError.visibility = View.GONE

        if (identifier.isEmpty()) {
            binding.tvError.text = "Identifier is required"
            binding.tvError.visibility = View.VISIBLE
            return
        }

        if (password.isEmpty()) {
            binding.tvError.text = "Password is required"
            binding.tvError.visibility = View.VISIBLE
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        val request = LoginRequest(identifier, password)

        authRepository.login(request).enqueue(object : Callback<ApiMessageResponse<LoginResponse>> {
            override fun onResponse(
                call: Call<ApiMessageResponse<LoginResponse>>,
                response: Response<ApiMessageResponse<LoginResponse>>
            ) {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                    val loginData = response.body()!!.data!!

                    sessionManager.saveSession(
                        loginData.token,
                        loginData.user.role,
                        loginData.user.id,
                        loginData.user.fullName
                    )

                    if (loginData.user.role.uppercase() == "ADMIN") {
                        startActivity(Intent(this@LoginActivity, AdminDashboardActivity::class.java))
                    } else {
                        startActivity(Intent(this@LoginActivity, ClientDashboardActivity::class.java))
                    }
                    finish()
                } else {
                    binding.tvError.text = "Login failed"
                    binding.tvError.visibility = View.VISIBLE
                }
            }

            override fun onFailure(
                call: Call<ApiMessageResponse<LoginResponse>>,
                t: Throwable
            ) {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.tvError.text = t.message ?: "Something went wrong"
                binding.tvError.visibility = View.VISIBLE
            }
        })
    }
}