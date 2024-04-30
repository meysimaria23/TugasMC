package com.example.inventory.Activity

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.example.inventory.Adapter.BarangAdapter
import com.example.inventory.Model.ModelItem
import com.example.inventory.Model.ModelKategori
import com.example.inventory.R
import com.example.inventory.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var itemAdapter: BarangAdapter
    private lateinit var originalList: MutableList<ModelItem>
    private lateinit var listBarang: MutableList<ModelItem>


    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference.child("kategori")

        binding.btnAddItem.setOnClickListener {
            startActivity(Intent(this, AddItem::class.java))
        }

        listBarang = mutableListOf()
        originalList = mutableListOf()


// Menambahkan listener untuk menangani perubahan teks
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // Tidak ada tindakan sebelum teks diubah
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                // Setiap kali teks berubah, kita jalankan filter
                val query = s?.toString() ?: ""
                filterData(query)
            }

            override fun afterTextChanged(s: Editable?) {
                // Tidak ada tindakan setelah teks diubah
            }
        })


        binding.btnKategori.setOnClickListener {
            showAddCategoryDialog()
        }

        // Inisialisasi FirebaseAuth
        mAuth = FirebaseAuth.getInstance()

        val layoutManager = GridLayoutManager(this, 2)
        binding.rvBarang.layoutManager = layoutManager
        binding.rvBarang.setHasFixedSize(true)
        getBarang()
    }

    private fun filterData(query: String) {
        if (query.isEmpty()) {
            itemAdapter.updateData(originalList) // Kembali ke daftar asli jika query kosong
        } else {
            val filteredList = originalList.filter {
                it.nama?.contains(query, ignoreCase = true) == true
            }
            itemAdapter.updateData(filteredList)
        }
    }

    private fun getBarang() {
        // Show loading spinner
        binding.rvBarang.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.pbData.visibility = View.VISIBLE

        database = FirebaseDatabase.getInstance().getReference("Item")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listBarang.clear()
                originalList.clear() // Clear originalList as well

                if (snapshot.exists()) {
                    for (menuSnap in snapshot.children) {
                        val barangData = menuSnap.getValue(ModelItem::class.java)
                        if (barangData != null) {
                            listBarang.add(barangData)
                        }
                    }

                    // Sort list based on name
                    listBarang.sortBy { it.nama }

                    // Copy sorted list to originalList
                    originalList.addAll(listBarang)

                    itemAdapter = BarangAdapter(listBarang)
                    binding.rvBarang.adapter = itemAdapter

                    // Set the OnItemClickListener
                    itemAdapter.setOnItemClickListener(object : BarangAdapter.onItemClickListener {
                        override fun onItemClick(position: Int) {
                            // Open DetailActivity with the selected item data
                            val intent = Intent(this@MainActivity, DetailActivity::class.java)
                            val selectedItem = listBarang[position]
                            intent.putExtra("idItem", selectedItem.idItem)
                            intent.putExtra("nama", selectedItem.nama)
                            intent.putExtra("deskripsi", selectedItem.deskripsi)
                            intent.putExtra("idKategori", selectedItem.idKategori)
                            intent.putExtra("harga", selectedItem.harga)
                            intent.putExtra("kuantitas", selectedItem.kuantitas)
                            intent.putExtra("gambarUrl", selectedItem.gambarUrl)
                            startActivity(intent)
                        }
                    })

                    // Hide the loading spinner and show the RecyclerView
                    binding.pbData.visibility = View.GONE
                    if (listBarang.isNotEmpty()) {
                        binding.rvBarang.visibility = View.VISIBLE
                        binding.tvEmpty.visibility = View.GONE
                    } else {
                        binding.rvBarang.visibility = View.GONE
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                } else {
                    // Hide the loading spinner and show the empty text if there's no data
                    binding.pbData.visibility = View.GONE
                    binding.rvBarang.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any cancellation scenarios
            }
        })
    }


    private fun showAddCategoryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tambah Kategori Baru")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        builder.setView(input)

        builder.setPositiveButton("Tambah") { dialog, which ->
            val categoryName = input.text.toString()
            if (categoryName.isNotBlank()) {
                val categoryId = database.push().key

                if (categoryId != null) {
                    val newCategory = ModelKategori(idKategori = categoryId, nama = categoryName)
                    database.child(categoryId).setValue(newCategory)
                    Toast.makeText(this, "Kategori '$categoryName' ditambahkan", Toast.LENGTH_SHORT)
                        .show()
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Nama kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Batal") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }
}