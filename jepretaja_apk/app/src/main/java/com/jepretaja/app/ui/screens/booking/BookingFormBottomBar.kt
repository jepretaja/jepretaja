package com.jepretaja.app.ui.screens.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.util.Formatters

@Composable
internal fun BilahLangkah(
    state: BookingFormState,
    onMundur: () -> Unit,
    onLanjut: () -> Unit,
    onKirim: () -> Unit,
    termsAccepted: Boolean,
    onTermsChanged: (Boolean) -> Unit,
) {
    val terakhir = state.langkah == LangkahBooking.KONFIRMASI
    val total = (state.priceBreakdown?.get("total") as? Number)?.toLong()

    Surface(color = AppColors.Surface, shadowElevation = 12.dp) {
        Column(Modifier.padding(horizontal = PadTepi, vertical = 12.dp)) {
            if (total != null) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Total sementara",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        Formatters.currency(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W700,
                        color = AppColors.Primary,
                    )
                }
            }

            // Checkbox syarat punya barisnya sendiri, di ATAS baris tombol —
            // sebelumnya ketiganya (tombol Kembali, checkbox+teks panjang,
            // tombol Buat Booking) berebut satu Row yang sama, dan di layar
            // HP biasa itu jelas kepotong/numpuk karena hanya tombol utama
            // yang punya weight(1f), sisanya minta lebar sesukanya.
            if (terakhir) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                ) {
                    Checkbox(checked = termsAccepted, onCheckedChange = onTermsChanged)
                    Text(
                        "Saya menyetujui syarat layanan dan kebijakan pembatalan JepretAja.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(visible = state.langkah != LangkahBooking.LAYANAN) {
                    Row {
                        OutlinedButton(
                            onClick = onMundur,
                            shape = RoundedCornerShape(percent = 50),
                            modifier = Modifier.height(52.dp),
                        ) { Text("Kembali") }
                        Spacer(Modifier.width(10.dp))
                    }
                }
                Button(
                    onClick = if (terakhir) onKirim else onLanjut,
                    // Sebelumnya `enabled` di sini tidak mempedulikan
                    // `termsAccepted` sama sekali — tombol "Buat Booking"
                    // kelihatan aktif walau checkbox belum dicentang, tapi
                    // `onKirim` di BookingFormScreen memang sengaja tidak
                    // berbuat apa-apa kalau belum disetujui. Dari sisi
                    // pengguna itu tampak seperti tombolnya rusak/error:
                    // ditekan, tidak terjadi apa-apa, tanpa pesan sama
                    // sekali. Sekarang tombolnya sendiri yang nonaktif
                    // (abu-abu) sampai centangnya dicentang.
                    enabled = !state.submitting && !(terakhir && state.previewLoading) && !(terakhir && !termsAccepted),
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary,
                        contentColor = AppColors.OnPrimary,
                    ),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Text(
                        when {
                            state.submitting -> "Memproses..."
                            terakhir -> "Buat Booking"
                            else -> "Lanjut"
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}