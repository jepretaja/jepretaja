package com.jepretaja.app.data.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.jepretaja.app.R
import kotlin.math.roundToInt

object UploadNotifier {
    private const val CHANNEL = "upload_status"

    private fun manager(context: Context): NotificationManager {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Status upload", NotificationManager.IMPORTANCE_LOW))
        }
        return manager
    }

    fun update(context: Context, id: String, title: String, body: String, progress: Int? = null, ongoing: Boolean = true) {
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setOnlyAlertOnce(true)
            .setAutoCancel(!ongoing)
        if (progress != null) builder.setProgress(100, progress.coerceIn(0, 100), false)
        else builder.setProgress(0, 0, ongoing)
        manager(context).notify(id.hashCode(), builder.build())
    }

    fun started(context: Context, id: String) = update(context, id, "Upload dimulai", "Foto/video sedang diproses dan diunggah. 0%", 0)
    fun processing(context: Context, id: String) = update(context, id, "Sedang memproses", "Menyesuaikan media ke 1920 x 1080 px (16:9).", null)
    fun uploading(context: Context, id: String, progress: Double) {
        val percent = (progress * 100).roundToInt()
        update(context, id, "Sedang mengunggah", "Media sedang diunggah. $percent%", percent)
    }
    fun saving(context: Context, id: String) = update(context, id, "Menyimpan", "Upload selesai, sedang menyimpan postingan.", null)
    fun success(context: Context, id: String) = update(context, id, "Upload selesai", "Postingan berhasil diunggah dan siap dilihat di JepretAja.", 100, false)
    fun failure(context: Context, id: String, message: String) = update(context, id, "Upload gagal", "$message Coba lagi dari menu Upload.", null, false)
}
