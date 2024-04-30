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
import com.example.inventory.Model.ModelKategori
import com.example.inventory.R
import com.example.inventory.databinding.ActivityEditBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBinding
    private lateinit var idItem: String
    private lateinit var database: DatabaseReference
    private lateinit var storage: StorageReference
    private lateinit var progressDialog: ProgressDialog

    private var oldImageUrl: String? = null
    private var selectedUri: Uri? = null

    companion object {
        private const val REQUEST_CODE_IMAGE_PICK = 1001 // Kode request untuk memilih gambar
        private const val MAX_IMAGE_SIZE = 2 * 1024 * 1024 // 2 MB
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etKategori.setOnClickListener {
            showCategorySelectionDialog()
        }

        binding.btnUpdate.setOnClickListener {
            updateItem()
        }

        binding.btnUpload.setOnClickListener {
            selectImage()
        }

        storage = FirebaseStorage.getInstance().reference.child("images")
        database = FirebaseDatabase.getInstance().reference.child("Item")

        idItem = intent.getStringExtra("idItem").toString()
        if (idItem.isNotEmpty()) {
            fatchItem(idItem)
        } else {
            showDataNotFoundDialog()
        }
    }

    private fun updateItem() {
        val newNama = binding.etNama.text.toString().trim()
        val newDeskripsi = binding.etDeskripsi.text.toString().trim()
        val newHarga = binding.etHarga.text.toString().trim()
        val newKuantitas = binding.etKuantitas.text.toString().trim()
        val newKategori = binding.etKategori.tag?.toString()

        if (newNama.isEmpty()) {
            binding.etNama.error = "Nama harus diisi"
            binding.etNama.requestFocus()
            return
        }

        if (newDeskripsi.isEmpty()) {
            binding.etDeskripsi.error = "Deskripsi harus diisi"
            binding.etDeskripsi.requestFocus()
            return
        }

        if (selectedUri != null) {
            uploadNewImageAndUpdateData(newNama, newDeskripsi, newHarga, newKuantitas, newKategori)
        } else {
            updateDataOnly(newNama, newDeskripsi, newHarga, newKuantitas, newKategori)
        }
    }

    private fun uploadNewImageAndUpdateData(
        newNama: String,
        newDeskripsi: String,
        newHarga: String,
        newKuantitas: String,
        newKategori: String?
    ) {
        showProgressDialog("Memperbaharui Barang...")
        val imageRef = storage.child("${newNama}-${System.currentTimeMillis()}.jpg")
        val uploadTask = imageRef.putFile(selectedUri!!)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: RuntimeException("Upload gambar gagal")
            }
            return@continueWithTask imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result.toString()

                deleteOldImage(oldImageUrl)
                updateData(newNama, newDeskripsi, newHarga, newKuantitas, newKategori, downloadUri)
            } else {
                hideProgressDialog()
                Toast.makeText(this, "Gagal mengunggah gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDataOnly(
        newNama: String,
        newDeskripsi: String,
        newHarga: String,
        newKuantitas: String,
        newKategori: String?
    ) {
        updateData(newNama, newDeskripsi, newHarga, newKuantitas, newKategori, oldImageUrl)
    }

    private fun updateData(
        newNama: String,
        newDeskripsi: String,
        newHarga: String,
        newKuantitas: String,
        newKategori: String?,
        imageUrl: String?
    ) {
        showProgressDialog("Memperbaharui Barang...")
        val updateMap = hashMapOf<String, Any>(
            "nama" to newNama,
            "deskripsi" to newDeskripsi,
            "harga" to newHarga,
            "kuantitas" to newKuantitas,
            "idKategori" to (newKategori ?: ""),
            "gambarUrl" to (imageUrl ?: "")
        )
        database.child(idItem).updateChildren(updateMap)
            .addOnCompleteListener {
                hideProgressDialog()
                if (it.isSuccessful) {
                    Toast.makeText(this, "Item berhasil diperbarui", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Gagal memperbarui item", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                hideProgressDialog()
                Toast.makeText(this, "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
            }

        val intent = Intent().apply {
            putExtra("nama", newNama)
            putExtra("deskripsi", newDeskripsi)
            putExtra("harga", newHarga)
            putExtra("kuantitas", newKuantitas)
            putExtra("idKategori", newKategori)
            putExtra("gambarUrl", imageUrl)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun deleteOldImage(imageUrl: String?) {
        if (imageUrl != null) {
            val oldImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            oldImageRef.delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Gambar lama berhasil dihapus", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal menghapus gambar lama", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun fatchItem(idItem: String) {
        val database = FirebaseDatabase.getInstance().getReference("Item").child(idItem)
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {

                    val nama = snapshot.child("nama").getValue(String::class.java)
                    val deskripsi = snapshot.child("deskripsi").getValue(String::class.java)
                    val harga = snapshot.child("harga").getValue(String::class.java)
                    val kuantitas = snapshot.child("kuantitas").getValue(String::class.java)
                    val gambarUrl = snapshot.child("gambarUrl").getValue(String::class.java)
                    val idKategori = snapshot.child("idKategori")
                        .getValue(String::class.java) // Ambil idKategori

                    binding.tvNama.text = nama
                    binding.etNama.setText(nama)
                    binding.etDeskripsi.setText(deskripsi)
                    binding.etHarga.setText(harga)
                    binding.etKuantitas.setText(kuantitas)

                    oldImageUrl = gambarUrl
                    if (gambarUrl != null) {
                        Glide.with(this@EditActivity)
                            .load(gambarUrl)
                            .into(binding.ivResult)
                    }

                    idKategori?.let {
                        setCategoryName(it)
                    }
                } else {
                    showDataNotFoundDialog()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Tangani error
            }
        })
    }

    private fun setCategoryName(idKategori: String) {
        val categoryDatabase = FirebaseDatabase.getInstance().reference.child("kategori")
        categoryDatabase.child(idKategori).get().addOnSuccessListener { snapshot ->
            val category = snapshot.getValue(ModelKategori::class.java)
            if (category != null) {
                binding.etKategori.setText(category.nama) // Tampilkan nama kategori di EditText
                binding.etKategori.tag = idKategori // Simpan idKategori pada tag
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal memuat kategori", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDataNotFoundDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Data Tidak Ditemukan")
        builder.setMessage("Maaf,  data tidak ditemukan")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            onBackPressed()
        }
        builder.setCancelable(false)
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, EditActivity.REQUEST_CODE_IMAGE_PICK)
    }

    private fun getFileSize(uri: Uri): Long {
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return -1
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EditActivity.REQUEST_CODE_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data ?: return

            val imageSize = getFileSize(imageUri)
            if (imageSize > EditActivity.MAX_IMAGE_SIZE) {
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