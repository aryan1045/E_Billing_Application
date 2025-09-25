package com.example.e_billing

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.e_billing.Fragments.AddFragment
import com.example.e_billing.Fragments.Dashboard
import com.example.e_billing.Fragments.Invoice
import com.example.e_billing.Fragments.ShopInfo
import com.example.e_billing.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set default fragment only if there’s no saved instance
        if (savedInstanceState == null) {
            setFragment(Dashboard()) // Default fragment is Dashboard
        }

        // Handle bottom navigation item selection
        binding.bottom.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.dashboard -> Dashboard()
                R.id.Invoice -> Invoice()  // Ensure correct ID from XML
                R.id.profile -> ShopInfo()
                R.id.add -> AddFragment()
                else -> return@setOnItemSelectedListener false
            }
            setFragment(selectedFragment)
            true
        }
    }

    // ✅ Prevents reloading the same fragment
    private fun setFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val currentFragment = fragmentManager.findFragmentById(R.id.main_frame)

        if (currentFragment != null && currentFragment::class == fragment::class) {
            return
        }

        fragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .commit()
    }

    // Inflate the menu (for logout option)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Handle menu item selection
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logoutUser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ✅ Fixed Logout Functionality - Redirects to Login activity
    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)  // ✅ Fixed reference
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
