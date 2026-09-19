package com.jepretaja.app.ui.screens.creator

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.util.Formatters
import com.jepretaja.app.data.model.CreatorModel
import com.jepretaja.app.data.model.PortfolioModel
import com.jepretaja.app.data.model.ReviewModel
import com.jepretaja.app.ui.components.AppAvatar
import com.jepretaja.app.ui.components.AppTopBar
import com.jepretaja.app.ui.components.EmptyState
import com.jepretaja.app.ui.components.PremiumCard
import com.jepretaja.app.ui.components.SkeletonBox
import com.jepretaja.app.ui.components.SkeletonList
import com.jepretaja.app.ui.state.AuthViewModel
import com.jepretaja.app.ui.copy.AppMode
import com.jepretaja.app.ui.copy.LocalJepretAjaCopy
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Indeks item TabRow di dalam grid: 0 kepala, 1 badan keterangan, 2 TabRow.
 *
 * Dipakai tombol "Booking" untuk menggulir ke daftar paket. Sebelumnya posisi
 * itu diukur lewat `onGloballyPositioned` lalu digulir dengan koordinat piksel —
 * cara yang selalu meleset kalau isi di atasnya berubah tinggi (bio panjang,
 * kartu ketersediaan muncul-hilang) sesudah pengukuran terakhir.
 */
private const val INDEKS_TABROW = 2

/**
 * Profil publik creator — halaman paling menentukan di aplikasi ini, karena di
 * sinilah calon pelanggan memutuskan memesan atau menutup aplikasi.
 *
 * Perubahan terbesar di versi ini adalah **satu area gulir, bukan dua**.
 * Sebelumnya halaman ini berupa Column yang menggulir, dan isi tab (portofolio,
 * Explore, paket) ditumpangkan di dalam `Box(Modifier.height(520.dp))` yang
 * menggulir sendiri di tengahnya. Akibatnya: menggeser di dalam kotak
 * menggerakkan grid tapi tidak halamannya, menggeser di luar kotak menggerakkan
 * halaman tapi tidak gridnya, dan portofolio berisi 40 foto terkurung di
 * jendela setinggi separuh layar. Sekarang seluruh halaman adalah satu
 * LazyVerticalGrid tiga kolom: kepala profil dan blok keterangan menjadi item
 * selebar penuh, ubin portofolio menjadi item biasa di grid yang sama.
 *
 * Selebihnya sudah ada sejak versi sebelumnya dan dipertahankan: kepala dengan
 * sampul 220dp + avatar 108dp, lencana terverifikasi bertulisan, kartu angka
 * (rating, ulasan, proyek selesai, pengikut) dengan harga mulai di barisnya
 * sendiri, kalender ketersediaan 14 hari, dan bilah Chat/Booking yang menempel
 * di dasar layar.
 */
@Composable
fun CreatorProfileScreen(
    onBack: () -> Unit,
    onChatOpen: (String) -> Unit,
    onReviewsClick: (String) -> Unit,
    onPackageClick: (String) -> Unit,
    onLoginRequired: () -> Unit,
    authViewModel: AuthViewModel,
    onFollowList: (String, Int) -> Unit = { _, _ -> },
    onCategoryClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: CreatorProfileViewModel = hiltViewModel(),
) {
    val creator by viewModel.creator.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val copy = LocalJepretAjaCopy.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val tabs = when (copy.mode) {
        AppMode.GUEST -> listOf("Karya", "Inspirasi", "Paket")
        AppMode.CUSTOMER -> listOf("Portofolio", "Explore", "Paket Sesi")
        AppMode.CREATOR -> listOf("Portofolio", "Etalase", "Paket Layanan")
    }
    var tabIndex by remember { mutableIntStateOf(0) }
    var menuTerbuka by remember { mutableStateOf(false) }
    var dialogLapor by remember { mutableStateOf(false) }
    var dialogBlokir by remember { mutableStateOf(false) }
    var pesan by remember { mutableStateOf<String?>(null) }
    var portofolioDibuka by remember { mutableStateOf<PortfolioModel?>(null) }
    var fotoProfilDibuka by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pesan) {
        pesan?.let { snackbarHostState.showSnackbar(it); pesan = null }
    }

    val gridState = rememberLazyGridState()

    fun bukaPaket() {
        tabIndex = 2
        // Menggulir ke indeks item, bukan ke koordinat piksel. Tombol "Booking"
        // dulu hanya mengubah tabIndex; kalau tabnya sedang jauh di bawah
        // layar, ketukan itu tampak tidak menghasilkan apa-apa dan pengguna
        // menekannya berulang kali.
        scope.launch { gridState.animateScrollToItem(INDEKS_TABROW) }
    }

    fun mulaiChat(c: CreatorModel) {
        val uid = authState.uid
        if (uid == null) onLoginRequired()
        else scope.launch { onChatOpen(viewModel.openChat(uid, c.displayName)) }
    }

    val mengikutiJumlah by remember(viewModel.creatorId) { viewModel.jumlahMengikuti() }
        .collectAsStateWithLifecycle(initialValue = 0)
    // `null` = belum ada jawaban, dibedakan dari daftar kosong. Tanpa
    // pembedaan itu bagian ulasan selalu berkedip "belum ada ulasan" sekejap
    // pada creator yang justru ulasannya banyak.
    val ulasan by remember(viewModel.creatorId) { viewModel.ulasanTeratas() }
        .collectAsStateWithLifecycle(initialValue = null)
    val tanggalPenuh by remember(viewModel.creatorId) { viewModel.tanggalPenuh() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val portofolio by remember(viewModel.creatorId) { viewModel.portofolio() }
        .collectAsStateWithLifecycle(initialValue = null)
    val karyaExplore by remember(viewModel.creatorId) { viewModel.karyaExplore() }
        .collectAsStateWithLifecycle(initialValue = null)
    val paket by remember(viewModel.creatorId) { viewModel.paket() }
        .collectAsStateWithLifecycle(initialValue = null)
    val sudahDiikuti by remember(viewModel.creatorId, authState.uid) {
        val uid = authState.uid
        if (uid != null) viewModel.apakahDiikuti(uid) else kotlinx.coroutines.flow.flowOf(false)
    }.collectAsStateWithLifecycle(initialValue = false)

    // Dihitung dari daftar karya yang memang sudah diambil untuk tab Explore,
    // bukan dari langganan Firestore kedua ke koleksi yang sama.
    val totalSuka = remember(karyaExplore) { karyaExplore.orEmpty().sumOf { it.likeCount.toInt() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = creator?.displayName ?: "",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        val tautan = "https://jepretaja.app/creator/${viewModel.creatorId}"
                        val kirim = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Lihat profil ${creator?.displayName.orEmpty()} di JepretAja: $tautan")
                        }
                        runCatching { context.startActivity(Intent.createChooser(kirim, "Bagikan profil")) }
                    }) { Icon(Icons.Default.Share, contentDescription = "Bagikan profil") }

                    IconButton(onClick = { menuTerbuka = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu lainnya")
                    }
                    DropdownMenu(expanded = menuTerbuka, onDismissRequest = { menuTerbuka = false }) {
                        DropdownMenuItem(
                            text = { Text("Laporkan creator") },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                            onClick = { menuTerbuka = false; dialogLapor = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Blokir creator") },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                            onClick = { menuTerbuka = false; dialogBlokir = true },
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> KerangkaProfil(Modifier.padding(padding))

            error != null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.CloudOff,
                    title = "Profil creator belum siap",
                    description = error,
                    actionLabel = "Coba Lagi",
                    onAction = { viewModel.load() },
                )
            }

            creator == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.PersonOff,
                    title = "Profil creator tidak ditemukan",
                    description = "Profil ini mungkin sudah dihapus atau sedang tidak tersedia.",
                    actionLabel = "Kembali",
                    onAction = onBack,
                )
            }

            else -> {
                val c = creator!!
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    itemPenuh("kepala") {
                        KepalaProfil(c = c, onFotoClick = { fotoProfilDibuka = true })
                    }

                    itemPenuh("badan") {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            Spacer(Modifier.height(14.dp))

                            // --- Nama + lencana verifikasi ---
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    c.displayName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = AppColors.TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (c.verified) {
                                    Spacer(Modifier.width(8.dp))
                                    LencanaVerified()
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn, contentDescription = "Lokasi",
                                    tint = AppColors.TextSecondary, modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    c.city?.takeIf { it.isNotBlank() } ?: "Lokasi belum diisi",
                                    color = AppColors.TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text("  ·  ", color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    statusKehadiran(c),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (sedangOnline(c)) AppColors.Success else AppColors.TextSecondary,
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                            KartuAngka(
                                creator = c,
                                mengikutiJumlah = mengikutiJumlah,
                                onUlasan = { onReviewsClick(c.creatorId) },
                                onPengikut = { onFollowList(viewModel.creatorId, 1) },
                                onMengikuti = { onFollowList(viewModel.creatorId, 0) },
                                onLihatPaket = { bukaPaket() },
                            )

                            Spacer(Modifier.height(14.dp))
                            BadgeStatusBooking(c)

                            Spacer(Modifier.height(10.dp))
                            TrustBadgeRow(c)

                            // Sinyal kepercayaan yang lebih menentukan daripada
                            // jumlah pengikut ketika yang dibeli adalah jasa.
                            val penyelesaian = tingkatPenyelesaian(c)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                c.avgResponseMinutes?.let {
                                    ChipInfo(Icons.Default.Bolt, "Balas ${durasiSingkat(it)}")
                                }
                                penyelesaian?.let { ChipInfo(Icons.Default.TaskAlt, "$it% booking selesai") }
                                // "Suka" adalah keterangan, jadi digambar
                                // sebagai keterangan — bukan angka besar yang
                                // bisa diketuk tapi tidak menuju ke mana pun.
                                if (totalSuka > 0) {
                                    ChipInfo(Icons.Default.FavoriteBorder, "${Formatters.compactCount(totalSuka.toLong())} suka")
                                }
                            }

                            c.bio?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(14.dp))
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                            }

                            if (c.categories.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    c.categories.take(4).forEach { kategori ->
                                        AssistChip(
                                            // Chip kategori adalah jalan pintas
                                            // paling wajar dari profil ke
                                            // "siapa lagi yang mengerjakan ini".
                                            onClick = { onCategoryClick(kategori) },
                                            label = { Text(kategori, style = MaterialTheme.typography.labelSmall) },
                                            shape = RoundedCornerShape(percent = 50),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val uid = authState.uid
                                    if (uid == null) {
                                        onLoginRequired()
                                    } else {
                                        viewModel.toggleFollow(
                                            uid,
                                            authState.profile?.name.orEmpty(),
                                            authState.profile?.photoUrl,
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(percent = 50),
                                colors = if (sudahDiikuti) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = AppColors.SurfaceVariant,
                                        contentColor = AppColors.TextPrimary,
                                    )
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = AppColors.Primary,
                                        contentColor = AppColors.OnPrimary,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) {
                                Icon(
                                    if (sudahDiikuti) Icons.Default.Check else Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (sudahDiikuti) "Mengikuti" else "Ikuti")
                            }

                            Spacer(Modifier.height(12.dp))
                            AksiProfilCreator(
                                creator = c,
                                onBooking = {
                                    val packageId = paket.orEmpty()
                                        .minByOrNull { it.price }
                                        ?.packageId
                                    if (packageId != null) onPackageClick(packageId) else bukaPaket()
                                },
                                onChat = { mulaiChat(c) },
                            )

                            Spacer(Modifier.height(20.dp))
                            KartuKetersediaan(creator = c, tanggalPenuh = tanggalPenuh, onTanya = { mulaiChat(c) })

                            Spacer(Modifier.height(20.dp))
                            BagianUlasan(
                                creator = c,
                                ulasan = ulasan,
                                onLihatSemua = { onReviewsClick(c.creatorId) },
                            )

                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    itemPenuh("tab") {
                        TabRow(
                            selectedTabIndex = tabIndex,
                            containerColor = AppColors.Background,
                            contentColor = AppColors.Primary,
                        ) {
                            tabs.forEachIndexed { i, judul ->
                                Tab(
                                    selected = tabIndex == i,
                                    onClick = { tabIndex = i },
                                    text = { Text(judul, style = MaterialTheme.typography.labelLarge) },
                                    icon = {
                                        Icon(
                                            ikonTab(i), contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    },
                                    selectedContentColor = AppColors.Primary,
                                    unselectedContentColor = AppColors.TextSecondary,
                                )
                            }
                        }
                    }

                    when (tabIndex) {
                        0 -> isiPortofolio(portofolio) { portofolioDibuka = it }
                        1 -> isiExplore(karyaExplore, c.pinnedPostId, onPostClick)
                        else -> isiPaket(paket, onPackageClick) { mulaiChat(c) }
                    }

                    itemPenuh("ekor") { Spacer(Modifier.height(24.dp)) }
                }
            }
        }

        portofolioDibuka?.let { item ->
            PratinjauPortfolio(item = item, onTutup = { portofolioDibuka = null })
        }

        if (fotoProfilDibuka) {
            creator?.let { c ->
                PratinjauFotoProfil(
                    url = c.photoUrl,
                    nama = c.displayName,
                    onTutup = { fotoProfilDibuka = false },
                )
            }
        }

        if (dialogBlokir) {
            AlertDialog(
                onDismissRequest = { dialogBlokir = false },
                title = { Text("Blokir ${creator?.displayName.orEmpty()}?") },
                text = {
                    Text(
                        "Kalian tidak akan bisa saling mengirim pesan. " +
                            "Blokir bisa dibatalkan kapan saja lewat daftar chat.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val uid = authState.uid
                        dialogBlokir = false
                        if (uid == null) onLoginRequired()
                        else viewModel.blokir(uid) { pesan = "Creator diblokir." }
                    }) { Text("Blokir", color = AppColors.Danger) }
                },
                dismissButton = { TextButton(onClick = { dialogBlokir = false }) { Text("Batal") } },
            )
        }

        if (dialogLapor) {
            val alasanList = listOf(
                "inappropriate_content" to "Konten tidak pantas",
                "scam" to "Penipuan / meminta bayaran di luar aplikasi",
                "impersonation" to "Mengaku sebagai orang lain",
                "other" to "Lainnya",
            )
            AlertDialog(
                onDismissRequest = { dialogLapor = false },
                title = { Text("Laporkan creator") },
                text = {
                    Column {
                        alasanList.forEach { (kode, label) ->
                            TextButton(
                                onClick = {
                                    val uid = authState.uid
                                    dialogLapor = false
                                    if (uid == null) {
                                        onLoginRequired()
                                    } else {
                                        viewModel.laporkan(uid, kode) {
                                            pesan = "Laporan terkirim. Tim kami akan meninjaunya."
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(label, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { dialogLapor = false }) { Text("Batal") } },
            )
        }
    }
}

private fun ikonTab(index: Int): ImageVector = when (index) {
    0 -> Icons.Default.PhotoLibrary
    1 -> Icons.Default.GridView
    else -> Icons.Default.Inventory2
}
