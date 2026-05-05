package com.jhonlauro.callamechanic.ui.common

data class DashboardNotification(
    val id: String,
    val title: String,
    val message: String,
    val tone: Tone = Tone.INFO,
    val time: String? = null
) {
    enum class Tone {
        INFO,
        WARNING,
        SUCCESS,
        DANGER
    }
}
