package com.example.e_billing.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.e_billing.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddFragment : Fragment() {

    private lateinit var productCodeEditText: TextInputEditText
    private lateinit var productNameEditText: TextInputEditText
    private lateinit var productPriceEditText: TextInputEditText
    private lateinit var itemsTable: TableLayout
    private lateinit var addButton: MaterialButton
    private lateinit var removeButton: MaterialButton
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add, container, false)

        // Initialize views
        productCodeEditText = view.findViewById(R.id.productCode)
        productNameEditText = view.findViewById(R.id.productname)
        productPriceEditText = view.findViewById(R.id.productPrice)
        itemsTable = view.findViewById(R.id.itemsTable)
        addButton = view.findViewById(R.id.addButton)
        removeButton = view.findViewById(R.id.removeButton)

        // Fetch products from Firestore for the logged-in user
        fetchProductsFromFirestore()

        // Add button click listener
        addButton.setOnClickListener {
            addProductToFirestore()
        }

        // Remove button click listener
        removeButton.setOnClickListener {
            removeProductFromFirestore()
        }

        return view
    }

    private fun addProductToFirestore() {
        val productCode = productCodeEditText.text.toString()
        val productName = productNameEditText.text.toString()
        val productPrice = productPriceEditText.text.toString()
        val userEmail = auth.currentUser?.email

        if (userEmail == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (productCode.isNotEmpty() && productName.isNotEmpty() && productPrice.isNotEmpty()) {
            val product = hashMapOf(
                "code" to productCode,
                "name" to productName,
                "price" to productPrice
            )

            // Store product under Users/{userEmail}/Products/{productName}
            db.collection("Users").document(userEmail).collection("Products").document(productName)
                .set(product)
                .addOnSuccessListener {
                    Toast.makeText(context, "Product Added!", Toast.LENGTH_SHORT).show()
                    addItemToTable(productCode, productName, productPrice)
                    clearInputs()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error adding product", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchProductsFromFirestore() {
        val userEmail = auth.currentUser?.email

        if (userEmail == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch products under Users/{userEmail}/Products
        db.collection("Users").document(userEmail).collection("Products").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val productCode = document.getString("code") ?: ""
                    val productName = document.getString("name") ?: ""
                    val productPrice = document.getString("price") ?: ""
                    addItemToTable(productCode, productName, productPrice)
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error fetching products", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addItemToTable(productCode: String, productName: String, productPrice: String) {
        val tableRow = TableRow(activity)

        val productCodeTextView = TextView(activity).apply {
            text = productCode
            setPadding(16, 16, 16, 16)
        }

        val productNameTextView = TextView(activity).apply {
            text = productName
            setPadding(16, 16, 16, 16)
        }

        val productPriceTextView = TextView(activity).apply {
            text = productPrice
            setPadding(16, 16, 16, 16)
        }

        // Add TextViews to TableRow
        tableRow.addView(productCodeTextView)
        tableRow.addView(productNameTextView)
        tableRow.addView(productPriceTextView)

        // Add TableRow to TableLayout
        itemsTable.addView(tableRow)
    }

    private fun clearInputs() {
        productCodeEditText.text?.clear()
        productNameEditText.text?.clear()
        productPriceEditText.text?.clear()
    }

    private fun removeProductFromFirestore() {
        val productNameToRemove = productNameEditText.text.toString()
        val userEmail = auth.currentUser?.email

        if (userEmail == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (productNameToRemove.isNotEmpty()) {
            // Remove product from Users/{userEmail}/Products/{productName}
            db.collection("Users").document(userEmail).collection("Products").document(productNameToRemove)
                .delete()
                .addOnSuccessListener {
                    removeItemFromTable(productNameToRemove)
                    Toast.makeText(context, "Product Removed!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error removing product", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Enter product name to remove", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeItemFromTable(productNameToRemove: String) {
        val childCount = itemsTable.childCount
        for (i in 1 until childCount) {
            val tableRow = itemsTable.getChildAt(i) as TableRow
            val productNameTextView = tableRow.getChildAt(1) as TextView // Get product name column

            if (productNameTextView.text.toString() == productNameToRemove) {
                itemsTable.removeViewAt(i)
                break
            }
        }
    }
}
