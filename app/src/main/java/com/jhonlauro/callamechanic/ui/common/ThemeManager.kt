package com.jhonlauro.callamechanic.ui.common

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREF_NAME = "cam_theme_preferences"
    private const val KEY_THEME = "theme_mode"
    private const val MODE_LIGHT = "light"
    private const val MODE_DARK = "dark"

    fun applySavedTheme(context: Context) {
        val mode = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, MODE_LIGHT)

        AppCompatDelegate.setDefaultNightMode(
            if (mode == MODE_DARK) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun isDarkMode(context: Context): Boolean {
        val savedMode = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, null)

        if (savedMode != null) return savedMode == MODE_DARK

        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    fun toggleTheme(context: Context) {
        setDarkMode(context, !isDarkMode(context))
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, if (enabled) MODE_DARK else MODE_LIGHT)
            .commit()

        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun menuLabel(context: Context): String {
        return if (isDarkMode(context)) "Light Mode" else "Dark Mode"
    }
}
