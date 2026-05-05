package com.jhonlauro.callamechanic.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.jhonlauro.callamechanic.R

fun clearFieldErrors(vararg fields: EditText) {
    fields.forEach { field ->
        field.error = null
        field.setBackgroundResource(R.drawable.bg_input)
    }
}

fun showFieldError(field: EditText, message: String) {
    field.error = message
    field.setBackgroundResource(R.drawable.bg_input_error)
    field.requestFocus()
}

fun clearFieldErrorOnInput(vararg fields: EditText) {
    fields.forEach { field ->
        field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                field.error = null
                field.setBackgroundResource(R.drawable.bg_input)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }
}
