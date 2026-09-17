package com.jepretaja.app.ui.screens.booking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.util.Formatters
import com.jepretaja.app.core.util.SlipPembayaran
import com.jepretaja.app.data.model.BookingStatus
import com.jepretaja.app.services.AnalyticsService
import com.jepretaja.app.ui.components.AppTopBar
import com.jepretaja.app.ui.components.EmptyState
import com.jepretaja.app.ui.components.ErrorState
import com.jepretaja.app.ui.components.InfoRow
import com.jepretaja.app.ui.components.LiveLocationTracker
import com.jepretaja.app.data.repository.LiveLocationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.jepretaja.app.ui.components.PremiumCard
import com.jepretaja.app.ui.components.SectionHeader
import com.jepretaja.app.ui.components.SkeletonBox
import com.jepretaja.app.ui.components.StatusBadge
import com.jepretaja.app.ui.components.rememberAppTopBarScrollBehavior
import com.jepretaja.app.ui.state.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class BookingSlipAction { UNDUH, BAGIKAN }

/** Booking Detail — tombol aksi mengikuti state machine production (sama
 * dengan backend bookingStatus.js). Setiap tombol memanggil Cloud
 * Function, TIDAK ADA write Firestore langsung untuk status. */
@Composable
fun BookingDetailScreen(
    bookingId: String,
    onBack: () -> Unit,
    onChatOpen: (String) -> Unit,
    authViewModel: AuthViewModel,
    onWriteReview: (String, String) -> Unit = { _, _ -> },
    viewModel: BookingDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(bookingId) { viewModel.observeLive(bookingId) }
    val booking by viewModel.booking.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val actionLoading by viewModel.actionLoading.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val myUid = authState.uid
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDisputeSheet by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("change_of_plans") }
    var showCompleteConfirm by remember { mutableStateOf(false) }
    var sosBusy by remember { mutableStateOf(false) }
    var sosSent by remember { mutableStateOf(false) }

    val payment by viewModel.payment.collectAsState()
    val context = LocalContext.current
    val liveLocationRepository = remember { LiveLocationRepository(FirebaseFirestore.getInstance()) }
    var aksiSlip by remember { mutableStateOf<BookingSlipAction?>(null) }
    var slipSibuk by remember { mutableStateOf(false) }

    val izinSimpan = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { diberi ->
        if (diberi) {
            aksiSlip = BookingSlipAction.UNDUH
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Tanpa izin penyimpanan, slip tidak bisa disimpan ke galeri. Kamu masih bisa membagikannya."
                )
            }
        }
    }

    LaunchedEffect(aksiSlip) {
        val aksi = aksiSlip ?: return@LaunchedEffect
        val b = booking
        if (b == null) { aksiSlip = null; return@LaunchedEffect }

        // Android 9 ke bawah belum punya scoped storage, jadi menulis ke galeri
        // masih butuh izin. Dimintanya di sini — saat pengguna benar-benar
        // menekan Unduh — bukan saat layar dibuka, supaya alasannya jelas.
        if (aksi == BookingSlipAction.UNDUH &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            aksiSlip = null
            izinSimpan.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return@LaunchedEffect
        }

        slipSibuk = true
        val isi = SlipPembayaran.isiDari(b, payment?.paymentId, payment?.createdAt?.toDate()?.time)
        val hasil = runCatching {
            // Menggambar bitmap 1080 piksel bukan pekerjaan untuk benang utama —
            // di HP kelas bawah itu cukup untuk membuat layar tersendat.
            val bitmap = withContext(Dispatchers.Default) { SlipPembayaran.gambar(isi) }
            when (aksi) {
                BookingSlipAction.UNDUH -> {
                    SlipPembayaran.unduh(context, isi, bitmap)
                    "Slip disimpan ke galeri, folder JepretAja."
                }
                BookingSlipAction.BAGIKAN -> {
                    val uri = SlipPembayaran.siapkanUntukBagikan(context, isi, bitmap)
                    context.startActivity(SlipPembayaran.intentBagikan(isi, uri))
                    null
                }
            }
        }
        slipSibuk = false
        aksiSlip = null
        hasil
            .onSuccess { pesan -> pesan?.let { snackbarHostState.showSnackbar(it) } }
            .onFailure { snackbarHostState.showSnackbar(it.message ?: "Slip gagal dibuat.") }
    }

    LaunchedEffect(actionMessage) { actionMessage?.let { snackbarHostState.showSnackbar(it) } }

    val scrollBehavior = rememberAppTopBarScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AppColors.Background,
        topBar = { AppTopBar(title = "Detail Booking", onBack = onBack, scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            loading -> BookingDetailSkeleton(Modifier.padding(padding))
            error != null -> ErrorState(
                onRetry = { viewModel.observeLive(bookingId) },
                description = error,
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
            booking == null -> EmptyState(
                title = "Booking tidak ditemukan",
                description = "Booking mungkin sudah dihapus atau tautannya tidak lagi berlaku.",
                icon = Icons.Default.EventBusy,
                modifier = Modifier.padding(padding).fillMaxSize(),
            )
            else -> {
                val b = booking!!
                val isCreator = b.creatorId == myUid
                val isCustomer = b.customerId == myUid

                if (myUid != null && (isCreator || isCustomer)) {
                    LiveLocationTracker(
                        bookingId = b.bookingId,
                        userId = myUid,
                        role = if (isCreator) "creator" else "customer",
                        targetLatitude = b.latitude,
                        targetLongitude = b.longitude,
                        enabled = b.status == BookingStatus.IN_PROGRESS,
                    )
                }

                LazyColumn(Modifier.padding(padding).padding(vertical = 20.dp)) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("#${b.bookingId.take(8).uppercase()}", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                            StatusBadge(status = b.status)
                        }
                        Spacer(Modifier.height(16.dp))
                        PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), elevation = 3.dp) {
                            InfoRow("Paket", b.packageName)
                            b.date?.let { InfoRow("Tanggal", Formatters.date(it)) }
                            InfoRow("Jam", b.time)
                            InfoRow("Lokasi", b.location)
                            InfoRow("Total", Formatters.currency(b.total), valueColor = AppColors.Primary)
                        }

                        b.priceBreakdown?.let { pb ->
                            Spacer(Modifier.height(20.dp))
                            SectionHeader("Rincian Harga")
                            Spacer(Modifier.height(10.dp))
                            PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), elevation = 3.dp) {
                                InfoRow("Harga Paket", Formatters.currency(pb.packagePrice))
                                InfoRow("Biaya Perjalanan", Formatters.currency(pb.travelFee))
                                InfoRow("Diskon", "- ${Formatters.currency(pb.discount)}", valueColor = AppColors.Success)
                                InfoRow("Platform Fee (${pb.platformFeePercent}%)", Formatters.currency(pb.platformFee))
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        SectionHeader("Timeline Booking")
                        Spacer(Modifier.height(10.dp))
                        BookingTimeline(status = b.status)
                        // Slip hanya masuk akal setelah ada uang yang benar-benar
                        // masuk. Menawarkannya pada booking yang belum dibayar
                        // berarti memberi "bukti pembayaran" untuk pembayaran
                        // yang belum terjadi.
                        if (b.status !in listOf(BookingStatus.DRAFT, BookingStatus.PENDING_PAYMENT)) {
                            Spacer(Modifier.height(20.dp))
                            SectionHeader("Slip Pembayaran", subtitle = "Simpan ke galeri atau kirim ke creator")
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { aksiSlip = BookingSlipAction.UNDUH },
                                    enabled = !slipSibuk,
                                    shape = RoundedCornerShape(percent = 50),
                                    modifier = Modifier.weight(1f).height(52.dp),
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp)); Text("Unduh")
                                }
                                OutlinedButton(
                                    onClick = { aksiSlip = BookingSlipAction.BAGIKAN },
                                    enabled = !slipSibuk,
                                    shape = RoundedCornerShape(percent = 50),
                                    modifier = Modifier.weight(1f).height(52.dp),
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp)); Text("Bagikan")
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Column(Modifier.padding(horizontal = 20.dp)) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch { onChatOpen(viewModel.openChat(b.bookingId, b.customerId, b.creatorId, b.packageName)) }
                                },
                                shape = RoundedCornerShape(percent = 50),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) { Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Message Creator") }
                            Spacer(Modifier.height(12.dp))

                            // --- Aksi Creator ---
                            if (isCreator && b.status == BookingStatus.PAID) {
                                ActionButton("Terima Booking", Icons.Default.CheckCircle, actionLoading) {
                                    viewModel.confirmBooking(b.bookingId)
                                }
                            }
                            if (isCreator && (b.status == BookingStatus.CONFIRMED || b.status == BookingStatus.UPCOMING)) {
                                ActionButton("Mulai Pengerjaan", Icons.Default.PlayCircleOutline, actionLoading) { viewModel.startService(b.bookingId) }
                            }
                            if (isCreator && b.status == BookingStatus.IN_PROGRESS) {
                                ActionButton("Tandai Selesai Dikerjakan", Icons.Default.CheckCircleOutline, actionLoading) { showCompleteConfirm = true }
                            }

                            // --- Aksi Customer ---
                            if (isCustomer && b.status == BookingStatus.COMPLETED) {
                                ActionButton("Konfirmasi Selesai & Cairkan Dana", Icons.Default.VerifiedUser, actionLoading) {
                                    viewModel.confirmCompletion(b.bookingId)
                                    AnalyticsService.logBookingCompleted(b.bookingId)
                                }
                            }
                            if (isCustomer && b.status in listOf(BookingStatus.DRAFT, BookingStatus.PENDING_PAYMENT, BookingStatus.PAID, BookingStatus.CONFIRMED, BookingStatus.UPCOMING)) {
                                ActionButton("Batalkan Booking", Icons.Default.Cancel, actionLoading, danger = true) { showCancelConfirm = true }
                            }
                            if (isCustomer && b.status in listOf(BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED)) {
                                ActionButton("Buka Dispute", Icons.Default.Report, actionLoading, danger = true) { showDisputeSheet = true }
                            }
                            if ((isCustomer || isCreator) && b.status == BookingStatus.IN_PROGRESS && !sosSent) {
                                ActionButton("SOS / Hubungi Support", Icons.Default.Emergency, sosBusy, danger = true) {
                                    sosBusy = true
                                    scope.launch {
                                        runCatching {
                                            liveLocationRepository.raiseSos(
                                                b.bookingId,
                                                myUid.orEmpty(),
                                                if (isCreator) "creator" else "customer",
                                                "Permintaan bantuan darurat dari halaman perjalanan.",
                                            )
                                        }.onSuccess {
                                            sosSent = true
                                            snackbarHostState.showSnackbar("SOS terkirim ke support.")
                                        }.onFailure {
                                            snackbarHostState.showSnackbar("SOS gagal dikirim. Coba lagi.")
                                        }
                                        sosBusy = false
                                    }
                                }
                            }
                            // Menulis ulasan baru masuk akal setelah pekerjaan
                            // benar-benar rampung — dan sebelumnya tidak ada
                            // jalan sama sekali menuju layar ini.
                            if (isCustomer && b.status in listOf(
                                    BookingStatus.CUSTOMER_CONFIRMED,
                                    BookingStatus.FUNDS_RELEASED,
                                    BookingStatus.REVIEWED,
                                )
                            ) {
                                ActionButton("Beri Ulasan", Icons.Default.StarRate, actionLoading) {
                                    onWriteReview(b.bookingId, b.creatorId)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCancelConfirm) {
        CancelBookingDialog(
            total = booking?.total ?: 0L,
            reason = cancelReason,
            onReason = { cancelReason = it },
            onConfirm = { showCancelConfirm = false; viewModel.cancelBooking(bookingId, cancelReason) },
            onDismiss = { showCancelConfirm = false },
        )
    }
    if (showCompleteConfirm) {
        ConfirmDialog(
            title = "Tandai Selesai?", message = "Customer akan diminta mengonfirmasi hasil sebelum dana dicairkan.",
            onConfirm = { showCompleteConfirm = false; viewModel.markServiceCompleted(bookingId) },
            onDismiss = { showCompleteConfirm = false },
        )
    }
    if (showDisputeSheet) {
        DisputeBottomSheet(
            onDismiss = { showDisputeSheet = false },
            onSubmit = { reason -> showDisputeSheet = false; viewModel.openDispute(bookingId, reason) },
        )
    }
}

@Composable
private fun BookingTimeline(status: String) {
    val steps = listOf(
        "Booking requested" to setOf(BookingStatus.DRAFT, BookingStatus.PENDING_PAYMENT, BookingStatus.PAID, BookingStatus.CONFIRMED, BookingStatus.UPCOMING, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED, BookingStatus.CUSTOMER_CONFIRMED, BookingStatus.FUNDS_RELEASED, BookingStatus.REVIEWED),
        "Payment received" to setOf(BookingStatus.PAID, BookingStatus.CONFIRMED, BookingStatus.UPCOMING, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED, BookingStatus.CUSTOMER_CONFIRMED, BookingStatus.FUNDS_RELEASED, BookingStatus.REVIEWED),
        "Creator confirmed" to setOf(BookingStatus.CONFIRMED, BookingStatus.UPCOMING, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED, BookingStatus.CUSTOMER_CONFIRMED, BookingStatus.FUNDS_RELEASED, BookingStatus.REVIEWED),
        "Event day" to setOf(BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED, BookingStatus.CUSTOMER_CONFIRMED, BookingStatus.FUNDS_RELEASED, BookingStatus.REVIEWED),
        "Delivery" to setOf(BookingStatus.COMPLETED, BookingStatus.CUSTOMER_CONFIRMED, BookingStatus.FUNDS_RELEASED, BookingStatus.REVIEWED),
        "Completed" to setOf(BookingStatus.CUSTOMER_CONFIRMED, BookingStatus.FUNDS_RELEASED, BookingStatus.REVIEWED),
    )
    PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), elevation = 3.dp) {
        steps.forEachIndexed { index, (label, completedFor) ->
            val done = status in completedFor
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (done) AppColors.Success else AppColors.TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(label, color = if (done) AppColors.TextPrimary else AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            if (index < steps.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun BookingDetailSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        SkeletonBox(Modifier.fillMaxWidth(0.4f).height(18.dp))
        Spacer(Modifier.height(16.dp))
        PremiumCard(contentPadding = PaddingValues(16.dp), elevation = 3.dp) {
            repeat(5) { index ->
                SkeletonBox(Modifier.fillMaxWidth(if (index % 2 == 0) 0.72f else 0.45f).height(16.dp))
                if (index < 4) Spacer(Modifier.height(18.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(52.dp), MaterialTheme.shapes.large)
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, loading: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        if (danger) {
            OutlinedButton(
                onClick = onClick, enabled = !loading,
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Danger),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(label)
            }
        } else {
            Button(
                onClick = onClick, enabled = !loading,
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(label)
            }
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Ya, lanjutkan", color = AppColors.Primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun CancelBookingDialog(total: Long, reason: String, onReason: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val reasons = listOf("change_of_plans" to "Change of plans", "creator_unavailable" to "Creator unavailable", "price_issue" to "Price issue", "emergency" to "Emergency", "other" to "Other")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel booking") },
        text = {
            Column {
                Text("Why?", style = MaterialTheme.typography.titleSmall)
                reasons.forEach { (value, label) -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(reason == value, { onReason(value) }); Text(label) } }
                Spacer(Modifier.height(8.dp))
                Text("Estimated refund: ${Formatters.currency(total)}", color = AppColors.Primary, style = MaterialTheme.typography.titleSmall)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Cancel booking", color = AppColors.Danger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep booking") } },
    )
}

@Composable
private fun DisputeBottomSheet(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.Surface) {
        Column(Modifier.padding(20.dp)) {
            Text("Buka Dispute", style = MaterialTheme.typography.headlineSmall, color = AppColors.TextPrimary)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(reason, { reason = it }, label = { Text("Jelaskan masalahnya") }, minLines = 3, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSubmit(reason) },
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Kirim") }
            Spacer(Modifier.height(20.dp))
        }
    }
}
