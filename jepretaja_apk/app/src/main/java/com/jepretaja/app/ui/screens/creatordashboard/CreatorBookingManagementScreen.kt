package com.jepretaja.app.ui.screens.creatordashboard

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.util.Formatters
import com.jepretaja.app.data.model.BookingModel
import com.jepretaja.app.data.model.BookingStatus
import com.jepretaja.app.data.repository.BookingRepository
import com.jepretaja.app.ui.components.AppTopBar
import com.jepretaja.app.ui.components.EmptyState
import com.jepretaja.app.ui.components.PremiumCard
import com.jepretaja.app.ui.components.StatusBadge
import com.jepretaja.app.ui.components.rememberAppTopBarScrollBehavior
import com.jepretaja.app.ui.state.AuthViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Tab filter status untuk daftar booking creator. */
private enum class BookingFilterTab(val label: String) {
    PERLU_KONFIRMASI("Perlu Konfirmasi"),
    AKAN_DATANG("Akan Datang"),
    BERLANGSUNG("Berlangsung"),
    SELESAI("Selesai"),
    DIBATALKAN("Dibatalkan"),
    SEMUA("Semua"),
}

private fun BookingModel.masukTab(tab: BookingFilterTab): Boolean = when (tab) {
    BookingFilterTab.PERLU_KONFIRMASI -> status == BookingStatus.PAID
    BookingFilterTab.AKAN_DATANG -> status == BookingStatus.CONFIRMED || status == BookingStatus.UPCOMING
    BookingFilterTab.BERLANGSUNG -> status == BookingStatus.IN_PROGRESS
    BookingFilterTab.SELESAI -> status in setOf(
        BookingStatus.COMPLETED, BookingStatus.CUSTOMER_CONFIRMED, BookingStatus.FUNDS_RELEASED, BookingStatus.REVIEWED,
    )
    BookingFilterTab.DIBATALKAN -> status in setOf(BookingStatus.CANCELLED, BookingStatus.REJECTED)
    BookingFilterTab.SEMUA -> true
}

@HiltViewModel
class CreatorBookingManagementViewModel @Inject constructor(
    val repository: BookingRepository,
) : ViewModel() {

    /**
     * Kabar hasil aksi terima/tolak. Tanpa ini, kegagalan jaringan saat
     * menerima atau menolak booking terlihat sama seperti berhasil — creator
     * baru sadar booking-nya tidak berubah status setelah membuka ulang layar.
     */
    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()
    fun pesanDibaca() { _pesan.value = null }

    private val _aksiBerjalan = MutableStateFlow<String?>(null)
    val aksiBerjalan: StateFlow<String?> = _aksiBerjalan.asStateFlow()

    fun terimaBooking(bookingId: String) {
        viewModelScope.launch {
            _aksiBerjalan.value = bookingId
            runCatching { repository.confirmBooking(bookingId) }
                .onSuccess { _pesan.value = "Booking diterima" }
                .onFailure { _pesan.value = "Gagal menerima booking. Periksa koneksimu lalu coba lagi." }
            _aksiBerjalan.value = null
        }
    }

    fun tolakBooking(bookingId: String, alasan: String) {
        viewModelScope.launch {
            _aksiBerjalan.value = bookingId
            runCatching { repository.cancelBooking(bookingId, alasan) }
                .onSuccess { _pesan.value = "Booking ditolak" }
                .onFailure { _pesan.value = "Gagal menolak booking. Periksa koneksimu lalu coba lagi." }
            _aksiBerjalan.value = null
        }
    }
}

/** Creator Booking Management (section 13 & 28) — daftar booking masuk,
 * dengan filter status, pencarian, dan aksi terima/tolak langsung dari
 * daftar untuk booking yang masih menunggu konfirmasi. */
@Composable
fun CreatorBookingManagementScreen(
    onBack: () -> Unit,
    onBookingClick: (String) -> Unit,
    authViewModel: AuthViewModel,
    viewModel: CreatorBookingManagementViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val uid = authState.uid

    val scrollBehavior = rememberAppTopBarScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val pesan by viewModel.pesan.collectAsStateWithLifecycle()
    val aksiBerjalan by viewModel.aksiBerjalan.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(BookingFilterTab.SEMUA) }
    var query by remember { mutableStateOf("") }
    var ditolak by remember { mutableStateOf<BookingModel?>(null) }

    LaunchedEffect(pesan) {
        pesan?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.pesanDibaca()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AppTopBar(title = "Kelola Booking", onBack = onBack, scrollBehavior = scrollBehavior) },
    ) { padding ->
        if (uid == null) return@Scaffold
        val bookings by remember(uid) { viewModel.repository.streamCreatorBookings(uid) }.collectAsStateWithLifecycle(initialValue = emptyList())

        val perluKonfirmasiCount = bookings.count { it.masukTab(BookingFilterTab.PERLU_KONFIRMASI) }
        val hasilFilter = bookings
            .filter { it.masukTab(tab) }
            .filter { b ->
                query.isBlank() ||
                    b.packageName.contains(query, ignoreCase = true) ||
                    b.location.contains(query, ignoreCase = true)
            }

        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Cari paket atau lokasi") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = "Hapus") }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = MaterialTheme.shapes.medium,
            )

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BookingFilterTab.entries.forEach { t ->
                    val label = if (t == BookingFilterTab.PERLU_KONFIRMASI && perluKonfirmasiCount > 0) "${t.label} ($perluKonfirmasiCount)" else t.label
                    FilterChip(
                        selected = tab == t,
                        onClick = { tab = t },
                        label = { Text(label) },
                    )
                }
            }

            if (hasilFilter.isEmpty()) {
                EmptyState(
                    title = if (bookings.isEmpty()) "Belum ada booking masuk" else "Tidak ada booking di kategori ini",
                    description = if (bookings.isEmpty()) "Booking dari customer akan muncul di sini." else "Coba pilih tab atau kata kunci pencarian lain.",
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.weight(1f)) {
                    items(hasilFilter, key = { it.bookingId }) { b ->
                        PremiumCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            onClick = { onBookingClick(b.bookingId) },
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(40.dp).clip(MaterialTheme.shapes.small).background(AppColors.PrimarySoft),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Default.Event, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(20.dp)) }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(b.packageName, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${b.date?.let { Formatters.date(it) } ?: "-"} • ${b.time}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary,
                                    )
                                    Text(Formatters.currency(b.total), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                                }
                                Spacer(Modifier.width(8.dp))
                                StatusBadge(status = b.status)
                            }
                            if (b.status == BookingStatus.PAID) {
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { ditolak = b },
                                        enabled = aksiBerjalan != b.bookingId,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger),
                                        modifier = Modifier.weight(1f),
                                    ) { Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Tolak") }
                                    Button(
                                        onClick = { viewModel.terimaBooking(b.bookingId) },
                                        enabled = aksiBerjalan != b.bookingId,
                                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Success, contentColor = AppColors.OnPrimary),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        if (aksiBerjalan == b.bookingId) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AppColors.OnPrimary)
                                        } else {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Terima")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val bookingDitolak = ditolak
    if (bookingDitolak != null) {
        TolakBookingDialog(
            onConfirm = { alasan -> viewModel.tolakBooking(bookingDitolak.bookingId, alasan); ditolak = null },
            onDismiss = { ditolak = null },
        )
    }
}

@Composable
private fun TolakBookingDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val reasons = listOf(
        "creator_unavailable" to "Tidak tersedia di jadwal itu",
        "schedule_conflict" to "Bentrok dengan booking lain",
        "price_issue" to "Masalah harga/paket",
        "other" to "Lainnya",
    )
    var alasan by remember { mutableStateOf(reasons.first().first) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tolak booking ini?") },
        text = {
            Column {
                Text("Customer akan diberi tahu dan dana yang sudah dibayar akan dikembalikan.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                Spacer(Modifier.height(12.dp))
                reasons.forEach { (value, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = alasan == value, onClick = { alasan = value })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(alasan) }) { Text("Tolak Booking", color = AppColors.Danger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
