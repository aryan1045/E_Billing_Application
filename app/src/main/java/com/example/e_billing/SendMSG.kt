package com.example.e_billing

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.e_billing.Fragments.Dashboard
import okhttp3.*
import java.io.IOException
import java.util.concurrent.Executors

class SendMSG : AppCompatActivity() {

    companion object {
        private const val ACCOUNT_SID = "AC5ca7a983b2598ede4ebc1f9b7074ee22"
        private const val AUTH_TOKEN = "437c2ef7eecd4756e3748f492d99bacb"
        private const val TWILIO_WHATSAPP_NUMBER = "whatsapp:+14155238886"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AppDebug", "SendMSG Activity Started")

        val extras = intent.extras
        if (extras == null) {
            Log.e("AppDebug", "Intent extras are NULL! Activity finishing...")
            Toast.makeText(this, "Intent data missing!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val customerNumber = extras.getString("customerPhone")
        if (customerNumber.isNullOrEmpty()) {
            Log.e("AppDebug", "Customer number not found!")
            Toast.makeText(this, "Customer number not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val shopName = extras.getString("shopName", "Unknown Shop")
        val shopAddress = extras.getString("shopAddress", "No Address")
        val shopContact = extras.getString("shopContact", "No Contact")
        val customerName = extras.getString("customerName", "Customer")
        val invoiceNumber = extras.getString("invoiceNumber", "N/A")
        val invoiceDate = extras.getString("invoiceDate", "N/A")
        val subtotal = extras.getString("subtotal", "0.00")
        val tax = extras.getString("tax", "0.00")
        val discount = extras.getString("discount", "0.00")
        val grandTotal = extras.getString("grandTotal", "0.00")
        val paymentMethod = extras.getString("paymentMethod", "Unknown")

        val productData: ArrayList<ProductParcelable>? = extras.getParcelableArrayList("products")
        if (productData.isNullOrEmpty()) {
            Log.e("IntentError", "Failed to retrieve products or it's empty!")
            Toast.makeText(this, "Product data missing!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val billMessage = buildBillMessage(
            shopName, shopAddress, shopContact, customerName, invoiceNumber,
            invoiceDate, subtotal, tax, discount, grandTotal, paymentMethod, productData
        )

        val formattedNumber = formatPhoneNumber(customerNumber)

        sendWhatsAppMessage(formattedNumber, billMessage)
    }

    private fun formatPhoneNumber(number: String?): String {
        if (number.isNullOrEmpty()) {
            Log.e("PhoneFormat", "Received null or empty number!")
            return "whatsapp:UNKNOWN"
        }

        val formatted = when {
            number.length == 10 && number.all { it.isDigit() } -> "whatsapp:+91$number"
            number.startsWith("+") -> "whatsapp:$number"
            number.startsWith("whatsapp:") -> number
            else -> {
                Log.e("PhoneFormat", "Invalid phone number format: $number")
                "whatsapp:UNKNOWN"
            }
        }
        Log.d("PhoneFormat", "Final Formatted Number: $formatted")
        return formatted
    }

    private fun sendWhatsAppMessage(customerNumber: String, messageBody: String) {
        Log.d("TwilioDebug", "Preparing to send message to: $customerNumber")

        val client = OkHttpClient()
        val url = "https://api.twilio.com/2010-04-01/Accounts/$ACCOUNT_SID/Messages.json"

        val body = FormBody.Builder()
            .add("To", customerNumber)
            .add("From", TWILIO_WHATSAPP_NUMBER)
            .add("Body", messageBody)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", Credentials.basic(ACCOUNT_SID, AUTH_TOKEN))
            .build()

        Executors.newSingleThreadExecutor().execute {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("TwilioError", "Failed to send message: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@SendMSG, "Failed to send bill!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    Log.d("TwilioDebug", "Response: $responseBody")

                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@SendMSG, "Bill sent successfully!", Toast.LENGTH_SHORT).show()
                           // val intent = Intent(this@SendMSG, Dashboard::class.java)
                            //intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            // startActivity(intent)


                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@SendMSG, "Failed to send bill!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }

    private fun buildBillMessage(
        shopName: String, shopAddress: String, shopContact: String,
        customerName: String, invoiceNumber: String, invoiceDate: String,
        subtotal: String, tax: String, discount: String, grandTotal: String,
        paymentMethod: String, productList: List<ProductParcelable>
    ): String {
        val productDetails = productList.joinToString("\n") { product ->
            "📌 *${product.name}*  |  🛒 *Qty:* ${product.quantity}  |  💰 *₹${product.price}*  ➝  🏷 *₹${product.total}*"
        }

        return """
        ┏━━━━━━━━━━━━━━━💳 *INVOICE* 💳━━━━━━━━━━━━━━━┓
        🏪 *$shopName*
        📍 _$shopAddress
        📞 *Contact:* $shopContact
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        👤 *Customer:* $customerName
        🧾 *Invoice No:* $invoiceNumber
        📅 *Date:* $invoiceDate
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        🛍 *Items Purchased:*  
        $productDetails
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        💰 *Subtotal:* ₹$subtotal  
        🏛 *Tax:* ₹$tax  
        🎉 *Discount:* ₹$discount  
        💵 *Grand Total:* ₹$grandTotal  
        💳 *Payment Mode:* $paymentMethod  
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        
        ✅ *Thank you for shopping with us!*  
        🛒 Visit Again! 🛍  
        ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    """.trimIndent()
    }

}
