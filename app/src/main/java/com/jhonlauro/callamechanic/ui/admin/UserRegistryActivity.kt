package com.jhonlauro.callamechanic.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jhonlauro.callamechanic.data.model.AdminUser
import com.jhonlauro.callamechanic.data.model.AdminUserListData
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.DeleteUserResponse
import com.jhonlauro.callamechanic.data.repository.AdminRepository
import com.jhonlauro.callamechanic.databinding.ActivityUserRegistryBinding
import com.jhonlauro.callamechanic.session.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserRegistryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserRegistryBinding
    private lateinit var adminRepository: AdminRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserRegistryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adminRepository = AdminRepository()
        sessionManager = SessionManager(this)

        adapter = UserAdapter(emptyList()) { user ->
            deleteUser(user)
        }

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        binding.btnBackRegistry.setOnClickListener {
            finish()
        }

        loadUsers()
    }

    private fun loadUsers() {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            binding.tvEmptyUsers.visibility = View.VISIBLE
            binding.tvEmptyUsers.text = "No active session found"
            return
        }

        binding.progressBarUsers.visibility = View.VISIBLE
        binding.tvEmptyUsers.visibility = View.GONE

        adminRepository.getUsers(token)
            .enqueue(object : Callback<ApiMessageResponse<AdminUserListData>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<AdminUserListData>>,
                    response: Response<ApiMessageResponse<AdminUserListData>>
                ) {
                    binding.progressBarUsers.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        val users = response.body()?.data?.users ?: emptyList()
                        adapter.updateData(users)

                        if (users.isEmpty()) {
                            binding.tvEmptyUsers.visibility = View.VISIBLE
                            binding.tvEmptyUsers.text = "No users found"
                        } else {
                            binding.tvEmptyUsers.visibility = View.GONE
                        }
                    } else {
                        binding.tvEmptyUsers.visibility = View.VISIBLE
                        binding.tvEmptyUsers.text = "Failed to load users"
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<AdminUserListData>>,
                    t: Throwable
                ) {
                    binding.progressBarUsers.visibility = View.GONE
                    binding.tvEmptyUsers.visibility = View.VISIBLE
                    binding.tvEmptyUsers.text = t.message ?: "Something went wrong"
                }
            })
    }

    private fun deleteUser(user: AdminUser) {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) return

        adminRepository.deleteUser(token, user.id)
            .enqueue(object : Callback<ApiMessageResponse<DeleteUserResponse>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<DeleteUserResponse>>,
                    response: Response<ApiMessageResponse<DeleteUserResponse>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@UserRegistryActivity,
                            response.body()?.data?.message ?: "User deleted",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadUsers()
                    } else {
                        Toast.makeText(
                            this@UserRegistryActivity,
                            "Failed to delete user",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<DeleteUserResponse>>,
                    t: Throwable
                ) {
                    Toast.makeText(
                        this@UserRegistryActivity,
                        t.message ?: "Something went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}