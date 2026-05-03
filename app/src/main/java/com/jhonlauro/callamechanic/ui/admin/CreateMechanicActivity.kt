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

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || mechanicId.isEmpty()) {
            binding.tvCreateMechanicError.text = "All fields are required"
            binding.tvCreateMechanicError.visibility = View.VISIBLE
            return
        }

        if (!email.contains("@")) {
            binding.tvCreateMechanicError.text = "Invalid email"
            binding.tvCreateMechanicError.visibility = View.VISIBLE
            return
        }

        if (password.length < 8) {
            binding.tvCreateMechanicError.text = "Password must be at least 8 characters"
            binding.tvCreateMechanicError.visibility = View.VISIBLE
            return
        }

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            binding.tvCreateMechanicError.text = "No active session found"
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
                        binding.tvCreateMechanicError.text =
                            response.errorBody()?.string() ?: "Failed to create mechanic"
                        binding.tvCreateMechanicError.visibility = View.VISIBLE
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<CreateMechanicResponse>>,
                    t: Throwable
                ) {
                    binding.progressBarCreateMechanic.visibility = View.GONE
                    binding.btnCreateMechanicSubmit.isEnabled = true
                    binding.tvCreateMechanicError.text = t.message ?: "Something went wrong"
                    binding.tvCreateMechanicError.visibility = View.VISIBLE
                }
            })
    }
}
