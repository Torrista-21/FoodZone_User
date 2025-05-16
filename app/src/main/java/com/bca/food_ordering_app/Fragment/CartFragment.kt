package com.bca.food_ordering_app.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bca.food_ordering_app.PayOutActivity
import com.bca.food_ordering_app.adaptar.CartAdapter
import com.bca.food_ordering_app.databinding.FragmentCartBinding
import com.bca.food_ordering_app.model.cartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var foodNames: MutableList<String>
    private lateinit var foodPrices: MutableList<String>
    private lateinit var foodDescriptions: MutableList<String>
    private lateinit var foodIngredients: MutableList<String>
    private lateinit var foodQuantities: MutableList<Int>
    private lateinit var cartAdapter: CartAdapter
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        userId = auth.currentUser?.uid ?: ""


        foodNames = mutableListOf()
        foodPrices = mutableListOf()
        foodDescriptions = mutableListOf()
        foodIngredients = mutableListOf()
        foodQuantities = mutableListOf()


        cartAdapter = CartAdapter(
            context = requireContext(),
            cartItems = foodNames,
            cartItemPrices = foodPrices,
            cartDescriptions = foodDescriptions,
            cartIngredients = foodIngredients,
            cartQuantity = foodQuantities,
            onCartItemCountChanged = { itemCount ->
                binding.proceedButton.isEnabled = itemCount > 0
            }
        )
        binding.cartRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.cartRecyclerView.adapter = cartAdapter


        retrieveCartItems()


        binding.proceedButton.isEnabled = false
        binding.proceedButton.setOnClickListener {
            getOrderItemsDetails()
        }

        return binding.root
    }

    private fun retrieveCartItems() {
        val foodReference = database.reference.child("UsersInformation").child(userId).child("CartItems")
        foodReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodNames.clear()
                foodPrices.clear()
                foodDescriptions.clear()
                foodIngredients.clear()
                foodQuantities.clear()

                for (foodSnapshot in snapshot.children) {
                    val cartItem = foodSnapshot.getValue(cartItems::class.java)
                    cartItem?.foodName?.let { foodNames.add(it) }
                    cartItem?.foodPrice?.let { foodPrices.add(it) }
                    cartItem?.foodDescription?.let { foodDescriptions.add(it) }
                    cartItem?.foodIngredients?.let { foodIngredients.add(it) }
                    cartItem?.foodQuantity?.let { foodQuantities.add(it) }
                }

                cartAdapter.notifyDataSetChanged()
                binding.proceedButton.isEnabled = foodNames.size > 0
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to fetch cart items: ", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getOrderItemsDetails() {
        val orderIdReference = database.reference.child("UsersInformation").child(userId).child("CartItems")
        val foodName = mutableListOf<String>()
        val foodPrice = mutableListOf<String>()
        val foodDescription = mutableListOf<String>()
        val foodIngredients = mutableListOf<String>()
        val foodQuantities = cartAdapter.getUpdatedItemsQuantities()

        orderIdReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (foodSnapshot in snapshot.children) {
                    val orderItem = foodSnapshot.getValue(cartItems::class.java)
                    orderItem?.foodName?.let { foodName.add(it) }
                    orderItem?.foodPrice?.let { foodPrice.add(it) }
                    orderItem?.foodDescription?.let { foodDescription.add(it) }
                    orderItem?.foodIngredients?.let { foodIngredients.add(it) }
                }
                orderNow(foodName, foodPrice, foodDescription, foodIngredients, foodQuantities)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to fetch order details: ", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun orderNow(
        foodName: MutableList<String>,
        foodPrice: MutableList<String>,
        foodDescription: MutableList<String>,
        foodIngredients: MutableList<String>,
        foodQuantities: MutableList<Int>
    ) {
        if (isAdded && context != null) {
            val intent = Intent(requireContext(), PayOutActivity::class.java)
            intent.putExtra("FoodItemName", ArrayList(foodName))
            intent.putExtra("FoodItemPrice", ArrayList(foodPrice))
            intent.putExtra("FoodItemDescription", ArrayList(foodDescription))
            intent.putExtra("FoodItemIngredient", ArrayList(foodIngredients))
            intent.putExtra("FoodItemQuantities", ArrayList(foodQuantities))
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}