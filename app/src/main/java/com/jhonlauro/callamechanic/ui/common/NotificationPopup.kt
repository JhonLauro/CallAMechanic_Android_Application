package com.jhonlauro.callamechanic.ui.common

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import com.jhonlauro.callamechanic.R

object NotificationPopup {
    private const val PREF_NAME = "cam_notification_state"
    private const val KEY_DISMISSED = "dismissed_ids"

    fun updateBadge(context: Context, badge: TextView, items: List<DashboardNotification>) {
        val count = visibleItems(context, items).size
        badge.text = count.toString()
        badge.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    fun show(
        anchor: View,
        items: List<DashboardNotification>,
        onChanged: () -> Unit
    ) {
        val context = anchor.context
        val colors = PopupColors.from(context)
        val density = context.resources.displayMetrics.density
        val width = (320 * density).toInt().coerceAtMost(context.resources.displayMetrics.widthPixels - (24 * density).toInt())

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(colors.surface, colors.border, 18f, density)
        }

        val popup = PopupWindow(
            root,
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            elevation = 18f
        }

        fun refreshList() {
            val visibleItems = visibleItems(context, items)
            root.removeAllViews()
            root.addView(headerView(context, colors, visibleItems.size, density) {
                saveDismissed(context, readDismissed(context) + visibleItems.map { it.id })
                onChanged()
                popup.dismiss()
            })
            root.addView(listView(context, colors, visibleItems, density) { notification ->
                saveDismissed(context, readDismissed(context) + notification.id)
                onChanged()
                refreshList()
            })
        }

        refreshList()
        popup.showAsDropDown(anchor, -(width - anchor.width), (8 * density).toInt())
    }

    private fun headerView(
        context: Context,
        colors: PopupColors,
        count: Int,
        density: Float,
        onClearAll: () -> Unit
    ): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16, density), dp(14, density), dp(14, density), dp(12, density))
            background = rounded(colors.header, colors.border, 18f, density)

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(context).apply {
                    text = "Notifications"
                    setTextColor(colors.text)
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(TextView(context).apply {
                    text = if (count == 0) "No updates right now" else "$count update${if (count == 1) "" else "s"} available"
                    setTextColor(colors.muted)
                    textSize = 12f
                })
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            if (count > 0) {
                addView(TextView(context).apply {
                    text = "Clear all"
                    gravity = Gravity.CENTER
                    setTextColor(colors.primary)
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dp(10, density), 0, dp(10, density), 0)
                    minHeight = dp(32, density)
                    background = rounded(colors.chip, colors.border, 999f, density)
                    setOnClickListener { onClearAll() }
                })
            }
        }
    }

    private fun listView(
        context: Context,
        colors: PopupColors,
        items: List<DashboardNotification>,
        density: Float,
        onDismiss: (DashboardNotification) -> Unit
    ): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8, density), dp(8, density), dp(8, density), dp(8, density))
        }

        if (items.isEmpty()) {
            container.addView(TextView(context).apply {
                text = "Everything is clear."
                gravity = Gravity.CENTER
                setTextColor(colors.muted)
                textSize = 13f
                setPadding(0, dp(26, density), 0, dp(26, density))
            })
        } else {
            items.forEach { item ->
                container.addView(notificationRow(context, colors, item, density, onDismiss))
            }
        }

        val targetHeight = if (items.size > 4) dp(360, density) else ViewGroup.LayoutParams.WRAP_CONTENT

        return ScrollView(context).apply {
            addView(container)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                targetHeight
            )
        }
    }

    private fun notificationRow(
        context: Context,
        colors: PopupColors,
        item: DashboardNotification,
        density: Float,
        onDismiss: (DashboardNotification) -> Unit
    ): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(dp(10, density), dp(10, density), dp(8, density), dp(10, density))
            background = rounded(Color.TRANSPARENT, Color.TRANSPARENT, 12f, density)

            addView(View(context).apply {
                background = rounded(toneColor(item.tone), toneColor(item.tone), 999f, density)
            }, LinearLayout.LayoutParams(dp(9, density), dp(9, density)).apply {
                topMargin = dp(6, density)
                rightMargin = dp(10, density)
            })

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                    addView(TextView(context).apply {
                        text = item.title
                        setTextColor(colors.text)
                        textSize = 13f
                        typeface = Typeface.DEFAULT_BOLD
                    }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

                    item.time?.takeIf { it.isNotBlank() }?.let { value ->
                        addView(TextView(context).apply {
                            text = value
                            setTextColor(colors.muted)
                            textSize = 11f
                        })
                    }
                })

                addView(TextView(context).apply {
                    text = item.message
                    setTextColor(colors.muted)
                    textSize = 12f
                    setLineSpacing(0f, 1.1f)
                })
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            addView(ImageButton(context).apply {
                setImageResource(R.drawable.ic_trash)
                setColorFilter(colors.danger)
                background = rounded(colors.dangerSoft, colors.dangerBorder, 9f, density)
                contentDescription = "Delete notification"
                setPadding(dp(8, density), dp(8, density), dp(8, density), dp(8, density))
                setOnClickListener { onDismiss(item) }
            }, LinearLayout.LayoutParams(dp(34, density), dp(34, density)).apply {
                leftMargin = dp(8, density)
            })
        }
    }

    private fun visibleItems(context: Context, items: List<DashboardNotification>): List<DashboardNotification> {
        val dismissed = readDismissed(context)
        return items.filter { it.id !in dismissed }
    }

    private fun readDismissed(context: Context): Set<String> {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_DISMISSED, emptySet())
            .orEmpty()
    }

    private fun saveDismissed(context: Context, ids: Collection<String>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_DISMISSED, ids.map { it }.toSet())
            .apply()
    }

    private fun rounded(color: Int, strokeColor: Int, radiusDp: Float, density: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusDp * density
            if (strokeColor != Color.TRANSPARENT) setStroke(dp(1, density), strokeColor)
        }
    }

    private fun toneColor(tone: DashboardNotification.Tone): Int {
        return when (tone) {
            DashboardNotification.Tone.INFO -> Color.parseColor("#2F6BFF")
            DashboardNotification.Tone.WARNING -> Color.parseColor("#F59E0B")
            DashboardNotification.Tone.SUCCESS -> Color.parseColor("#16A34A")
            DashboardNotification.Tone.DANGER -> Color.parseColor("#EF4444")
        }
    }

    private fun dp(value: Int, density: Float): Int = (value * density).toInt()

    private data class PopupColors(
        val surface: Int,
        val header: Int,
        val border: Int,
        val text: Int,
        val muted: Int,
        val primary: Int,
        val chip: Int,
        val danger: Int,
        val dangerSoft: Int,
        val dangerBorder: Int
    ) {
        companion object {
            fun from(context: Context): PopupColors {
                val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
                return if (dark) {
                    PopupColors(
                        surface = Color.parseColor("#0F1F36"),
                        header = Color.parseColor("#132743"),
                        border = Color.parseColor("#2A4367"),
                        text = Color.parseColor("#EAF2FF"),
                        muted = Color.parseColor("#9FB4D2"),
                        primary = Color.parseColor("#93C5FD"),
                        chip = Color.parseColor("#172D4A"),
                        danger = Color.parseColor("#FCA5A5"),
                        dangerSoft = Color.parseColor("#331A24"),
                        dangerBorder = Color.parseColor("#7F1D1D")
                    )
                } else {
                    PopupColors(
                        surface = Color.WHITE,
                        header = Color.parseColor("#F8FBFF"),
                        border = Color.parseColor("#D9E2F2"),
                        text = Color.parseColor("#0F172A"),
                        muted = Color.parseColor("#64748B"),
                        primary = Color.parseColor("#2F6BFF"),
                        chip = Color.parseColor("#EFF6FF"),
                        danger = Color.parseColor("#EF4444"),
                        dangerSoft = Color.parseColor("#FEF2F2"),
                        dangerBorder = Color.parseColor("#FECACA")
                    )
                }
            }
        }
    }
}
