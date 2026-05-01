package com.jhonlauro.callamechanic.ui.common

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.jhonlauro.callamechanic.databinding.MenuProfileDropdownBinding

object ProfileDropdown {

    fun show(
        anchor: View,
        onViewProfile: () -> Unit,
        onSignOut: () -> Unit
    ) {
        val binding = MenuProfileDropdownBinding.inflate(LayoutInflater.from(anchor.context))
        val menuWidth = (anchor.resources.displayMetrics.density * 158).toInt()
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

        binding.menuViewProfile.setOnClickListener {
            popup.dismiss()
            onViewProfile()
        }
        binding.menuSignOut.setOnClickListener {
            popup.dismiss()
            onSignOut()
        }

        popup.showAsDropDown(anchor, -(menuWidth - anchor.width), 8)
    }
}
