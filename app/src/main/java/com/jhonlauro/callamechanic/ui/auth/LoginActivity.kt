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
import com.jhonlauro.callamechanic.data.model.LoginRequest
import com.jhonlauro.callamechanic.data.model.LoginResponse
import com.jhonlauro.callamechanic.data.repository.AuthRepository
import com.jhonlauro.callamechanic.databinding.ActivityLoginBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.admin.AdminDashboardActivity
import com.jhonlauro.callamechanic.ui.client.ClientDashboardActivity
import com.jhonlauro.callamechanic.ui.common.AppTransitions
import com.jhonlauro.callamechanic.ui.common.FormScrollHelper
import com.jhonlauro.callamechanic.ui.common.FriendlyError
import com.jhonlauro.callamechanic.ui.common.clearFieldErrorOnInput
import com.jhonlauro.callamechanic.ui.common.clearFieldErrors
import com.jhonlauro.callamechanic.ui.common.showFieldError
import com.jhonlauro.callamechanic.ui.mechanic.MechanicDashboardActivity
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
        FormScrollHelper.enable(binding.root)
        clearFieldErrorOnInput(binding.etIdentifier, binding.etPassword)
        setupPasswordToggle(binding.etPassword, binding.btnTogglePassword)

        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            AppTransitions.open(this)
        }

        binding.btnBackHome.setOnClickListener {
            finish()
            AppTransitions.close(this)
        }
    }

    private fun setupPasswordToggle(field: EditText, button: ImageButton) {
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
            button.contentDescription = if (visible) "Hide password" else "Show password"
        }
    }

    private fun loginUser() {
        val identifier = binding.etIdentifier.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.tvError.visibility = View.GONE
        clearFieldErrors(binding.etIdentifier, binding.etPassword)

        if (identifier.isEmpty()) {
            showFieldError(binding.etIdentifier, "Email or ID is required.")
            return
        }

        if (password.isEmpty()) {
            showFieldError(binding.etPassword, "Password is required.")
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
                        token = loginData.token,
                        role = loginData.user.role,
                        userId = loginData.user.id,
                        fullName = loginData.user.fullName,
                        email = loginData.user.email,
                        adminId = loginData.user.adminId,
                        mechanicId = loginData.user.mechanicId,
                        phoneNumber = loginData.user.phoneNumber,
                        photoUrl = loginData.user.photoUrl
                    )

                    val nextScreen = when (loginData.user.role.uppercase()) {
                        "ADMIN" -> Intent(this@LoginActivity, AdminDashboardActivity::class.java)
                        "MECHANIC" -> Intent(this@LoginActivity, MechanicDashboardActivity::class.java)
                        else -> Intent(this@LoginActivity, ClientDashboardActivity::class.java)
                    }
                    startActivity(nextScreen)
                    AppTransitions.open(this@LoginActivity)
                    finish()
                } else {
                    val message = FriendlyError.fromResponse(response, FriendlyError.invalidLogin())
                    if (message == FriendlyError.invalidLogin()) {
                        showFieldError(binding.etIdentifier, "Check the email, mechanic ID, or admin ID you entered.")
                        showFieldError(binding.etPassword, "Check your password and try again.")
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
                binding.btnLogin.isEnabled = true
                binding.tvError.text = FriendlyError.fromThrowable(t, "Request failed. Please try again.")
                binding.tvError.visibility = View.VISIBLE
            }
        })
    }
}
