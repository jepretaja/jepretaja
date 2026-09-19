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
import com.jepretaja.app.ui.copy.AppMode
import com.jepretaja.app.ui.copy.LocalJepretAjaCopy

@Composable
internal fun AksiProfilCreator(
    creator: CreatorModel,
    onBooking: () -> Unit,
    onChat: () -> Unit,
) {
    val libur = !creator.acceptingBookings
    val copy = LocalJepretAjaCopy.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onChat,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(percent = 50),
        ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                when (copy.mode) {
                    AppMode.GUEST -> "Tanya creator"
                    AppMode.CUSTOMER -> "Chat creator"
                    AppMode.CREATOR -> "Hubungi creator"
                },
            )
        }
        Button(
            onClick = onBooking,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(percent = 50),
            enabled = !libur || creator.minPrice != null,
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    libur -> "Lihat paket"
                    copy.mode == AppMode.GUEST -> "Lihat paket"
                    copy.mode == AppMode.CREATOR -> "Ajukan kolaborasi"
                    else -> "Booking sesi"
                },
            )
        }
    }
}

/* --------------------------------------------------------------------------
 * Kepala
 * ----------------------------------------------------------------------- */

/**
 * Sampul besar + avatar yang menimpanya.
 *
 * Avatar diberi cincin warna latar selebar 4dp supaya tetap terpisah dari foto
 * sampul apa pun warnanya — tanpa cincin, avatar bertepi gelap di atas sampul
 * gelap kehilangan bentuknya sama sekali.
 */
@Composable
internal fun KepalaProfil(c: CreatorModel, onFotoClick: () -> Unit) {
    val tinggiSampul = 220.dp
    val ukuranAvatar = 108.dp

    Box(Modifier.fillMaxWidth().height(tinggiSampul + ukuranAvatar / 2)) {
        Box(
            Modifier.fillMaxWidth().height(tinggiSampul)
                .background(Brush.linearGradient(listOf(AppColors.Primary, AppColors.PrimaryDark))),
        ) {
            if (!c.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = c.coverUrl,
                    contentDescription = "Sampul ${c.displayName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Scrim bawah: menjaga tepi sampul menyatu ke latar halaman, sekaligus
            // memberi kontras di belakang avatar.
            Box(
                Modifier.fillMaxWidth().height(90.dp).align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, AppColors.Background))
                    ),
            )
        }

        Box(
            Modifier.align(Alignment.BottomStart).padding(start = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(ukuranAvatar + 8.dp)
                    .clip(CircleShape)
                    .background(AppColors.Background)
                    // Foto profil bisa diketuk untuk dilihat penuh. Di halaman
                    // yang menjual seseorang, foto orang itu adalah satu-satunya
                    // gambar yang tidak bisa dibuka besar sebelum ini — padahal
                    // setiap ubin portofolio di bawahnya bisa.
                    .clickable(enabled = !c.photoUrl.isNullOrBlank(), onClick = onFotoClick),
                contentAlignment = Alignment.Center,
            ) {
                AppAvatar(url = c.photoUrl, name = c.displayName, size = ukuranAvatar, verified = false)
            }
            if (sedangOnline(c)) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape)
                        .background(AppColors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(15.dp).clip(CircleShape).background(AppColors.Success))
                }
            }
        }
    }
}

/** Lencana terverifikasi bertulisan, bukan sekadar ikon centang. */
@Composable
internal fun LencanaVerified() {
    Row(
        Modifier.clip(RoundedCornerShape(percent = 50))
            .background(AppColors.Info.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Verified, contentDescription = null,
            tint = AppColors.Info, modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text("Terverifikasi", style = MaterialTheme.typography.labelSmall, color = AppColors.Info)
    }
}

@Composable
internal fun TrustBadgeRow(creator: CreatorModel) {
    val completion = if (creator.totalBookings == 0) 0 else (creator.completedBookings * 100 / creator.totalBookings).coerceIn(0, 100)
    val badges = buildList {
        if (creator.verified) add("Verified")
        if (creator.rating >= 4.8 && creator.reviewCount >= 5) add("Highly Rated")
        if ((creator.avgResponseMinutes ?: Int.MAX_VALUE) <= 60) add("Fast Responder")
        if (creator.followerCount >= 100) add("Popular")
        if (creator.totalBookings >= 50) add("Pro")
        if (creator.totalBookings in 1..20) add("Rising")
        if (completion >= 98) add("Top Creator")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        badges.take(4).forEach { badge ->
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = AppColors.SurfaceVariant,
                contentColor = AppColors.TextSecondary,
            ) {
                Row(
                    Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(badge, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text("${completion}% completion · ${creator.avgResponseMinutes?.let { "< ${durasiSingkat(it)} response" } ?: "Response time belum tersedia"}", style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
}

@Composable
internal fun BadgeStatusBooking(c: CreatorModel) {
    val libur = !c.acceptingBookings
    Row(
        Modifier.clip(RoundedCornerShape(percent = 50))
            .background(if (libur) AppColors.Warning.copy(alpha = 0.15f) else AppColors.Success.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (libur) Icons.Default.EventBusy else Icons.Default.EventAvailable,
            contentDescription = null,
            tint = if (libur) AppColors.Warning else AppColors.Success,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            when {
                !libur -> "Menerima booking"
                c.awayUntil != null -> "Libur sampai ${Formatters.dateShort(c.awayUntil)}"
                else -> "Sedang libur"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (libur) AppColors.Warning else AppColors.Success,
        )
    }
    c.awayNote?.takeIf { libur && it.isNotBlank() }?.let {
        Spacer(Modifier.height(6.dp))
        Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
    }
}

@Composable
internal fun ChipInfo(icon: ImageVector, teks: String) {
    Row(
        Modifier.clip(RoundedCornerShape(percent = 50)).background(AppColors.SurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(teks, style = MaterialTheme.typography.labelMedium, color = AppColors.TextPrimary)
    }
}
