package com.jhonlauro.callamechanic.session

import android.content.Context
import android.content.SharedPreferences
import com.jhonlauro.callamechanic.utils.Constants

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    fun saveSession(
        token: String,
        role: String,
        userId: Long,
        fullName: String,
        email: String? = null,
        adminId: String? = null,
        mechanicId: String? = null,
        phoneNumber: String? = null,
        photoUrl: String? = null
    ) {
        prefs.edit()
            .putString(Constants.KEY_TOKEN, token)
            .putString(Constants.KEY_ROLE, role)
            .putLong(Constants.KEY_USER_ID, userId)
            .putString(Constants.KEY_FULL_NAME, fullName)
            .putString(Constants.KEY_EMAIL, email)
            .putString(Constants.KEY_ADMIN_ID, adminId)
            .putString(Constants.KEY_MECHANIC_ID, mechanicId)
            .putString(Constants.KEY_PHONE_NUMBER, phoneNumber)
            .putString(Constants.KEY_PHOTO_URL, photoUrl)
            .apply()
    }

    fun getToken(): String? = prefs.getString(Constants.KEY_TOKEN, null)
    fun getRole(): String? = prefs.getString(Constants.KEY_ROLE, null)
    fun getUserId(): Long = prefs.getLong(Constants.KEY_USER_ID, -1L)
    fun getFullName(): String? = prefs.getString(Constants.KEY_FULL_NAME, null)
    fun getEmail(): String? = prefs.getString(Constants.KEY_EMAIL, null)
    fun getAdminId(): String? = prefs.getString(Constants.KEY_ADMIN_ID, null)
    fun getMechanicId(): String? = prefs.getString(Constants.KEY_MECHANIC_ID, null)
    fun getPhoneNumber(): String? = prefs.getString(Constants.KEY_PHONE_NUMBER, null)
    fun getPhotoUrl(): String? = prefs.getString(Constants.KEY_PHOTO_URL, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun updateProfileInfo(
        fullName: String,
        phoneNumber: String?,
        photoUrl: String? = getPhotoUrl()
    ) {
        prefs.edit()
            .putString(Constants.KEY_FULL_NAME, fullName)
            .putString(Constants.KEY_PHONE_NUMBER, phoneNumber)
            .putString(Constants.KEY_PHOTO_URL, photoUrl)
            .apply()
    }

    fun updateProfilePhoto(photoUrl: String?) {
        prefs.edit()
            .putString(Constants.KEY_PHOTO_URL, photoUrl)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
