package com.jhonlauro.callamechanic.ui.admin

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.R
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.CreateMechanicRequest
import com.jhonlauro.callamechanic.data.model.CreateMechanicResponse
import com.jhonlauro.callamechanic.data.repository.AdminRepository
import com.jhonlauro.callamechanic.databinding.ActivityCreateMechanicBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.common.AppTransitions
import com.jhonlauro.callamechanic.ui.common.FormScrollHelper
import com.jhonlauro.callamechanic.ui.common.FriendlyError
import com.jhonlauro.callamechanic.ui.common.clearFieldErrorOnInput
import com.jhonlauro.callamechanic.ui.common.clearFieldErrors
import com.jhonlauro.callamechanic.ui.common.showFieldError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateMechanicActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateMechanicBinding
    private lateinit var adminRepository: AdminRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateMechanicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adminRepository = AdminRepository()
        sessionManager = SessionManager(this)
        FormScrollHelper.enable(binding.root)
        clearFieldErrorOnInput(binding.etMechanicFullName, binding.etMechanicEmail, binding.etMechanicPhone, binding.etMechanicPassword, binding.etMechanicId)
        setupPasswordToggle(binding.etMechanicPassword, binding.btnToggleMechanicPassword)

        binding.btnCreateMechanicSubmit.setOnClickListener {
            createMechanic()
        }

        binding.btnCreateMechanicCancel.setOnClickListener {
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
            button.contentDescription = if (visible) "Hide mechanic password" else "Show mechanic password"
        }
    }

    private fun createMechanic() {
        val fullName = binding.etMechanicFullName.text.toString().trim()
        val email = binding.etMechanicEmail.text.toString().trim()
        val phone = binding.etMechanicPhone.text.toString().trim()
        val password = binding.etMechanicPassword.text.toString().trim()
        val mechanicId = binding.etMechanicId.text.toString().trim()

        binding.tvCreateMechanicError.visibility = View.GONE
        clearFieldErrors(binding.etMechanicFullName, binding.etMechanicEmail, binding.etMechanicPhone, binding.etMechanicPassword, binding.etMechanicId)

        if (fullName.isEmpty()) {
            showFieldError(binding.etMechanicFullName, "Full name is required.")
            return
        }

        if (email.isEmpty()) {
            showFieldError(binding.etMechanicEmail, "Email is required.")
            return
        }

        if (phone.isEmpty()) {
            showFieldError(binding.etMechanicPhone, "Phone number is required.")
            return
        }

        if (password.isEmpty()) {
            showFieldError(binding.etMechanicPassword, "Password is required.")
            return
        }

        if (mechanicId.isEmpty()) {
            showFieldError(binding.etMechanicId, "Mechanic ID is required.")
            return
        }

        if (!email.contains("@")) {
            showFieldError(binding.etMechanicEmail, "Invalid email format.")
            return
        }

        if (password.length < 8) {
            showFieldError(binding.etMechanicPassword, "Password must be at least 8 characters.")
            return
        }

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            binding.tvCreateMechanicError.text = "Session expired. Please sign in again."
            binding.tvCreateMechanicError.visibility = View.VISIBLE
            return
        }

        binding.progressBarCreateMechanic.visibility = View.VISIBLE
        binding.btnCreateMechanicSubmit.isEnabled = false

        val request = CreateMechanicRequest(
            fullName = fullName,
            email = email,
            phoneNumber = phone,
            password = password,
            mechanicId = mechanicId
        )

        adminRepository.createMechanic(token, request)
            .enqueue(object : Callback<ApiMessageResponse<CreateMechanicResponse>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<CreateMechanicResponse>>,
                    response: Response<ApiMessageResponse<CreateMechanicResponse>>
                ) {
                    binding.progressBarCreateMechanic.visibility = View.GONE
                    binding.btnCreateMechanicSubmit.isEnabled = true

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@CreateMechanicActivity,
                            response.body()?.data?.message ?: "Mechanic created",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                        AppTransitions.close(this@CreateMechanicActivity)
                    } else {
                        val message = FriendlyError.fromResponse(response, "Validation failed. Please review your input.")
                        when {
                            message.contains("email", ignoreCase = true) && message.contains("already", ignoreCase = true) -> {
                                showFieldError(binding.etMechanicEmail, "This email is already registered.")
                            }
                            message.contains("mechanic", ignoreCase = true) && message.contains("already", ignoreCase = true) -> {
                                showFieldError(binding.etMechanicId, "This mechanic ID is already in use.")
                            }
                            else -> {
                                binding.tvCreateMechanicError.text = message
                                binding.tvCreateMechanicError.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<CreateMechanicResponse>>,
                    t: Throwable
                ) {
                    binding.progressBarCreateMechanic.visibility = View.GONE
                    binding.btnCreateMechanicSubmit.isEnabled = true
                    binding.tvCreateMechanicError.text = FriendlyError.fromThrowable(t, "Request failed. Please try again.")
                    binding.tvCreateMechanicError.visibility = View.VISIBLE
                }
            })
    }
}
