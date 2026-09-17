package com.jepretaja.app.ui.screens.root

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.theme.AppRadii

/**
 * Pilihan yang muncul dari tombol + di tengah bottom nav.
 *
 * Satu tombol membuka tiga jenis "unggahan" yang berbeda bentuknya — karya ke
 * feed, foto ke portfolio, dan paket booking — daripada memberi creator tiga
 * tombol terpisah yang berebut tempat di bilah bawah.
 */
@Composable
fun CreatorUploadSheet(
    onDismiss: () -> Unit,
    onUploadMedia: () -> Unit,
    onNewPackage: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(AppRadii.medium))
                        .background(Brush.linearGradient(listOf(AppColors.PrimaryDark, AppColors.Primary))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AppColors.OnPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Creator Studio", style = MaterialTheme.typography.headlineSmall, color = AppColors.TextPrimary)
                    Text("Bangun karya yang layak tampil.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = AppColors.Accent)
            }
            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadii.medium),
                color = AppColors.PrimarySoft,
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Satu halaman untuk media, portfolio, dan publikasi.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text("PRO", style = MaterialTheme.typography.labelSmall, color = AppColors.Primary)
                }
            }
            Spacer(Modifier.height(18.dp))

            UploadOption(
                icon = Icons.Default.PermMedia,
                title = "Unggah Karya",
                subtitle = "Pilih Explore atau Portfolio dalam satu halaman",
                onClick = onUploadMedia,
            )
            Spacer(Modifier.height(10.dp))
            UploadOption(
                icon = Icons.Default.Inventory2,
                title = "Buat Paket Booking",
                subtitle = "Paket yang bisa dipesan konsumen dari profilmu",
                onClick = onNewPackage,
            )
        }
    }
}

@Composable
private fun UploadOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .shadow(5.dp, MaterialTheme.shapes.large, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.10f))
            .background(AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(MaterialTheme.shapes.medium).background(AppColors.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Buka", tint = AppColors.Primary)
    }
}
