package com.jhonlauro.callamechanic.ui.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.databinding.ActivityProfileBinding
import com.jhonlauro.callamechanic.session.SessionManager

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
    }
}