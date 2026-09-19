package com.jepretaja.app.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.util.Formatters
import com.jepretaja.app.ui.components.LocationField
import com.jepretaja.app.ui.components.PremiumCard

@Composable
internal fun IndikatorLangkah(
    state: BookingFormState,
    onPilih: (LangkahBooking) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        LangkahBooking.entries.forEach { langkah ->
            val sekarang = state.langkah == langkah
            val lewat = langkah.ordinal < state.langkah.ordinal
            val beres = lewat && state.langkahBeres(langkah)
            val bermasalah = lewat && !state.langkahBeres(langkah)
            val warna = when {
                bermasalah -> AppColors.Danger
                sekarang -> AppColors.Primary
                beres -> AppColors.Success
                else -> AppColors.TextSecondary
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPilih(langkah) }
                    .padding(vertical = 2.dp),
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(
                            color = if (sekarang) warna else warna.copy(alpha = 0.14f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        beres -> Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = warna,
                            modifier = Modifier.size(16.dp),
                        )
                        bermasalah -> Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = warna,
                            modifier = Modifier.size(16.dp),
                        )
                        else -> Text(
                            "${langkah.nomor}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.W700,
                            color = if (sekarang) AppColors.OnPrimary else warna,
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    langkah.judul,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sekarang) AppColors.TextPrimary else AppColors.TextSecondary,
                    maxLines = 1,
                )
            }

            if (langkah.ordinal < LangkahBooking.entries.lastIndex) {
                Box(
                    Modifier
                        .padding(top = 13.dp)
                        .width(14.dp)
                        .height(2.dp)
                        .background(
                            if (langkah.ordinal < state.langkah.ordinal) AppColors.Primary else AppColors.Border,
                        ),
                )
            }
        }
    }
}

@Composable
internal fun LangkahLayanan(state: BookingFormState, onToggleAddOn: (String) -> Unit) {
    val pkg = state.pkg ?: return

    KartuBagian(judul = "Data Layanan", ikon = Icons.Default.ReceiptLong) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pkg.name, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${pkg.duration} • ${pkg.personnel ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
            }
            Text(
                Formatters.currency(pkg.price),
                color = AppColors.Primary,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        pkg.output?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text("Hasil: $it", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
    }

    Spacer(Modifier.height(14.dp))
    KartuBagian(
        judul = "Add-on",
        ikon = Icons.Default.LocalOffer,
        keterangan = if (state.availableAddOns.isEmpty()) null else "Opsional, harganya ditambahkan ke total.",
    ) {
        if (state.availableAddOns.isEmpty()) {
            Text("Paket ini tidak punya add-on.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        } else {
            state.availableAddOns.forEachIndexed { index, addOn ->
                Row(
                    Modifier.fillMaxWidth().clickable { onToggleAddOn(addOn.addOnId) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = addOn.addOnId in state.selectedAddOnIds,
                        onCheckedChange = { onToggleAddOn(addOn.addOnId) },
                    )
                    Text(addOn.name, modifier = Modifier.weight(1f), color = AppColors.TextPrimary)
                    Text(
                        "+ ${Formatters.currency(addOn.price)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TextSecondary,
                    )
                }
                if (index != state.availableAddOns.lastIndex) Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
internal fun LangkahJadwal(
    state: BookingFormState,
    onPilihTanggal: () -> Unit,
    onPilihJam: () -> Unit,
) {
    KartuBagian(
        judul = "Tanggal",
        ikon = Icons.Default.CalendarToday,
        keterangan = if (state.unavailableDates.isEmpty()) null else {
            "${state.unavailableDates.size} tanggal sudah terisi dan tampil kelabu di kalender."
        },
        galat = state.galatTampil(BidangForm.TANGGAL),
    ) {
        BarisPilih(
            nilai = state.date?.let { Formatters.date(it) },
            placeholder = "Pilih tanggal pemotretan",
            galat = state.galatTampil(BidangForm.TANGGAL) != null,
            onClick = onPilihTanggal,
        )
    }

    Spacer(Modifier.height(14.dp))
    KartuBagian(
        judul = "Jam",
        ikon = Icons.Default.AccessTime,
        keterangan = "Jam mulai pemotretan.",
        galat = state.galatTampil(BidangForm.JAM),
    ) {
        BarisPilih(
            nilai = state.time,
            placeholder = "Pilih jam mulai",
            galat = state.galatTampil(BidangForm.JAM) != null,
            onClick = onPilihJam,
        )
    }
}

@Composable
private fun BarisPilih(
    nilai: String?,
    placeholder: String,
    galat: Boolean,
    onClick: () -> Unit,
) {
    val warnaTepi = if (galat) AppColors.Danger else AppColors.Border
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Background)
            .border(1.dp, warnaTepi, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            nilai ?: placeholder,
            style = MaterialTheme.typography.bodyLarge,
            color = if (nilai == null) AppColors.TextSecondary else AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun LangkahDetail(
    state: BookingFormState,
    onLokasi: (String) -> Unit,
    onLocationSelected: (Double, Double) -> Unit,
    onCatatan: (String) -> Unit,
    onBiayaJalan: (String) -> Unit,
) {
    val galatLokasi = state.galatTampil(BidangForm.LOKASI)

    KartuBagian(judul = "Lokasi", ikon = Icons.Default.Place) {
        LocationField(
            value = state.location,
            onValueChange = onLokasi,
            onLocationSelected = onLocationSelected,
            isError = galatLokasi != null,
            errorText = galatLokasi,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(Modifier.height(14.dp))
    KartuBagian(
        judul = "Catatan",
        ikon = Icons.Default.Notes,
        keterangan = "Konsep, jumlah orang, atau permintaan khusus. Opsional.",
    ) {
        OutlinedTextField(
            state.note,
            onCatatan,
            placeholder = { Text("Misal: outdoor, 2 keluarga, butuh 1 jam ekstra") },
            shape = MaterialTheme.shapes.medium,
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(Modifier.height(14.dp))
    KartuBagian(
        judul = "Biaya Perjalanan",
        ikon = Icons.Default.Route,
        keterangan = "Isi kalau lokasinya di luar kota creator. Kosongkan bila tidak ada.",
    ) {
        OutlinedTextField(
            state.travelFee,
            onBiayaJalan,
            prefix = { Text("Rp", color = AppColors.TextSecondary) },
            supportingText = {
                state.travelFee.toLongOrNull()?.takeIf { it > 0 }?.let {
                    Text(Formatters.currency(it), style = MaterialTheme.typography.bodySmall)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun LangkahKonfirmasi(
    state: BookingFormState,
    onVoucher: (String) -> Unit,
    onTerapkanVoucher: () -> Unit,
    onUbahLangkah: (LangkahBooking) -> Unit,
) {
    KartuBagian(judul = "Ringkasan Pesanan", ikon = Icons.Default.ReceiptLong) {
        BarisRingkas("Paket", state.pkg?.name ?: "-") { onUbahLangkah(LangkahBooking.LAYANAN) }
        if (state.selectedAddOnIds.isNotEmpty()) {
            val nama = state.availableAddOns
                .filter { it.addOnId in state.selectedAddOnIds }
                .joinToString(", ") { it.name }
            BarisRingkas("Add-on", nama) { onUbahLangkah(LangkahBooking.LAYANAN) }
        }
        BarisRingkas(
            "Jadwal",
            listOfNotNull(state.date?.let { Formatters.date(it) }, state.time).joinToString(", ").ifBlank { "-" },
        ) { onUbahLangkah(LangkahBooking.JADWAL) }
        BarisRingkas("Lokasi", state.location.ifBlank { "-" }) { onUbahLangkah(LangkahBooking.DETAIL) }
        if (state.note.isNotBlank()) {
            BarisRingkas("Catatan", state.note) { onUbahLangkah(LangkahBooking.DETAIL) }
        }
    }

    Spacer(Modifier.height(14.dp))
    KartuBagian(judul = "Voucher", ikon = Icons.Default.LocalOffer, keterangan = "Opsional.") {
        Row(verticalAlignment = Alignment.Top) {
            OutlinedTextField(
                state.voucherCode,
                onVoucher,
                placeholder = { Text("Kode voucher") },
                isError = state.voucherError != null,
                supportingText = state.voucherError?.let { msg -> { Text(msg) } },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            OutlinedButton(
                onClick = onTerapkanVoucher,
                enabled = state.voucherCode.isNotBlank() && !state.previewLoading,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier.height(56.dp),
            ) { Text("Terapkan") }
        }
    }

    Spacer(Modifier.height(14.dp))
    Row(
        Modifier.fillMaxWidth().padding(horizontal = PadTepi, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Ringkasan Harga", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
    }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.padding(horizontal = PadTepi)) { PriceBreakdownCard(state) }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun BarisRingkas(label: String, nilai: String, onUbah: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, modifier = Modifier.width(76.dp))
        Text(nilai, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
        TextButton(onClick = onUbah, contentPadding = PaddingValues(horizontal = 6.dp)) {
            Text("Ubah", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun KartuBagian(
    judul: String,
    ikon: ImageVector,
    keterangan: String? = null,
    galat: String? = null,
    isi: @Composable ColumnScope.() -> Unit,
) {
    PremiumCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = PadTepi),
        elevation = 3.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(ikon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(judul, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
        }
        keterangan?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
        Spacer(Modifier.height(12.dp))
        isi()
        galat?.let {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AppColors.Danger, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
            }
        }
    }
}

@Composable
internal fun PitaGalat(pesan: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Danger.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AppColors.Danger, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(pesan, style = MaterialTheme.typography.bodySmall, color = AppColors.Danger)
    }
}