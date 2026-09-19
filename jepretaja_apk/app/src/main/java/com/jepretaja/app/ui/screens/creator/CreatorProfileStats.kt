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

private val LOKAL_ID = Locale("in", "ID")
private val HARI_PENDEK = DateTimeFormatter.ofPattern("EEE", LOKAL_ID)


/* --------------------------------------------------------------------------
 * Kartu angka
 * ----------------------------------------------------------------------- */

/**
 * Empat angka penentu keputusan + harga mulai.
 *
 * Harga diberi barisnya sendiri di bawah pemisah, bukan diperlakukan sebagai
 * angka kelima. Harga adalah satu-satunya angka di kartu ini yang menjawab
 * "mampu tidak saya", dan menyusunnya sejajar dengan jumlah pengikut membuatnya
 * hilang di antara angka-angka yang lebih kecil urusannya.
 */
@Composable
internal fun KartuAngka(
    creator: CreatorModel,
    mengikutiJumlah: Int,
    onUlasan: () -> Unit,
    onPengikut: () -> Unit,
    onMengikuti: () -> Unit,
    onLihatPaket: () -> Unit,
) {
    PremiumCard(modifier = Modifier.fillMaxWidth(), elevation = 4.dp, contentPadding = PaddingValues(0.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AngkaProfil(
                nilai = if (creator.reviewCount == 0) "Baru" else "%.1f".format(creator.rating),
                label = if (creator.reviewCount == 0) "belum ada ulasan" else "${creator.reviewCount} ulasan",
                ikon = Icons.Default.Star,
                warnaIkon = AppColors.Warning,
                onClick = if (creator.reviewCount > 0) onUlasan else null,
            )
            AngkaProfil(
                nilai = "${creator.completedBookings}",
                label = "proyek selesai",
                ikon = Icons.Default.CameraAlt,
            )
            AngkaProfil(
                nilai = Formatters.compactCount(creator.followerCount.toLong()),
                label = "pengikut",
                onClick = onPengikut,
            )
            AngkaProfil(
                nilai = Formatters.compactCount(mengikutiJumlah.toLong()),
                label = "mengikuti",
                onClick = onMengikuti,
            )
        }

        HorizontalDivider(color = AppColors.Border)

        Row(
            Modifier.fillMaxWidth().clickable(onClick = onLihatPaket).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Mulai dari", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
                Text(
                    creator.minPrice?.let { Formatters.currency(it) } ?: "Harga belum diatur",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.W700,
                    color = AppColors.TextPrimary,
                )
            }
            Text("Lihat paket", style = MaterialTheme.typography.labelLarge, color = AppColors.Primary)
            Icon(
                Icons.Default.ChevronRight, contentDescription = null,
                tint = AppColors.Primary, modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Satu angka.
 *
 * [onClick] boleh null, dan itu penting: yang tidak menuju ke mana pun tidak
 * dipasangi `clickable` sama sekali, sehingga tidak memberi riak sentuhan yang
 * menjanjikan sesuatu yang tidak akan terjadi.
 */
@Composable
private fun AngkaProfil(
    nilai: String,
    label: String,
    ikon: ImageVector? = null,
    warnaIkon: Color = AppColors.TextSecondary,
    onClick: (() -> Unit)? = null,
) {
    val dasar = Modifier.widthIn(min = 64.dp).padding(horizontal = 4.dp, vertical = 4.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick).then(dasar) else dasar,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ikon != null) {
                Icon(ikon, contentDescription = null, tint = warnaIkon, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(3.dp))
            }
            Text(
                nilai,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W700,
                color = AppColors.TextPrimary,
                maxLines = 1,
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* --------------------------------------------------------------------------
 * Ketersediaan
 * ----------------------------------------------------------------------- */

/**
 * Ketersediaan 14 hari ke depan.
 *
 * Datanya sudah lama ada di availability_blocks tapi hanya terlihat oleh
 * creator sendiri — padahal justru calon pelangganlah yang perlu tahu tanggal
 * mana yang sudah penuh. Sebelumnya ia tampil sebagai empat tanggal yang
 * dipisah koma, yang memaksa pengguna mencocokkan sendiri dengan tanggal
 * acaranya. Deretan hari membuat jawabannya bisa dibaca sekilas.
 */
@Composable
internal fun KartuKetersediaan(
    creator: CreatorModel,
    tanggalPenuh: List<LocalDate>,
    onTanya: () -> Unit,
) {
    val hariIni = remember { LocalDate.now() }
    val penuh = remember(tanggalPenuh) { tanggalPenuh.toSet() }
    val duaMinggu = remember(hariIni) { (0L until 14L).map { hariIni.plusDays(it) } }
    val jumlahKosong = duaMinggu.count { it !in penuh }

    PremiumCard(modifier = Modifier.fillMaxWidth(), elevation = 3.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.CalendarMonth, contentDescription = null,
                tint = AppColors.Primary, modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Ketersediaan", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                Text(
                    if (creator.acceptingBookings) {
                        "$jumlahKosong dari 14 hari ke depan masih kosong"
                    } else {
                        "Sedang tidak menerima booking baru"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
            }
        }

        // Jam kerja yang diisi creator di Studio. Tanpa ini, pelanggan menebak
        // sendiri dan sering mengajukan jam yang memang tidak bisa diambil —
        // penolakan yang bisa dihindari hanya dengan satu baris keterangan.
        creator.workingHours?.let { jam ->
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule, contentDescription = null,
                    tint = AppColors.TextSecondary, modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Jam kerja ${jam.start}–${jam.end}" + (jam.note?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            duaMinggu.forEach { tanggal ->
                val terisi = tanggal in penuh || !creator.acceptingBookings
                Column(
                    Modifier
                        .width(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (terisi) AppColors.Danger.copy(alpha = 0.10f) else AppColors.Success.copy(alpha = 0.12f)
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        tanggal.format(HARI_PENDEK),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${tanggal.dayOfMonth}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.W700,
                        color = if (terisi) AppColors.Danger else AppColors.Success,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            KeteranganWarna(AppColors.Success, "Kosong")
            Spacer(Modifier.width(12.dp))
            KeteranganWarna(AppColors.Danger, "Penuh")
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onTanya) { Text("Tanya tanggal lain") }
        }
    }
}

@Composable
private fun KeteranganWarna(warna: Color, teks: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(warna))
        Spacer(Modifier.width(5.dp))
        Text(teks, style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
    }
}
