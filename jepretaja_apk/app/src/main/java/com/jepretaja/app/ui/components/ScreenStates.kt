package com.jepretaja.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import com.jepretaja.app.core.theme.AppColors

/** State gagal yang seragam untuk layar yang memuat data dari jaringan. */
@Composable
fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Terjadi kesalahan",
    description: String? = null,
    icon: ImageVector = Icons.Default.CloudOff,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.Danger)
        Text(title, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary, textAlign = TextAlign.Center)
        description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary, textAlign = TextAlign.Center) }
        AppButton(text = "Coba Lagi", onClick = onRetry, modifier = Modifier.fillMaxWidth(0.72f))
    }
}

/** State offline yang eksplisit, bukan daftar kosong yang terlihat seperti bug. */
@Composable
fun OfflineState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ErrorState(
        onRetry = onRetry,
        modifier = modifier,
        title = "Tidak ada koneksi",
        description = "Periksa jaringan internet Anda lalu coba lagi.",
        icon = Icons.Default.WifiOff,
    )
}
