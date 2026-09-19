package com.jepretaja.app.ui.screens.booking

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Kalender yang benar-benar mematikan tanggal yang tidak tersedia.
 *
 * `SelectableDates` menjaga aturan tetap berada di dalam kalender: tanggal yang
 * sudah terisi tampak kelabu dan tidak bisa ditekan, bukan baru ditolak setelah
 * pengguna memilihnya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KalenderBooking(
    terpilih: LocalDate?,
    tidakTersedia: Set<LocalDate>,
    onDismiss: () -> Unit,
    onPilih: (LocalDate) -> Unit,
) {
    val hariIni = remember { LocalDate.now() }
    val aturan = remember(tidakTersedia, hariIni) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val tanggal = runCatching {
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                }.getOrNull() ?: return false
                return !tanggal.isBefore(hariIni) && tanggal !in tidakTersedia
            }

            override fun isSelectableYear(year: Int): Boolean = year >= hariIni.year
        }
    }

    val state = rememberDatePickerState(
        initialSelectedDateMillis = terpilih?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = aturan,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        onPilih(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    } else {
                        onDismiss()
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) { Text("Pilih") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    ) {
        DatePicker(
            state = state,
            title = { Text("Tanggal Pemotretan", modifier = Modifier.padding(start = 24.dp, top = 20.dp)) },
            headline = {
                Text(
                    if (tidakTersedia.isEmpty()) "Pilih tanggal" else "Tanggal kelabu sudah terisi",
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
        )
    }
}