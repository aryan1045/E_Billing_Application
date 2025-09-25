package com.example.e_billing.Fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.e_billing.R
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import androidx.appcompat.widget.SwitchCompat
import com.example.e_billing.InvoicePreviewActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot

class Dashboard : Fragment() {

    private lateinit var searchProduct: AutoCompleteTextView
    private lateinit var discountInput: EditText
    private lateinit var grandTotal: TextView
    private lateinit var productTable: TableLayout
    private lateinit var firestore: FirebaseFirestore
    private val productMap = mutableMapOf<String, Double>()
    private val selectedProducts = mutableMapOf<String, Product>()
    private lateinit var previewInvoiceBtn: Button
    private lateinit var customerNameInput: EditText
    private lateinit var customerContactInput: EditText
    private lateinit var customerNameLayout: TextInputLayout
    private lateinit var customerContactLayout: TextInputLayout
    private lateinit var discountToggle: SwitchCompat
    private lateinit var taxInput: EditText


    // Payment UI Elements
    private lateinit var paymentMethodSpinner: AutoCompleteTextView
    private lateinit var paymentDetailsLayout: TextInputLayout
    private lateinit var paymentDetails: TextInputEditText

    data class Product(val name: String, var price: Double, var quantity: Int) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readDouble(),
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            parcel.writeDouble(price)
            parcel.writeInt(quantity)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<Product> {
            override fun createFromParcel(parcel: Parcel) = Product(parcel)
            override fun newArray(size: Int) = arrayOfNulls<Product?>(size)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        firestore = FirebaseFirestore.getInstance()
        searchProduct = view.findViewById(R.id.searchProduct)
        discountInput = view.findViewById(R.id.discountInput)
        discountToggle = view.findViewById(R.id.discountToggle)
        taxInput = view.findViewById(R.id.taxInput)

        grandTotal = view.findViewById(R.id.grandTotal)
        productTable = view.findViewById(R.id.billingTable)
        previewInvoiceBtn = view.findViewById(R.id.previewInvoiceBtn)
        customerNameInput = view.findViewById(R.id.customerNameInput)
        customerContactInput = view.findViewById(R.id.customerPhoneInput)
        customerNameLayout = view.findViewById(R.id.customerNameLayout)
        customerContactLayout = view.findViewById(R.id.customerPhoneLayout)

        // Payment UI Elements
        paymentMethodSpinner = view.findViewById(R.id.paymentMethodSpinner)
        paymentDetailsLayout = view.findViewById(R.id.paymentDetailsLayout)
        paymentDetails = view.findViewById(R.id.paymentDetailsEditText)

        setupPaymentMethodDropdown()
        fetchProducts()
        setupCustomerNameValidation()
        setupCustomerPhoneValidation()

        searchProduct.setOnItemClickListener { _, _, position, _ ->
            val selectedProduct = searchProduct.adapter.getItem(position) as String
            val selectedPrice = productMap[selectedProduct] ?: 0.0
            addOrUpdateProduct(selectedProduct, selectedPrice)
            searchProduct.text.clear()
        }

        discountToggle.setOnCheckedChangeListener { _, isChecked ->
            discountToggle.text = if (isChecked) "%" else "₹" // Force update visibility
            updateTotals()
        }

        // Listen for discount input changes
        discountInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                discountInput.removeTextChangedListener(this)
                updateTotals()
                discountInput.addTextChangedListener(this)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


        taxInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                taxInput.removeTextChangedListener(this)
                updateTotals() // Ensure total updates dynamically
                taxInput.addTextChangedListener(this)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })



        previewInvoiceBtn.setOnClickListener { previewInvoice() }

        return view
    }

    private fun setupCustomerNameValidation() {
        val handler = android.os.Handler()
        var runnable: Runnable? = null

        // Restrict input type to text only (No numbers or special characters)
        customerNameInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS

        customerNameInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                runnable?.let { handler.removeCallbacks(it) }  // Remove any pending validation

                runnable = Runnable {
                    val name = s.toString().trim()
                    when {
                        name.isEmpty() -> customerNameLayout.error = "Name cannot be empty"
                        name.length < 3 -> customerNameLayout.error = "Name must be at least 3 characters"
                        !name.matches(Regex("^[a-zA-Z\\s]+$")) -> customerNameLayout.error = "Only alphabets are allowed"
                        else -> customerNameLayout.error = null
                    }
                }

                handler.postDelayed(runnable!!, 500)  // Delay validation by 500ms
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }


    private fun setupCustomerPhoneValidation() {
        val handler = android.os.Handler()
        var runnable: Runnable? = null

        customerContactInput.inputType = InputType.TYPE_CLASS_NUMBER  // Restrict input to numbers

        customerContactInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                runnable?.let { handler.removeCallbacks(it) }  // Remove any pending validation

                runnable = Runnable {
                    val phone = s.toString().trim()
                    when {
                        phone.isEmpty() -> customerContactLayout.error = "Phone number cannot be empty"
                        !phone.matches(Regex("^[0-9]{10}$")) -> customerContactLayout.error = "Enter a valid 10-digit phone number"
                        else -> customerContactLayout.error = null
                    }
                }

                handler.postDelayed(runnable!!, 500)  // Delay validation by 500ms
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }




    private fun setupPaymentMethodDropdown() {
        val paymentOptions = listOf("Cash", "Card", "UPI", "Net Banking")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, paymentOptions)
        paymentMethodSpinner.setAdapter(adapter)

        paymentMethodSpinner.setOnItemClickListener { _, _, position, _ ->
            val selectedMethod = paymentOptions[position]
            if (selectedMethod == "Cash") {
                paymentDetailsLayout.visibility = View.GONE
            } else {
                paymentDetailsLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun fetchProducts() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        if (userEmail == null) {
            Log.e("Firestore", "User not logged in")
            return
        }

        val firestore = FirebaseFirestore.getInstance() // Ensure Firestore is initialized

        firestore.collection("Users").document(userEmail) // Using email as the document ID
            .collection("Products")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded) return@addOnSuccessListener  // Ensure fragment is still attached

                val productList = mutableListOf<String>()
                val tempProductMap = mutableMapOf<String, Double>() // Temporary map to prevent concurrency issues

                for (document in documents) {
                    val name = document.id // Since product name is the document ID
                    val price = getProductPrice(document) // Use the safe getProductPrice method
                    tempProductMap[name] = price
                    productList.add(name)
                }

                productMap.clear()
                productMap.putAll(tempProductMap)

                activity?.runOnUiThread {
                    if (productList.isEmpty()) {
                        Toast.makeText(requireContext(), "No products available", Toast.LENGTH_SHORT).show()
                    } else {
                        searchProduct.setAdapter(
                            ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                productList
                            )
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching products", e)
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Error fetching products", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Safely retrieve price and handle unexpected types
    private fun getProductPrice(document: DocumentSnapshot): Double {
        return when (val price = document.get("price")) {
            is Double -> price
            is Float -> price.toDouble()
            is String -> price.toDoubleOrNull() ?: 0.0 // If it's a string, try to convert it to Double
            else -> 0.0 // Default to 0.0 if price is not found or in an unsupported type
        }
    }



    private fun addOrUpdateProduct(name: String, price: Double) {
        val existingProduct = selectedProducts[name]
        if (existingProduct != null) {
            existingProduct.quantity++
            updateProductRow(existingProduct)
        } else {
            val newProduct = Product(name, price, 1)
            selectedProducts[name] = newProduct
            addProductRow(newProduct)
        }
        updateTotals()
    }

    private fun addProductRow(product: Product) {
        val row = TableRow(requireContext())
        row.addView(TextView(requireContext()).apply {
            text = product.name
            setPadding(8, 8, 8, 8)
        })
        row.addView(EditText(requireContext()).apply {
            setText(product.quantity.toString())
            setPadding(8, 8, 8, 8)
            inputType = InputType.TYPE_CLASS_NUMBER
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val newQuantity = s.toString().toIntOrNull() ?: 1
                    product.quantity = newQuantity

                    // ✅ Update total price dynamically in the row
                    (row.getChildAt(3) as TextView).text = "₹${product.price * newQuantity}"

                    updateTotals()
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        })
        row.addView(TextView(requireContext()).apply {
            text = "₹${product.price}"
            setPadding(8, 8, 8, 8)
        })
        row.addView(TextView(requireContext()).apply {
            text = "₹${product.price * product.quantity}"
            setPadding(8, 8, 8, 8)
        })
        productTable.addView(row)
    }


    private fun updateProductRow(product: Product) {
        for (i in 0 until productTable.childCount) {
            val row = productTable.getChildAt(i) as TableRow
            val nameView = row.getChildAt(0) as TextView
            if (nameView.text.toString() == product.name) {
                (row.getChildAt(1) as EditText).setText(product.quantity.toString())
                (row.getChildAt(3) as TextView).text = "₹${product.price * product.quantity}"
                break
            }
        }
    }


    private fun updateTotals() {
        var total = selectedProducts.values.sumOf { it.price * it.quantity }

        val discountValue = discountInput.text.toString().toDoubleOrNull() ?: 0.0
        val isPercentageDiscount = discountToggle.isChecked

        // ✅ Check tax input
        Log.d("TaxDebug", "Raw Tax Input: '${taxInput.text}'")
        val taxRate = taxInput.text.toString().trim().toDoubleOrNull() ?: 0.0
        Log.d("TaxDebug", "Parsed Tax Rate: $taxRate%")

        if (taxRate < 0) {
            taxInput.error = "Invalid tax value"
            return
        }

        // Apply Discount
        val discountAmount = if (isPercentageDiscount) {
            (total * discountValue) / 100
        } else {
            discountValue
        }

        val subtotal = maxOf(total - discountAmount, 0.0)
        Log.d("TaxDebug", "Subtotal after discount: ₹$subtotal")

        // ✅ Ensure Tax is Calculated Properly
        val taxAmount = (subtotal * taxRate) / 100.0
        Log.d("TaxDebug", "Calculated Tax Amount: ₹$taxAmount") // Debugging

        val finalTotal = subtotal + taxAmount
        Log.d("TaxDebug", "Final Grand Total: ₹$finalTotal")

        // ✅ Update UI
        grandTotal.text = "₹${"%.2f".format(finalTotal)}"
    }





    private fun previewInvoice() {
        val customerName = customerNameInput.text.toString()
        val customerContact = customerContactInput.text.toString()

        if (customerName.isBlank() || customerContact.isBlank()) {
            Toast.makeText(requireContext(), "Enter customer details", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Get tax percentage from taxInput
        val taxPercentage = taxInput.text.toString().toDoubleOrNull() ?: 0.0

        // ✅ Get discount value and type (₹ or %)
        val discountValue = discountInput.text.toString().toDoubleOrNull() ?: 0.0
        val isPercentageDiscount = discountToggle.isChecked  // true = %, false = ₹

        val intent = Intent(requireContext(), InvoicePreviewActivity::class.java).apply {
            putParcelableArrayListExtra("products", ArrayList(selectedProducts.values))
            putExtra("customerName", customerName)
            putExtra("customerContact", customerContact)

            // ✅ Pass discount and its type
            putExtra("discountValue", discountValue)
            putExtra("isPercentageDiscount", isPercentageDiscount)

            // ✅ Pass tax percentage retrieved from input
            putExtra("taxPercentage", taxPercentage)

            // ✅ Correct way to get selected value from AutoCompleteTextView
            val selectedPaymentMethod = paymentMethodSpinner.text.toString().trim()
            putExtra("paymentMethod", selectedPaymentMethod)

            // ✅ Debug logs
            Log.d("InvoiceDebug", "Sending Discount: $discountValue (${if (isPercentageDiscount) "%" else "₹"})")
            Log.d("InvoiceDebug", "Sending Tax Percentage: $taxPercentage")
            Log.d("PaymentDebug", "Sending Payment Method: '$selectedPaymentMethod'")
        }

        startActivity(intent)
    }

}
