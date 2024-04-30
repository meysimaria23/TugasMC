package com.example.inventory.Model

data class ModelUser(
    val uid: String,
    val fullName: String,
    val phoneNumber: String?,
    val dateOfBirth: String?,
    val gender: String?,
    val role: String,
    val creationDate: String
)
