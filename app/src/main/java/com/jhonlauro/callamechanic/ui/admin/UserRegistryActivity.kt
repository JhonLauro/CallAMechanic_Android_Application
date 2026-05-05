package com.jhonlauro.callamechanic.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jhonlauro.callamechanic.data.model.AdminUser
import com.jhonlauro.callamechanic.data.model.AdminUserListData
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.DeleteUserResponse
import com.jhonlauro.callamechanic.data.repository.AdminRepository
import com.jhonlauro.callamechanic.databinding.ActivityUserRegistryBinding
import com.jhonlauro.callamechanic.databinding.DialogDeleteUserBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.common.AppTransitions
import com.jhonlauro.callamechanic.ui.common.FriendlyError
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
            confirmDeleteUser(user)
        }

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        binding.btnBackRegistry.setOnClickListener {
            finish()
            AppTransitions.close(this)
        }

        loadUsers()
    }

    private fun loadUsers() {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            binding.tvEmptyUsers.visibility = View.VISIBLE
            binding.tvEmptyUsers.text = "Session expired. Please sign in again."
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
                        binding.tvRegistryTotal.text = users.size.toString()
                        binding.tvRegistryMechanics.text =
                            users.count { it.role.equals("MECHANIC", ignoreCase = true) }.toString()
                        binding.tvRegistryClients.text =
                            users.count { it.role.equals("CLIENT", ignoreCase = true) }.toString()

                        if (users.isEmpty()) {
                            binding.tvEmptyUsers.visibility = View.VISIBLE
                            binding.tvEmptyUsers.text = "No users found"
                        } else {
                            binding.tvEmptyUsers.visibility = View.GONE
                        }
                    } else {
                        binding.tvRegistryTotal.text = "0"
                        binding.tvRegistryMechanics.text = "0"
                        binding.tvRegistryClients.text = "0"
                        binding.tvEmptyUsers.visibility = View.VISIBLE
                        binding.tvEmptyUsers.text = FriendlyError.fromResponse(response, "Request failed. Please try again.")
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<AdminUserListData>>,
                    t: Throwable
                ) {
                    binding.progressBarUsers.visibility = View.GONE
                    binding.tvRegistryTotal.text = "0"
                    binding.tvRegistryMechanics.text = "0"
                    binding.tvRegistryClients.text = "0"
                    binding.tvEmptyUsers.visibility = View.VISIBLE
                    binding.tvEmptyUsers.text = FriendlyError.fromThrowable(t, "Request failed. Please try again.")
                }
            })
    }

    private fun confirmDeleteUser(user: AdminUser) {
        val dialogBinding = DialogDeleteUserBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvDeleteUserName.text = user.fullName ?: "Unnamed user"
        dialogBinding.tvDeleteUserDetails.text = listOfNotNull(
            user.role,
            user.email,
            user.mechanicId
        ).filter { it.isNotBlank() }.joinToString("  •  ")

        dialogBinding.btnCancelDelete.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmDelete.setOnClickListener {
            dialog.dismiss()
            deleteUser(user)
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
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
                            FriendlyError.fromResponse(response, "Request failed. Please try again."),
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
                        FriendlyError.fromThrowable(t, "Request failed. Please try again."),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
