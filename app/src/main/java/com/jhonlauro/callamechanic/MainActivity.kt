package com.jhonlauro.callamechanic

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.databinding.ActivityMainBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.admin.AdminDashboardActivity
import com.jhonlauro.callamechanic.ui.auth.LoginActivity
import com.jhonlauro.callamechanic.ui.auth.RegisterActivity
import com.jhonlauro.callamechanic.ui.client.ClientDashboardActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            val nextIntent = when (sessionManager.getRole()?.uppercase()) {
                "ADMIN" -> Intent(this, AdminDashboardActivity::class.java)
                else -> Intent(this, ClientDashboardActivity::class.java)
            }
            startActivity(nextIntent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnGetStarted.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnBookService.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}