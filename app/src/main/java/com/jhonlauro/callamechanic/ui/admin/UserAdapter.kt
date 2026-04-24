package com.jhonlauro.callamechanic.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jhonlauro.callamechanic.data.model.AdminUser
import com.jhonlauro.callamechanic.databinding.ItemUserBinding

class UserAdapter(
    private var items: List<AdminUser>,
    private val onDeleteClick: (AdminUser) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AdminUser) {
            binding.tvUserName.text = item.fullName ?: "No name"
            binding.tvUserEmail.text = item.email ?: "-"
            binding.tvUserRole.text = item.role ?: "-"
            binding.tvUserExtra.text = if (!item.mechanicId.isNullOrEmpty()) {
                "Mechanic ID: ${item.mechanicId}"
            } else {
                "Phone: ${item.phoneNumber ?: "-"}"
            }

            binding.btnDeleteUser.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<AdminUser>) {
        items = newItems
        notifyDataSetChanged()
    }
}