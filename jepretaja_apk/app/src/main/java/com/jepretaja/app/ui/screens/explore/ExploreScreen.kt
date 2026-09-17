package com.jepretaja.app.ui.screens.explore

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import com.jepretaja.app.ui.components.AppAvatar
import com.jepretaja.app.ui.components.AdMobNativeExplore
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.util.AppConstants
import com.jepretaja.app.ui.components.ErrorState
import com.jepretaja.app.ui.screens.chat.SendToChatSheet
import com.jepretaja.app.ui.screens.explore.ExploreCommentsSheet
import com.jepretaja.app.ui.state.AuthViewModel
import kotlinx.coroutines.flow.flowOf

/** EXPLORE — Fitur Inti JepretAja. Feed vertikal fullscreen dengan tab
 * kategori dan swipe-up antar post. */
@Composable
fun ExploreScreen(
    authViewModel: AuthViewModel,
    onLoginRequired: () -> Unit,
    onCreatorClick: (String) -> Unit,
    onSearch: () -> Unit,
    onPackageClick: (String) -> Unit,
    /** [isCategory] true untuk kategori resmi, false untuk tagar bebas. */
    onTagClick: (String, Boolean) -> Unit = { _, _ -> },
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val posts by viewModel.posts.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val myUid = authState.uid
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var editingPost by remember { mutableStateOf<com.jepretaja.app.data.model.ExplorePostModel?>(null) }
    var sharingPost by remember { mutableStateOf<com.jepretaja.app.data.model.ExplorePostModel?>(null) }
    var commentsPostId by remember { mutableStateOf<String?>(null) }
    val habis by viewModel.habis.collectAsState()
    val kecepatan by viewModel.kecepatan.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val feedError by viewModel.error.collectAsState()
    val saranCreator by viewModel.saranCreator.collectAsState()

    // Tab "Following" perlu tahu siapa yang sedang masuk untuk bisa menyaring.
    LaunchedEffect(myUid) { viewModel.setUserId(myUid) }

    // Tab "Nearby" butuh lokasi; izinnya baru diminta saat tab itu dibuka,
    // bukan di awal — pengguna yang tidak memakai Nearby tidak perlu ditanya.
    val needsLocation by viewModel.needsLocation.collectAsState()
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun loadLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return
        runCatching {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) viewModel.setLocation(loc.latitude, loc.longitude)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) loadLocation() }

    LaunchedEffect(activeTab, needsLocation) {
        if (activeTab == "Nearby" && needsLocation) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                loadLocation()
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(AppColors.ExploreBackground)) {
        if (feedError != null) {
            ErrorState(
                title = "Feed gagal dimuat",
                description = feedError ?: "Terjadi masalah saat mengambil konten. Pastikan koneksi internet Anda stabil lalu coba lagi.",
                onRetry = { viewModel.retryLoad() },
                modifier = Modifier.align(Alignment.Center),
            )
        } else if (isLoading && posts.isEmpty()) {
            ExploreFeedSkeleton(modifier = Modifier.fillMaxSize())
        } else if (posts.isEmpty() && activeTab == "Following" && saranCreator.isNotEmpty()) {
            // Layar kosong pada tab Following adalah jalan buntu: pengguna
            // diminta mengikuti seseorang tanpa diberi satu pun nama untuk
            // mulai. Jadi ruang kosongnya diisi saran, bukan kalimat penjelasan.
            Column(
                Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(40.dp))
                Text(
                    "Belum ada karya dari yang kamu ikuti",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Mulai dari creator berikut — feed tab ini akan langsung terisi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(18.dp))
                saranCreator.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            .clickable { onCreatorClick(c.creatorId) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppAvatar(url = c.photoUrl, name = c.displayName, size = 44.dp, verified = c.verified)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.displayName, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${c.followerCount} pengikut · ${c.city ?: "-"}",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                myUid?.let {
                                    viewModel.toggleFollow(
                                        creatorId = c.creatorId,
                                        userId = it,
                                        userName = authState.profile?.name.orEmpty(),
                                        userPhotoUrl = authState.profile?.photoUrl,
                                        creatorName = c.displayName,
                                        creatorPhotoUrl = c.photoUrl,
                                    )
                                }
                            },
                            shape = RoundedCornerShape(percent = 50),
                        ) { Text("Ikuti") }
                    }
                }
            }
        } else if (posts.isEmpty()) {
            Text(
                when {
                    activeTab == "Following" -> "Belum ada post dari creator yang kamu ikuti"
                    activeTab == "Nearby" && needsLocation -> "Aktifkan izin lokasi untuk melihat karya creator di sekitarmu"
                    activeTab == "Nearby" -> "Belum ada creator dengan karya di sekitarmu"
                    else -> "Belum ada konten di kategori ini"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            )
        } else {
            // Satu halaman ekstra di ujung: penanda "sudah lihat semua" berikut
            // tombol segarkan. Feed yang diam saat digulir sampai bawah terbaca
            // sebagai macet, bukan sebagai habis.
            val adaHalamanAkhir = habis
            val jumlahIklan = ((posts.size - 1) / 4).coerceAtLeast(0)
            val pagerState = rememberPagerState(pageCount = { posts.size + jumlahIklan + if (adaHalamanAkhir) 1 else 0 })

            LaunchedEffect(activeTab) {
                pagerState.scrollToPage(0)
            }

            // Muat lagi begitu tersisa tiga halaman — cukup jauh supaya
            // pengambilannya selesai sebelum pengguna sampai ke ujung.
            LaunchedEffect(pagerState.currentPage, posts.size, isLoading, habis) {
                if (!habis && !isLoading && pagerState.currentPage >= maxOf(0, pagerState.pageCount - 3)) {
                    viewModel.muatLagi()
                }
            }

            // Hitung tayangan untuk post yang benar-benar berhenti di layar.
            // Ditahan sebentar supaya menggulir cepat melewati banyak post tidak
            // ikut terhitung, dan diingat per sesi supaya bolak-balik ke post yang
            // sama tidak menggelembungkan angkanya.
            val viewedPosts = rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
                mutableStateListOf<String>()
            }
            LaunchedEffect(pagerState.currentPage, posts) {
                val currentPage = pagerState.currentPage
                if (currentPage >= 4 && currentPage % 5 == 4) return@LaunchedEffect
                val postIndex = currentPage - (currentPage / 5)
                val current = posts.getOrNull(postIndex) ?: return@LaunchedEffect
                if (current.postId in viewedPosts) return@LaunchedEffect
                kotlinx.coroutines.delay(1500)
                if (current.postId !in viewedPosts) {
                    viewedPosts.add(current.postId)
                    viewModel.registerView(current.postId)
                }
            }

            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 0,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val isEndPage = adaHalamanAkhir && page == pagerState.pageCount - 1
                val isAdPage = page >= 4 && page % 5 == 4
                if (isEndPage) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.CheckCircle, contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Kamu sudah lihat semua", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Kembali lagi nanti, atau segarkan untuk mencari yang baru.",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.segarkan() }, shape = RoundedCornerShape(percent = 50)) {
                            Text("Segarkan")
                        }
                    }
                    return@VerticalPager
                }
                if (isAdPage) {
                    AdMobNativeExplore(Modifier.fillMaxSize())
                    return@VerticalPager
                }
                val postIndex = page - (page / 5)
                val post = posts.getOrNull(postIndex) ?: return@VerticalPager
                // Pager mengomposisikan halaman tetangga. Listener interaksi
                // hanya dibutuhkan post aktif; halaman buffer memakai default
                // agar tiga listener Firestore per post tidak menumpuk.
                val isActive = pagerState.currentPage == page
                val liked by remember(post.postId, myUid, isActive) {
                    if (isActive && myUid != null) viewModel.isLiked(post.postId, myUid) else flowOf(false)
                }.collectAsState(initial = false)
                val saved by remember(post.postId, myUid, isActive) {
                    if (isActive && myUid != null) viewModel.isSaved(post.postId, myUid) else flowOf(false)
                }.collectAsState(initial = false)
                val following by remember(post.creatorId, myUid, isActive) {
                    if (isActive && myUid != null) viewModel.isFollowing(post.creatorId, myUid) else flowOf(false)
                }.collectAsState(initial = false)

                ExplorePostCard(
                    post = post,
                    currentUserId = myUid,
                    liked = liked,
                    saved = saved,
                    following = following,
                    // Hanya halaman yang sedang dilihat yang memutar video.
                    isActive = isActive,
                    onLike = { if (myUid == null) onLoginRequired() else viewModel.like(post.postId, myUid) },
                    onSave = { if (myUid == null) onLoginRequired() else viewModel.save(post.postId, myUid) },
                    onFollow = {
                        if (myUid == null) {
                            onLoginRequired()
                        } else {
                            viewModel.toggleFollow(
                                creatorId = post.creatorId,
                                userId = myUid,
                                userName = authState.profile?.name.orEmpty(),
                                userPhotoUrl = authState.profile?.photoUrl,
                                creatorName = post.creatorName,
                                creatorPhotoUrl = post.creatorPhotoUrl,
                            )
                        }
                    },
                    onReport = { reason ->
                        if (myUid == null) onLoginRequired() else viewModel.report(post.postId, myUid, reason)
                    },
                    onShare = {
                        // Share sungguhan lewat share sheet Android — sebelumnya
                        // tombol ini hanya menaikkan angka tanpa membagikan apa pun.
                        val link = "https://jepretaja.app/explore/${post.postId}"
                        val text = buildString {
                            if (post.caption.isNotBlank()) append(post.caption).append("\n\n")
                            append("Karya @${post.creatorName} di JepretAja\n")
                            append(link)
                        }
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            putExtra(Intent.EXTRA_SUBJECT, "Karya ${post.creatorName} di JepretAja")
                        }
                        runCatching { context.startActivity(Intent.createChooser(send, "Bagikan lewat")) }
                        viewModel.incrementShare(post.postId)
                    },
                    onCreatorClick = { onCreatorClick(post.creatorId) },
                    onCommentClick = { commentsPostId = post.postId },
                    onDeletePost = { myUid?.let { viewModel.deletePost(post.postId, it) } },
                    onEditPost = { editingPost = post },
                    onPackageClick = onPackageClick,
                    onNotInterested = { viewModel.tidakTertarik(post.postId) },
                    speed = kecepatan,
                    onSpeedChange = { viewModel.setKecepatan(it) },
                    onTagClick = { tag -> onTagClick(tag, false) },
                    onMentionClick = { creatorId -> onCreatorClick(creatorId) },
                    onCategoryClick = { kategori -> onTagClick(kategori, true) },
                    onSendToChat = if (myUid != null) ({ sharingPost = post }) else null,
                    onPin = if (myUid != null && myUid == post.creatorId) {
                        { viewModel.togglePin(myUid, post.postId) }
                    } else {
                        null
                    },
                    onCommentPolicy = if (myUid != null && myUid == post.creatorId) {
                        { policy -> viewModel.setCommentPolicy(post.postId, myUid, policy) }
                    } else {
                        null
                    },
                )
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp))

        // Tab kategori mengambang di atas (section 6.1)
        Row(
            Modifier.align(Alignment.TopStart).statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AppConstants.EXPLORE_TABS.forEach { tab ->
                val selected = tab == activeTab
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setTab(tab) },
                    label = {
                        Text(
                            when (tab) {
                                "Following" -> "Mengikuti"
                                "For You" -> "Untukmu"
                                "Nearby" -> "Terdekat"
                                else -> tab
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    shape = RoundedCornerShape(percent = 50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Black.copy(alpha = 0.35f),
                        labelColor = Color.White,
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                    ),
                )
            }
        }
        IconButton(
            onClick = onSearch,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 12.dp, top = 8.dp)
                .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(percent = 50)),
        ) {
            Icon(Icons.Default.Search, contentDescription = "Cari Explore", tint = Color.White)
        }
    }

    // Edit post milik sendiri: caption & kategori. Media tidak bisa diganti di
    // sini — mengganti berkas berarti unggah baru, bukan sunting.
    editingPost?.let { post ->
        var caption by remember(post.postId) { mutableStateOf(post.caption) }
        var category by remember(post.postId) { mutableStateOf(post.category) }
        var categoryMenuOpen by remember(post.postId) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingPost = null },
            title = { Text("Edit Post") },
            text = {
                Column {
                    OutlinedTextField(
                        value = caption, onValueChange = { caption = it },
                        label = { Text("Caption") }, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    ExposedDropdownMenuBox(expanded = categoryMenuOpen, onExpandedChange = { categoryMenuOpen = it }) {
                        OutlinedTextField(
                            value = category, onValueChange = {}, readOnly = true,
                            label = { Text("Kategori") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                        )
                        ExposedDropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                            AppConstants.SERVICE_CATEGORIES.forEach { c ->
                                DropdownMenuItem(text = { Text(c) }, onClick = { category = c; categoryMenuOpen = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    myUid?.let { viewModel.updatePost(post.postId, it, caption.trim(), category) }
                    editingPost = null
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { editingPost = null }) { Text("Batal") } },
        )
    }

    // Kirim post ke percakapan yang sudah ada. Yang dikirim adalah tautan
    // beserta caption singkatnya, bukan salinan medianya — media tetap tinggal
    // di post aslinya supaya penghitung tayangan dan tombol booking ikut terbawa.
    sharingPost?.let { post ->
        val uid = myUid
        if (uid == null) {
            sharingPost = null
        } else {
            SendToChatSheet(
                myUserId = uid,
                pesan = buildString {
                    if (post.caption.isNotBlank()) append(post.caption.take(120)).append("\n\n")
                    append("Lihat karya ").append(post.creatorName).append(": ")
                    append("https://jepretaja.app/explore/").append(post.postId)
                },
                onDismiss = { sharingPost = null },
            )
        }
    }

    commentsPostId?.let { postId ->
        ExploreCommentsSheet(
            postId = postId,
            authViewModel = authViewModel,
            onLoginRequired = onLoginRequired,
            onDismiss = { commentsPostId = null },
        )
    }
}

@Composable
private fun ExploreFeedSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                )
            }
        }
    }
}
