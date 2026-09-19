package com.jepretaja.app.data.work

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jepretaja.app.core.util.FirestorePaths
import com.jepretaja.app.core.util.MediaNormalizer
import com.jepretaja.app.core.util.MediaUnggahan
import com.jepretaja.app.services.StorageService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException

/** Portfolio upload yang tetap berjalan saat layar atau proses aplikasi ditutup. */
class PortfolioUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val creatorId = inputData.getString(KEY_CREATOR_ID) ?: return Result.failure()
        val uris = inputData.getStringArray(KEY_URIS)?.toList().orEmpty()
        if (uris.isEmpty()) return Result.failure()
        val order = inputData.getLong(KEY_ORDER, 0L)
        val storage = StorageService(applicationContext)
        val db = FirebaseFirestore.getInstance()
        val uploadId = id.toString()
        val urls = mutableListOf<String>()
        var hasVideo = false

        return try {
            UploadNotifier.started(applicationContext, uploadId)
            coroutineScope {
                uris.forEachIndexed { index, raw ->
                    val source = Uri.parse(raw)
                    val video = applicationContext.contentResolver.getType(source)?.startsWith("video") == true
                    hasVideo = hasVideo || video
                    val normalized = if (video) {
                        runCatching { MediaNormalizer.reframeVideo(applicationContext, source) }.getOrElse { source }
                    } else {
                        MediaNormalizer.normalizeImage(applicationContext, source)
                    }
                    val url = storage.uploadPortfolioMedia(
                        creatorId = creatorId,
                        uri = normalized,
                        extension = if (video) "mp4" else "jpg",
                        resourceType = if (video) "video" else "image",
                    ) { progress ->
                        launch { setProgress(workDataOf(KEY_PROGRESS to ((index + progress) / uris.size).toFloat())) }
                    }
                    urls += url
                    UploadNotifier.uploading(applicationContext, uploadId, urls.size.toDouble() / uris.size)
                }
            }
            db.collection(FirestorePaths.PORTFOLIOS).add(
                mapOf(
                    "creatorId" to creatorId,
                    "media" to urls,
                    "title" to "",
                    "category" to "",
                    "type" to if (hasVideo) "video" else "image",
                    "thumbnailUrl" to null,
                    "status" to "draft",
                    "order" to order,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            MediaUnggahan.bersihkan(applicationContext, uris)
            UploadNotifier.success(applicationContext, uploadId)
            Result.success(workDataOf(KEY_PROGRESS to 1f))
        } catch (error: IOException) {
            if (runAttemptCount < 3) {
                UploadNotifier.update(
                    applicationContext,
                    uploadId,
                    "Upload dilanjutkan",
                    "Koneksi terputus. Portfolio akan dicoba lagi otomatis.",
                    ongoing = true,
                )
                return Result.retry()
            }
            MediaUnggahan.bersihkan(applicationContext, uris)
            UploadNotifier.failure(applicationContext, uploadId, error.message ?: "Portfolio gagal diunggah.")
            Result.failure(workDataOf(KEY_ERROR to (error.message ?: "Portfolio gagal diunggah.")))
        } catch (error: Exception) {
            MediaUnggahan.bersihkan(applicationContext, uris)
            UploadNotifier.failure(applicationContext, uploadId, "Portfolio gagal diproses.")
            Result.failure(workDataOf(KEY_ERROR to "Portfolio gagal diproses."))
        }
    }

    companion object {
        const val TAG = "unggah_portfolio"
        const val KEY_CREATOR_ID = "creatorId"
        const val KEY_URIS = "uris"
        const val KEY_ORDER = "order"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"

        fun data(creatorId: String, uris: List<String>, order: Long): Data = workDataOf(
            KEY_CREATOR_ID to creatorId,
            KEY_URIS to uris.toTypedArray(),
            KEY_ORDER to order,
        )
    }
}
