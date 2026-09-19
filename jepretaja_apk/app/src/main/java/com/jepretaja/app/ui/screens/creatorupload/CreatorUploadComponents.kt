package com.jepretaja.app.ui.screens.creatorupload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import com.jepretaja.app.core.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun AntreanSection(
    antrean: List<com.jepretaja.app.data.work.AntreanUnggah>,
    onBatal: (java.util.UUID) -> Unit,
    onRetry: (java.util.UUID) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text("Antrean unggah", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        antrean.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when (item.state) {
                            WorkInfo.State.ENQUEUED -> "Menunggu giliran / jaringan"
                            WorkInfo.State.RUNNING -> "Mengunggah ${(item.progress * 100).toInt()}%"
                            WorkInfo.State.SUCCEEDED -> "Tersimpan dan tayang"
                            WorkInfo.State.FAILED -> item.error ?: "Gagal"
                            WorkInfo.State.BLOCKED -> "Tertunda"
                            WorkInfo.State.CANCELLED -> "Dibatalkan"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.state == WorkInfo.State.FAILED) AppColors.Danger else AppColors.TextSecondary,
                    )
                    if (item.state == WorkInfo.State.RUNNING) {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { item.progress },
                            color = AppColors.Primary,
                            trackColor = AppColors.SurfaceVariant,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small),
                        )
                    }
                }
                if (item.state == WorkInfo.State.ENQUEUED || item.state == WorkInfo.State.RUNNING) {
                    TextButton(onClick = { onBatal(item.id) }) { Text("Batal") }
                }
                if (item.state == WorkInfo.State.FAILED || item.state == WorkInfo.State.CANCELLED) {
                    TextButton(onClick = { onRetry(item.id) }) { Text("Coba Lagi") }
                }
            }
        }
    }
}

@Composable
internal fun UploadStatusPill(label: String, active: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White.copy(alpha = if (active) 0.18f else 0.10f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(6.dp).clip(RoundedCornerShape(percent = 50))
                .background(if (active) AppColors.OnPrimary else AppColors.OnPrimary.copy(alpha = 0.55f)),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = AppColors.OnPrimary, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PemilihJadwal(onDismiss: () -> Unit, onPilih: (Long) -> Unit) {
    var tanggalMillis by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    val stateTanggal = rememberDatePickerState()
    val stateJam = rememberTimePickerState(is24Hour = true)

    if (tanggalMillis == null) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { tanggalMillis = stateTanggal.selectedDateMillis }) { Text("Lanjut") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        ) { DatePicker(state = stateTanggal) }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Jam tayang") },
            text = { TimePicker(state = stateJam) },
            confirmButton = {
                TextButton(onClick = {
                    val kalender = Calendar.getInstance().apply {
                        timeInMillis = tanggalMillis!!
                        set(Calendar.HOUR_OF_DAY, stateJam.hour)
                        set(Calendar.MINUTE, stateJam.minute)
                        set(Calendar.SECOND, 0)
                    }
                    onPilih(kalender.timeInMillis)
                }) { Text("Jadwalkan") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        )
    }
}

internal fun detik(ms: Float): String {
    val total = (ms / 1000).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

internal fun formatWaktu(millis: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale("in", "ID")).format(Date(millis))

@Composable
internal fun StudioToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}