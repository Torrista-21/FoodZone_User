package com.bca.food_ordering_app.Fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bca.food_ordering_app.LoginActivity
import com.bca.food_ordering_app.R
import com.bca.food_ordering_app.RecentOrderItems
import com.bca.food_ordering_app.adaptar.BuyAgainAdapter
import com.bca.food_ordering_app.databinding.FragmentHistoryBinding
import com.bca.food_ordering_app.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private lateinit var buyAgainAdapter: BuyAgainAdapter
    private var listOfOrderItems: MutableList<OrderDetails> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        userId = auth.currentUser?.uid ?: ""

        binding.buyAgainRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        orderHistory()

        binding.recentBuyItem.setOnClickListener {
            seeItemsRecentBuy()
        }
        binding.receivedbutton.setOnClickListener {
            updateOrderStatus()
        }

        if (!auth.currentUser?.isEmailVerified!!) {
            Toast.makeText(requireContext(), "Please verify your email", Toast.LENGTH_LONG).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        return binding.root
    }

    private fun orderHistory() {
        binding.recentBuyItem.visibility = View.GONE
        val buyItemReference = database.reference
            .child("UsersInformation").child(userId).child("History")
        val sortingQuery = buyItemReference.orderByChild("currentTime")
        sortingQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("HistoryFragment", "Fetched ${snapshot.childrenCount} history items")
                listOfOrderItems.clear()
                for (buySnapshot in snapshot.children) {
                    try {
                        val buyItemHistory = buySnapshot.getValue(OrderDetails::class.java)
                        buyItemHistory?.let {
                            listOfOrderItems.add(it)
                        } ?: Log.w("HistoryFragment", "Null OrderDetails for snapshot: ${buySnapshot.key}")
                    } catch (e: Exception) {
                        Log.e("HistoryFragment", "Error parsing OrderDetails: ${e.message}")
                    }
                }

                listOfOrderItems.sortByDescending { it.currentTime }
                if (listOfOrderItems.isNotEmpty()) {
                    Log.d("HistoryFragment", "Order count: ${listOfOrderItems.size}")
                    setDataInRecentBuyItem()
                    setPreviousBuyItems()
                } else {
                    binding.buyAgainRecyclerView.visibility = View.GONE
                    binding.recentBuyItem.visibility = View.GONE
                    Toast.makeText(requireContext(), "No order history found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HistoryFragment", "Firebase error: ${error.message}")
                Toast.makeText(requireContext(), "Failed to load history: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setDataInRecentBuyItem() {
        binding.recentBuyItem.visibility = View.VISIBLE
        val recentOrderItem = listOfOrderItems.firstOrNull()
        recentOrderItem?.let {
            with(binding) {
                try {
                    buyAgainFoodName.text = it.foodName?.firstOrNull() ?: ""
                    buyAgainFoodPrice.text = it.foodPrice?.firstOrNull() ?: ""
                    val isOrderAccepted = it.orderAccepted
                    val isPaymentReceived = it.paymentReceived
                    if (isOrderAccepted) {
                        orderStatus.background.setTint(Color.GREEN)
                        receivedbutton.visibility = if (isPaymentReceived) View.GONE else View.VISIBLE
                    } else {
                        orderStatus.background.setTint(Color.RED)
                        receivedbutton.visibility = View.GONE
                    }
                } catch (e: NullPointerException) {
                    Log.e("HistoryFragment", "View not found in setDataInRecentBuyItem: ${e.message}")
                    Toast.makeText(requireContext(), "UI error: Check layout IDs", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setPreviousBuyItems() {
        val buyAgainFoodName = mutableListOf<String>()
        val buyAgainFoodPrice = mutableListOf<String>()
        for (i in 1 until listOfOrderItems.size) {
            listOfOrderItems[i].foodName?.firstOrNull()?.let { buyAgainFoodName.add(it) }
            listOfOrderItems[i].foodPrice?.firstOrNull()?.let { buyAgainFoodPrice.add(it) }
        }

        buyAgainAdapter = BuyAgainAdapter(
            buyAgainFoodName,
            buyAgainFoodPrice,
            requireContext()
        )
        binding.buyAgainRecyclerView.visibility = View.VISIBLE
        binding.buyAgainRecyclerView.adapter = buyAgainAdapter
    }

    private fun seeItemsRecentBuy() {
        listOfOrderItems.firstOrNull()?.let { recentBuy ->
            val intent = Intent(requireContext(), RecentOrderItems::class.java)
            intent.putExtra("RecentBuyOrderItem", ArrayList(listOfOrderItems))
            startActivity(intent)
        }
    }

    private fun updateOrderStatus() {
        val recentOrder = listOfOrderItems.firstOrNull()
        if (recentOrder == null) {
            Toast.makeText(requireContext(), "No order found", Toast.LENGTH_SHORT).show()
            return
        }

        val itemPushKey = recentOrder.itemPushKey
        if (itemPushKey.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid order ID", Toast.LENGTH_SHORT).show()
            return
        }

        val historyReference = database.reference
            .child("UsersInformation").child(userId).child("History").child(itemPushKey)
        Log.d("HistoryFragment", "Updating paymentReceived for History: $itemPushKey")
        historyReference.child("paymentReceived").setValue(true)
            .addOnSuccessListener {
                Log.d("HistoryFragment", "paymentReceived set to true in History: $itemPushKey")
                historyReference.child("dispatched").get().addOnSuccessListener { snapshot ->
                    val isDispatched = snapshot.getValue(Boolean::class.java) ?: false
                    Log.d("HistoryFragment", "Dispatched status: $isDispatched for $itemPushKey")
                    if (isDispatched) {
                        val dispatchReference = database.reference
                            .child("DispatchedOrders").child(itemPushKey)
                        Log.d("HistoryFragment", "Updating paymentReceived for DispatchedOrders: $itemPushKey")
                        dispatchReference.child("paymentReceived").setValue(true)
                            .addOnSuccessListener {
                                Log.d("HistoryFragment", "paymentReceived set to true in DispatchedOrders: $itemPushKey")
                                Toast.makeText(requireContext(), "Payment marked as received", Toast.LENGTH_SHORT).show()
                                binding.receivedbutton.visibility = View.GONE
                            }
                            .addOnFailureListener { error ->
                                Log.e("HistoryFragment", "Failed to update DispatchedOrders: ${error.message}")
                                Toast.makeText(requireContext(), "Failed to update dispatched order: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(requireContext(), "Payment marked as received", Toast.LENGTH_SHORT).show()
                        binding.receivedbutton.visibility = View.GONE
                    }
                }.addOnFailureListener { error ->
                    Log.e("HistoryFragment", "Failed to check dispatch status: ${error.message}")
                    Toast.makeText(requireContext(), "Failed to check dispatch status: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { error ->
                Log.e("HistoryFragment", "Failed to update payment in History: ${error.message}")
                Toast.makeText(requireContext(), "Failed to update payment: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}