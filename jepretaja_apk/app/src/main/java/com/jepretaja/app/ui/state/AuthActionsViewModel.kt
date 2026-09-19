package com.jepretaja.app.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.jepretaja.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Aksi autentikasi one-shot (login/register/google/reset) — terpisah dari
 * AuthViewModel yang menyimpan state sesi berjalan, supaya tidak saling
 * ganggu lifecycle-nya. */
@HiltViewModel
class AuthActionsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.login(email, password)
                onSuccess()
            } catch (e: Exception) {
                onError("Login gagal. Periksa email/password Anda.")
            }
        }
    }

    fun registerCustomer(name: String, email: String, password: String, phone: String?, address: String?, city: String?, province: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                validateRegistration(name, email, password)
                authRepository.registerCustomer(name, email, password, phone, address, city, province)
                onSuccess()
            } catch (e: Exception) {
                onError(pesanRegistrasi(e))
            }
        }
    }

    fun registerCreator(name: String, email: String, password: String, city: String, phone: String?, address: String?, province: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                validateRegistration(name, email, password)
                authRepository.registerCreator(name, email, password, city, phone, address, province)
                onSuccess()
            } catch (e: Exception) {
                onError(pesanRegistrasi(e))
            }
        }
    }

    private fun validateRegistration(name: String, email: String, password: String) {
        require(name.isNotBlank()) { "Nama wajib diisi." }
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { "Format email tidak valid." }
        require(password.length >= 8) { "Password minimal 8 karakter." }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.signInWithGoogleIdToken(idToken)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Google Sign-In gagal.")
            }
        }
    }

    /**
     * Mengirim tautan pembuatan password baru ke email.
     *
     * Versi lama memakai `runCatching { ... }` lalu memanggil onDone() apa pun
     * hasilnya, dan satu-satunya pemanggil mengirim lambda kosong. Artinya:
     * berhasil atau gagal, layar tidak berubah sedikit pun — tombol "Lupa
     * password?" terlihat mati total padahal kadang emailnya benar-benar
     * terkirim. Sekarang hasilnya dilaporkan balik ke UI.
     */
    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                authRepository.resetPassword(email)
                onSuccess()
            } catch (e: Exception) {
                onError(pesanAuth(e))
            }
        }
    }

    /** Menerjemahkan kegagalan Firebase Auth jadi kalimat yang bisa ditindaklanjuti. */
    private fun pesanAuth(e: Throwable): String = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "Format email tidak valid."
        is FirebaseAuthInvalidUserException -> "Email ini belum terdaftar di JepretAja."
        is FirebaseNetworkException -> "Tidak ada koneksi internet. Sambungkan dulu lalu coba lagi."
        is FirebaseTooManyRequestsException -> "Terlalu banyak percobaan. Tunggu beberapa menit lalu coba lagi."
        else -> e.message ?: "Gagal mengirim email pemulihan. Coba lagi."
    }

    private fun pesanRegistrasi(e: Throwable): String = when (e) {
        is IllegalArgumentException -> e.message ?: "Data pendaftaran belum lengkap."
        is FirebaseAuthUserCollisionException -> "Email ini sudah terdaftar. Gunakan email lain atau masuk melalui Login."
        is FirebaseAuthWeakPasswordException -> "Password terlalu lemah. Gunakan minimal 8 karakter."
        is FirebaseAuthInvalidCredentialsException -> "Format email tidak valid."
        is FirebaseNetworkException -> "Tidak ada koneksi internet. Sambungkan dulu lalu coba lagi."
        is FirebaseTooManyRequestsException -> "Terlalu banyak percobaan. Tunggu beberapa menit lalu coba lagi."
        is FirebaseAuthException -> when (e.errorCode) {
            "ERROR_OPERATION_NOT_ALLOWED" -> "Pendaftaran email sedang dinonaktifkan di server."
            else -> "Registrasi gagal. Periksa data lalu coba lagi."
        }
        else -> "Registrasi gagal. Periksa koneksi dan data pendaftaran lalu coba lagi."
    }
}
