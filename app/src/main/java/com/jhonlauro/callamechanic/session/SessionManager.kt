package com.jhonlauro.callamechanic.session

import android.content.Context
import android.content.SharedPreferences
import com.jhonlauro.callamechanic.utils.Constants

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    fun saveSession(token: String, role: String, userId: Long, fullName: String) {
        prefs.edit()
            .putString(Constants.KEY_TOKEN, token)
            .putString(Constants.KEY_ROLE, role)
            .putLong(Constants.KEY_USER_ID, userId)
            .putString(Constants.KEY_FULL_NAME, fullName)
            .apply()
    }

    fun getToken(): String? = prefs.getString(Constants.KEY_TOKEN, null)

    fun getRole(): String? = prefs.getString(Constants.KEY_ROLE, null)

    fun getUserId(): Long = prefs.getLong(Constants.KEY_USER_ID, -1L)

    fun getFullName(): String? = prefs.getString(Constants.KEY_FULL_NAME, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}