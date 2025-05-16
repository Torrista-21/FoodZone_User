package com.bca.food_ordering_app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bca.food_ordering_app.databinding.ActivityDetailsBinding
import com.bca.food_ordering_app.model.cartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DetailsActivity : AppCompatActivity() {

    private var foodName: String? = null
    private var foodDescription: String? = null
    private var foodIngredient: String? = null
    private var foodPrice: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var binding: ActivityDetailsBinding
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        foodName = intent.getStringExtra("MenuItemName")
        foodDescription = intent.getStringExtra("MenuItemDescription")
        foodIngredient = intent.getStringExtra("MenuItemIngredient")
        foodPrice = intent.getStringExtra("MenuItemPrice")


        with(binding) {
            detailFoodName.text = foodName
            descriptionTextVIew.text = foodDescription
            ingredientTextVIew.text = foodIngredient
        }
        binding.imageButton.setOnClickListener {
            finish()
        }

        binding.addToCartButton.setOnClickListener {
            addToCart()
        }


    }

    private fun addToCart() {
        val database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid ?: ""
        val cartRef = database.child("UsersInformation").child(userId).child("CartItems")

        cartRef.orderByChild("foodName").equalTo(foodName).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (itemSnapshot in snapshot.children) {
                        val cartItem = itemSnapshot.getValue(cartItems::class.java)
                        if (cartItem != null) {

                            val newQuantity =(cartItem.foodQuantity?: 1) + 1
                            itemSnapshot.ref.child("foodQuantity").setValue(newQuantity)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this@DetailsActivity,
                                        "Item quantity updated in Cart",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this@DetailsActivity,
                                        "Failed to update item quantity",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                } else {

                    val cartItems = cartItems(
                        foodName.toString(),
                        foodPrice.toString(),
                        foodDescription.toString(),
                        1
                    )
                    cartRef.push().setValue(cartItems).addOnSuccessListener {
                        Toast.makeText(
                            this@DetailsActivity,
                            "Item added to Cart Successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.addOnFailureListener {
                        Toast.makeText(
                            this@DetailsActivity,
                            "Cannot add to Cart! Sorry!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DetailsActivity, "Error Checking Cart", Toast.LENGTH_SHORT)
                    .show()
            }

        })
    }
}
