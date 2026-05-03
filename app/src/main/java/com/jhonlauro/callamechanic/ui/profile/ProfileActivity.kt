package com.jhonlauro.callamechanic.ui.profile

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.ChangePasswordRequest
import com.jhonlauro.callamechanic.data.model.UpdateProfileRequest
import com.jhonlauro.callamechanic.data.model.UploadPhotoResponse
import com.jhonlauro.callamechanic.data.model.User
import com.jhonlauro.callamechanic.data.model.AdminUserListData
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.model.Vehicle
import com.jhonlauro.callamechanic.data.repository.AdminRepository
import com.jhonlauro.callamechanic.data.repository.AppointmentRepository
import com.jhonlauro.callamechanic.data.repository.ProfileRepository
import com.jhonlauro.callamechanic.data.repository.VehicleRepository
import com.jhonlauro.callamechanic.databinding.ActivityProfileBinding
import com.jhonlauro.callamechanic.databinding.DialogChangePasswordBinding
import com.jhonlauro.callamechanic.databinding.DialogEditProfileBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.auth.LoginActivity
import com.jhonlauro.callamechanic.ui.common.AppTransitions
import com.jhonlauro.callamechanic.ui.common.FormScrollHelper
import com.jhonlauro.callamechanic.ui.client.ManageVehiclesActivity
import com.jhonlauro.callamechanic.utils.Constants
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URL
import kotlin.concurrent.thread

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var profileRepository: ProfileRepository
    private lateinit var adminRepository: AdminRepository
    private lateinit var appointmentRepository: AppointmentRepository
    private lateinit var vehicleRepository: VehicleRepository
    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadProfilePhoto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        profileRepository = ProfileRepository()
        adminRepository = AdminRepository()
        appointmentRepository = AppointmentRepository()
        vehicleRepository = VehicleRepository()
        FormScrollHelper.enable(binding.root)

        renderProfile()
        loadProfile()
        loadRoleSections()

        binding.btnBack.setOnClickListener {
            finish()
            AppTransitions.close(this)
        }

        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.btnChangePhoto.setOnClickListener {
            photoPicker.launch("image/*")
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnManageVehicles.setOnClickListener {
            startActivity(Intent(this, ManageVehiclesActivity::class.java))
            AppTransitions.open(this)
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            AppTransitions.fade(this)
            finishAffinity()
        }
    }

    private fun loadProfile() {
        val token = sessionManager.getToken() ?: return

        profileRepository.getProfile(token).enqueue(object : Callback<ApiMessageResponse<User>> {
            override fun onResponse(
                call: Call<ApiMessageResponse<User>>,
                response: Response<ApiMessageResponse<User>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { user ->
                        sessionManager.updateProfileInfo(user.fullName, user.phoneNumber, user.photoUrl)
                        showProfilePhoto(user.photoUrl)
                        renderProfile()
                    }
                }
            }

            override fun onFailure(call: Call<ApiMessageResponse<User>>, t: Throwable) = Unit
        })
    }

    private fun renderProfile() {
        val role = sessionManager.getRole()?.uppercase() ?: "USER"
        val accountId = when (role) {
            "ADMIN" -> sessionManager.getAdminId() ?: "Admin ID unavailable"
            "MECHANIC" -> sessionManager.getMechanicId() ?: "Mechanic ID unavailable"
            else -> "User ID: ${sessionManager.getUserId()}"
        }
        val accountLabel = when (role) {
            "ADMIN" -> "Admin ID"
            "MECHANIC" -> "Mechanic ID"
            else -> "User ID"
        }
        val fullName = sessionManager.getFullName() ?: "No name"
        val email = sessionManager.getEmail() ?: "No email"
        val contact = sessionManager.getPhoneNumber() ?: "No contact"

        binding.tvProfileTitle.text = "My Profile"
        binding.tvProfileSubtitle.text = "Manage your account details and security settings"
        binding.tvFullName.text = fullName
        binding.tvRole.text = role
        binding.tvAdminId.text = accountId
        binding.tvPersonalFullName.text = fullName
        binding.tvPersonalEmail.text = email
        binding.tvPersonalContact.text = contact
        binding.tvAccountIdLabel.text = accountLabel
        binding.tvAccountId.text = accountId
        binding.tvAccountType.text = role
        binding.tvAccountStatus.text = "ACTIVE"
        binding.tvStatus.text = "ACTIVE"

        binding.cardAdminOverview.visibility = if (role == "ADMIN") View.VISIBLE else View.GONE
        binding.cardClientGarage.visibility = if (role == "CLIENT") View.VISIBLE else View.GONE
    }

    private fun loadRoleSections() {
        val role = sessionManager.getRole()?.uppercase() ?: return
        val token = sessionManager.getToken() ?: return

        when (role) {
            "ADMIN" -> loadAdminOverview(token)
            "CLIENT" -> loadClientGarage(token)
        }
    }

    private fun loadAdminOverview(token: String) {
        adminRepository.getUsers(token).enqueue(object : Callback<ApiMessageResponse<AdminUserListData>> {
            override fun onResponse(
                call: Call<ApiMessageResponse<AdminUserListData>>,
                response: Response<ApiMessageResponse<AdminUserListData>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val users = response.body()?.data?.users.orEmpty()
                    binding.tvOverviewUsers.text = users.size.toString()
                    binding.tvOverviewMechanics.text =
                        users.count { it.role.equals("MECHANIC", ignoreCase = true) }.toString()
                }
            }

            override fun onFailure(call: Call<ApiMessageResponse<AdminUserListData>>, t: Throwable) = Unit
        })

        appointmentRepository.getAppointments(token).enqueue(object : Callback<ApiMessageResponse<List<Appointment>>> {
            override fun onResponse(
                call: Call<ApiMessageResponse<List<Appointment>>>,
                response: Response<ApiMessageResponse<List<Appointment>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val activeStatuses = setOf("PENDING", "IN_PROGRESS")
                    val activeJobs = response.body()?.data.orEmpty().count {
                        activeStatuses.contains(it.status?.uppercase())
                    }
                    binding.tvOverviewActive.text = activeJobs.toString()
                }
            }

            override fun onFailure(call: Call<ApiMessageResponse<List<Appointment>>>, t: Throwable) = Unit
        })
    }

    private fun loadClientGarage(token: String) {
        vehicleRepository.getVehicles(token).enqueue(object : Callback<ApiMessageResponse<List<Vehicle>>> {
            override fun onResponse(
                call: Call<ApiMessageResponse<List<Vehicle>>>,
                response: Response<ApiMessageResponse<List<Vehicle>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    binding.tvGarageCount.text = response.body()?.data.orEmpty().size.toString()
                }
            }

            override fun onFailure(call: Call<ApiMessageResponse<List<Vehicle>>>, t: Throwable) {
                binding.tvGarageCount.text = "0"
            }
        })
    }

    private fun uploadProfilePhoto(uri: Uri) {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "No active session found.", Toast.LENGTH_SHORT).show()
            return
        }

        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) {
            Toast.makeText(this, "Could not read selected image.", Toast.LENGTH_SHORT).show()
            return
        }

        if (bytes.size > 5 * 1024 * 1024) {
            Toast.makeText(this, "Image must be less than 5MB.", Toast.LENGTH_SHORT).show()
            return
        }

        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "profile_photo.jpg", requestBody)

        setPhotoBusy(true)
        profileRepository.uploadProfilePhoto(token, part)
            .enqueue(object : Callback<ApiMessageResponse<UploadPhotoResponse>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<UploadPhotoResponse>>,
                    response: Response<ApiMessageResponse<UploadPhotoResponse>>
                ) {
                    setPhotoBusy(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val uploadedPhotoUrl = response.body()?.data?.photoUrl
                        if (uploadedPhotoUrl.isNullOrBlank()) {
                            showLocalProfilePhoto(uri)
                        } else {
                            sessionManager.updateProfilePhoto(uploadedPhotoUrl)
                            showProfilePhoto(uploadedPhotoUrl)
                        }
                        Toast.makeText(this@ProfileActivity, "Profile photo updated.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Failed to upload profile photo.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<UploadPhotoResponse>>,
                    t: Throwable
                ) {
                    setPhotoBusy(false)
                    Toast.makeText(this@ProfileActivity, t.message ?: "Failed to upload profile photo.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showProfilePhoto(photoUrl: String?) {
        if (photoUrl.isNullOrBlank()) {
            showDefaultProfilePhoto()
            return
        }

        decodeBase64Photo(photoUrl)?.let {
            showUploadedProfileBitmap(it)
            return
        }

        val resolvedUrl = resolvePhotoUrl(photoUrl)
        if (resolvedUrl == null) {
            showDefaultProfilePhoto()
            return
        }

        thread {
            val bitmap = runCatching {
                URL(resolvedUrl).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()

            runOnUiThread {
                if (bitmap != null) {
                    showUploadedProfileBitmap(bitmap)
                } else {
                    showDefaultProfilePhoto()
                }
            }
        }
    }

    private fun showLocalProfilePhoto(uri: Uri) {
        val bitmap = contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }

        if (bitmap != null) {
            showUploadedProfileBitmap(bitmap)
        } else {
            showDefaultProfilePhoto()
        }
    }

    private fun decodeBase64Photo(photoValue: String): Bitmap? {
        val trimmed = photoValue.trim()
        if (trimmed.startsWith("http", ignoreCase = true) || trimmed.startsWith("/")) return null

        return runCatching {
            val base64Data = if (trimmed.startsWith("data:", ignoreCase = true)) {
                trimmed.substringAfter(",", missingDelimiterValue = "")
            } else {
                trimmed
            }

            if (base64Data.isBlank()) return null

            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun resolvePhotoUrl(photoValue: String): String? {
        val trimmed = photoValue.trim()
        if (trimmed.startsWith("data:", ignoreCase = true)) return null
        if (trimmed.startsWith("http", ignoreCase = true)) return trimmed

        val apiBase = Constants.BASE_URL.trimEnd('/')
        val serverRoot = apiBase.substringBefore("/api/v1")
        return if (trimmed.startsWith("/")) {
            "$serverRoot$trimmed"
        } else {
            "$apiBase/$trimmed"
        }
    }

    private fun showDefaultProfilePhoto() {
        binding.profilePhotoContainer.setBackgroundResource(com.jhonlauro.callamechanic.R.drawable.bg_avatar_circle)
        binding.ivProfilePhoto.setImageResource(com.jhonlauro.callamechanic.R.drawable.ic_person)
        binding.ivProfilePhoto.imageTintList = ColorStateList.valueOf(getColor(android.R.color.white))
        binding.ivProfilePhoto.setColorFilter(getColor(android.R.color.white))
        binding.ivProfilePhoto.setPadding(dp(22), dp(22), dp(22), dp(22))
        binding.ivProfilePhoto.scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    private fun showUploadedProfileBitmap(bitmap: Bitmap) {
        binding.profilePhotoContainer.setBackgroundResource(com.jhonlauro.callamechanic.R.drawable.bg_avatar_photo_surface)
        binding.ivProfilePhoto.imageTintList = null
        binding.ivProfilePhoto.clearColorFilter()
        binding.ivProfilePhoto.setPadding(dp(3), dp(3), dp(3), dp(3))
        binding.ivProfilePhoto.scaleType = ImageView.ScaleType.FIT_CENTER
        binding.ivProfilePhoto.setImageBitmap(createCircularProfileBitmap(bitmap))
    }

    private fun createCircularProfileBitmap(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2
        val square = Bitmap.createBitmap(source, x, y, size, size)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        canvas.drawOval(RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)
        if (square != source) square.recycle()
        return output
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun setPhotoBusy(isBusy: Boolean) {
        binding.progressPhoto.visibility = if (isBusy) View.VISIBLE else View.GONE
        binding.btnChangePhoto.isEnabled = !isBusy
        binding.btnChangePhoto.text = if (isBusy) "Uploading..." else "Change Photo"
    }

    private fun showEditProfileDialog() {
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.etEditFullName.setText(sessionManager.getFullName() ?: "")
        dialogBinding.etEditPhone.setText(sessionManager.getPhoneNumber() ?: "")
        FormScrollHelper.enable(dialogBinding.root)

        dialogBinding.btnCancelEditProfile.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSaveEditProfile.setOnClickListener {
            updateProfile(dialogBinding, dialog)
        }

        dialog.show()
    }

    private fun updateProfile(
        dialogBinding: DialogEditProfileBinding,
        dialog: AlertDialog
    ) {
        val fullName = dialogBinding.etEditFullName.text.toString().trim()
        val phoneNumber = dialogBinding.etEditPhone.text.toString().trim()

        if (fullName.isBlank()) {
            showEditProfileError(dialogBinding, "Full name is required.")
            return
        }

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            showEditProfileError(dialogBinding, "No active session found.")
            return
        }

        setEditProfileBusy(dialogBinding, true)
        profileRepository.updateProfile(token, UpdateProfileRequest(fullName, phoneNumber))
            .enqueue(object : Callback<ApiMessageResponse<User>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<User>>,
                    response: Response<ApiMessageResponse<User>>
                ) {
                    setEditProfileBusy(dialogBinding, false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val updatedUser = response.body()?.data
                        val updatedName = updatedUser?.fullName ?: fullName
                        val updatedPhone = updatedUser?.phoneNumber ?: phoneNumber
                        sessionManager.updateProfileInfo(updatedName, updatedPhone)
                        renderProfile()
                        dialog.dismiss()
                        Toast.makeText(this@ProfileActivity, "Profile updated.", Toast.LENGTH_SHORT).show()
                    } else {
                        showEditProfileError(dialogBinding, "Failed to update profile.")
                    }
                }

                override fun onFailure(call: Call<ApiMessageResponse<User>>, t: Throwable) {
                    setEditProfileBusy(dialogBinding, false)
                    showEditProfileError(dialogBinding, t.message ?: "Failed to update profile.")
                }
            })
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        FormScrollHelper.enable(dialogBinding.root)

        dialogBinding.btnCancelPassword.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSavePassword.setOnClickListener {
            changePassword(dialogBinding, dialog)
        }

        dialog.show()
    }

    private fun changePassword(
        dialogBinding: DialogChangePasswordBinding,
        dialog: AlertDialog
    ) {
        val currentPassword = dialogBinding.etCurrentPassword.text.toString()
        val newPassword = dialogBinding.etNewPassword.text.toString()
        val confirmPassword = dialogBinding.etConfirmPassword.text.toString()

        when {
            currentPassword.isBlank() -> {
                showPasswordError(dialogBinding, "Current password is required.")
                return
            }
            newPassword.length < 8 -> {
                showPasswordError(dialogBinding, "New password must be at least 8 characters.")
                return
            }
            newPassword != confirmPassword -> {
                showPasswordError(dialogBinding, "New passwords do not match.")
                return
            }
            currentPassword == newPassword -> {
                showPasswordError(dialogBinding, "New password must be different from your current password.")
                return
            }
        }

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            showPasswordError(dialogBinding, "No active session found.")
            return
        }

        setPasswordBusy(dialogBinding, true)
        val request = ChangePasswordRequest(currentPassword, newPassword, confirmPassword)
        profileRepository.changePassword(token, request)
            .enqueue(object : Callback<ApiMessageResponse<Any>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<Any>>,
                    response: Response<ApiMessageResponse<Any>>
                ) {
                    setPasswordBusy(dialogBinding, false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        dialog.dismiss()
                        Toast.makeText(this@ProfileActivity, "Password updated.", Toast.LENGTH_SHORT).show()
                    } else {
                        showPasswordError(dialogBinding, "Failed to update password. Check your current password.")
                    }
                }

                override fun onFailure(call: Call<ApiMessageResponse<Any>>, t: Throwable) {
                    setPasswordBusy(dialogBinding, false)
                    showPasswordError(dialogBinding, t.message ?: "Failed to update password.")
                }
            })
    }

    private fun showEditProfileError(
        dialogBinding: DialogEditProfileBinding,
        message: String
    ) {
        dialogBinding.tvEditProfileError.text = message
        dialogBinding.tvEditProfileError.visibility = View.VISIBLE
    }

    private fun showPasswordError(
        dialogBinding: DialogChangePasswordBinding,
        message: String
    ) {
        dialogBinding.tvPasswordError.text = message
        dialogBinding.tvPasswordError.visibility = View.VISIBLE
    }

    private fun setEditProfileBusy(
        dialogBinding: DialogEditProfileBinding,
        isBusy: Boolean
    ) {
        dialogBinding.progressEditProfile.visibility = if (isBusy) View.VISIBLE else View.GONE
        dialogBinding.btnSaveEditProfile.isEnabled = !isBusy
        dialogBinding.btnCancelEditProfile.isEnabled = !isBusy
        dialogBinding.tvEditProfileError.visibility = View.GONE
    }

    private fun setPasswordBusy(
        dialogBinding: DialogChangePasswordBinding,
        isBusy: Boolean
    ) {
        dialogBinding.progressChangePassword.visibility = if (isBusy) View.VISIBLE else View.GONE
        dialogBinding.btnSavePassword.isEnabled = !isBusy
        dialogBinding.btnCancelPassword.isEnabled = !isBusy
        dialogBinding.tvPasswordError.visibility = View.GONE
    }
}
