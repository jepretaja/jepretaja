package com.jepretaja.app.ui.screens.creatordashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.data.model.PortfolioModel
import com.jepretaja.app.data.model.PortfolioStatus
import com.jepretaja.app.ui.components.premiumShadow

internal data class TampilanStatus(val label: String, val ikon: ImageVector, val warna: Color)

@Composable
internal fun tampilanStatus(status: String): TampilanStatus = when (status) {
    PortfolioStatus.DRAFT -> TampilanStatus("Draft", Icons.Default.EditNote, AppColors.TextSecondary)
    PortfolioStatus.MENUNGGU -> TampilanStatus("Menunggu Review", Icons.Default.HourglassTop, AppColors.Warning)
    PortfolioStatus.DITOLAK -> TampilanStatus("Ditolak", Icons.Default.Cancel, AppColors.Danger)
    PortfolioStatus.DISEMBUNYIKAN -> TampilanStatus("Disembunyikan", Icons.Default.VisibilityOff, AppColors.Danger)
    else -> TampilanStatus("Disetujui", Icons.Default.CheckCircle, AppColors.Success)
}

@Composable
internal fun KartuAlbum(
    item: PortfolioModel,
    bolehGeser: Boolean,
    modifier: Modifier = Modifier,
    onBuka: () -> Unit,
    onEdit: () -> Unit,
    onHapus: () -> Unit,
    onAjukan: () -> Unit,
    onJadikanDraft: () -> Unit,
) {
    val bentuk = RoundedCornerShape(16.dp)
    val t = tampilanStatus(item.status)
    val sampul = item.thumbnailUrl ?: item.media.firstOrNull()
    val video = item.type == "video"

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .premiumShadow(4.dp, bentuk)
                .clip(bentuk)
                .background(AppColors.SurfaceVariant)
                .clickable(onClick = onBuka),
        ) {
            when {
                sampul != null && !video -> AsyncImage(
                    model = sampul,
                    contentDescription = item.title.ifBlank { "Album portfolio" },
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                sampul != null -> Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)))
                else -> Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            if (video) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(40.dp),
                )
            }

            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(t.ikon, contentDescription = null, tint = t.warna, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(t.label, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
            }

            Row(Modifier.align(Alignment.TopEnd).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (bolehGeser) {
                    Icon(
                        Icons.Default.DragIndicator,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                TombolBulat(Icons.Default.Edit, "Edit album", onEdit)
                Spacer(Modifier.width(4.dp))
                TombolBulat(Icons.Default.Close, "Hapus album", onHapus)
            }

            if (item.media.size > 1) {
                Row(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Collections, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${item.media.size}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            item.title.ifBlank { "Tanpa judul" },
            style = MaterialTheme.typography.labelLarge,
            color = if (item.title.isBlank()) AppColors.TextSecondary else AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        item.category.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary, maxLines = 1)
        }
        if (item.status == PortfolioStatus.DITOLAK) {
            item.rejectedReason?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Danger,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        when (item.status) {
            PortfolioStatus.DRAFT, PortfolioStatus.DITOLAK -> TombolStatus(
                ikon = Icons.Default.Send,
                label = "Ajukan review",
                warna = AppColors.Primary,
                onClick = onAjukan,
            )
            PortfolioStatus.MENUNGGU -> TombolStatus(
                ikon = Icons.Default.EditNote,
                label = "Tarik pengajuan",
                warna = AppColors.TextSecondary,
                onClick = onJadikanDraft,
            )
            PortfolioStatus.DISETUJUI -> TombolStatus(
                ikon = Icons.Default.VisibilityOff,
                label = "Sembunyikan",
                warna = AppColors.TextSecondary,
                onClick = onJadikanDraft,
            )
            else -> Unit
        }
    }
}

@Composable
private fun TombolStatus(ikon: ImageVector, label: String, warna: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(ikon, contentDescription = null, tint = warna, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = warna)
    }
}

@Composable
private fun TombolBulat(ikon: ImageVector, deskripsi: String, onClick: () -> Unit) {
    Box(Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))) {
        IconButton(onClick = onClick, modifier = Modifier.size(26.dp)) {
            Icon(ikon, contentDescription = deskripsi, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}