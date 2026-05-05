package com.jhonlauro.callamechanic.ui.client

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.DatePicker
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jhonlauro.callamechanic.R
import com.jhonlauro.callamechanic.data.model.ApiMessageResponse
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.data.model.CreateAppointmentRequest
import com.jhonlauro.callamechanic.data.model.Vehicle
import com.jhonlauro.callamechanic.data.repository.AppointmentRepository
import com.jhonlauro.callamechanic.data.repository.VehicleRepository
import com.jhonlauro.callamechanic.databinding.ActivityBookAppointmentBinding
import com.jhonlauro.callamechanic.session.SessionManager
import com.jhonlauro.callamechanic.ui.common.AppTransitions
import com.jhonlauro.callamechanic.ui.common.FormScrollHelper
import com.jhonlauro.callamechanic.ui.common.FriendlyError
import com.jhonlauro.callamechanic.ui.common.clearFieldErrorOnInput
import com.jhonlauro.callamechanic.ui.common.clearFieldErrors
import com.jhonlauro.callamechanic.ui.common.showFieldError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookAppointmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookAppointmentBinding
    private lateinit var appointmentRepository: AppointmentRepository
    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var sessionManager: SessionManager

    private var vehicles: List<Vehicle> = emptyList()

    private var selectedVehicleInfo: String? = null
    private var selectedDateValue: String? = null
    private var selectedTimeValue: String? = null

    private val serviceTypes = arrayOf(
        "Oil Change",
        "Brake Service",
        "Engine Diagnostics",
        "Tire Service",
        "AC Repair",
        "Battery Replacement",
        "Transmission Service",
        "General Checkup",
        "Other"
    )

    private val timeLabels = arrayOf(
        "8:00 AM",
        "9:00 AM",
        "10:00 AM",
        "11:00 AM",
        "12:00 PM",
        "1:00 PM",
        "2:00 PM",
        "3:00 PM",
        "4:00 PM",
        "5:00 PM"
    )

    private val timeValues = arrayOf(
        "08:00",
        "09:00",
        "10:00",
        "11:00",
        "12:00",
        "13:00",
        "14:00",
        "15:00",
        "16:00",
        "17:00"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appointmentRepository = AppointmentRepository()
        vehicleRepository = VehicleRepository()
        sessionManager = SessionManager(this)

        FormScrollHelper.enable(binding.root)

        setupDropdownFields()
        setupClickListeners()
        loadVehicles()
    }

    private fun setupDropdownFields() {
        makePickerField(binding.etServiceType)
        makePickerField(binding.etVehicleInfo)
        makePickerField(binding.etPreferredDate)
        makePickerField(binding.etPreferredTime)

        clearFieldErrorOnInput(binding.etProblemDescription)

        binding.etVehicleInfo.hint = "Loading registered vehicles..."
    }

    private fun makePickerField(field: EditText) {
        field.isFocusable = false
        field.isCursorVisible = false
        field.isClickable = true
        field.keyListener = null
    }

    private fun setupClickListeners() {
        binding.btnSubmitAppointment.setOnClickListener {
            submitAppointment()
        }

        binding.btnCancel.setOnClickListener {
            finish()
            AppTransitions.close(this)
        }

        binding.etServiceType.setOnClickListener {
            showServiceTypePicker()
        }

        binding.etVehicleInfo.setOnClickListener {
            showVehiclePicker()
        }

        binding.etPreferredDate.setOnClickListener {
            showDatePicker()
        }

        binding.etPreferredTime.setOnClickListener {
            showTimePicker()
        }
    }

    private fun loadVehicles() {
        val token = sessionManager.getToken() ?: return

        vehicleRepository.getVehicles(token)
            .enqueue(object : Callback<ApiMessageResponse<List<Vehicle>>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<List<Vehicle>>>,
                    response: Response<ApiMessageResponse<List<Vehicle>>>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        vehicles = response.body()?.data ?: emptyList()
                        selectedVehicleInfo = null
                        binding.etVehicleInfo.text?.clear()

                        binding.etVehicleInfo.hint = if (vehicles.isEmpty()) {
                            "Register a vehicle before booking"
                        } else {
                            "Tap to select a registered vehicle"
                        }
                    } else {
                        vehicles = emptyList()
                        selectedVehicleInfo = null
                        binding.etVehicleInfo.text?.clear()
                        binding.etVehicleInfo.hint = "Register a vehicle before booking"
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<List<Vehicle>>>,
                    t: Throwable
                ) {
                    vehicles = emptyList()
                    selectedVehicleInfo = null
                    binding.etVehicleInfo.text?.clear()
                    binding.etVehicleInfo.hint = "Register a vehicle before booking"
                }
            })
    }

    private fun showServiceTypePicker() {
        binding.tvError.visibility = View.GONE
        clearFieldErrors(binding.etServiceType)

        showStyledOptionDialog(
            title = "Select Service Type",
            options = serviceTypes
        ) { which ->
            val selected = serviceTypes[which]
            binding.etServiceType.setText(selected)
            clearFieldErrors(binding.etServiceType)
        }
    }

    private fun showVehiclePicker() {
        binding.tvError.visibility = View.GONE
        clearFieldErrors(binding.etVehicleInfo)

        if (vehicles.isEmpty()) {
            showFieldError(binding.etVehicleInfo, "Register a vehicle before booking an appointment.")
            showRegisterVehiclePrompt()
            return
        }

        val labels = vehicles.map { it.displayName() }.toTypedArray()

        showStyledOptionDialog(
            title = "Select Vehicle",
            options = labels
        ) { which ->
            selectedVehicleInfo = labels[which]
            binding.etVehicleInfo.setText(labels[which])
            clearFieldErrors(binding.etVehicleInfo)
        }
    }

    private fun showDatePicker() {
        binding.tvError.visibility = View.GONE
        clearFieldErrors(binding.etPreferredDate)
        hideKeyboard()

        val dialog = Dialog(this)
        dialog.setCanceledOnTouchOutside(true)

        val container = createDialogContainer()

        val title = createDialogTitle("Select Preferred Date")
        container.addView(title)

        val datePicker = DatePicker(this)
        datePicker.minDate = Calendar.getInstance().timeInMillis

        val pickerParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        pickerParams.topMargin = dp(12)
        container.addView(datePicker, pickerParams)

        val buttonRow = LinearLayout(this)
        buttonRow.orientation = LinearLayout.HORIZONTAL
        buttonRow.gravity = Gravity.END

        val cancelButton = createDialogTextButton("Cancel", false)
        val selectButton = createDialogTextButton("Select", true)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        selectButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, datePicker.year)
            calendar.set(Calendar.MONTH, datePicker.month)
            calendar.set(Calendar.DAY_OF_MONTH, datePicker.dayOfMonth)

            val backendFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

            selectedDateValue = backendFormat.format(calendar.time)
            binding.etPreferredDate.setText(displayFormat.format(calendar.time))
            clearFieldErrors(binding.etPreferredDate)

            dialog.dismiss()
        }

        buttonRow.addView(cancelButton)
        buttonRow.addView(selectButton)

        val rowParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        rowParams.topMargin = dp(12)
        container.addView(buttonRow, rowParams)

        dialog.setContentView(container)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        val displayMetrics = resources.displayMetrics
        val dialogWidth = displayMetrics.widthPixels - dp(40)
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showTimePicker() {
        binding.tvError.visibility = View.GONE
        clearFieldErrors(binding.etPreferredTime)

        showStyledOptionDialog(
            title = "Select Preferred Time",
            options = timeLabels
        ) { which ->
            selectedTimeValue = timeValues[which]
            binding.etPreferredTime.setText(timeLabels[which])
            clearFieldErrors(binding.etPreferredTime)
        }
    }

    private fun submitAppointment() {
        val serviceType = binding.etServiceType.text.toString().trim()
        val vehicleInfo = selectedVehicleInfo.orEmpty()
        val problemDescription = binding.etProblemDescription.text.toString().trim()
        val dateValue = selectedDateValue.orEmpty()
        val timeValue = selectedTimeValue.orEmpty()

        binding.tvError.visibility = View.GONE

        clearFieldErrors(
            binding.etServiceType,
            binding.etVehicleInfo,
            binding.etPreferredDate,
            binding.etPreferredTime,
            binding.etProblemDescription
        )

        if (serviceType.isEmpty()) {
            showFieldError(binding.etServiceType, "Service type is required.")
            return
        }

        if (vehicles.isEmpty()) {
            showFieldError(binding.etVehicleInfo, "Register a vehicle before booking an appointment.")
            showRegisterVehiclePrompt()
            return
        }

        if (vehicleInfo.isEmpty()) {
            showFieldError(binding.etVehicleInfo, "Select one of your registered vehicles.")
            return
        }

        if (dateValue.isEmpty()) {
            showFieldError(binding.etPreferredDate, "Date is required.")
            return
        }

        if (timeValue.isEmpty()) {
            showFieldError(binding.etPreferredTime, "Time is required.")
            return
        }

        if (problemDescription.isEmpty()) {
            showFieldError(binding.etProblemDescription, "Problem description is required.")
            return
        }

        val token = sessionManager.getToken()

        if (token.isNullOrEmpty()) {
            binding.tvError.text = "Session expired. Please sign in again."
            binding.tvError.visibility = View.VISIBLE
            return
        }

        val scheduledDate = "${dateValue}T${timeValue}:00"

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmitAppointment.isEnabled = false

        val request = CreateAppointmentRequest(
            serviceType = serviceType,
            vehicleInfo = vehicleInfo,
            problemDescription = problemDescription,
            scheduledDate = scheduledDate
        )

        appointmentRepository.createAppointment(token, request)
            .enqueue(object : Callback<ApiMessageResponse<Appointment>> {
                override fun onResponse(
                    call: Call<ApiMessageResponse<Appointment>>,
                    response: Response<ApiMessageResponse<Appointment>>
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitAppointment.isEnabled = true

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(
                            this@BookAppointmentActivity,
                            "Appointment booked successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                        AppTransitions.close(this@BookAppointmentActivity)
                    } else {
                        binding.tvError.text = FriendlyError.fromResponse(
                            response,
                            "Validation failed. Please review your input."
                        )
                        binding.tvError.visibility = View.VISIBLE
                    }
                }

                override fun onFailure(
                    call: Call<ApiMessageResponse<Appointment>>,
                    t: Throwable
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitAppointment.isEnabled = true

                    binding.tvError.text = FriendlyError.fromThrowable(
                        t,
                        "Request failed. Please try again."
                    )
                    binding.tvError.visibility = View.VISIBLE
                }
            })
    }

    private fun showRegisterVehiclePrompt() {
        showStyledMessageDialog(
            title = "Vehicle Required",
            message = "Please register one of your vehicles before booking an appointment.",
            positiveText = "Manage Vehicles",
            negativeText = "Not Now"
        ) {
            startActivity(Intent(this, ManageVehiclesActivity::class.java))
            AppTransitions.open(this)
        }
    }

    private fun showStyledOptionDialog(
        title: String,
        options: Array<String>,
        onSelected: (Int) -> Unit
    ) {
        hideKeyboard()

        val dialog = Dialog(this)
        dialog.setCanceledOnTouchOutside(true)

        val container = createDialogContainer()

        val titleView = createDialogTitle(title)
        container.addView(titleView)

        val scrollView = ScrollView(this)
        val listContainer = LinearLayout(this)
        listContainer.orientation = LinearLayout.VERTICAL

        options.forEachIndexed { index, option ->
            val item = TextView(this)
            item.text = option
            item.textSize = 16f
            item.setTextColor(ContextCompat.getColor(this, R.color.cam_text))
            item.gravity = Gravity.CENTER_VERTICAL
            item.setPadding(dp(14), dp(16), dp(14), dp(16))
            applySelectableBackground(item)

            item.setOnClickListener {
                dialog.dismiss()
                onSelected(index)
            }

            listContainer.addView(
                item,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            if (index != options.lastIndex) {
                listContainer.addView(createDivider())
            }
        }

        scrollView.addView(listContainer)

        val scrollParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        scrollParams.topMargin = dp(12)
        val maxHeight = resources.displayMetrics.heightPixels / 2
        scrollParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

        container.addView(scrollView, scrollParams)

        dialog.setContentView(container)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        val displayMetrics = resources.displayMetrics
        val dialogWidth = displayMetrics.widthPixels - dp(40)

        scrollView.post {
            if (scrollView.height > maxHeight) {
                scrollView.layoutParams = scrollParams.apply { height = maxHeight }
            }
        }

        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showStyledMessageDialog(
        title: String,
        message: String,
        positiveText: String,
        negativeText: String,
        onPositive: () -> Unit
    ) {
        hideKeyboard()

        val dialog = Dialog(this)
        dialog.setCanceledOnTouchOutside(true)

        val container = createDialogContainer()

        container.addView(createDialogTitle(title))

        val messageView = TextView(this)
        messageView.text = message
        messageView.textSize = 15f
        messageView.setTextColor(ContextCompat.getColor(this, R.color.cam_subtext))
        messageView.setPadding(0, dp(10), 0, dp(12))
        container.addView(messageView)

        val buttonRow = LinearLayout(this)
        buttonRow.orientation = LinearLayout.HORIZONTAL
        buttonRow.gravity = Gravity.END

        val negativeButton = createDialogTextButton(negativeText, false)
        val positiveButton = createDialogTextButton(positiveText, true)

        negativeButton.setOnClickListener {
            dialog.dismiss()
        }

        positiveButton.setOnClickListener {
            dialog.dismiss()
            onPositive()
        }

        buttonRow.addView(negativeButton)
        buttonRow.addView(positiveButton)

        container.addView(buttonRow)

        dialog.setContentView(container)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        val displayMetrics = resources.displayMetrics
        val dialogWidth = displayMetrics.widthPixels - dp(40)
        dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun createDialogContainer(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@BookAppointmentActivity, R.drawable.bg_dialog_rounded)
            setPadding(dp(22), dp(20), dp(22), dp(22))
        }
    }

    private fun createDialogTitle(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(this@BookAppointmentActivity, R.color.cam_text))
        }
    }

    private fun createDialogTextButton(textValue: String, primary: Boolean): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(10), dp(16), dp(10))

            if (primary) {
                setTextColor(Color.WHITE)
                background = ContextCompat.getDrawable(this@BookAppointmentActivity, R.drawable.bg_primary_button)
            } else {
                setTextColor(ContextCompat.getColor(this@BookAppointmentActivity, R.color.cam_primary))
                background = ContextCompat.getDrawable(this@BookAppointmentActivity, R.drawable.bg_secondary_button)
            }

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.marginStart = dp(8)
            layoutParams = params
        }
    }

    private fun createDivider(): View {
        return View(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@BookAppointmentActivity, R.color.cam_border))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
            )
        }
    }

    private fun applySelectableBackground(view: View) {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        view.setBackgroundResource(typedValue.resourceId)
    }

    private fun hideKeyboard() {
        binding.root.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
