package com.jepretaja.app.ui.screens.creatordashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jepretaja.app.data.model.PortfolioModel
import com.jepretaja.app.data.model.PortfolioStatus
import com.jepretaja.app.data.repository.CreatorRepository
import com.jepretaja.app.core.util.MediaUnggahan
import com.jepretaja.app.data.work.PortfolioUploadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

/** Kemajuan unggahan banyak berkas sekaligus. */
data class ProgresUnggah(
    val selesai: Int = 0,
    val total: Int = 0,
    val gagal: Int = 0,
) {
    val sedangJalan: Boolean get() = total > 0
}

@HiltViewModel
class CreatorPortfolioManagementViewModel @Inject constructor(
    val repository: CreatorRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /** Kabar terakhir untuk creator — ditampilkan layar sebagai snackbar. */
    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()
    fun pesanDibaca() { _pesan.value = null }

    private val _progres = MutableStateFlow(ProgresUnggah())
    val progres: StateFlow<ProgresUnggah> = _progres.asStateFlow()

    /** Semua album milik creator, TERMASUK draft dan yang ditolak. */
    fun albumSaya(creatorId: String): Flow<List<PortfolioModel>> =
        repository.streamPortfolio(creatorId, hanyaTayang = false).catch { emit(emptyList()) }

    /**
     * Mengunggah beberapa berkas sekaligus menjadi SATU album.
     *
     * Album baru selalu berstatus draft, tidak langsung tayang. Sesi unggah
     * biasanya belum selesai saat berkas pertama masuk — memublikasikannya
     * seketika berarti calon pelanggan melihat album setengah jadi tanpa judul,
     * dan creator tidak punya kesempatan memeriksanya lebih dulu.
     *
     * Berkas yang gagal tidak membatalkan yang lain: dari sepuluh foto, sembilan
     * yang berhasil tetap disimpan dan yang satu dilaporkan jumlahnya.
     */
    fun unggahAlbum(creatorId: String, uris: List<Uri>, urutanBerikutnya: Long) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val salinan = runCatching {
                uris.map { MediaUnggahan.salin(context, it, if (apakahVideo(it)) "mp4" else "jpg") }
            }.getOrElse {
                _pesan.value = "Media tidak bisa disiapkan. Pilih ulang dari galeri."
                return@launch
            }
            val request = OneTimeWorkRequestBuilder<PortfolioUploadWorker>()
                .setInputData(PortfolioUploadWorker.data(creatorId, salinan.map { it.toString() }, urutanBerikutnya))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                .addTag(PortfolioUploadWorker.TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "portfolio_${request.id}", ExistingWorkPolicy.KEEP, request,
            )
            _pesan.value = "Portfolio masuk antrean. Kamu boleh menutup layar ini."
        }
    }

    fun updatePortfolioItem(portfolioId: String, title: String, category: String) {
        viewModelScope.launch {
            runCatching { repository.updatePortfolioItem(portfolioId, title, category) }
                // Kegagalan yang ditelan diam-diam lebih buruk daripada pesan
                // error: creator menutup layar dengan yakin perubahannya
                // tersimpan, dan baru tahu tidak lama kemudian.
                .onFailure { _pesan.value = "Perubahan gagal disimpan." }
        }
    }

    fun ajukanReview(portfolioId: String) {
        viewModelScope.launch {
            runCatching { repository.updatePortfolioStatus(portfolioId, PortfolioStatus.MENUNGGU) }
                .onSuccess { _pesan.value = "Album diajukan. Moderator akan meninjau." }
                .onFailure { _pesan.value = "Gagal mengajukan album." }
        }
    }

    /** Menarik kembali pengajuan, atau mengembalikan album tayang jadi draft. */
    fun jadikanDraft(portfolioId: String) {
        viewModelScope.launch {
            runCatching { repository.updatePortfolioStatus(portfolioId, PortfolioStatus.DRAFT) }
                .onSuccess { _pesan.value = "Album dikembalikan ke draft dan tidak lagi tampil publik." }
                .onFailure { _pesan.value = "Gagal mengubah status album." }
        }
    }

    fun simpanUrutan(idBerurutan: List<String>) {
        viewModelScope.launch {
            runCatching { repository.simpanUrutanPortfolio(idBerurutan) }
                .onFailure { _pesan.value = "Urutan gagal disimpan. Coba lagi." }
        }
    }

    fun deletePortfolioItem(portfolioId: String) {
        viewModelScope.launch {
            runCatching { repository.deletePortfolioItem(portfolioId) }
                .onFailure { _pesan.value = "Karya gagal dihapus." }
        }
    }

    /**
     * Menebak jenis berkas dari MIME type yang dilaporkan ContentResolver.
     *
     * Bukan dari ekstensi nama berkas: pemilih media Android mengembalikan URI
     * `content://` yang sering tidak punya nama berkas sama sekali.
     */
    private fun apakahVideo(uri: Uri): Boolean =
        context.contentResolver.getType(uri)?.startsWith("video") == true
}
