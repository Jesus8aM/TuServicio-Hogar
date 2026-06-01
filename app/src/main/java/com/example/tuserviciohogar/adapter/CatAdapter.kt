package com.example.tuserviciohogar.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tuserviciohogar.databinding.ItemCatBinding
import com.example.tuserviciohogar.model.CatImage

class CatAdapter : RecyclerView.Adapter<CatAdapter.CatViewHolder>() {
    private var cats: List<CatImage> = emptyList()

    fun updateCats(newCats: List<CatImage>) {
        cats = newCats
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatViewHolder {
        val binding = ItemCatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatViewHolder, position: Int) {
        holder.bind(cats[position])
    }

    override fun getItemCount() = cats.size

    class CatViewHolder(private val binding: ItemCatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(cat: CatImage) {
            Glide.with(binding.root.context)
                .load(cat.url)
                .centerCrop()
                .into(binding.catImage)
        }
    }
} 