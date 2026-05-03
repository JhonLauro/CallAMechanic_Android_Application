package com.jhonlauro.callamechanic.ui.client

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jhonlauro.callamechanic.data.model.Vehicle
import com.jhonlauro.callamechanic.databinding.ItemVehicleBinding

class VehicleAdapter(
    private var items: List<Vehicle>,
    private val onDelete: (Vehicle) -> Unit
) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    inner class VehicleViewHolder(
        private val binding: ItemVehicleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Vehicle) {
            binding.tvVehicleName.text = listOfNotNull(item.make, item.model)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "Registered Vehicle" }
            binding.tvVehicleMeta.text = listOfNotNull(item.year, item.color, item.type)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
                .ifBlank { "Vehicle details" }
            binding.tvVehiclePlate.text =
                "Plate: ${item.plateNumber ?: "N/A"} • Recall: ${item.recallStatus ?: "No Recall"}"
            binding.btnDeleteVehicle.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val binding = ItemVehicleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VehicleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Vehicle>) {
        items = newItems
        notifyDataSetChanged()
    }
}

