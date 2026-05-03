package com.jhonlauro.callamechanic.ui.common

import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.EditText
import android.widget.ScrollView
import kotlin.math.max

object FormScrollHelper {
    fun enable(root: View) {
        findEditTexts(root).forEach { field ->
            field.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    view.postDelayed({ scrollFieldIntoView(view) }, 250)
                }
            }
        }
    }

    private fun findEditTexts(view: View): List<EditText> {
        if (view is EditText) return listOf(view)
        if (view !is ViewGroup) return emptyList()

        val fields = mutableListOf<EditText>()
        for (i in 0 until view.childCount) {
            fields += findEditTexts(view.getChildAt(i))
        }
        return fields
    }

    private fun scrollFieldIntoView(field: View) {
        val scrollView = findParentScrollView(field) ?: return
        val offset = (field.resources.displayMetrics.density * 96).toInt()
        val y = max(0, fieldTopInsideScroll(field, scrollView) - offset)
        scrollView.smoothScrollTo(0, y)
    }

    private fun findParentScrollView(view: View): ScrollView? {
        var parent: ViewParent? = view.parent
        while (parent is View) {
            if (parent is ScrollView) return parent
            parent = (parent as View).parent
        }
        return null
    }

    private fun fieldTopInsideScroll(field: View, scrollView: ScrollView): Int {
        var top = field.top
        var parent: ViewParent? = field.parent
        while (parent is View && parent != scrollView) {
            top += parent.top
            parent = (parent as View).parent
        }
        return top
    }
}
