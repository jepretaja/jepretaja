package com.jepretaja.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jepretaja.app.data.remote.ApiClient
import com.jepretaja.app.core.util.CloudFunctions
import com.jepretaja.app.core.util.FirestorePaths
import com.jepretaja.app.core.util.asFlow
import com.jepretaja.app.data.model.PaymentModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pembayaran punya dua alur aktif di server (api/_lib/actions/appPayment.js):
 *
 * - "transfer_manual" (default): server menerbitkan nomor rekening, nominal
 *   berikut kode unik, dan batas waktu; admin memverifikasi bukti transfer di
 *   panel web lewat aksi confirmManualPayment.
 * - "midtrans": server membuat transaksi Snap sungguhan ke Midtrans dan
 *   mengembalikan redirectUrl (dibuka di WebView oleh PaymentScreen);
 *   statusnya diperbarui otomatis lewat webhook (api/midtrans.js) begitu
 *   pelanggan menyelesaikan pembayaran di Snap, apa pun kanal yang ia pilih
 *   di sana (GoPay, QRIS, kartu, VA bank, dll).
 */
@Singleton
class PaymentRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val api: ApiClient,
) {
    /**
     * Membuat (atau mengambil kembali) instruksi/transaksi pembayaran untuk
     * satu booking.
     *
     * Kunci yang dikembalikan server untuk transfer_manual: paymentId, amount,
     * uniqueCode, transferAmount, bankName, bankAccountNumber,
     * bankAccountName, expiresAt, instruction. Untuk midtrans: paymentId,
     * amount, redirectUrl, snapToken.
     */
    suspend fun createPaymentOrder(bookingId: String, method: String = "transfer_manual"): Map<String, Any?> {
        return api.call(CloudFunctions.CREATE_PAYMENT_ORDER, mapOf("bookingId" to bookingId, "method" to method))
    }

    /**
     * Status pembayaran satu booking.
     *
     * Filter `customerId` WAJIB ada. Aturan Firestore untuk koleksi payments
     * berbunyi `resource.data.customerId == uid()`, dan Firestore menilai
     * query terhadap kemungkinan hasilnya, bukan dokumen yang benar-benar
     * kembali. Query yang hanya menyaring bookingId tidak membuktikan syarat
     * itu, jadi seluruh query ditolak PERMISSION_DENIED — bukan sekadar
     * menyaring dokumen yang tidak boleh dibaca.
     */
    fun streamPaymentForBooking(bookingId: String): Flow<List<PaymentModel>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return db.collection(FirestorePaths.PAYMENTS)
            .whereEqualTo("bookingId", bookingId)
            .whereEqualTo("customerId", uid)
            .limit(1).asFlow()
    }
}
