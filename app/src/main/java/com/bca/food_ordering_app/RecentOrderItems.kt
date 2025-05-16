package com.bca.food_ordering_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bca.food_ordering_app.adaptar.RecentBuyAdapter
import com.bca.food_ordering_app.databinding.ActivityRecentOrderItemsBinding
import com.bca.food_ordering_app.model.OrderDetails

class RecentOrderItems : AppCompatActivity() {

    private val binding: ActivityRecentOrderItemsBinding by lazy {
        ActivityRecentOrderItemsBinding.inflate(layoutInflater)
    }

    private lateinit var allFoodName: ArrayList<String>
    private lateinit var allFoodPrice: ArrayList<String>
    private lateinit var allFoodQuantity: ArrayList<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
        val recentOrderItem =
            intent.getSerializableExtra("RecentBuyOrderItem") as ArrayList<OrderDetails>

        recentOrderItem.let { orderDetails ->
            if (orderDetails.isNotEmpty()) {
                val recentOrderItems = orderDetails[0]

                allFoodName = recentOrderItems.foodName as ArrayList<String>
                allFoodPrice = recentOrderItems.foodPrice as ArrayList<String>
                allFoodQuantity = recentOrderItems.foodQuantities as ArrayList<Int>
            }

        }

        val rv = binding.recyclerViewRecentBuy
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = RecentBuyAdapter(this,allFoodName,
            allFoodPrice,
            allFoodQuantity)
        rv.adapter = adapter

    }

}