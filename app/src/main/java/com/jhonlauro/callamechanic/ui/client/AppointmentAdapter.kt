package com.jhonlauro.callamechanic.ui.client

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jhonlauro.callamechanic.data.model.Appointment
import com.jhonlauro.callamechanic.databinding.ItemAppointmentBinding

class AppointmentAdapter(
    private var items: List<Appointment>
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    inner class AppointmentViewHolder(
        private val binding: ItemAppointmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Appointment) {
            binding.tvJobId.text = "#${item.id}"
            binding.tvVehicle.text = item.vehicleInfo ?: "Vehicle"
            binding.tvDescription.text = item.problemDescription ?: item.serviceType ?: "-"
            binding.tvDate.text = item.scheduledDate ?: "-"
            binding.tvMechanic.text = item.mechanic?.fullName ?: "Awaiting Assignment"
            binding.tvStatus.text = formatStatus(item.status ?: "PENDING")
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Appointment>) {
        items = newItems
        notifyDataSetChanged()
    }
}