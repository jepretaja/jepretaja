package com.jepretaja.app.ui.screens.creatordashboard

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.data.model.PortfolioModel
import com.jepretaja.app.data.model.PortfolioStatus
import com.jepretaja.app.ui.components.AppTopBar
import com.jepretaja.app.ui.components.EmptyState
import com.jepretaja.app.ui.components.rememberAppTopBarScrollBehavior
import com.jepretaja.app.ui.state.AuthViewModel
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress

/** Saringan status di atas grid. */
private enum class SaringStatus(val judul: String, val status: String?) {
    SEMUA("Semua", null),
    DRAFT("Draft", PortfolioStatus.DRAFT),
    MENUNGGU("Menunggu Review", PortfolioStatus.MENUNGGU),
    DISETUJUI("Disetujui", PortfolioStatus.DISETUJUI),
    DITOLAK("Ditolak", PortfolioStatus.DITOLAK),
}

/**
 * Layar pengelolaan portfolio hanya mengurus state layar dan aksi pengguna.
 * Kartu, dialog, pratinjau, dan reorder dipisah agar perubahan visual tidak
 * harus menyentuh logika sinkronisasi album.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorPortfolioManagementScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel,
    viewModel: CreatorPortfolioManagementViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val uid = authState.uid
    var editing by remember { mutableStateOf<PortfolioModel?>(null) }
    var deleting by remember { mutableStateOf<PortfolioModel?>(null) }
    var pratinjau by remember { mutableStateOf<PortfolioModel?>(null) }
    var saring by remember { mutableStateOf(SaringStatus.SEMUA) }
    val progres by viewModel.progres.collectAsStateWithLifecycle()
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val pesan by viewModel.pesan.collectAsStateWithLifecycle()

    LaunchedEffect(pesan) {
        pesan?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.pesanDibaca()
        }
    }

    val semua by remember(uid) {
        if (uid != null) viewModel.albumSaya(uid) else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var urutan by remember { mutableStateOf<List<PortfolioModel>>(emptyList()) }
    val gridState = rememberLazyGridState()
    val reorder = rememberPenggeser(gridState) { dari, ke ->
        urutan = urutan.toMutableList().apply { add(ke, removeAt(dari)) }
    }
    LaunchedEffect(semua, reorder.indexDiseret) {
        if (reorder.indexDiseret == null) urutan = semua
    }

    val pilihMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        if (uris.isNotEmpty() && uid != null) {
            viewModel.unggahAlbum(uid, uris, urutanBerikutnya = urutan.size.toLong())
        }
    }

    val tersaring = remember(urutan, saring) {
        urutan.filter { saring.status == null || it.status == saring.status }
    }
    val bolehGeser = saring == SaringStatus.SEMUA

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "Kelola Portfolio",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = { pilihMedia.launch("*/*") },
                        enabled = !progres.sedangJalan,
                    ) { Icon(Icons.Default.Add, contentDescription = "Tambah album") }
                },
            )
        },
    ) { padding ->
        if (uid == null) return@Scaffold

        Column(Modifier.padding(padding).fillMaxSize()) {
            if (progres.sedangJalan) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        "Mengunggah ${progres.selesai + 1} dari ${progres.total}...",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TextSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (progres.total == 0) 0f else progres.selesai.toFloat() / progres.total },
                        color = AppColors.Primary,
                        trackColor = AppColors.SurfaceVariant,
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(percent = 50)),
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SaringStatus.entries, key = { it.name }) { s ->
                    val jumlah = if (s.status == null) urutan.size else urutan.count { it.status == s.status }
                    FilterChip(
                        selected = saring == s,
                        onClick = { saring = s },
                        label = { Text(if (jumlah > 0) "${s.judul} · $jumlah" else s.judul) },
                        shape = RoundedCornerShape(percent = 50),
                    )
                }
            }

            if (bolehGeser && tersaring.size > 1) {
                Text(
                    "Tekan lama sebuah album untuk menggeser urutannya.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            if (tersaring.isEmpty()) {
                EmptyState(
                    title = if (saring == SaringStatus.SEMUA) "Belum ada portfolio" else "Tidak ada album ${saring.judul.lowercase()}",
                    description = if (saring == SaringStatus.SEMUA) {
                        "Tambahkan karya terbaikmu lewat tombol + di pojok kanan atas. Bisa pilih beberapa berkas sekaligus."
                    } else {
                        "Ganti saringan di atas untuk melihat album lainnya."
                    },
                    icon = Icons.Default.PhotoLibrary,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (bolehGeser) {
                                Modifier.pointerInput(tersaring.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { posisi -> reorder.mulai(posisi) },
                                        onDrag = { perubahan, jarak ->
                                            perubahan.consume()
                                            reorder.seret(jarak)
                                        },
                                        onDragEnd = {
                                            reorder.selesai()
                                            viewModel.simpanUrutan(urutan.map { it.portfolioId })
                                        },
                                        onDragCancel = { reorder.selesai() },
                                    )
                                }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    itemsIndexed(tersaring, key = { _, item -> item.portfolioId }) { indeks, item ->
                        val diseret = reorder.indexDiseret == indeks
                        KartuAlbum(
                            item = item,
                            bolehGeser = bolehGeser,
                            modifier = Modifier
                                .zIndex(if (diseret) 1f else 0f)
                                .graphicsLayer {
                                    if (diseret) {
                                        translationX = reorder.geser.x
                                        translationY = reorder.geser.y
                                        scaleX = 1.04f
                                        scaleY = 1.04f
                                    }
                                },
                            onBuka = { pratinjau = item },
                            onEdit = { editing = item },
                            onHapus = { deleting = item },
                            onAjukan = { viewModel.ajukanReview(item.portfolioId) },
                            onJadikanDraft = { viewModel.jadikanDraft(item.portfolioId) },
                        )
                    }
                }
            }
        }
    }

    pratinjau?.let { item ->
        PratinjauLayarPenuh(item = item, onTutup = { pratinjau = null })
    }

    editing?.let { item ->
        DialogEditAlbum(
            item = item,
            onBatal = { editing = null },
            onSimpan = { judul, kategori ->
                viewModel.updatePortfolioItem(item.portfolioId, judul, kategori)
                editing = null
            },
        )
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Hapus album ini?") },
            text = {
                Text(
                    "${item.media.size} media di dalamnya ikut hilang dari portfolio " +
                        "dan tidak bisa dikembalikan.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePortfolioItem(item.portfolioId)
                    deleting = null
                }) { Text("Hapus", color = AppColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Batal") } },
        )
    }
}