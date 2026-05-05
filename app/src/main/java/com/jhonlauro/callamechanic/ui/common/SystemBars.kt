package com.jhonlauro.callamechanic.ui.common

import android.app.Activity
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.jhonlauro.callamechanic.R

object SystemBars {
    fun applyLight(activity: Activity) {
        val window = activity.window
        val decorView = window.decorView
        val isDarkMode = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        decorView.post {
            window.statusBarColor = ContextCompat.getColor(activity, R.color.cam_bg)
            window.navigationBarColor = ContextCompat.getColor(activity, R.color.cam_white)

            WindowCompat.getInsetsController(window, decorView).apply {
                isAppearanceLightStatusBars = !isDarkMode
                isAppearanceLightNavigationBars = !isDarkMode
            }
        }
    }
}
