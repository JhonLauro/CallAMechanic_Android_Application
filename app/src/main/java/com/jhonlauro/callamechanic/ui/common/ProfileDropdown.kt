package com.jhonlauro.callamechanic.ui.common

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.jhonlauro.callamechanic.R
import com.jhonlauro.callamechanic.databinding.MenuProfileDropdownBinding

object ProfileDropdown {

    fun show(
        anchor: View,
        onViewProfile: () -> Unit,
        onSignOut: () -> Unit,
        onThemeToggle: ((Boolean) -> Unit)? = null
    ) {
        val binding = MenuProfileDropdownBinding.inflate(LayoutInflater.from(anchor.context))
        val menuWidth = (anchor.resources.displayMetrics.density * 190).toInt()
        val popup = PopupWindow(
            binding.root,
            menuWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            elevation = 18f
        }

        var displayedDarkMode = ThemeManager.isDarkMode(anchor.context)
        var themeAnimationRunning = false

        fun renderThemeRow(isDarkMode: Boolean) {
            binding.tvThemeLabel.text = if (isDarkMode) "Light Mode" else "Dark Mode"
            binding.ivThemeIcon.setImageResource(if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon)
        }

        renderThemeRow(displayedDarkMode)

        binding.menuViewProfile.setOnClickListener {
            popup.dismiss()
            onViewProfile()
        }
        binding.menuToggleTheme.setOnClickListener {
            if (themeAnimationRunning) return@setOnClickListener
            themeAnimationRunning = true

            val nextDarkMode = !displayedDarkMode
            displayedDarkMode = nextDarkMode
            binding.tvThemeLabel.text = if (nextDarkMode) "Light Mode" else "Dark Mode"
            binding.ivThemeIcon.animate()
                .cancel()
            binding.ivThemeIcon.animate()
                .rotationBy(180f)
                .scaleX(0.78f)
                .scaleY(0.78f)
                .alpha(0.35f)
                .setDuration(140)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    binding.ivThemeIcon.setImageResource(if (nextDarkMode) R.drawable.ic_sun else R.drawable.ic_moon)
                    binding.ivThemeIcon.rotation = 0f
                    binding.ivThemeIcon.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .rotationBy(180f)
                        .setDuration(180)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            anchor.postDelayed({
                                popup.dismiss()
                                if (onThemeToggle != null) {
                                    onThemeToggle(nextDarkMode)
                                } else {
                                    ThemeManager.setDarkMode(anchor.context, nextDarkMode)
                                    (anchor.context as? Activity)?.recreate()
                                }
                            }, 120)
                        }
                        .start()
                }
                .start()
        }
        binding.menuSignOut.setOnClickListener {
            popup.dismiss()
            onSignOut()
        }

        popup.showAsDropDown(anchor, -(menuWidth - anchor.width), 8)
    }
}
