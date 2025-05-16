package com.bca.food_ordering_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bca.food_ordering_app.databinding.ActivitySignInBinding
import com.bca.food_ordering_app.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.regex.Pattern

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val passwordPattern = Pattern.compile("^(?=.*[!@#$%^&*]).{8,}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.createAccount.setOnClickListener {
            val userName = binding.editTextName.text.toString().trim()
            val email = binding.editTextEmailAddress.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            val phone = binding.editTextPhone.text.toString().trim()

            // Validate inputs
            if (userName.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!passwordPattern.matcher(password).matches()) {
                Toast.makeText(
                    this,
                    "Password must be at least 8 characters with one special character (!@#$%^&*)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (phone.length != 10 || !phone.all { it.isDigit() }) {
                Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("SignInActivity", "Attempting to create user: $email")
            createAccount(userName, email, password, phone)
        }

        binding.alreadyhavebutton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun createAccount(userName: String, email: String, password: String, phone: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        Log.d("SignInActivity", "User created: ${user.uid}")
                        // Send email verification
                        user.sendEmailVerification()
                            .addOnSuccessListener {
                                Log.d("SignInActivity", "Verification email sent to: $email")
                                Toast.makeText(
                                    this,
                                    "Verification email sent. Please check your inbox.",
                                    Toast.LENGTH_LONG
                                ).show()
                                saveUserData(userName, email, phone, user.uid)
                            }
                            .addOnFailureListener { error ->
                                Log.e("SignInActivity", "Failed to send verification email: ${error.message}")
                                Toast.makeText(
                                    this,
                                    "Failed to send verification email: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Log.e("SignInActivity", "Current user is null after sign-up")
                        Toast.makeText(this, "Account creation failed: User not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("SignInActivity", "Account creation failed: ${task.exception?.message}")
                    Toast.makeText(
                        this,
                        "Account creation failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun saveUserData(userName: String, email: String, phone: String, userId: String) {
        val user = UserModel(name = userName, email = email, phone = phone, emailVerified = false)
        database.reference
            .child("UsersInformation")
            .child(userId)
            .setValue(user)
            .addOnSuccessListener {
                Log.d("SignInActivity", "User data saved for: $userId")
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener { error ->
                Log.e("SignInActivity", "Failed to save user data: ${error.message}")
                Toast.makeText(this, "Failed to save user data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }
}