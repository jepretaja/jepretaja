package com.jepretaja.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

/**
 * Pemutar video feed Explore.
 *
 * `media3-exoplayer` sudah lama ada di daftar dependensi tapi tidak pernah
 * dipakai satu file pun: post bertipe "video" hanya menampilkan `thumbnailUrl`
 * sebagai gambar diam, sehingga video yang sudah diunggah creator praktis tidak
 * bisa ditonton siapa pun. Komponen ini yang membuatnya benar-benar berjalan.
 *
 * Perilakunya mengikuti kebiasaan feed vertikal: hanya post yang sedang aktif
 * di layar yang diputar ([playWhenActive]), sisanya di-pause supaya tidak ada
 * banyak video berbunyi bersamaan dan baterai/kuota tidak terbuang. Player juga
 * dilepas saat komposisi berakhir dan di-pause saat aplikasi masuk latar.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    playWhenActive: Boolean = true,
    thumbnailUrl: String? = null,
    muted: Boolean = false,
    /** Menampilkan bar posisi yang bisa digeser. */
    showScrubber: Boolean = false,
    speed: Float = 1f,
    /** Feed pager tidak perlu membuat decoder untuk halaman yang belum aktif. */
    releaseWhenInactive: Boolean = false,
) {
    if (url.isBlank()) {
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text("Video tidak tersedia", color = Color.White)
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Dulu di sini ada jalan pintas: kalau halaman belum aktif, ExoPlayer
    // sama sekali tidak dibuat — cuma thumbnail. Kedengarannya hemat, tapi
    // efeknya justru sebaliknya: begitu pengguna menggeser ke halaman itu,
    // ExoPlayer BARU dibangun dan di-prepare() persis di frame yang sama
    // dengan animasi geser, dan itu kerja berat di thread utama — pas terasa
    // "ngelet"/patah setiap kali pindah video.
    //
    // Sekarang player selalu dibuat begitu Composable ini hadir di
    // komposisi. VerticalPager di ExploreScreen dipasang dengan
    // beyondViewportPageCount = 1, jadi paling banyak cuma halaman
    // tetangga (±1) yang ikut kebuat playernya lebih awal — video-nya
    // sudah selesai di-buffer sebelum pengguna sampai di sana, playWhenReady
    // tetap false sampai halamannya benar-benar aktif. Begitu pager
    // menjauh dari jendela ±1 itu, Compose otomatis membongkar Composable
    // ini dan DisposableEffect di bawah yang melepas playernya.
    val player = remember(url) {
        ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(1500, 3000, 1500, 1500)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build(),
            )
            .build()
            .apply {
                setAudioAttributes(AudioAttributes.DEFAULT, false)
                setMediaItem(MediaItem.fromUri(url))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = if (muted) 0f else 1f
                prepare()
            }
    }

    // Hanya post yang sedang dilihat yang berputar.
    LaunchedEffect(playWhenActive) {
        player.playWhenReady = playWhenActive
        if (!playWhenActive) player.seekTo(0)
    }

    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    LaunchedEffect(speed) { player.setPlaybackSpeed(speed) }

    // Posisi diambil dengan polling 300ms, bukan lewat listener: ExoPlayer tidak
    // memberi peristiwa untuk setiap perubahan posisi, dan 300ms sudah cukup
    // halus untuk bar setipis ini sambil tetap ringan.
    var posisiMs by remember(url) { mutableLongStateOf(0L) }
    var durasiMs by remember(url) { mutableLongStateOf(0L) }
    var sedangGeser by remember(url) { mutableStateOf(false) }
    LaunchedEffect(player, showScrubber) {
        if (!showScrubber) return@LaunchedEffect
        while (true) {
            if (!sedangGeser) {
                posisiMs = player.currentPosition
                durasiMs = player.duration.coerceAtLeast(0L)
            }
            kotlinx.coroutines.delay(300)
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.playWhenReady = false
                Lifecycle.Event.ON_RESUME -> player.playWhenReady = playWhenActive
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    Box(modifier) {
        // Thumbnail tetap digambar di belakang sebagai poster: frame pertama
        // ExoPlayer baru muncul setelah buffering, tanpa ini layar berkedip hitam.
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Bar tipis nempel di tepi layar, ala TikTok — SEBELUMNYA memakai
        // Material3 Slider dengan thumb bulat besar + padding 12dp di kiri-
        // kanan, jadinya kelihatan seperti kontrol pemutar video biasa (mirip
        // YouTube), bukan indikator progres feed vertikal yang seharusnya
        // nyaris tak terlihat sampai disentuh. Di sini: tinggi 2.5dp, full
        // width, tanpa thumb kecuali sedang diseret.
        if (showScrubber && durasiMs > 0) {
            val progres = (posisiMs.toFloat() / durasiMs.toFloat()).coerceIn(0f, 1f)
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(if (sedangGeser) 5.dp else 2.5.dp)
                    .pointerInput(durasiMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { sedangGeser = true },
                            onDragEnd = {
                                player.seekTo(posisiMs)
                                sedangGeser = false
                            },
                            onDragCancel = { sedangGeser = false },
                        ) { change, _ ->
                            val fraksi = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            posisiMs = (fraksi * durasiMs).toLong()
                        }
                    },
            ) {
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.28f)))
                Box(
                    Modifier
                        .fillMaxWidth(progres)
                        .fillMaxSize()
                        .background(Color.White),
                )
            }
        }
    }
}
