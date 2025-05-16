package com.bca.food_ordering_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bca.food_ordering_app.databinding.ActivityPayOutBinding
import com.bca.food_ordering_app.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.String

class PayOutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPayOutBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var userId: String

    private var name: String = ""
    private var address: String = ""
    private var phone: String = ""
    private var totalAmount: String = ""
    private var foodItemName: ArrayList<String>? = null
    private var foodItemPrice: ArrayList<String>? = null
    private var foodItemIngredient: ArrayList<String>? = null
    private var foodItemDescription: ArrayList<String>? = null
    private var foodItemQuantities: ArrayList<Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        userId = auth.currentUser?.uid ?: ""

        // Retrieve data from intent
        intent.extras?.let { bundle ->
            foodItemName = bundle.getStringArrayList("FoodItemName")
            foodItemPrice = bundle.getStringArrayList("FoodItemPrice")
            foodItemDescription = bundle.getStringArrayList("FoodItemDescription")
            foodItemIngredient = bundle.getStringArrayList("FoodItemIngredient")
            foodItemQuantities = bundle.getIntegerArrayList("FoodItemQuantities")
        }

        // Validate intent data
        if (foodItemName == null || foodItemPrice == null || foodItemQuantities == null) {
            Toast.makeText(this, "Error: Missing order details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set user data and total amount
        setUserData()
        totalAmount = "${calculateTotalAmount()} Rs"
        binding.totalAmount.isEnabled = false
        binding.totalAmount.setText(totalAmount)

        // Place order button click listener
        binding.placeMyOrder.setOnClickListener {
            name = binding.name.text.toString().trim()
            address = binding.address.text.toString().trim()
            phone = binding.phone.text.toString().trim()

            if (name.isBlank() || address.isBlank() || phone.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                placeMyOrder()
            }
        }
        if (!auth.currentUser?.isEmailVerified!!) {
            Toast.makeText(this, "Please verify your email to place orders", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun placeMyOrder() {
        val time = System.currentTimeMillis()
        val itemPushKey = database.child("OrderDetails").push().key ?: return
        val orderDetails = OrderDetails(
            userId,
            name,
            foodItemName,
            foodItemPrice,
            foodItemQuantities,
            address,
            phone,
            totalAmount,
            time,
            itemPushKey,
            false,
            false,
        )

        database.child("OrderDetails").child(itemPushKey)
            .setValue(orderDetails)
            .addOnSuccessListener {
                Toast.makeText(this, "Order placed successfully", Toast.LENGTH_SHORT).show()
                val bottomSheetDialog = CongratsBottomSheet()
                bottomSheetDialog.show(supportFragmentManager, "CongratsBottomSheet")
                removeFromCart()
                addOrderToHistory(orderDetails)

            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to place order", Toast.LENGTH_SHORT)
                    .show()
                Log.e("PayOutActivity", "Order placement failed", e)
            }
    }

    private fun addOrderToHistory(orderDetails: OrderDetails) {
        database.child("UsersInformation").child(userId).child("History")
            .child(orderDetails.itemPushKey!!)
            .setValue(orderDetails).addOnSuccessListener {

            }
    }

    private fun calculateTotalAmount(): Int {
        var totalAmount = 0
        val prices = foodItemPrice ?: return 0

        val quantities = foodItemQuantities ?: return 0

        for (i in 0 until minOf(prices.size, quantities.size)) {
            try {
                // Remove non-numeric characters except decimal point
                val price = prices[i].replace("[^0-9.]".toRegex(), "")
                val priceValue = price.toFloatOrNull() ?: 0f
                val quantity = quantities[i]
                totalAmount += (priceValue * quantity).toInt()
                Log.d(
                    "PayOutActivity",
                    "Price: $priceValue, Quantity: $quantity, Running total: $totalAmount"
                )
            } catch (e: Exception) {
                Log.e("PayOutActivity", "Error calculating price at index ", e)
            }
        }
        return totalAmount
    }

    private fun setUserData() {
        if (userId.isEmpty()) return
        val userReference = database.child("UsersInformation").child(userId)
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    name = snapshot.child("name").getValue(String::class.java) ?: ""
                    address = snapshot.child("address").getValue(String::class.java) ?: ""
                    phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                    binding.apply {
                        name.setText(this@PayOutActivity.name)
                        address.setText(this@PayOutActivity.address)
                        phone.setText(this@PayOutActivity.phone)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PayOutActivity", "Failed to fetch user data:")
            }
        })
    }

    private fun removeFromCart() {
        if (userId.isEmpty()) return
        database.child("UsersInformation").child(userId).child("CartItems")
            .removeValue()
            .addOnFailureListener { e ->
                Log.e("PayOutActivity", "Failed to clear cart: ${e.message}", e)
            }
    }
}