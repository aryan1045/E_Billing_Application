package com.example.e_billing.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.e_billing.databinding.FragmentInvoiceBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Invoice : Fragment() {

    private lateinit var binding: FragmentInvoiceBinding
    private val invoiceList = mutableListOf<InvoiceModel>()
    private val filteredList = mutableListOf<InvoiceModel>()
    private lateinit var invoicesAdapter: InvoicesAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInvoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = auth.currentUser?.email

        if (userId != null) {
            fetchInvoices(userId)
        } else {
            showSnackbar("User not logged in")
        }

        setupRecyclerView()
        setupSearchFunctionality()
        setupFloatingButton()
    }

    private fun setupRecyclerView() {
        invoicesAdapter = InvoicesAdapter(filteredList)

        binding.TodayRV.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = invoicesAdapter
        }

        binding.allInvRV.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = invoicesAdapter
        }
    }

    private fun fetchInvoices(userId: String) {
        db.collection("Users").document(userId).collection("Invoices")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showSnackbar("No invoices found")
                } else {
                    invoiceList.clear()
                    for (document in documents) {
                        val invoice = InvoiceModel(
                            customerName = document.getString("customerName") ?: "Unknown",
                            invoiceDate = document.getString("invoiceDate") ?: "N/A",
                            grandTotal = document.getString("grandTotal") ?: "₹0.00"
                        )
                        invoiceList.add(invoice)
                    }
                    filteredList.clear()
                    filteredList.addAll(invoiceList)
                    invoicesAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                showSnackbar("Error fetching invoices: ${e.message}")
            }
    }

    private fun setupSearchFunctionality() {
        binding.searchInvoice.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val query = newText?.trim() ?: ""
                filteredList.clear()

                if (query.isEmpty()) {
                    filteredList.addAll(invoiceList)
                } else {
                    filteredList.addAll(invoiceList.filter {
                        it.customerName.contains(query, ignoreCase = true)
                    })
                }

                invoicesAdapter.notifyDataSetChanged()
                return true
            }
        })
    }

    private fun setupFloatingButton() {
        binding.addInvoiceFAB.setOnClickListener {
            showSnackbar("Add Invoice Clicked! Implement Navigation Here.")
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    data class InvoiceModel(
        val customerName: String,
        val invoiceDate: String,
        val grandTotal: String
    )

    class InvoicesAdapter(private val invoices: MutableList<InvoiceModel>) :
        RecyclerView.Adapter<InvoicesAdapter.InvoiceViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                com.example.e_billing.R.layout.item_invoice, parent, false
            )
            return InvoiceViewHolder(view)
        }

        override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
            val invoice = invoices[position]
            holder.customerName.text = invoice.customerName
            holder.invoiceDate.text = invoice.invoiceDate
            holder.totalAmount.text = invoice.grandTotal
        }

        override fun getItemCount(): Int = invoices.size

        class InvoiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val customerName: android.widget.TextView = itemView.findViewById(
                com.example.e_billing.R.id.customerNameTextView
            )
            val invoiceDate: android.widget.TextView = itemView.findViewById(
                com.example.e_billing.R.id.invoiceDateTextView
            )
            val totalAmount: android.widget.TextView = itemView.findViewById(
                com.example.e_billing.R.id.totalAmountTextView
            )
        }
    }
}
