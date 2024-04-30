package com.example.inventory.Activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.inventory.Model.ModelItem
import com.example.inventory.Model.ModelKategori
import com.example.inventory.databinding.ActivityAddItemBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask

class AddItem : AppCompatActivity() {
    private lateinit var binding: ActivityAddItemBinding
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var progressDialog: ProgressDialog

    companion object {
        private const val REQUEST_CODE_IMAGE_PICK = 1001 // Kode request untuk memilih gambar
        private const val MAX_IMAGE_SIZE = 2 * 1024 * 1024 // 2 MB
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference.child("Item")
        storage = FirebaseStorage.getInstance()

        binding.etKategori.setOnClickListener {
            showCategorySelectionDialog()
        }

        binding.btnUpload.setOnClickListener {
            selectImage()
        }

        binding.btnSave.setOnClickListener {
            saveItemToFirebase()
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data ?: return

            val imageSize = getFileSize(imageUri)
            if (imageSize > MAX_IMAGE_SIZE) {
                Toast.makeText(
                    this,
                    "Ukuran gambar terlalu besar. Maksimal 2 MB.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            binding.ivResult.tag = imageUri

            Glide.with(this)
                .load(imageUri)
                .into(binding.ivResult)
        }
    }

    private fun getFileSize(uri: Uri): Long {
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return -1
    }

    private fun showCategorySelectionDialog() {
        val categoryDatabase = FirebaseDatabase.getInstance().reference.child("kategori")
        categoryDatabase.get().addOnSuccessListener { snapshot ->
            val categoryList = snapshot.children.mapNotNull {
                it.getValue(ModelKategori::class.java)
            }

            // Urutkan daftar kategori berdasarkan nama
            val sortedCategoryList = categoryList.sortedBy { it.nama }

            val categoryNames = sortedCategoryList.map { it.nama }
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Pilih Kategori")
            builder.setItems(categoryNames.toTypedArray()) { _, which ->
                val selectedCategory = sortedCategoryList[which]
                binding.etKategori.tag = selectedCategory.idKategori
                binding.etKategori.setText(selectedCategory.nama)
            }

            builder.show()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal memuat kategori", Toast.LENGTH_SHORT).show()
        }
    }


    private fun saveItemToFirebase() {
        if (!validateInputs()) return

        val nama       = binding.etNama.text.toString().trim()
        val deskripsi  = binding.etDeskripsi.text.toString().trim()
        val kuantitas  = binding.etKuantitas.text.toString().trim()
        val harga      = binding.etHarga.text.toString().trim()
        val idKategori = binding.etKategori.tag as? String

        val itemId = database.push().key ?: return
        val item = ModelItem(
            idItem = itemId,
            nama = nama,
            deskripsi = deskripsi,
            kuantitas = kuantitas,
            harga = harga,
            idKategori = idKategori
        )

        val imageUri = binding.ivResult.tag as? Uri
        if (imageUri == null) {
            Toast.makeText(this, "Harap unggah gambar", Toast.LENGTH_SHORT).show()
            return
        }

        showProgressDialog("Menyimpan item...")

        val storageReference = storage.reference.child("images/${nama}-${System.currentTimeMillis()}.jpg")
        storageReference.putFile(imageUri)
            .addOnSuccessListener {
                storageReference.downloadUrl.addOnSuccessListener { downloadUri ->
                    item.gambarUrl = downloadUri.toString()
                    database.child(itemId).setValue(item)
                        .addOnCompleteListener { task ->
                            hideProgressDialog()
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Item berhasil disimpan", Toast.LENGTH_SHORT)
                                    .show()
                                finish() // Menghindari aktivitas yang berlebihan
                            } else {
                                Toast.makeText(this, "Gagal menyimpan item", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast.makeText(this, "Gagal mengunggah gambar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showProgressDialog(message: String) {
        if (!::progressDialog.isInitialized) {
            progressDialog = ProgressDialog(this)
            progressDialog.setCancelable(false)
        }
        progressDialog.setMessage(message)
        progressDialog.show()
    }

    private fun hideProgressDialog() {
        if (::progressDialog.isInitialized && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val itemName = binding.etNama.text.toString().trim()
        if (itemName.isEmpty()) {
            binding.etNama.error = "Nama item harus diisi"
            isValid = false
        }

        val itemDescription = binding.etDeskripsi.text.toString().trim()
        if (itemDescription.isEmpty()) {
            binding.etDeskripsi.error = "Deskripsi item harus diisi"
            isValid = false
        }

        val itemQuantity = binding.etKuantitas.text.toString().trim()
        if (itemQuantity.isEmpty()) {
            binding.etKuantitas.error = "Kuantitas item harus diisi"
            isValid = false
        }
        val itemPrice = binding.etHarga.text.toString().trim()
        if (itemPrice.isEmpty()) {
            binding.etHarga.error = "Harga item harus diisi"
            isValid = false
        }

        val itemCategory = binding.etKategori.text.toString().trim()
        if (itemCategory.isEmpty()) {
            Toast.makeText(this, "Pilih kategori terlebih dahulu", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }
}
