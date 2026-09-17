package com.jepretaja.app.data.model

import com.google.firebase.firestore.DocumentId

data class UserModel(
    @DocumentId val userId: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val address: String? = null,
    val city: String? = null,
    val province: String? = null,
    val kabupaten: String? = null,
    val kecamatan: String? = null,
    val desa: String? = null,
    val postalCode: String? = null,
    val bio: String? = null,
    val publicProfile: Boolean = true,
    val allowMessages: Boolean = true,
    val showActivity: Boolean = true,
    val role: String = "customer", // guest|customer|creator|admin|super_admin
    val photoUrl: String? = null,
    val status: String = "active",
    val fcmToken: String? = null,
)
