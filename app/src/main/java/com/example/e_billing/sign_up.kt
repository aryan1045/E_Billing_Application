package com.example.e_billing

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class SignUpActivity : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var txtLogin: TextView
    private lateinit var txtPasswordStrength: TextView
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        mAuth = FirebaseAuth.getInstance()

        // Initialize views
        edtName = findViewById(R.id.edt_name)
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        btnSignUp = findViewById(R.id.btn_signup)
        txtLogin = findViewById(R.id.txt_login)
        txtPasswordStrength = findViewById(R.id.txt_password_strength)

        // Sign-up button click listener
        btnSignUp.setOnClickListener {
            val name = edtName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (validateInput(name, email, password)) {
                signUpUser(name, email, password)
            }
        }

        // Redirect to login page
        txtLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Password strength indicator
        edtPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updatePasswordStrength(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        if (name.isEmpty()) {
            edtName.error = "Name is required"
            return false
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.error = "Enter a valid email"
            return false
        }
        if (password.length < 6) {
            edtPassword.error = "Password must be at least 6 characters"
            return false
        }
        return true
    }

    private fun signUpUser(name: String, email: String, password: String) {
        Log.d("SignUpActivity", "Sign-up started")

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("SignUpActivity", "Sign-up successful")
                    val user = mAuth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            Log.d("SignUpActivity", "Profile updated")
                            Toast.makeText(this, "Signup Successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Log.e("SignUpActivity", "Profile update failed: ${profileTask.exception?.message}")
                        }
                    }
                } else {
                    Log.e("SignUpActivity", "Sign-up failed: ${task.exception?.message}")
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updatePasswordStrength(password: String) {
        val strength = when {
            password.length >= 12 && password.contains(Regex("[A-Z]")) &&
                    password.contains(Regex("[0-9]")) && password.contains(Regex("[^A-Za-z0-9]")) -> "Very Strong"
            password.length >= 10 -> "Strong"
            password.length >= 8 -> "Moderate"
            else -> "Weak"
        }

        txtPasswordStrength.text = "Password Strength: $strength"
        txtPasswordStrength.setTextColor(
            when (strength) {
                "Very Strong" -> getColor(R.color.blue)
                "Strong" -> getColor(R.color.green)
                "Moderate" -> getColor(R.color.orange)
                else -> getColor(R.color.red)
            }
        )
    }
}
