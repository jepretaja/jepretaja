package com.jepretaja.app.ui.screens.creator

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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/* --------------------------------------------------------------------------
 * Ulasan
 * ----------------------------------------------------------------------- */

/**
 * Bagian ulasan: ringkasan rating, sebaran bintang, lalu dua ulasan terbaru.
 *
 * Sebaran bintangnya bukan hiasan. Rata-rata 4,6 bisa berarti "hampir semua
 * memberi 5" atau "banyak 5 dengan beberapa 1", dan dua kemungkinan itu adalah
 * dua creator yang sangat berbeda bagi orang yang sedang mempertaruhkan hari
 * pernikahannya. Satu angka rata-rata tidak pernah bisa membedakan keduanya.
 */
@Composable
internal fun BagianUlasan(
    creator: CreatorModel,
    ulasan: List<ReviewModel>?,
    onLihatSemua: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Ulasan",
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (!ulasan.isNullOrEmpty()) {
            TextButton(onClick = onLihatSemua) { Text("Lihat semua ${ulasan.size}") }
        }
    }
    Spacer(Modifier.height(4.dp))

    when {
        ulasan == null -> {
            SkeletonBox(Modifier.fillMaxWidth().height(96.dp), RoundedCornerShape(18.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.fillMaxWidth().height(64.dp), RoundedCornerShape(18.dp))
        }

        ulasan.isEmpty() -> PremiumCard(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.RateReview, contentDescription = null,
                    tint = AppColors.TextSecondary, modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Belum ada ulasan", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                    Text(
                        // Creator baru tidak pantas terbaca seperti creator
                        // buruk. Kalimat ini menyatakan keadaannya apa adanya
                        // tanpa menyiratkan penilaian.
                        "Creator ini belum pernah diulas. Kamu bisa jadi yang pertama.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                    )
                }
            }
        }

        else -> {
            RingkasanRating(creator = creator, ulasan = ulasan)
            Spacer(Modifier.height(10.dp))
            ulasan.take(2).forEach { r -> KartuUlasan(r) }
        }
    }
}

@Composable
private fun RingkasanRating(creator: CreatorModel, ulasan: List<ReviewModel>) {
    // Dihitung dari daftar ulasan yang sudah di tangan, bukan dari field
    // agregat: rata-rata di dokumen creator dan daftar ulasan yang tayang bisa
    // berselisih kalau ada ulasan yang disembunyikan moderator, dan yang harus
    // cocok dengan bintang-bintang di bawahnya adalah yang terlihat.
    val rerata = remember(ulasan) { ulasan.map { it.rating.toInt() }.average() }
    val sebaran = remember(ulasan) {
        (5 downTo 1).map { bintang -> bintang to ulasan.count { it.rating.toInt() == bintang } }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth(), elevation = 3.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(92.dp)) {
                Text(
                    "%.1f".format(rerata),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.W700,
                    color = AppColors.TextPrimary,
                )
                Row {
                    repeat(5) { i ->
                        Icon(
                            if (i < rerata.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (i < rerata.toInt()) AppColors.Warning else AppColors.Border,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${ulasan.size} ulasan",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                sebaran.forEach { (bintang, jumlah) ->
                    BarisSebaran(bintang = bintang, jumlah = jumlah, total = ulasan.size)
                }
            }
        }
        if (creator.completedBookings > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Dari ${creator.completedBookings} proyek yang sudah selesai",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun BarisSebaran(bintang: Int, jumlah: Int, total: Int) {
    val rasio = if (total == 0) 0f else jumlah.toFloat() / total
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
        Text(
            "$bintang",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
            modifier = Modifier.width(10.dp),
        )
        Icon(
            Icons.Default.Star, contentDescription = null,
            tint = AppColors.Border, modifier = Modifier.size(10.dp),
        )
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(percent = 50))
                .background(AppColors.SurfaceVariant),
        ) {
            if (rasio > 0f) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(rasio)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(AppColors.Warning),
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            "$jumlah",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
            modifier = Modifier.width(22.dp),
        )
    }
}

/** Foto profil ukuran penuh. */
@Composable
internal fun PratinjauFotoProfil(url: String?, nama: String, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f))) {
            AsyncImage(
                model = url,
                contentDescription = "Foto profil $nama",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(24.dp),
            )
            IconButton(onClick = onTutup, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
            }
        }
    }
}

@Composable
private fun KartuUlasan(r: ReviewModel) {
    PremiumCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), elevation = 2.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppAvatar(url = null, name = r.customerName, size = 28.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    r.customerName.ifBlank { "Pelanggan" },
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Lima bintang terbaca lebih cepat daripada angka "4": bentuknya
                // langsung memberi tahu seberapa jauh dari penuh tanpa perlu
                // mengingat bahwa skalanya sampai lima.
                Row {
                    repeat(5) { i ->
                        Icon(
                            if (i < r.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (i < r.rating) AppColors.Warning else AppColors.Border,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
        if (r.text.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                r.text,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
