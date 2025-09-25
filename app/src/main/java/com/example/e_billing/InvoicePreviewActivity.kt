package com.example.e_billing

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.e_billing.Fragments.Dashboard
import com.example.e_billing.databinding.ActivityInvoicePreviewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

class InvoicePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInvoicePreviewBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var products: List<Dashboard.Product>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityInvoicePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve data from intent
        products = intent.getParcelableArrayListExtra<Dashboard.Product>("products") ?: arrayListOf()
        val customer = intent.getStringExtra("customerName") ?: "Unknown"
        val contact = intent.getStringExtra("customerContact") ?: "N/A"
        val discountValue = intent.getDoubleExtra("discountValue", 0.0)
        val isPercentageDiscount = intent.getBooleanExtra("isPercentageDiscount", false)
        val totalAmount = intent.getDoubleExtra("totalAmount", 0.0)
        val taxPercentage = intent.getDoubleExtra("taxInput", 0.0)
        val receivedPaymentMethod = intent.getStringExtra("paymentMethod")

        // Ensure valid payment method text
        val paymentMethod = receivedPaymentMethod?.trim()?.takeIf { it.isNotEmpty() } ?: "Not Specified"
        binding.paymentMethodDetails.text = "Payment Method: $paymentMethod"

        // Set customer details
        binding.billcustomerName.text = customer
        binding.billcustomerContact.text = contact

        // Generate invoice number
        binding.billinvoiceNumber.text = "INV-${System.currentTimeMillis().toString().takeLast(6)}"
        binding.billinvoiceDate.text = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        // Fill product table
        addProductsToTable(products)

        val subtotal = products.sumOf { it.price * it.quantity }
        Log.d("InvoiceDebug", "Subtotal: $subtotal")

// Calculate discount
        val discountAmount = if (isPercentageDiscount) {
            (subtotal * discountValue) / 100  // Percentage-based discount
        } else {
            discountValue  // Fixed amount discount
        }
        Log.d("InvoiceDebug", "Discount Amount: $discountAmount")

        val discountedSubtotal = maxOf(subtotal - discountAmount, 0.0)

// Get tax percentage from intent
        // Retrieve tax value as a String and convert it safely to Double
        val receivedTaxPercentage = intent.getDoubleExtra("taxPercentage", 0.0)
        Log.d("InvoiceDebug", "Received Tax Percentage: $receivedTaxPercentage")

        val taxAmount = (discountedSubtotal * receivedTaxPercentage) / 100
        Log.d("InvoiceDebug", "Calculated Tax Amount: $taxAmount")


// Calculate grand total
        val grandTotal = discountedSubtotal + taxAmount
        Log.d("InvoiceDebug", "Final Grand Total: $grandTotal")



        // Format amounts
        binding.subtotalAmount.text = "Subtotal: ₹${"%.2f".format(subtotal)}"
        binding.taxAmount.text = "Tax (${taxPercentage}%): ₹${"%.2f".format(taxAmount)}"
        binding.discountAmount.text = "Discount: ₹${"%.2f".format(discountAmount)}"
        binding.grandTotalAmount.text = "Total: ₹${"%.2f".format(grandTotal)}"


        // Fetch shop details from Firestore
        fetchShopDetails()

        // Proceed button to save invoice and navigate
        binding.fabProceed.setOnClickListener {
            saveInvoiceToDatabase()
        }
    }

    private fun fetchShopDetails() {
        val userEmail = auth.currentUser?.email
        if (userEmail == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            Log.e("FirestoreDebug", "User email is null, authentication required.")
            return
        }

        val shopRef = db.collection("Users")
            .document(userEmail)
            .collection("ShopInfo")
            .document("shopDetails") // ✅ Corrected document name

        shopRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val shopNameText = document.getString("shopName") ?: "No Shop Name"
                    val shopAddressText = document.getString("shopAddress") ?: "No Address"
                    val shopContactText = document.getString("contactNumber") ?: "No Contact Info"

                    runOnUiThread {
                        binding.shopName.text = shopNameText
                        binding.shopAddress.text = shopAddressText
                        binding.billshopContact.text = shopContactText
                    }
                } else {
                    Toast.makeText(this, "Shop details not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching shop details.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun saveInvoiceToDatabase() {
        val userEmail = auth.currentUser?.email
        if (userEmail == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            Log.e("FirestoreDebug", "User email is null, authentication required.")
            return
        }

        // Prepare invoice details
        val invoiceData = hashMapOf(
            "shopName" to binding.shopName.text.toString(),
            "shopAddress" to binding.shopAddress.text.toString(),
            "shopContact" to binding.billshopContact.text.toString(),
            "customerName" to binding.billcustomerName.text.toString(),
            "customerPhone" to binding.billcustomerContact.text.toString(),
            "invoiceDate" to binding.billinvoiceDate.text.toString(),
            "invoiceNumber" to binding.billinvoiceNumber.text.toString(),
            "subtotal" to binding.subtotalAmount.text.toString(),
            "tax" to binding.taxAmount.text.toString(),
            "discount" to binding.discountAmount.text.toString(),
            "grandTotal" to binding.grandTotalAmount.text.toString(),
            "paymentMethod" to binding.paymentMethodDetails.text.toString(),
            "products" to products.map { product ->
                mapOf(
                    "name" to product.name,
                    "quantity" to product.quantity,
                    "price" to product.price,
                    "total" to (product.price * product.quantity)
                )
            }
        )

        // Firestore Reference
        val invoiceRef = db.collection("Users")
            .document(userEmail)
            .collection("Invoices")
            .document() // Auto-generate unique invoice ID

        // Save data to Firestore
        invoiceRef.set(invoiceData)
            .addOnSuccessListener {
                Toast.makeText(this, "Invoice Saved Successfully!", Toast.LENGTH_SHORT).show()
                Log.d("FirestoreDebug", "Invoice successfully saved!")

                // ✅ Now, start SendBillActivity with the correct intent data
                val intent = Intent(this, SendMSG::class.java)

                // 🏪 Shop Details
                intent.putExtra("shopName", binding.shopName.text.toString())
                intent.putExtra("shopAddress", binding.shopAddress.text.toString())
                intent.putExtra("shopContact", binding.billshopContact.text.toString())

                // 👤 Customer Details
                intent.putExtra("customerName", binding.billcustomerName.text.toString())
                intent.putExtra("customerPhone", binding.billcustomerContact.text.toString())

                // 🧾 Invoice Details
                intent.putExtra("invoiceDate", binding.billinvoiceDate.text.toString())
                intent.putExtra("invoiceNumber", binding.billinvoiceNumber.text.toString())

                // 💰 Pricing Details
                intent.putExtra("subtotal", binding.subtotalAmount.text.toString())
                intent.putExtra("tax", binding.taxAmount.text.toString())
                intent.putExtra("discount", binding.discountAmount.text.toString())
                intent.putExtra("grandTotal", binding.grandTotalAmount.text.toString())
                intent.putExtra("paymentMethod", binding.paymentMethodDetails.text.toString())

                // 📦 Convert product list to a `Parcelable` ArrayList
                val productList = ArrayList<ProductParcelable>()
                for (product in products) {
                    productList.add(
                        ProductParcelable(
                            product.name,
                            product.quantity,
                            product.price,
                            product.price * product.quantity
                        )
                    )
                }

                // ✅ Pass the Parcelable product list
                intent.putParcelableArrayListExtra("products", productList)

                // Start SendBillActivity with all data
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDebug", "Error saving invoice", e)
                Toast.makeText(this, "Failed to save invoice", Toast.LENGTH_SHORT).show()
            }
    }




    private fun addProductsToTable(products: List<Dashboard.Product>) {
        binding.billproductTable.removeAllViews()

        // Table Header Row
        val headerRow = TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(ContextCompat.getColor(this@InvoicePreviewActivity, R.color.light_gray))
        }

        val headers = listOf("Product", "Qty", "Price", "Total")
        headers.forEach { text ->
            val textView = TextView(this).apply {
                this.text = text
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                textSize = 14f
                setPadding(10, 10, 10, 10)
                layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            headerRow.addView(textView)
        }

        binding.billproductTable.addView(headerRow)

        // Adding Products Dynamically
        for (product in products) {
            val row = TableRow(this).apply {
                layoutParams = TableRow.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val columns = listOf(
                product.name,
                product.quantity.toString(),
                "₹${"%.2f".format(product.price)}",
                "₹${"%.2f".format(product.price * product.quantity)}"
            )

            columns.forEach { text ->
                val textView = TextView(this).apply {
                    this.text = text
                    gravity = Gravity.CENTER
                    layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(8, 8, 8, 8)
                }
                row.addView(textView)
            }

            binding.billproductTable.addView(row)
        }
    }

}
