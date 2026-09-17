package com.jepretaja.app.data.work

import android.content.Context
import android.util.Base64
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Satu baris antrean unggah, sudah diringkas untuk ditampilkan. */
data class AntreanUnggah(
    val id: UUID,
    val state: WorkInfo.State,
    val progress: Float,
    val error: String?,
)

/**
 * Antrean unggahan.
 *
 * Semua unggahan lewat WorkManager, termasuk yang tidak dijadwalkan. Dengan
 * begitu hanya ada satu jalur yang perlu dipikirkan: kemajuan, percobaan ulang,
 * dan penjadwalan diperlakukan sama, dan menutup layar tidak pernah lagi berarti
 * kehilangan unggahan yang sedang berjalan.
 */
object UploadQueue {
    private const val PREFS = "upload_queue_payloads"

    fun enqueue(
        context: Context,
        data: androidx.work.Data,
        delayMs: Long = 0L,
    ): UUID {
        val permintaan = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(data)
            .addTag(UploadWorker.TAG)
            // Menunggu jaringan, bukan langsung gagal saat sedang offline.
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .apply { if (delayMs > 0) setInitialDelay(delayMs, TimeUnit.MILLISECONDS) }
            .build()

        // Nama unik per permintaan (bukan per creator): dua unggahan berbeda
        // harus bisa mengantre bersamaan, bukan saling membatalkan.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(permintaan.id.toString(), Base64.encodeToString(data.toByteArray(), Base64.NO_WRAP))
            .apply()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "unggah_${permintaan.id}",
            ExistingWorkPolicy.KEEP,
            permintaan,
        )
        return permintaan.id
    }

    fun batal(context: Context, id: UUID) {
        WorkManager.getInstance(context).cancelWorkById(id)
        clearPayload(context, id)
    }

    fun clearPayload(context: Context, id: UUID) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(id.toString())
            .apply()
    }

    suspend fun retry(context: Context, id: UUID): UUID? {
        val manager = WorkManager.getInstance(context)
        val old = manager.getWorkInfoById(id).get() ?: return null
        if (old.state != WorkInfo.State.FAILED && old.state != WorkInfo.State.CANCELLED) return null
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(id.toString(), null) ?: return null
        val data = androidx.work.Data.fromByteArray(Base64.decode(encoded, Base64.NO_WRAP))
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(data)
            .addTag(UploadWorker.TAG)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        manager.enqueueUniqueWork("unggah_${request.id}", ExistingWorkPolicy.KEEP, request)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(request.id.toString(), encoded)
            .remove(id.toString())
            .apply()
        return request.id
    }

    fun stream(context: Context): Flow<List<AntreanUnggah>> =
        WorkManager.getInstance(context)
            .getWorkInfosByTagFlow(UploadWorker.TAG)
            .map { daftar ->
                daftar
                    .filterNot { it.state == WorkInfo.State.SUCCEEDED && it.outputData.keyValueMap.isEmpty() }
                    .map { info ->
                        AntreanUnggah(
                            id = info.id,
                            state = info.state,
                            progress = info.progress.getFloat(UploadWorker.KEY_PROGRESS, 0f),
                            error = info.outputData.getString(UploadWorker.KEY_ERROR),
                        )
                    }
            }
}
