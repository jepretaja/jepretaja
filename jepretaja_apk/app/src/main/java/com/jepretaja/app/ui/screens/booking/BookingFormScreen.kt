package com.jepretaja.app.ui.screens.booking

// DatePickerDialog milik Android sengaja TIDAK diimpor: namanya bentrok dengan
// DatePickerDialog Material3 yang dipakai di bawah, dan yang bawaan Android
// tidak bisa menonaktifkan tanggal satuan — justru itu yang dibutuhkan di sini.
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.ui.components.SkeletonList
import java.util.Calendar

internal val PadTepi = 20.dp

/**
 * Form Booking, disusun ulang jadi empat langkah.
 *
 * **Kenapa berlangkah, bukan satu halaman panjang.** Versi sebelumnya menaruh
 * delapan urusan berbeda — paket, tanggal, jam, lokasi, catatan, add-on, biaya
 * perjalanan, voucher, rincian harga — dalam satu kolom yang digulir, dipisah
 * hanya oleh `Spacer` dan beberapa judul. Yang hilang di sana bukan keindahan
 * melainkan orientasi: pengguna tidak pernah tahu tinggal berapa lagi, dan
 * setiap kali menggulir ke bawah ia dihadapkan pada kolom baru yang tidak ia
 * duga. Empat langkah dengan indikator di atas menjawab satu pertanyaan yang
 * selalu dibawa orang ke formulir pembayaran: *masih berapa lama lagi?*
 *
 * **Validasi tampil di kolomnya masing-masing.** Dulu seluruh pemeriksaan baru
 * berjalan saat tombol kirim ditekan, dan hasilnya satu baris merah di dasar
 * halaman — jauh dari kolom yang bermasalah, dan hanya satu pesan sekalipun
 * tiga kolom kurang. Sekarang aturannya hidup di [BookingFormState] sebagai
 * nilai turunan, dipakai bersama oleh pesan di bawah kolom, warna indikator
 * langkah, dan penjagaan tombol Lanjut.
 *
 * Add-on ditaruh di langkah Layanan, bukan Detail: add-on mengubah isi jasa dan
 * harganya, jadi keputusannya satu tarikan napas dengan memilih paket.
 */
@Composable
fun BookingFormScreen(
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit,
    viewModel: BookingFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pilihTanggal by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    val gulir = rememberScrollState()

    // Setiap pindah langkah, halaman kembali ke atas. Tanpa ini, langkah baru
    // terbuka di posisi gulir langkah sebelumnya dan judulnya tidak terlihat.
    LaunchedEffect(state.langkah) { gulir.animateScrollTo(0) }

    fun showTimePicker() {
        val now = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute -> viewModel.setTime("%02d:%02d".format(hour, minute)) },
            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true,
        ).show()
    }

    if (pilihTanggal) {
        KalenderBooking(
            terpilih = state.date,
            tidakTersedia = state.unavailableDates,
            onDismiss = { pilihTanggal = false },
            onPilih = { tanggal -> viewModel.setDate(tanggal); pilihTanggal = false },
        )
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            com.jepretaja.app.ui.components.AppTopBar(title = "Form Booking", onBack = onBack)
        },
        bottomBar = {
            if (state.pkg != null) {
                BilahLangkah(
                    state = state,
                    onMundur = viewModel::mundur,
                    onLanjut = viewModel::lanjut,
                    onKirim = { if (termsAccepted) viewModel.submit(onSubmitted) },
                    termsAccepted = termsAccepted,
                    onTermsChanged = { termsAccepted = it },
                )
            }
        },
    ) { padding ->
        if (state.pkgLoading) {
            Box(Modifier.padding(padding).fillMaxSize().padding(PadTepi)) { SkeletonList(count = 5) }
            return@Scaffold
        }
        if (state.pkgError != null || state.pkg == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.pkgError ?: "Paket tidak ditemukan", color = AppColors.Danger)
            }
            return@Scaffold
        }

        Column(Modifier.padding(padding).fillMaxSize()) {
            IndikatorLangkah(state = state, onPilih = viewModel::keLangkah)
            HorizontalDivider(color = AppColors.Border)

            Column(
                Modifier.weight(1f).verticalScroll(gulir).padding(vertical = 18.dp),
            ) {
                when (state.langkah) {
                    LangkahBooking.LAYANAN -> LangkahLayanan(state, viewModel::toggleAddOn)
                    LangkahBooking.JADWAL -> LangkahJadwal(
                        state = state,
                        onPilihTanggal = { pilihTanggal = true },
                        onPilihJam = { showTimePicker() },
                    )
                    LangkahBooking.DETAIL -> LangkahDetail(
                        state = state,
                        onLokasi = viewModel::setLocation,
                        onLocationSelected = viewModel::setLocationCoordinates,
                        onCatatan = viewModel::setNote,
                        onBiayaJalan = viewModel::setTravelFee,
                    )
                    LangkahBooking.KONFIRMASI -> LangkahKonfirmasi(
                        state = state,
                        onVoucher = viewModel::setVoucherCode,
                        onTerapkanVoucher = { viewModel.refreshPreview(applyVoucher = true) },
                        onUbahLangkah = viewModel::keLangkah,
                    )
                }

                // Galat tingkat layar (bentrok tanggal, kegagalan server) tetap
                // ada tempatnya sendiri — bedanya sekarang ia hanya menampung
                // hal-hal yang memang tidak melekat pada satu kolom.
                state.error?.let { pesan ->
                    Spacer(Modifier.height(16.dp))
                    PitaGalat(pesan, Modifier.padding(horizontal = PadTepi))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

