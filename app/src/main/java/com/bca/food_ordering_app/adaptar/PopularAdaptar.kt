package com.bca.food_ordering_app.adaptar

import android.R
import android.content.Context
import android.content.Intent
import android.renderscript.ScriptGroup
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bca.food_ordering_app.DetailsActivity
import com.bca.food_ordering_app.adaptar.PopularAdaptar.PopularVIewHolder
import com.bca.food_ordering_app.databinding.PopularItemBinding

class PopularAdaptar(private  val items: List<String>,  private val price: List<String>,
                     private val image: List<Int>,
    private val requireContext : Context): RecyclerView.Adapter<PopularVIewHolder>
                   (){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PopularVIewHolder {
        return PopularVIewHolder(PopularItemBinding.inflate(LayoutInflater.from(parent.context)
        , parent,false))
    }

    override fun onBindViewHolder(
        holder: PopularVIewHolder,
        position: Int
    ) {
        val item = items[position]
        val images = image[position]
        val prices = price[position]
        holder.bind(item,prices,images)

        holder.itemView.setOnClickListener {
            val intent = Intent(requireContext, DetailsActivity::class.java)
            intent.putExtra("MenuItemName",item)
            intent.putExtra("MenuImage",images)
            requireContext.startActivity(intent)
        }

    }
    override fun getItemCount(): Int {
        return minOf(items.size, image.size, price.size)
    }

    class PopularVIewHolder(private val binding: PopularItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        private val imagesView = binding.imageView5
        fun bind(item: String,price: String,images: Int){
            binding.popularFoodName.text = item
            binding.popularFoodPrice.text = price
            imagesView.setImageResource(images)
        }
    }


}