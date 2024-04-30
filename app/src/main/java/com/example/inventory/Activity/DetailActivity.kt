@file:Suppress("DEPRECATION")

package com.example.inventory.Activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.inventory.Model.ModelKategori
import com.example.inventory.R
import com.example.inventory.databinding.ActivityDetailBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var database: DatabaseReference
    private lateinit var progressDialog: ProgressDialog

    companion object {
        const val EDIT_ACTIVITY_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setValueBarang()

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.ivEdit.setOnClickListener {
            startActivity(Intent(this, EditActivity::class.java))
        }

        binding.ivEdit.setOnClickListener {
            val idItem = intent.getStringExtra("idItem").toString()
            val intent = Intent(this, EditActivity::class.java)
            intent.putExtra("idItem", idItem)
            startActivityForResult(intent, EDIT_ACTIVITY_REQUEST_CODE)
        }

        binding.ivDelete.setOnClickListener {
            deletItem()
        }

    }

    private fun deletItem() {
        val itemId = intent.getStringExtra("idItem").toString()
        val gambarUrl = intent.getStringExtra("gambarUrl").toString()

        showProgressDialog("Menghapus item...")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi Hapus")
        builder.setMessage("Apakah Anda yakin ingin menghapus Barang ini?")
        builder.setPositiveButton("Ya") { dialog, which ->
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(gambarUrl)
            storageReference.delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Gambar Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                    database = FirebaseDatabase.getInstance().getReference("Item").child(itemId)
                    val mTask = database.removeValue()

                    mTask.addOnSuccessListener {
                        Toast.makeText(this, "Data Barang Berhasil dihapus", Toast.LENGTH_SHORT)
                            .show()
                        hideProgressDialog()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                        .addOnFailureListener { fail ->
                            hideProgressDialog()
                            val refreshDialog = AlertDialog.Builder(this)
                            refreshDialog.setTitle("Gagal Menghapus Data")
                            refreshDialog.setMessage("Terjadi Kesalahan pada saat ingin menghapus data Barang")
                            refreshDialog.setPositiveButton("Ya") { dialog, which ->
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                            }
                            refreshDialog.setNegativeButton("Tidak") { dialog, which ->
                                hideProgressDialog()
//                             Batal Menghapus Data
                            }
                            refreshDialog.show()
                        }
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    val refreshDialog = AlertDialog.Builder(this)
                    refreshDialog.setTitle("Gagal Menghapus Gambar")
                    refreshDialog.setMessage("Gagal Menghapus Gambar. Coba lagi?")
                    refreshDialog.setPositiveButton("Ya") { dialog, which ->
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    }
                    refreshDialog.setNegativeButton("Tidak") { dialog, which ->
//                    Batal Menghapus Data
                        hideProgressDialog()
                    }
                    refreshDialog.show()
                }
        }
        builder.setNegativeButton("Tidak") { dialog, which ->
            hideProgressDialog()
//                Batal Penghapusan
        }
        builder.show()
    }

    @SuppressLint("SetTextI18n")
    private fun setValueBarang() {
        val itemName = intent.getStringExtra("nama") ?: "Nama tidak tersedia"
        val itemDescription = intent.getStringExtra("deskripsi") ?: "Deskripsi tidak tersedia"
        val itemPrice = intent.getStringExtra("harga")
        val itemQuantity = intent.getStringExtra("kuantitas") ?: "Kuantitas tidak valid"

        val itemCategoryId = intent.getStringExtra("idKategori")
        val itemgambarUrl = intent.getStringExtra("gambarUrl")

        // Menetapkan data ke tampilan
        binding.tvItemName.text = itemName
        binding.tvItemDescription.text = itemDescription
        binding.tvItemQuantity.text = "$itemQuantity Pcs"
        binding.tvItemPrice.text = "Rp. $itemPrice"
        // Menampilkan gambar menggunakan Glide
        Glide.with(this)
            .load(itemgambarUrl)
            .placeholder(R.drawable.image_default)
            .into(binding.ivItemImage)

        // Mendapatkan dan menampilkan nama kategori
        setCategoryName(itemCategoryId)
    }

    @SuppressLint("SetTextI18n")
    private fun setCategoryName(itemCategoryId: String?) {
        if (itemCategoryId == null) {
            binding.tvItemKategori.text = "Kategori Tidak Ditemukan"
            return
        }

        val categoryReference = FirebaseDatabase.getInstance().reference.child("kategori")
        categoryReference.child(itemCategoryId).get().addOnSuccessListener { snapshot ->
            val category = snapshot.getValue(ModelKategori::class.java)
            if (category != null) {
                binding.tvItemKategori.text = category.nama
            } else {
                binding.tvItemKategori.text = "Kategori Tidak Ditemukan"
            }
        }.addOnFailureListener {
            binding.tvItemKategori.text = "Gagal Memuat Kategori"
        }
    }

    @SuppressLint("SetTextI18n")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val updateNama = data?.getStringExtra("nama")
            val updateDeskripsi = data?.getStringExtra("deskripsi")
            val updateHarga = data?.getStringExtra("harga")
            val updateKuantitas = data?.getStringExtra("kuantitas")
            val updateKategori = data?.getStringExtra("idKategori")
            val updateImgUrl = data?.getStringExtra("gambarUrl")

            binding.tvItemName.text = updateNama
            binding.tvItemDescription.text = updateDeskripsi
            binding.tvItemPrice.text = "Rp. $updateHarga"
            binding.tvItemQuantity.text = "$updateKuantitas Pcs"

            Glide.with(this)
                .load(updateImgUrl)
                .placeholder(R.drawable.image_default)
                .into(binding.ivItemImage)

            // Get the name of the category from the database
            val database = FirebaseDatabase.getInstance().reference.child("kategori")
            database.child(updateKategori ?: "").get().addOnSuccessListener { snapshot ->
                val category = snapshot.getValue(ModelKategori::class.java)
                if (category != null) {
                    binding.tvItemKategori.text = category.nama
                } else {
                    binding.tvItemKategori.text = "Kategori Tidak Ditemukan"
                }
            }.addOnFailureListener {
                binding.tvItemKategori.text = "Gagal Memuat Kategori"
            }
        }
    }

    private fun showProgressDialog(message: String) {
        if (!::progressDialog.isInitialized) {
            progressDialog = ProgressDialog(this)
            progressDialog.setCancelable(false)
            progressDialog.isIndeterminate = true
        }
        progressDialog.setMessage(message)
        progressDialog.show()
    }

    private fun hideProgressDialog() {
        if (::progressDialog.isInitialized && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }
}