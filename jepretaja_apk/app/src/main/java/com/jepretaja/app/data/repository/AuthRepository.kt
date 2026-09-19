package com.jepretaja.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.jepretaja.app.core.util.FirestorePaths
import com.jepretaja.app.data.model.UserModel
import com.jepretaja.app.data.remote.ApiClient
import com.jepretaja.app.core.util.CloudFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lapisan autentikasi. Sama seperti versi Flutter — TIDAK dipercaya untuk
 * validasi role sensitif; Firestore Rules & Cloud Functions tetap sumber
 * kebenaran server-side (section 24).
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val exploreRepository: ExploreRepository,
    private val messaging: FirebaseMessaging,
    private val api: ApiClient,
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun getCurrentUserProfile(): UserModel? {
        val uid = currentUser?.uid ?: return null
        val snap = db.collection(FirestorePaths.USERS).document(uid).get().await()
        val profile = snap.toObject(UserModel::class.java)
        val creatorRef = db.collection(FirestorePaths.CREATORS).document(uid)
        val creatorSnap = creatorRef.get().await()

        if (profile?.role == "creator" && !creatorSnap.exists()) {
            // Memulihkan akun creator lama yang role-nya sudah benar tetapi
            // dokumen etalasenya hilang karena pendaftaran terputus.
            creatorRef.set(
                mapOf(
                    "userId" to uid,
                    "displayName" to profile.name,
                    "username" to profile.username,
                    "city" to profile.city,
                    "province" to profile.province,
                    "photoUrl" to profile.photoUrl,
                    "categories" to emptyList<String>(),
                    "rating" to 0.0,
                    "reviewCount" to 0,
                    "followerCount" to 0,
                    "verified" to false,
                    "verificationStatus" to "unverified",
                    "status" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            return profile
        }

        // Creator lama/baru tetap dikenali bila role pada users belum tersinkron.
        // Endpoint server juga diminta menyamakan role agar operasi berikutnya
        // (booking, wallet, dan verifikasi) tidak melihat akun sebagai customer.
        if (creatorSnap.exists() && profile != null && profile.role != "creator") {
            runCatching { api.call(CloudFunctions.BECOME_CREATOR) }
            return profile.copy(role = "creator")
        }
        return profile
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
        syncFcmToken()
    }

    suspend fun becomeCreator() {
        api.call(CloudFunctions.BECOME_CREATOR)
    }

    suspend fun registerCustomer(name: String, email: String, password: String, phone: String?, address: String?, city: String?, province: String?) {
        val normalizedEmail = email.trim().lowercase()
        val cred = auth.createUserWithEmailAndPassword(normalizedEmail, password).await()
        val uid = cred.user!!.uid
        try {
            db.collection(FirestorePaths.USERS).document(uid).set(
                mapOf(
                    "userId" to uid, "name" to name, "email" to normalizedEmail, "phone" to phone,
                    "address" to address, "city" to city, "province" to province,
                    "role" to "customer", "status" to "active", "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            cred.user?.sendEmailVerification()?.await()
            syncFcmToken()
        } catch (error: Exception) {
            runCatching { cred.user?.delete()?.await() }
            throw error
        }
    }

    suspend fun registerCreator(name: String, email: String, password: String, city: String, phone: String?, address: String?, province: String?) {
        val normalizedEmail = email.trim().lowercase()
        val cred = auth.createUserWithEmailAndPassword(normalizedEmail, password).await()
        val uid = cred.user!!.uid
        val username = usernameAwal(name, uid)
        try {
            val batch = db.batch()
            batch.set(
                db.collection(FirestorePaths.USERS).document(uid),
                mapOf(
                    "userId" to uid, "name" to name, "email" to normalizedEmail, "phone" to phone,
                    "address" to address, "city" to city, "province" to province,
                    "username" to username, "role" to "creator", "status" to "active", "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(
                db.collection(FirestorePaths.CREATORS).document(uid),
                mapOf(
                    "userId" to uid, "displayName" to name, "username" to username, "city" to city, "categories" to emptyList<String>(),
                    "rating" to 0.0, "reviewCount" to 0, "followerCount" to 0, "verified" to false,
                    "status" to "active", "createdAt" to FieldValue.serverTimestamp(),
                )
            )
        // CATATAN: dokumen wallets/{uid} SENGAJA tidak dibuat di sini.
        // firestore.rules menutup total penulisan ke koleksi `wallets`
        // (allow write: if false) karena isinya saldo uang. Karena batch
        // Firestore bersifat atomic, satu penulisan terlarang dulu membuat
        // SELURUH pendaftaran creator gagal — dokumen users dan creators ikut
        // batal padahal akun Auth-nya sudah terlanjur dibuat, sehingga setiap
        // panggilan server balas "Akun tidak ditemukan".
        //
        // Wallet dibuat server saat dana pertama dilepas (releaseEscrow pakai
        // set + merge), dan layar wallet menampilkan saldo 0 selama dokumennya
        // belum ada.
            batch.commit().await()
            cred.user?.sendEmailVerification()?.await()
            syncFcmToken()
        } catch (error: Exception) {
            runCatching { cred.user?.delete()?.await() }
            throw error
        }
    }

    private fun usernameAwal(name: String, uid: String): String {
        val dasar = name.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "creator" }.take(12)
        return "$dasar${uid.takeLast(7)}".take(20)
    }

  /** Google Sign-In (section 8). Dokumen users/{uid} dibuat otomatis
   * dengan role customer bila belum ada — mendaftar & login jadi satu alur. */
  suspend fun signInWithGoogleIdToken(idToken: String) {
      val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
      val result = auth.signInWithCredential(credential).await()
      val user = result.user!!
      val docRef = db.collection(FirestorePaths.USERS).document(user.uid)
      val existing = docRef.get().await()
      if (!existing.exists()) {
          docRef.set(
              mapOf(
                  "userId" to user.uid, "name" to (user.displayName ?: "Pengguna"), "email" to (user.email ?: ""),
                  "photoUrl" to user.photoUrl?.toString(), "role" to "customer", "status" to "active",
                  "createdAt" to FieldValue.serverTimestamp(),
              )
          ).await()
      }
      syncFcmToken()
  }

  /** Token disinkronkan setelah sesi siap, bukan hanya saat Firebase merotasi token. */
  private suspend fun syncFcmToken() {
      val uid = currentUser?.uid ?: return
      runCatching {
          val token = messaging.token.await()
          db.collection(FirestorePaths.USERS).document(uid).update("fcmToken", token).await()
      }
  }

  fun logout() = auth.signOut()

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun sendEmailVerification() {
        currentUser?.let { if (!it.isEmailVerified) it.sendEmailVerification().await() }
    }

    /**
     * Menanyakan ulang status verifikasi ke server Firebase.
     *
     * `currentUser.isEmailVerified` dibaca dari sesi yang di-cache di perangkat,
     * dan nilainya TIDAK berubah dengan sendirinya setelah pengguna menekan
     * tautan verifikasi di email — cache itu hanya diperbarui oleh reload().
     * Tanpa ini, aplikasi terus menampilkan "Email belum diverifikasi" selamanya
     * walaupun pengguna sudah benar-benar memverifikasi, dan satu-satunya cara
     * memperbaruinya adalah keluar lalu masuk lagi.
     */
    suspend fun refreshEmailVerified(): Boolean {
        val user = currentUser ?: return false
        user.reload().await()
        return currentUser?.isEmailVerified == true
    }

    /**
     * Ubah profil sendiri (konsumen maupun creator).
     *
     * Sengaja hanya menyentuh field milik pengguna: `role` dan `status` tidak
     * ikut ditulis, supaya tidak ada jalan bagi klien untuk menaikkan haknya
     * sendiri lewat layar edit profil.
     */
    suspend fun updateMyProfile(
        name: String,
        username: String,
        phone: String?,
        dateOfBirth: String?,
        gender: String?,
        address: String?,
        city: String?,
        province: String?,
        kabupaten: String?,
        kecamatan: String?,
        desa: String?,
        postalCode: String?,
        bio: String?,
        photoUrl: String?,
        isCreator: Boolean,
    ) {
        val uid = currentUser?.uid ?: throw IllegalStateException("Belum masuk.")
        val cleanedUsername = username.trim().lowercase()
        require(cleanedUsername.isNotBlank()) { "Username wajib diisi." }
        require(cleanedUsername.matches(Regex("^[a-z0-9._]{3,20}$"))) {
            "Username hanya boleh huruf kecil, angka, titik, dan underscore dengan panjang 3–20 karakter."
        }

        val updates = mutableMapOf<String, Any?>(
            "name" to name.trim(),
            "username" to cleanedUsername,
            "phone" to phone,
            "dateOfBirth" to dateOfBirth?.trim(),
            "gender" to gender?.trim(),
            "address" to address?.trim(),
            "city" to city?.trim(),
            "province" to province?.trim(),
            "kabupaten" to kabupaten?.trim(),
            "kecamatan" to kecamatan?.trim(),
            "desa" to desa?.trim(),
            "postalCode" to postalCode?.trim(),
            "bio" to bio?.trim(),
        )
        if (photoUrl != null) updates["photoUrl"] = photoUrl

        val batch = db.batch()
        batch.set(db.collection(FirestorePaths.USERS).document(uid), updates, com.google.firebase.firestore.SetOptions.merge())
        if (isCreator) {
            val creatorUpdates = mutableMapOf<String, Any?>(
                "userId" to uid,
                "displayName" to name.trim(),
                "username" to cleanedUsername,
                "city" to city?.trim(),
                "province" to province?.trim(),
                "status" to "active",
            )
            if (photoUrl != null) creatorUpdates["photoUrl"] = photoUrl
            batch.set(
                db.collection(FirestorePaths.CREATORS).document(uid),
                creatorUpdates,
                com.google.firebase.firestore.SetOptions.merge(),
            )
        }
        batch.commit().await()
        if (isCreator && photoUrl != null) {
            val posts = db.collection(FirestorePaths.EXPLORE_POSTS)
                .whereEqualTo("creatorId", uid)
                .limit(200)
                .get()
                .await()
            runCatching {
                posts.documents.chunked(400).forEach { chunk ->
                    val postBatch = db.batch()
                    chunk.forEach { postBatch.update(it.reference, "creatorPhotoUrl", photoUrl) }
                    postBatch.commit().await()
                }
            }
        }
        // PERBAIKAN: FollowModel (lihat catatan di modelnya) sengaja
        // menyimpan salinan nama & foto kedua pihak supaya daftar
        // Pengikut/Mengikuti tidak perlu membaca koleksi `users` yang
        // tertutup aturan. Tapi salinan itu sebelumnya cuma ditulis SEKALI
        // saat follow terjadi dan tidak pernah disamakan lagi — beda dari
        // creatorPhotoUrl di ExplorePost yang sudah disegarkan di atas.
        // Akibatnya foto lama tetap nongol selamanya di daftar
        // Pengikut/Mengikuti siapa pun yang sudah terhubung sebelum foto
        // ini diganti. Disamakan polanya di sini.
        if (photoUrl != null) {
            if (isCreator) {
                val asCreator = db.collection(FirestorePaths.FOLLOWS)
                    .whereEqualTo("creatorId", uid)
                    .limit(500)
                    .get()
                    .await()
                runCatching {
                    asCreator.documents.chunked(400).forEach { chunk ->
                        val followBatch = db.batch()
                        chunk.forEach { followBatch.update(it.reference, "creatorPhotoUrl", photoUrl) }
                        followBatch.commit().await()
                    }
                }
            }
            val asFollower = db.collection(FirestorePaths.FOLLOWS)
                .whereEqualTo("userId", uid)
                .limit(500)
                .get()
                .await()
            runCatching {
                asFollower.documents.chunked(400).forEach { chunk ->
                    val followBatch = db.batch()
                    chunk.forEach { followBatch.update(it.reference, "userPhotoUrl", photoUrl) }
                    followBatch.commit().await()
                }
            }
        }
    }

    suspend fun updatePrivacySettings(
        publicProfile: Boolean,
        allowMessages: Boolean,
        showActivity: Boolean,
    ) {
        val uid = currentUser?.uid ?: throw IllegalStateException("Belum masuk.")
        db.collection(FirestorePaths.USERS).document(uid).update(
            mapOf(
                "publicProfile" to publicProfile,
                "allowMessages" to allowMessages,
                "showActivity" to showActivity,
            )
        ).await()
    }

    /**
     * Hapus akun beserta jejak kontennya.
     *
     * Urutannya penting: konten dibersihkan SELAGI pengguna masih terautentikasi,
     * karena begitu akun Firebase Auth dihapus, aturan Firestore tidak lagi
     * mengenalinya sebagai pemilik dan penghapusan pasti ditolak. Sebelumnya
     * fungsi ini hanya menandai dokumen user sebagai "deleted", sehingga post dan
     * komentar lama tetap tayang atas nama akun yang sudah tidak ada.
     */
    suspend fun disableAccount() {
        val uid = currentUser?.uid ?: throw IllegalStateException("Belum masuk.")
        db.collection(FirestorePaths.USERS).document(uid).update(
            mapOf("status" to "disabled")
        ).await()
        auth.signOut()
    }

    suspend fun deleteAccount() {
        val uid = currentUser?.uid ?: return
        exploreRepository.purgeUserContent(uid)
        db.collection(FirestorePaths.USERS).document(uid).update(
            mapOf("status" to "deleted", "email" to "deleted_$uid@jepretaja.app", "name" to "Pengguna Dihapus")
        ).await()
        currentUser?.delete()?.await()
    }
}
