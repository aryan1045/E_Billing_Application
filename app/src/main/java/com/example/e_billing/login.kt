package com.example.e_billing

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var txtSignup: TextView  // Changed from Button to TextView

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (mAuth.currentUser != null) {
            navigateToMain()
            return
        }

        // Initialize views
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        btnLogin = findViewById(R.id.btn_login)
        txtSignup = findViewById(R.id.txt_signup) // Correct ID for Sign-Up TextView

        // Redirect to Sign-up screen
        txtSignup.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // Perform login action
        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
            } else {
                logIn(email, password)
            }
        }
    }

    // Function to log in with email and password
    private fun logIn(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    Log.e("LoginError", "Login failed: ${task.exception?.message}")
                    Toast.makeText(this, "User does not exist or incorrect credentials", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Navigate to MainActivity and finish login activity
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
