package com.jepretaja.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** JepretAja media contract: landscape 16:9, up to 1920x1080, never upscaled. */
object MediaNormalizer {
    const val WIDTH = 1920
    const val HEIGHT = 1080
    const val ASPECT = 16f / 9f

    suspend fun normalizeImage(context: Context, source: Uri): Uri = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Ukuran foto tidak terbaca." }

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        val bitmapOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, bitmapOptions)
        }
            ?: error("Foto tidak bisa dibaca.")
        val sourceRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val cropWidth: Int
        val cropHeight: Int
        if (sourceRatio > ASPECT) {
            cropHeight = bitmap.height
            cropWidth = (bitmap.height * ASPECT).toInt()
        } else {
            cropWidth = bitmap.width
            cropHeight = (bitmap.width / ASPECT).toInt()
        }
        val left = ((bitmap.width - cropWidth) / 2).coerceAtLeast(0)
        val top = ((bitmap.height - cropHeight) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
        val scale = minOf(1f, WIDTH.toFloat() / cropped.width, HEIGHT.toFloat() / cropped.height)
        val output = if (scale < 1f) Bitmap.createScaledBitmap(cropped, (cropped.width * scale).toInt(), (cropped.height * scale).toInt(), true) else cropped
        val file = File(context.filesDir, "upload_queue/normalized_${System.currentTimeMillis()}.jpg").apply { parentFile?.mkdirs() }
        FileOutputStream(file).use { output.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (output !== cropped) output.recycle()
        if (cropped !== bitmap) cropped.recycle()
        bitmap.recycle()
        file.toUri()
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        val largestSide = maxOf(width, height)
        while (largestSide / sampleSize > 2048) sampleSize *= 2
        return sampleSize
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    suspend fun reframeVideo(context: Context, source: Uri): Uri = withContext(Dispatchers.IO) {
        // Jangan encode ulang video yang sudah memenuhi kontrak feed. Encode
        // ulang dengan bitrate default Media3 membuat video 1080p terlihat
        // pecah, padahal sumber aslinya sudah bagus.
        val metadata = MediaMetadataRetriever()
        val sourceWidth: Int
        val sourceHeight: Int
        try {
            metadata.setDataSource(context, source)
            sourceWidth = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            sourceHeight = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        } finally {
            metadata.release()
        }
        val ratio = if (sourceHeight > 0) sourceWidth.toFloat() / sourceHeight else 0f
        if (sourceWidth in 1..WIDTH && sourceHeight in 1..HEIGHT && kotlin.math.abs(ratio - ASPECT) < 0.02f) {
            return@withContext source
        }

        val output = File(context.filesDir, "upload_queue/reframed_${System.currentTimeMillis()}.mp4").apply { parentFile?.mkdirs() }
        val item = MediaItem.fromUri(source)
        suspendCancellableCoroutine { continuation ->
            val bitrate = when {
                sourceWidth >= 1920 || sourceHeight >= 1080 -> 12_000_000
                sourceWidth >= 1280 || sourceHeight >= 720 -> 6_000_000
                else -> 3_000_000
            }
            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(bitrate).build())
                .build()
            val transformer = Transformer.Builder(context)
                .setEncoderFactory(encoderFactory)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setVideoEffects(listOf(Presentation.createForWidthAndHeight(WIDTH, HEIGHT, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP)))
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: androidx.media3.transformer.Composition, result: androidx.media3.transformer.ExportResult) { continuation.resume(output.toUri()) }
                    override fun onError(composition: androidx.media3.transformer.Composition, result: androidx.media3.transformer.ExportResult, exception: androidx.media3.transformer.ExportException) { continuation.resumeWithException(exception) }
                })
                .build()
            transformer.start(item, output.absolutePath)
            continuation.invokeOnCancellation { runCatching { transformer.cancel() } }
        }
    }
}
