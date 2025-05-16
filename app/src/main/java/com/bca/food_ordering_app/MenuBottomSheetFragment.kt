package com.bca.food_ordering_app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bca.food_ordering_app.adapter.MenuAdapter
import com.bca.food_ordering_app.databinding.FragmentMenuBottomSheetBinding
import com.bca.food_ordering_app.model.MenuItem
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MenuBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentMenuBottomSheetBinding
    private lateinit var database: FirebaseDatabase
    private var menuItems: MutableList<MenuItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMenuBottomSheetBinding.inflate(inflater, container, false)
        binding.backButton.setOnClickListener {
            dismiss()
        }
        retrieveMenuItems()
        return binding.root
    }

    private fun retrieveMenuItems() {
        database = FirebaseDatabase.getInstance()
        val foodRef: DatabaseReference = database.reference.child("menu")
        menuItems = mutableListOf()

        foodRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (foodSnapshot in snapshot.children) {
                    Log.d("MenuBottomSheetFragment", "Raw data: ${foodSnapshot.value}")
                    val menuItem = foodSnapshot.getValue(MenuItem::class.java)
                    if (menuItem != null) {
                        menuItems.add(menuItem)
                    } else {
                        Log.w("MenuBottomSheetFragment", "Failed to parse menu item: ${foodSnapshot.key}")
                    }
                }
                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MenuBottomSheetFragment", "Firebase error: ${error.message}")
                Toast.makeText(
                    requireContext(),
                    "Failed to load menu: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setAdapter() {
        val context = context ?: return
        if (menuItems.isEmpty()) {
            Toast.makeText(context, "No menu items available", Toast.LENGTH_SHORT).show()
        }
        val adapter = MenuAdapter(menuItems, context)
        binding.menuRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.menuRecyclerView.adapter = adapter
    }
}