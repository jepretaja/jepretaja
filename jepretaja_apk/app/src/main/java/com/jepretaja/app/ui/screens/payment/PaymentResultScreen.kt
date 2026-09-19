package com.jepretaja.app.ui.screens.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.ui.components.BigPrimaryButton

/**
 * Status setelah pengguna menyatakan sudah transfer.
 *
 * Verifikasi dilakukan MANUAL oleh admin di panel web (aksi
 * confirmManualPayment), bukan oleh webhook payment gateway. Status terbaru
 * bisa dilihat di detail booking.
 */
@Composable
fun PaymentResultScreen(
    bookingId: String,
    onSeeBookingDetail: (String) -> Unit,
    onBackToHome: () -> Unit,
) {
    Scaffold { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(32.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(AppColors.Success.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppColors.Success, modifier = Modifier.size(52.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("Pembayaran berhasil", style = MaterialTheme.typography.headlineSmall, color = AppColors.TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pembayaran untuk booking #${bookingId.take(8).uppercase()} berhasil diterima dan sedang diteruskan ke creator.",
                    textAlign = TextAlign.Center,
                    color = AppColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(28.dp))
                BigPrimaryButton(text = "Lihat Detail Booking", onClick = { onSeeBookingDetail(bookingId) })
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { onSeeBookingDetail(bookingId) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Unduh Struk")
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onBackToHome) { Text("Kembali ke Home") }
            }
        }
    }
}
