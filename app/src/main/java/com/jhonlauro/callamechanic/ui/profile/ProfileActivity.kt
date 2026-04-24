package com.jhonlauro.callamechanic.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.databinding.ActivityProfileBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.auth.LoginActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        binding.tvFullName.text = sessionManager.getFullName() ?: "No name"
        binding.tvRole.text = sessionManager.getRole() ?: "No role"
        binding.tvAdminId.text = sessionManager.getAdminId() ?: "No admin ID"
        binding.tvEmail.text = sessionManager.getEmail() ?: "No email"
        binding.tvContact.text = sessionManager.getPhoneNumber() ?: "No contact"
        binding.tvStatus.text = "ACTIVE"

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }
}