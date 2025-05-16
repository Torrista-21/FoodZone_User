package com.bca.food_ordering_app.adaptar

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bca.food_ordering_app.databinding.CartItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CartAdapter(
    private val context: Context,
    private val cartItems: MutableList<String>,
    private val cartItemPrices: MutableList<String>,
    private val cartDescriptions: MutableList<String>,
    private val cartIngredients: MutableList<String>,
    private val cartQuantity: MutableList<Int>,
    private val onCartItemCountChanged: (Int) -> Unit // Listener for item count changes
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var cartItemReference: DatabaseReference

    init {
        val database = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid
        if (userId != null) {
            cartItemReference =
                database.reference.child("UsersInformation").child(userId).child("CartItems")
        } else {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
        // Notify initial item count
        onCartItemCountChanged(cartItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = cartItems.size

    fun getUpdatedItemsQuantities(): MutableList<Int> {
        val itemQuantities = mutableListOf<Int>()
        itemQuantities.addAll(cartQuantity)
        return itemQuantities
    }

    inner class CartViewHolder(private val binding: CartItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply {
                val quantity = cartQuantity[position]
                cartFoodName.text = cartItems[position]
                cartItemPrice.text = cartItemPrices[position]
                cartItemQuantity.text = quantity.toString()

                minusButton.setOnClickListener {
                    decreaseQuantity(position)
                }
                plusButton.setOnClickListener {
                    increaseQuantity(position)
                }
                deleteButton.setOnClickListener {
                    val itemPosition = adapterPosition
                    if (itemPosition != RecyclerView.NO_POSITION) {
                        deleteQuantity(itemPosition)
                    }
                }
            }
        }

        private fun decreaseQuantity(position: Int) {
            if (cartQuantity[position] > 0) {
                cartQuantity[position]--
                binding.cartItemQuantity.text = cartQuantity[position].toString()
                updateQuantityInDatabase(position, cartQuantity[position])
            }
        }

        private fun increaseQuantity(position: Int) {
            if (cartQuantity[position] < 15) {
                cartQuantity[position]++
                binding.cartItemQuantity.text = cartQuantity[position].toString()
                updateQuantityInDatabase(position, cartQuantity[position])
            }
        }

        private fun updateQuantityInDatabase(position: Int, newQuantity: Int) {
            getUniqueKeyAtPosition(position) { uniqueKey ->
                if (uniqueKey != null) {
                    cartItemReference.child(uniqueKey).child("foodQuantity").setValue(newQuantity)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Cart Item Updated", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(context, "Failed to update quantity", Toast.LENGTH_SHORT)
                                .show()
                        }
                }
            }
        }

        private fun deleteQuantity(position: Int) {
            if (position < 0 || position >= cartItems.size) return
            getUniqueKeyAtPosition(position) { uniqueKey ->
                if (uniqueKey != null) {
                    removeItem(position, uniqueKey)
                } else {
                    Toast.makeText(context, "Unable to delete item", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun removeItem(position: Int, uniqueKey: String) {
            cartItemReference.child(uniqueKey).removeValue().addOnSuccessListener {
                cartItems.removeAt(position)
                cartQuantity.removeAt(position)
                cartDescriptions.removeAt(position)
                cartItemPrices.removeAt(position)
                cartIngredients.removeAt(position)
                notifyItemRemoved(position)
                onCartItemCountChanged(cartItems.size) // Notify item count change
                Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(context, "Item Removal failed", Toast.LENGTH_SHORT).show()
            }
        }

        private fun getUniqueKeyAtPosition(positionRetrieve: Int, onComplete: (String?) -> Unit) {
            cartItemReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.elementAtOrNull(positionRetrieve)?.key?.let { uniqueKey ->
                        onComplete(uniqueKey)
                    } ?: onComplete(null)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to fetch item", Toast.LENGTH_SHORT).show()
                    onComplete(null)
                }
            })
        }
    }
}