package com.jhonlauro.callamechanic.ui.mechanic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.databinding.ItemMechanicAppointmentBinding

class MechanicAppointmentAdapter(
    private val mode: Mode,
    private val onClaim: (Appointment) -> Unit,
    private val onStart: (Appointment) -> Unit,
    private val onFinish: (Appointment) -> Unit,
    private val onView: (Appointment) -> Unit
) : RecyclerView.Adapter<MechanicAppointmentAdapter.MechanicAppointmentViewHolder>() {

    enum class Mode {
        NEW_REQUESTS,
        ACTIVE_JOBS,
        FINISHED_JOBS
    }

    private var items: List<Appointment> = emptyList()

    inner class MechanicAppointmentViewHolder(
        private val binding: ItemMechanicAppointmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Appointment) {
            val normalizedStatus = normalizeStatus(item.status)

            binding.tvMechanicJobId.text = "#${item.id}"
            binding.tvMechanicStatus.text = formatStatus(normalizedStatus)
            binding.tvMechanicClient.text = item.client?.fullName ?: "Unknown Client"
            binding.tvMechanicContact.text = item.client?.phoneNumber ?: "No contact"
            binding.tvMechanicVehicle.text = item.vehicleInfo ?: "No vehicle info"
            binding.tvMechanicProblem.text = item.problemDescription ?: item.serviceType ?: "No problem description"
            binding.tvMechanicSchedule.text = item.scheduledDate ?: "No schedule"

            binding.btnMechanicPrimary.visibility = View.GONE
            binding.btnMechanicSecondary.visibility = View.VISIBLE
            binding.btnMechanicSecondary.setOnClickListener { onView(item) }
            binding.root.setOnClickListener { onView(item) }

            when (mode) {
                Mode.NEW_REQUESTS -> {
                    binding.btnMechanicPrimary.visibility = View.VISIBLE
                    binding.btnMechanicPrimary.text = "Claim Job"
                    binding.btnMechanicPrimary.setOnClickListener { onClaim(item) }
                }
                Mode.ACTIVE_JOBS -> {
                    if (normalizedStatus == "PENDING") {
                        binding.btnMechanicPrimary.visibility = View.VISIBLE
                        binding.btnMechanicPrimary.text = "Start Repair"
                        binding.btnMechanicPrimary.setOnClickListener { onStart(item) }
                    }
                    if (normalizedStatus == "IN_PROGRESS") {
                        binding.btnMechanicPrimary.visibility = View.VISIBLE
                        binding.btnMechanicPrimary.text = "Mark Finished"
                        binding.btnMechanicPrimary.setOnClickListener { onFinish(item) }
                    }
                }
                Mode.FINISHED_JOBS -> Unit
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MechanicAppointmentViewHolder {
        val binding = ItemMechanicAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MechanicAppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MechanicAppointmentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Appointment>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun normalizeStatus(status: String?): String {
        return if (status == "COMPLETED") "FINISHED" else status ?: "PENDING"
    }

    private fun formatStatus(status: String): String {
        return when (status) {
            "IN_PROGRESS" -> "In Progress"
            "FINISHED" -> "Finished"
            "CANCELLED" -> "Cancelled"
            else -> "Pending"
        }
    }
}
