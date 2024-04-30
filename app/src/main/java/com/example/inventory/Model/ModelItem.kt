package com.example.inventory.Model

data class ModelItem(
    val idItem: String? = null,
    val nama: String? = null,
    val deskripsi: String? = null,
    val kuantitas: String? = null,
    val harga: String? = null,
    val idKategori: String? = null,
    var gambarUrl: String? = null
)
