package com.example.inventory.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.inventory.R
import com.example.inventory.databinding.ActivityLoginBinding
import com.example.inventory.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            login()
        }

        binding.tvDaftar.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvReset.setOnClickListener {
            startActivity(Intent(this, ResetActivity::class.java))
        }

        mAuth = FirebaseAuth.getInstance()
    }

    private fun login() {
        val email: String = binding.etEmail.text.toString().trim()
        val pass: String = binding.etPass.text.toString().trim()

        if (email.isEmpty()) {
            binding.etEmail.error = "Email Tidak Boleh Kosong"
            binding.etEmail.requestFocus()
            return

        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Email Tidak Valid"
            binding.etEmail.requestFocus()
            return

        } else if (pass.isEmpty() || pass.length < 8) {
            binding.etPass.error = "Maksimal 8 karakter dan Tidak boleh kosong"
            binding.etPass.requestFocus()
            return

        } else {
            loginUser(email, pass)
        }
    }

    private fun loginUser(email: String, pass: String) {
        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Jika berhasil, tampilkan pesan berhasil dan pindah ke halaman Home
                Toast.makeText(this, "Berhasil Masuk", Toast.LENGTH_SHORT).show()
                Intent(this, MainActivity::class.java).also { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            } else {
                // Jika gagal, tampilkan pesan error khusus
                val errorMessage = "Gagal Masuk: Terjadi kesalahan saat masuk. Silakan coba lagi."
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
}