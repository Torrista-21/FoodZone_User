package com.bca.food_ordering_app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bca.food_ordering_app.adapter.MenuAdapter
import com.bca.food_ordering_app.databinding.FragmentSearchBinding
import com.bca.food_ordering_app.model.MenuItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var adapter: MenuAdapter
    private lateinit var database: FirebaseDatabase
    private val originalMenuItems = mutableListOf<MenuItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSearchBinding.inflate(inflater, container, false)

        // Set RecyclerView layout manager once
        binding.menuRecyclerView.layoutManager = LinearLayoutManager(requireContext())



        adapter = MenuAdapter(emptyList(), requireContext())
        binding.menuRecyclerView.adapter = adapter

        retrieveMenuItems()
        setupSearchView()

        return binding.root
    }

    private fun retrieveMenuItems() {

        database = FirebaseDatabase.getInstance()
        val foodRef: DatabaseReference = database.reference.child("menu")

        foodRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                originalMenuItems.clear() // Clear to avoid duplicates
                for (foodSnapshot in snapshot.children) {
                    try {
                        val menuItem = foodSnapshot.getValue(MenuItem::class.java)

                        menuItem?.let {
                            originalMenuItems.add(it)
                        }
                    } catch (e: Exception) {

                    }
                }
                showAllMenu()
            }

            private fun showAllMenu() {

                setAdapter(ArrayList(originalMenuItems))
            }

            private fun setAdapter(filteredMenuItem: ArrayList<MenuItem>) {
                Log.d("SearchFragment", "Setting adapter with ${filteredMenuItem.size} items")
                adapter = MenuAdapter(filteredMenuItem, requireContext())
                binding.menuRecyclerView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {

                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSearchView() {

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {

                filterMenuItems(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {

                filterMenuItems(newText)
                return true
            }
        })
    }

    private fun filterMenuItems(query: String) {

        val filteredMenuItem = originalMenuItems.filter {
            it.foodName?.contains(query, ignoreCase = true) == true
        }
        adapter = MenuAdapter(filteredMenuItem, requireContext())
        binding.menuRecyclerView.adapter = adapter
    }
}