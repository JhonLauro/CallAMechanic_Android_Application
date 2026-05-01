package com.jhonlauro.callamechanic.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.UpdateProfileRequest
import com.jhonlauro.callamechanic.data.model.User
import com.jhonlauro.callamechanic.data.repository.ProfileRepository
import com.jhonlauro.callamechanic.databinding.ActivityProfileBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.auth.LoginActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var profileRepository: ProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        profileRepository = ProfileRepository()

        renderProfile()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.btnChangePassword.setOnClickListener {
            Toast.makeText(this, "Change password screen is not implemented yet.", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun renderProfile() {
        val role = sessionManager.getRole()?.uppercase() ?: "USER"
        val accountId = when (role) {
            "ADMIN" -> sessionManager.getAdminId() ?: "Admin ID unavailable"
            "MECHANIC" -> sessionManager.getMechanicId() ?: "Mechanic ID unavailable"
            else -> "User ID: ${sessionManager.getUserId()}"
        }

        binding.tvProfileTitle.text = "My Profile"
        binding.tvProfileSubtitle.text = "Manage your account details and security settings"
        binding.tvFullName.text = sessionManager.getFullName() ?: "No name"
        binding.tvRole.text = role
        binding.tvAdminId.text = accountId
        binding.tvEmail.text = sessionManager.getEmail() ?: "No email"
        binding.tvContact.text = sessionManager.getPhoneNumber() ?: "No contact"
        binding.tvStatus.text = "ACTIVE"
    }

    private fun showEditProfileDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 12, 48, 0)
        }

        val fullNameInput = EditText(this).apply {
            hint = "Full name"
            setText(sessionManager.getFullName() ?: "")
        }
        val phoneInput = EditText(this).apply {
            hint = "Phone number"
            setText(sessionManager.getPhoneNumber() ?: "")
        }

        container.addView(fullNameInput)
        container.addView(phoneInput)

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                updateProfile(
                    fullNameInput.text.toString().trim(),
                    phoneInput.text.toString().trim()
                )
            }
            .show()
    }

    private fun updateProfile(fullName: String, phoneNumber: String) {
        if (fullName.isBlank()) {
            Toast.makeText(this, "Full name is required.", Toast.LENGTH_SHORT).show()
            return
        }

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "No active session found.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnEditProfile.visibility = View.INVISIBLE
        profileRepository.updateProfile(token, UpdateProfileRequest(fullName, phoneNumber))
            .enqueue(object : Callback<ApiMessageResponse<User>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<User>>,
                    response: Response<ApiMessageResponse<User>>
                ) {
                    binding.btnEditProfile.visibility = View.VISIBLE
                    if (response.isSuccessful && response.body()?.success == true) {
                        val updatedUser = response.body()?.data
                        val updatedName = updatedUser?.fullName ?: fullName
                        val updatedPhone = updatedUser?.phoneNumber ?: phoneNumber
                        sessionManager.updateProfileInfo(updatedName, updatedPhone)
                        renderProfile()
                        Toast.makeText(this@ProfileActivity, "Profile updated.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Failed to update profile.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiMessageResponse<User>>, t: Throwable) {
                    binding.btnEditProfile.visibility = View.VISIBLE
                    Toast.makeText(this@ProfileActivity, t.message ?: "Failed to update profile.", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
