package com.jhonlauro.callamechanic.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.R
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.LoginResponse
import com.jhonlauro.callamechanic.data.model.RegisterRequest
import com.jhonlauro.callamechanic.data.repository.AuthRepository
import com.jhonlauro.callamechanic.databinding.ActivityRegisterBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.admin.AdminDashboardActivity
import com.jhonlauro.callamechanic.ui.client.ClientDashboardActivity
import com.jhonlauro.callamechanic.ui.common.AppTransitions
import com.jhonlauro.callamechanic.ui.common.FormScrollHelper
import com.jhonlauro.callamechanic.ui.common.FriendlyError
import com.jhonlauro.callamechanic.ui.common.clearFieldErrorOnInput
import com.jhonlauro.callamechanic.ui.common.clearFieldErrors
import com.jhonlauro.callamechanic.ui.common.showFieldError
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
        FormScrollHelper.enable(binding.root)
        clearFieldErrorOnInput(binding.etFullName, binding.etEmail, binding.etPhoneNumber, binding.etPassword, binding.etConfirmPassword)
        setupPasswordToggle(binding.etPassword, binding.btnTogglePassword, "password")
        setupPasswordToggle(binding.etConfirmPassword, binding.btnToggleConfirmPassword, "confirm password")

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvGoLogin.setOnClickListener {
            finish()
            AppTransitions.close(this)
        }

        binding.btnBackHome.setOnClickListener {
            finish()
            AppTransitions.close(this)
        }
    }

    private fun setupPasswordToggle(field: EditText, button: ImageButton, label: String) {
        var visible = false
        button.setOnClickListener {
            visible = !visible
            field.transformationMethod = if (visible) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            field.setSelection(field.text?.length ?: 0)
            button.setImageResource(if (visible) R.drawable.ic_eye else R.drawable.ic_eye_off)
            button.contentDescription = if (visible) "Hide $label" else "Show $label"
        }
    }

    private fun registerUser() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        binding.tvError.visibility = View.GONE
        clearFieldErrors(binding.etFullName, binding.etEmail, binding.etPhoneNumber, binding.etPassword, binding.etConfirmPassword)

        if (fullName.isEmpty()) {
            showFieldError(binding.etFullName, "Full name is required.")
            return
        }

        if (email.isEmpty()) {
            showFieldError(binding.etEmail, "Email is required.")
            return
        }

        if (phoneNumber.isEmpty()) {
            showFieldError(binding.etPhoneNumber, "Phone number is required.")
            return
        }

        if (password.isEmpty()) {
            showFieldError(binding.etPassword, "Password is required.")
            return
        }

        if (confirmPassword.isEmpty()) {
            showFieldError(binding.etConfirmPassword, "Please confirm your password.")
            return
        }

        if (!email.contains("@")) {
            showFieldError(binding.etEmail, "Please enter a valid email address.")
            return
        }

        if (password.length < 8) {
            showFieldError(binding.etPassword, "Password must be at least 8 characters.")
            return
        }

        if (password != confirmPassword) {
            showFieldError(binding.etConfirmPassword, "Passwords do not match.")
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        val request = RegisterRequest(
            fullName = fullName,
            email = email,
            phoneNumber = phoneNumber,
            password = password
        )

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
                        token = registerData.token,
                        role = registerData.user.role,
                        userId = registerData.user.id,
                        fullName = registerData.user.fullName,
                        email = registerData.user.email,
                        adminId = registerData.user.adminId,
                        phoneNumber = registerData.user.phoneNumber,
                        photoUrl = registerData.user.photoUrl
                    )

                    if (registerData.user.role.uppercase() == "ADMIN") {
                        startActivity(Intent(this@RegisterActivity, AdminDashboardActivity::class.java))
                    } else {
                        startActivity(Intent(this@RegisterActivity, ClientDashboardActivity::class.java))
                    }
                    AppTransitions.open(this@RegisterActivity)
                    finishAffinity()
                } else {
                    val message = FriendlyError.fromResponse(response, "Validation failed. Please review your input.")
                    if (message.contains("email", ignoreCase = true) && message.contains("already", ignoreCase = true)) {
                        showFieldError(binding.etEmail, "This email is already registered. Use another email or sign in.")
                    } else {
                        binding.tvError.text = message
                        binding.tvError.visibility = View.VISIBLE
                    }
                }
            }

            override fun onFailure(
                call: Call<ApiMessageResponse<LoginResponse>>,
                t: Throwable
            ) {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
                binding.tvError.text = FriendlyError.fromThrowable(t, "Request failed. Please try again.")
                binding.tvError.visibility = View.VISIBLE
            }
        })
    }
}
