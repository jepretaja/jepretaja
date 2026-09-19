package com.jepretaja.app.ui.screens.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.util.Formatters
import com.jepretaja.app.data.model.ExplorePostModel
import com.jepretaja.app.ui.components.AppAvatar
import com.jepretaja.app.ui.components.CaptionText
import com.jepretaja.app.ui.components.VideoPlayer
import androidx.compose.ui.platform.LocalContext

/** Satu post di feed Explore fullscreen. Interaksi (section 6.2): double
 * tap like, tap avatar, comment, save, share, follow, booking, report.
 *
 * Tetap sinematik full-bleed hitam secara sengaja (feed foto/video ala
 * Instagram/TikTok) — polesan di sini terbatas pada bentuk tombol (pill),
 * scrim yang lebih halus bertingkat, dan avatar dengan fallback inisial. */
/** Label manusiawi untuk nilai commentPolicy yang disimpan di Firestore. */
private fun labelKebijakanKomentar(policy: String): String = when (policy) {
    "followers" -> "Hanya pengikut"
    "off" -> "Komentar dimatikan"
    else -> "Semua orang"
}

@Composable
fun ExplorePostCard(
    post: ExplorePostModel,
    currentUserId: String?,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onFollow: () -> Unit,
    onReport: (String) -> Unit,
    onShare: () -> Unit,
    onCreatorClick: () -> Unit,
    onCommentClick: () -> Unit,
    liked: Boolean = false,
    saved: Boolean = false,
    following: Boolean = false,
    isActive: Boolean = true,
    onDeletePost: (() -> Unit)? = null,
    onEditPost: (() -> Unit)? = null,
    onTagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onSendToChat: (() -> Unit)? = null,
    onCommentPolicy: ((String) -> Unit)? = null,
    onPin: (() -> Unit)? = null,
    isPinned: Boolean = false,
    onPackageClick: (String) -> Unit,
    onNotInterested: (() -> Unit)? = null,
    speed: Float = 1f,
    onSpeedChange: (Float) -> Unit = {},
) {
    val context = LocalContext.current
    var showHeart by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCommentPolicy by remember { mutableStateOf(false) }
    var showLongPress by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var showPlayState by remember { mutableStateOf(false) }
    val isOwner = currentUserId != null && currentUserId == post.creatorId

    Box(Modifier.fillMaxSize()) {
        val mediaModifier = Modifier.fillMaxSize().pointerInput(post.postId) {
            detectTapGestures(
                onDoubleTap = {
                    onLike()
                    showHeart = true
                },
                // Satu ketukan menjeda/melanjutkan video; suara punya tombol
                // sendiri supaya kontrolnya mudah ditemukan seperti feed video.
                onTap = {
                    if (post.type == "video") {
                        paused = !paused
                        showPlayState = true
                    }
                },
                onLongPress = { showLongPress = true },
            )
        }

        if (post.type == "video") {
            val videoUrl = post.mediaUrls.firstOrNull()
            if (videoUrl != null) {
                Box(mediaModifier) {
                    VideoPlayer(
                        url = videoUrl,
                        thumbnailUrl = post.thumbnailUrl,
                        playWhenActive = isActive && !paused,
                        muted = muted,
                        // Bar posisi hanya untuk video yang sedang ditonton:
                        // menggambarnya pada halaman tetangga yang sedang di-
                        // buffer hanya membuang siklus tanpa ada yang melihat.
                        showScrubber = isActive,
                        speed = speed,
                        releaseWhenInactive = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else if (post.mediaUrls.size > 1) {
            // Post berisi beberapa foto digeser mendatar, bukan hanya
            // menampilkan foto pertama dan diam-diam menyembunyikan sisanya.
            val statePager = rememberPagerState(pageCount = { post.mediaUrls.size })
            Box(mediaModifier) {
                HorizontalPager(state = statePager, modifier = Modifier.fillMaxSize()) { halaman ->
                    SubcomposeAsyncImage(
                        model = remember(post.mediaUrls[halaman]) {
                            ImageRequest.Builder(context).data(post.mediaUrls[halaman]).size(1080).build()
                        },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = { MediaLoading() },
                        error = { MediaError() },
                    )
                }
                Row(
                    Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(post.mediaUrls.size) { index ->
                        Box(
                            Modifier.size(6.dp).clip(CircleShape)
                                .background(
                                    if (index == statePager.currentPage) Color.White
                                    else Color.White.copy(alpha = 0.4f)
                                ),
                        )
                    }
                }
                Text(
                    "${statePager.currentPage + 1}/${post.mediaUrls.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 16.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                )
            }
        } else {
            SubcomposeAsyncImage(
                model = remember(post.thumbnailUrl, post.mediaUrls) {
                    ImageRequest.Builder(context)
                        .data(post.thumbnailUrl ?: post.mediaUrls.firstOrNull())
                        .size(1080)
                        .build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = mediaModifier,
                loading = { MediaLoading() },
                error = { MediaError() },
            )
        }

        if (post.type == "video") {
            Icon(
                if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (muted) "Nyalakan suara" else "Matikan suara",
                tint = Color.White,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()
                    .padding(top = 56.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
                    .clickable { muted = !muted }
                    .padding(8.dp).size(20.dp),
            )
            AnimatedVisibility(visible = showPlayState, modifier = Modifier.align(Alignment.Center)) {
                Icon(
                    if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (paused) "Putar video" else "Jeda video",
                    tint = Color.White,
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .padding(16.dp),
                )
                LaunchedEffect(paused, showPlayState) {
                    kotlinx.coroutines.delay(650)
                    showPlayState = false
                }
            }
        }

        AnimatedVisibility(visible = showHeart, modifier = Modifier.align(Alignment.Center)) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(100.dp))
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(500); showHeart = false }
        }

        // Scrim bertingkat (3 stop) — transisi ke gelap lebih halus daripada
        // gradient 2-stop sebelumnya, supaya teks tetap kontras tanpa terasa
        // seperti kotak hitam solid menempel di bawah.
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
                ),
        )

        // Booking ditempatkan paling awal agar harga dan aksi utama terlihat
        // sebelum identitas creator, seperti pola commerce pada feed video.
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 84.dp, bottom = 22.dp),
        ) {
            post.packageId?.let { idPaket ->
                Row(
                    Modifier.padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(AppColors.Primary)
                        .clickable { onPackageClick(idPaket) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.CalendarMonth, contentDescription = null,
                        tint = AppColors.OnPrimary, modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        buildString {
                            append(post.packageName ?: "Paket ini")
                            post.packagePrice?.let { append(" · ").append(Formatters.currency(it)) }
                        },
                        color = AppColors.OnPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { post.packageId?.let(onPackageClick) ?: onCreatorClick() },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                contentPadding = PaddingValues(horizontal = 18.dp),
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier.height(36.dp),
            ) {
                Text(
                    if (post.packagePrice != null) "Booking · ${Formatters.currency(post.packagePrice)}" else "Lihat & Booking",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.combinedClickable(onClick = onCreatorClick)) {
                Text("@${post.creatorName}", color = Color.White, fontWeight = FontWeight.W700)
                if (post.creatorVerified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            val bagianJudul = post.caption.substringBefore("\n\n").trim()
            val bagianDeskripsi = post.caption.substringAfter("\n\n", missingDelimiterValue = post.caption).trim()
            if (post.caption.contains("\n\n") && bagianJudul.isNotBlank()) {
                Text(
                    bagianJudul,
                    color = Color.White,
                    fontWeight = FontWeight.W800,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            CaptionText(
                caption = bagianDeskripsi,
                mentions = post.mentions.mapNotNull { m ->
                    val nama = m["name"]; val id = m["creatorId"]
                    if (nama != null && id != null) nama to id else null
                }.toMap(),
                onTagClick = onTagClick,
                onMentionClick = onMentionClick,
                maxLines = 2,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(percent = 50))
                        .background(Color.White.copy(alpha = 0.24f))
                        .clickable { onCategoryClick(post.category) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(post.category, color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                post.location?.let {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                    Text(it, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = "Audio asli", tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Audio asli · ${post.creatorName.ifBlank { "JepretAja" }}",
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }

        // Action rail kanan
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Avatar creator ala TikTok: lingkaran dengan lencana "+" mengambang
            // di bawahnya untuk follow cepat — SEBELUMNYA follow hanya berupa
            // tombol pill di samping username, terpisah dari action rail dan
            // tidak sesuai pola yang diminta (ala TikTok, avatar+follow selalu
            // di puncak rail kanan). Lencana hilang begitu sudah mengikuti.
            if (!isOwner) {
                Box(
                    Modifier.padding(bottom = 22.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCreatorClick,
                            )
                            .padding(2.dp),
                    ) {
                        AppAvatar(url = post.creatorPhotoUrl, name = post.creatorName, size = 42.dp)
                    }
                    if (!following) {
                        Box(
                            Modifier
                                .size(19.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF5350))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onFollow,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Ikuti ${post.creatorName}", tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
            // Ikon sekarang mencerminkan keadaan nyata: hati terisi merah kalau
            // sudah disukai, bookmark terisi kuning kalau sudah disimpan.
            // Sebelumnya keduanya selalu tampak sama, jadi pengguna tidak punya
            // cara tahu apakah ketukannya tadi berhasil.
            ExploreActionButton(
                if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "${post.likeCount}",
                if (liked) Color(0xFFEF5350) else Color.White,
                onLike,
                active = liked,
            )
            Spacer(Modifier.height(16.dp))
            ExploreActionButton(Icons.Default.ModeComment, "${post.commentCount}", Color.White, onCommentClick)
            // Tombol simpan hilang sepenuhnya kalau creator mematikannya —
            // menampilkan tombol yang pasti ditolak server hanya membuat
            // ketukan terasa seperti kerusakan.
            if (post.allowSave) {
                Spacer(Modifier.height(16.dp))
                ExploreActionButton(
                    if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    "${post.saveCount}",
                    if (saved) AppColors.Accent else Color.White,
                    onSave,
                    active = saved,
                )
            }
            Spacer(Modifier.height(16.dp))
            ExploreActionButton(Icons.Default.Share, "${post.shareCount}", Color.White, onShare)
            Spacer(Modifier.height(16.dp))
            ExploreActionButton(Icons.Default.MoreHoriz, null, Color.White, onClick = { showReportSheet = true })
            Spacer(Modifier.height(18.dp))
            // Piringan berputar ala TikTok — penanda audio yang jalan
            // sekaligus penutup rail yang jadi terasa "lengkap". Berhenti
            // berputar begitu videonya sendiri dijeda/tidak aktif, bukan
            // berputar terus tanpa peduli status pemutaran.
            SpinningAudioDisc(imageUrl = post.creatorPhotoUrl, spinning = isActive && !paused)
        }
    }

    if (showReportSheet) {
        ModalBottomSheet(onDismissRequest = { showReportSheet = false }) {
            // Pemilik post melihat aksi kelola (ubah/hapus) — sebelumnya tidak
            // ada jalan sama sekali untuk menghapus atau mengedit post sendiri
            // setelah diunggah.
            if (isOwner) {
                if (onEditPost != null) {
                    ListItem(
                        headlineContent = { Text("Edit caption & kategori") },
                        leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                        modifier = Modifier.combinedClickable(onClick = { showReportSheet = false; onEditPost() }),
                    )
                }
                if (onPin != null) {
                    ListItem(
                        headlineContent = { Text(if (isPinned) "Lepas sematan" else "Sematkan di profil") },
                        supportingContent = { Text("Karya yang disematkan tampil paling depan di grid profilmu") },
                        leadingContent = { Icon(Icons.Default.PushPin, contentDescription = null) },
                        modifier = Modifier.combinedClickable(onClick = { showReportSheet = false; onPin() }),
                    )
                }
                if (onCommentPolicy != null) {
                    ListItem(
                        headlineContent = { Text("Siapa yang boleh komentar") },
                        supportingContent = { Text(labelKebijakanKomentar(post.commentPolicy)) },
                        leadingContent = { Icon(Icons.Default.ModeComment, contentDescription = null) },
                        modifier = Modifier.combinedClickable(onClick = { showReportSheet = false; showCommentPolicy = true }),
                    )
                }
                if (onDeletePost != null) {
                    ListItem(
                        headlineContent = { Text("Hapus post", color = AppColors.Danger) },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = AppColors.Danger) },
                        modifier = Modifier.combinedClickable(onClick = { showReportSheet = false; showDeleteConfirm = true }),
                    )
                }
            }
            if (onSendToChat != null) {
                ListItem(
                    headlineContent = { Text("Kirim ke chat") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                    modifier = Modifier.combinedClickable(onClick = { showReportSheet = false; onSendToChat() }),
                )
            }
            if (!isOwner) {
                ListItem(
                    headlineContent = { Text("Laporkan konten") },
                    leadingContent = { Icon(Icons.Default.Flag, contentDescription = null) },
                    modifier = Modifier.combinedClickable(onClick = { showReportSheet = false; onReport("inappropriate_content") }),
                )
                ListItem(
                    headlineContent = { Text("Tidak tertarik") },
                    leadingContent = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                    modifier = Modifier.combinedClickable(onClick = {
                        showReportSheet = false
                        onNotInterested?.invoke()
                    }),
                )
            }
        }
    }

    if (showLongPress) {
        ModalBottomSheet(onDismissRequest = { showLongPress = false }, containerColor = AppColors.Surface) {
            if (onNotInterested != null) {
                ListItem(
                    headlineContent = { Text("Tidak tertarik") },
                    supportingContent = { Text("Karya serupa akan lebih jarang muncul di For You") },
                    leadingContent = { Icon(Icons.Default.ThumbDown, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = AppColors.Surface),
                    modifier = Modifier.combinedClickable(onClick = { showLongPress = false; onNotInterested() }),
                )
            }
            ListItem(
                headlineContent = { Text(if (saved) "Hapus dari simpanan" else "Simpan karya") },
                leadingContent = {
                    Icon(if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = null)
                },
                colors = ListItemDefaults.colors(containerColor = AppColors.Surface),
                modifier = Modifier.combinedClickable(onClick = { showLongPress = false; onSave() }),
            )
            if (post.type == "video") {
                Text(
                    "Kecepatan putar",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(0.5f, 1f, 1.5f, 2f).forEach { nilai ->
                        FilterChip(
                            selected = speed == nilai,
                            onClick = { onSpeedChange(nilai) },
                            label = { Text(if (nilai == 1f) "Normal" else "${nilai}x") },
                        )
                    }
                }
            }
            ListItem(
                headlineContent = { Text("Laporkan") },
                leadingContent = { Icon(Icons.Default.Flag, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = AppColors.Surface),
                modifier = Modifier.combinedClickable(onClick = { showLongPress = false; showReportSheet = true }),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCommentPolicy && onCommentPolicy != null) {
        AlertDialog(
            onDismissRequest = { showCommentPolicy = false },
            title = { Text("Siapa yang boleh komentar") },
            text = {
                Column {
                    listOf("all", "followers", "off").forEach { pilihan ->
                        Row(
                            Modifier.fillMaxWidth().combinedClickable(onClick = {
                                onCommentPolicy(pilihan)
                                showCommentPolicy = false
                            }).padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = post.commentPolicy == pilihan, onClick = null)
                            Spacer(Modifier.width(10.dp))
                            Text(labelKebijakanKomentar(pilihan))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCommentPolicy = false }) { Text("Tutup") }
            },
        )
    }

    // Menghapus post tidak bisa dibatalkan, jadi selalu lewat konfirmasi.
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus post ini?") },
            text = { Text("Post akan dihapus permanen dan tidak bisa dikembalikan.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDeletePost?.invoke() }) {
                    Text("Hapus", color = AppColors.Danger)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun MediaLoading() {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun MediaError() {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)), contentAlignment = Alignment.Center) {
        Text("Media tidak dapat dimuat", color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

/** Piringan hitam berputar ala TikTok di ujung bawah action rail — penanda
 * visual bahwa audio sedang berjalan. Berputar pelan & konstan (linear,
 * bukan easing) supaya terasa seperti piringan sungguhan, bukan animasi
 * yang tersentak di awal/akhir putaran. */
@Composable
private fun SpinningAudioDisc(imageUrl: String?, spinning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "discRotation")
    val rotasi by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "discRotationValue",
    )
    Box(
        Modifier
            .size(34.dp)
            .rotate(if (spinning) rotasi else 0f)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(Color(0xFF1A1A1A)))
        AppAvatar(url = imageUrl, name = "", size = 16.dp)
    }
}

/** Ikon aksi dengan bantalan lingkaran semi-transparan — memberi kontras
 * lembut di atas foto apa pun (sebelumnya ikon polos langsung di atas foto,
 * kadang tenggelam di foto terang). */
@Composable
private fun ExploreActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String?,
    tint: Color,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    // Sedikit "memantul" saat status berubah aktif — umpan balik sentuhan yang
    // membuat aksi terasa hidup, bukan sekadar ikon berganti warna diam-diam.
    val scale by animateFloatAsState(
        targetValue = if (active) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "actionScale",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, contentDescription = null, tint = tint,
                modifier = Modifier.size(24.dp).graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
        label?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}
