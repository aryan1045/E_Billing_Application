package com.example.e_billing.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.e_billing.databinding.InvoiceItemsBinding

class InvoicesAdapter(private val list: List<String>) :
    RecyclerView.Adapter<InvoicesAdapter.Holder>() {

    class Holder(binding: InvoiceItemsBinding) : RecyclerView.ViewHolder(binding.root) {
        val custName = binding.CustName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = InvoiceItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.custName.text = list[position]
    }

    override fun getItemCount(): Int = list.size
}
