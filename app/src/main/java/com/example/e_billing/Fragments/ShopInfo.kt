package com.example.e_billing.Fragments

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.e_billing.databinding.FragmentShopInfoBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ShopInfo : Fragment() {

    private var _binding: FragmentShopInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences("ShopInfo", Context.MODE_PRIVATE)
        loadShopData() // Load locally stored data first

        val userEmail = auth.currentUser?.email
        val userName = auth.currentUser?.displayName ?: "User"

        if (userEmail != null) {
            loadShopDataFromFirestore(userEmail)

            binding.saveShopInfoButton.setOnClickListener {
                saveShopData(userEmail, userName)
            }

            binding.resetShopInfoButton.setOnClickListener {
                showResetConfirmationDialog()
            }
        } else {
            showSnackbar("User not logged in")
        }
    }

    private fun saveShopData(userEmail: String, userName: String) {
        if (!isNetworkAvailable()) {
            showSnackbar("No internet connection!")
            return
        }

        val name = binding.shopName.text.toString().trim()
        val owner = binding.ownerName.text.toString().trim()
        val contact = binding.contactNumber.text.toString().trim()
        val address = binding.shopAddress.text.toString().trim()

        if (name.isEmpty() || owner.isEmpty() || contact.isEmpty() || address.isEmpty()) {
            showSnackbar("Please fill all fields")
            return
        }

        if (!Patterns.PHONE.matcher(contact).matches() || contact.length != 10) {
            showSnackbar("Enter a valid 10-digit contact number")
            return
        }

        showLoading(true)

        val shopInfo = hashMapOf(
            "shopName" to name,
            "ownerName" to owner,
            "contactNumber" to contact,
            "shopAddress" to address,
            "userName" to userName
        )

        val shopDocRef = db.collection("Users").document(userEmail)
            .collection("ShopInfo").document("shopDetails")

        shopDocRef.set(shopInfo)
            .addOnSuccessListener {
                showSnackbar("Shop info saved successfully!")
                saveToSharedPreferences(name, owner, contact, address, userName)
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showSnackbar("Error saving shop info: ${e.message}")
                showLoading(false)
            }
    }

    private fun loadShopData() {
        binding.apply {
            shopName.setText(sharedPreferences.getString("shopName", "My Shop"))
            ownerName.setText(sharedPreferences.getString("ownerName", ""))
            contactNumber.setText(sharedPreferences.getString("contactNumber", ""))
            shopAddress.setText(sharedPreferences.getString("shopAddress", ""))
        }
    }

    private fun loadShopDataFromFirestore(userEmail: String) {
        if (!isNetworkAvailable()) {
            showSnackbar("No internet connection!")
            return
        }

        showLoading(true)
        val shopDocRef = db.collection("Users").document(userEmail)
            .collection("ShopInfo").document("shopDetails")

        shopDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.apply {
                        shopName.setText(document.getString("shopName") ?: "My Shop")
                        ownerName.setText(document.getString("ownerName") ?: "")
                        contactNumber.setText(document.getString("contactNumber") ?: "")
                        shopAddress.setText(document.getString("shopAddress") ?: "")
                    }
                }
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showSnackbar("Error loading data: ${e.message}")
                showLoading(false)
            }
    }

    private fun saveToSharedPreferences(name: String, owner: String, contact: String, address: String, userName: String) {
        sharedPreferences.edit().apply {
            putString("shopName", name)
            putString("ownerName", owner)
            putString("contactNumber", contact)
            putString("shopAddress", address)
            putString("userName", userName)
            apply()
        }
    }

    private fun resetFields() {
        binding.apply {
            shopName.text?.clear()
            ownerName.text?.clear()
            contactNumber.text?.clear()
            shopAddress.text?.clear()
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset Fields")
            .setMessage("Are you sure you want to clear all fields?")
            .setPositiveButton("Yes") { _, _ -> resetFields() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.saveShopInfoButton.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
