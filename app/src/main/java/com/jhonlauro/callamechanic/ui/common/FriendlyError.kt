package com.jhonlauro.callamechanic.ui.common

import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object FriendlyError {
    fun fromThrowable(error: Throwable, fallback: String = "Request failed. Please try again."): String {
        return when (error) {
            is SocketTimeoutException -> "Request timed out. Please try again."
            is UnknownHostException -> "Network error. Please check your connection."
            is IOException -> "Network error. Please check your connection."
            else -> fallback
        }
    }

    fun fromResponse(response: Response<*>, fallback: String = "Request failed. Please try again."): String {
        return when (response.code()) {
            400 -> extractKnownMessage(response, fallback)
            401 -> extractKnownMessage(response, "Session expired. Please sign in again.")
            403 -> "Access denied. You do not have permission to perform this action."
            404 -> "Not found. The requested resource could not be found."
            409 -> extractKnownMessage(response, "Conflict. This information already exists.")
            422 -> extractKnownMessage(response, "Validation failed. Please review your input.")
            429 -> "Too many requests. Please try again later."
            in 500..599 -> "Service unavailable. Please try again later."
            else -> extractKnownMessage(response, fallback)
        }
    }

    fun invalidLogin(): String = "Invalid credentials."

    private fun extractKnownMessage(response: Response<*>, fallback: String): String {
        val raw = response.errorBody()?.string().orEmpty()
        if (raw.isBlank()) return fallback

        val backendMessage = runCatching {
            val json = JSONObject(raw)
            when {
                json.has("message") -> json.optString("message")
                json.has("error") && json.opt("error") is JSONObject -> {
                    val error = json.optJSONObject("error")
                    error?.optString("message")
                        ?: error?.optString("details")
                        ?: error?.optString("detail")
                        ?: ""
                }
                json.has("error") -> json.optString("error")
                else -> ""
            }
        }.getOrDefault(raw)

        return sanitizeBackendMessage(backendMessage, fallback)
    }

    private fun sanitizeBackendMessage(message: String, fallback: String): String {
        val normalized = message.trim()
        if (normalized.isBlank()) return fallback

        val lower = normalized.lowercase()
        return when {
            lower.contains("bad credentials") ||
                lower.contains("invalid credentials") ||
                lower.contains("identifier or password") ||
                lower.contains("check your credentials") -> invalidLogin()

            lower.contains("email") && (lower.contains("exists") || lower.contains("already")) ->
                "Email already exists."

            lower.contains("mechanic") && lower.contains("exists") ->
                "Mechanic ID already exists."

            lower.contains("duplicate") || lower.contains("constraint") ->
                "Duplicate entry. Please review your input."

            lower.contains("jwt") || lower.contains("token") ->
                "Session expired. Please sign in again."

            lower.contains("exception") ||
                lower.contains("trace") ||
                lower.contains("sql") ||
                lower.contains("hibernate") ||
                lower.contains("null") ||
                lower.contains("{") ->
                fallback

            else -> normalized.replaceFirstChar { it.uppercase() }
        }
    }
}
