package com.jhonlauro.callamechanic.ui.common

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.Base64
import android.widget.ImageView
import com.jhonlauro.callamechanic.R
import com.jhonlauro.callamechanic.utils.Constants
import java.net.URL
import kotlin.concurrent.thread

object ProfilePhotoRenderer {
    fun show(imageView: ImageView, photoUrl: String?) {
        if (photoUrl.isNullOrBlank()) {
            showDefault(imageView)
            return
        }

        decodeBase64Photo(photoUrl)?.let { bitmap ->
            showBitmap(imageView, bitmap)
            return
        }

        val resolvedUrl = resolvePhotoUrl(photoUrl) ?: run {
            showDefault(imageView)
            return
        }

        thread {
            val bitmap = runCatching {
                URL(resolvedUrl).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()

            imageView.post {
                if (bitmap != null) showBitmap(imageView, bitmap) else showDefault(imageView)
            }
        }
    }

    fun showBitmap(imageView: ImageView, bitmap: Bitmap) {
        val padding = (2 * imageView.resources.displayMetrics.density).toInt()
        imageView.imageTintList = null
        imageView.clearColorFilter()
        imageView.setPadding(padding, padding, padding, padding)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setImageBitmap(createCircularProfileBitmap(bitmap))
    }

    private fun showDefault(imageView: ImageView) {
        val padding = (10 * imageView.resources.displayMetrics.density).toInt()
        imageView.setImageResource(R.drawable.ic_person)
        imageView.imageTintList = ColorStateList.valueOf(imageView.context.getColor(R.color.cam_primary))
        imageView.setPadding(padding, padding, padding, padding)
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    private fun decodeBase64Photo(photoValue: String): Bitmap? {
        val trimmed = photoValue.trim()
        if (trimmed.startsWith("http", ignoreCase = true) || trimmed.startsWith("/")) return null

        return runCatching {
            val base64Data = if (trimmed.startsWith("data:", ignoreCase = true)) {
                trimmed.substringAfter(",", missingDelimiterValue = "")
            } else {
                trimmed
            }

            if (base64Data.isBlank()) return null

            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun resolvePhotoUrl(photoValue: String): String? {
        val trimmed = photoValue.trim()
        if (trimmed.startsWith("data:", ignoreCase = true)) return null
        if (trimmed.startsWith("http", ignoreCase = true)) return trimmed

        val apiBase = Constants.BASE_URL.trimEnd('/')
        val serverRoot = apiBase.substringBefore("/api/v1")
        return if (trimmed.startsWith("/")) "$serverRoot$trimmed" else "$apiBase/$trimmed"
    }

    private fun createCircularProfileBitmap(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2
        val square = Bitmap.createBitmap(source, x, y, size, size)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        canvas.drawOval(RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)
        if (square != source) square.recycle()
        return output
    }
}
