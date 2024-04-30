package com.example.inventory.Activity

import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.inventory.Model.ModelUser
import com.example.inventory.R
import com.example.inventory.databinding.ActivityRegisterBinding
import com.example.inventory.databinding.ActivityResetBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var Dreference: FirebaseDatabase

    private var selectedDateOfBirth: String? = null
    private var selectedGender: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textViewLogin.setOnClickListener {
            startActivity(Intent(this,LoginActivity::class.java))
        }

        binding.etBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    selectedDateOfBirth = "$year/${month + 1}/$dayOfMonth"
                    binding.etBirth.setText(selectedDateOfBirth)
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        binding.etGender.setOnClickListener {
            val genderOptions = arrayOf("Laki-Laki", "Perempuan", "Other")

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select Gender")
            builder.setItems(genderOptions) { dialog, which ->
                selectedGender = genderOptions[which]
                binding.etGender.setText(selectedGender)
                dialog.dismiss()
            }
            builder.show()
        }

        mAuth = FirebaseAuth.getInstance()
        Dreference = FirebaseDatabase.getInstance()

        binding.buttonRegister.setOnClickListener {
            daftar()
        }
    }

    private fun validasiInput(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPass.text.toString().trim()
        val confirmPassword = binding.etConfirm.text.toString().trim()
        val fullName = binding.etFullname.text.toString().trim()
        val phoneNumber = binding.etPhone.text.toString().trim()
        val dateOfBirth = binding.etBirth.text.toString().trim()
        val gender = binding.etGender.text.toString().trim()

        if (fullName.isEmpty()) {
            binding.etFullname.error = "Nama lengkap tidak boleh kosong"
            binding.etFullname.requestFocus()
            return false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email tidak boleh kosong"
            binding.etEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Email tidak valid"
            binding.etEmail.requestFocus()
            return false
        }

        if (phoneNumber.isEmpty()) {
            binding.etPhone.error = "Nomor telepon tidak boleh kosong"
            binding.etPhone.requestFocus()
            return false
        }

        if (dateOfBirth.isEmpty()) {
            Toast.makeText(this, "Tanggal lahir tidak boleh kosong", Toast.LENGTH_SHORT).show()
            binding.etBirth.requestFocus()
            return false
        }

        if (gender.isEmpty()) {
            Toast.makeText(this, "Jenis kelamin tidak boleh kosong", Toast.LENGTH_SHORT).show()
            binding.etGender.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.etPass.error = "Password tidak boleh kosong"
            binding.etPass.requestFocus()
            return false
        }

        if (password.length < 8) {
            binding.etPass.error = "Password minimal 8 karakter"
            binding.etPass.requestFocus()
            return false
        }

        if (confirmPassword.isEmpty()) {
            binding.etConfirm.error = "Konfirmasi password tidak boleh kosong"
            binding.etConfirm.requestFocus()
            return false
        }

        if (password != confirmPassword) {
            binding.etConfirm.error = "Password tidak sesuai"
            binding.etConfirm.requestFocus()
            return false
        }

        return true
    }

    private fun daftar() {

        if (validasiInput()) {
            val email       = binding.etEmail.text.toString().trim()
            val password    = binding.etPass.text.toString().trim()
            val fullName    = binding.etFullname.text.toString().trim()
            val phoneNumber = binding.etPhone.text.toString().trim()
            val dateOfBirth = binding.etBirth.text.toString().trim()
            val gender      = binding.etGender.text.toString().trim()

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // User registration successful, save user data to database
                        val uid = mAuth.currentUser?.uid
                        if (uid != null) {
                            val user = ModelUser(
                                uid,
                                fullName,
                                phoneNumber,
                                dateOfBirth,
                                gender,
                                "User",
                                SimpleDateFormat(
                                    "dd/MM/yyyy",
                                    Locale.getDefault()
                                ).format(Date())
                            )
                            Dreference.reference.child("Users").child(uid).setValue(user)
                        }
                        Toast.makeText(this, "Pendaftaran berhasil", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Pendaftaran gagal. ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

    }
}