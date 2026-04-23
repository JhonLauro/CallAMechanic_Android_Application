package com.jhonlauro.callamechanic

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.admin.AdminDashboardActivity
import com.jhonlauro.callamechanic.ui.auth.LoginActivity
import com.jhonlauro.callamechanic.ui.client.ClientDashboardActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)

        val nextIntent = if (sessionManager.isLoggedIn()) {
            when (sessionManager.getRole()?.uppercase()) {
                "ADMIN" -> Intent(this, AdminDashboardActivity::class.java)
                else -> Intent(this, ClientDashboardActivity::class.java)
            }
        } else {
            Intent(this, LoginActivity::class.java)
        }

        startActivity(nextIntent)
        finish()
    }
}