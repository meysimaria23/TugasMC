package com.example.inventory.Adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.inventory.Model.ModelItem
import com.example.inventory.R

class BarangAdapter(
    private var listBarang: MutableList<ModelItem>
) : RecyclerView.Adapter<BarangAdapter.BarangViewHolder>() {

    private var itemClickListener: onItemClickListener? = null

    // ViewHolder untuk item barang
    class BarangViewHolder(itemView: View, clickListener: onItemClickListener?) :
        RecyclerView.ViewHolder(itemView) {
        val nama: TextView = itemView.findViewById(R.id.tvItemTitle)
        val harga: TextView = itemView.findViewById(R.id.tvItemharga)
        val gambar: ImageView = itemView.findViewById(R.id.ivItemImage)

        init {
            itemView.setOnClickListener {
                clickListener?.onItemClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarangViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.barang_item, parent, false)
        return BarangViewHolder(itemView, itemClickListener)
    }

    override fun onBindViewHolder(holder: BarangViewHolder, position: Int) {
        val currentItem = listBarang[position]
        holder.nama.text = currentItem.nama
        holder.harga.text = "Rp. ${currentItem.harga}"
        Glide.with(holder.itemView.context)
            .load(currentItem.gambarUrl)
            .into(holder.gambar)
    }

    override fun getItemCount(): Int {
        return listBarang.size
    }

    // Metode untuk memperbarui data di dalam adapter
    fun updateData(newData: List<ModelItem>) {
        listBarang.clear() // Hapus data yang lama
        listBarang.addAll(newData) // Tambahkan data yang baru
        notifyDataSetChanged() // Beritahu RecyclerView bahwa datanya berubah
    }

    // Interface untuk item click listener
    interface onItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: onItemClickListener) {
        this.itemClickListener = listener
    }
}