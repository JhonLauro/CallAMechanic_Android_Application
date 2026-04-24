package com.jhonlauro.callamechanic.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.databinding.ItemAdminAppointmentBinding

class AdminAppointmentAdapter(
    private var items: List<Appointment>
) : RecyclerView.Adapter<AdminAppointmentAdapter.AdminAppointmentViewHolder>() {

    inner class AdminAppointmentViewHolder(
        private val binding: ItemAdminAppointmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Appointment) {
            binding.tvJobIdAdmin.text = "#${item.id}"
            binding.tvClientName.text = item.client?.fullName ?: "Unknown Client"
            binding.tvVehicleInfo.text = item.vehicleInfo ?: "-"
            binding.tvServiceTypeAdmin.text = item.serviceType ?: "-"
            binding.tvMechanicAdmin.text = item.mechanic?.fullName ?: "Unassigned"
            binding.tvStatusAdmin.text = formatStatus(item.status ?: "PENDING")
        }

        private fun formatStatus(status: String): String {
            return when (status) {
                "IN_PROGRESS" -> "In Progress"
                "COMPLETED" -> "Completed"
                "CANCELLED" -> "Cancelled"
                else -> "Pending"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminAppointmentViewHolder {
        val binding = ItemAdminAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdminAppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminAppointmentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Appointment>) {
        items = newItems
        notifyDataSetChanged()
    }
}