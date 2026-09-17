package com.jepretaja.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.jepretaja.app.data.repository.LiveLocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LiveLocationForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val repository by lazy { LiveLocationRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance()) }
    private var callback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bookingId = intent?.getStringExtra(EXTRA_BOOKING_ID).orEmpty()
        val userId = intent?.getStringExtra(EXTRA_USER_ID).orEmpty()
        val role = intent?.getStringExtra(EXTRA_ROLE).orEmpty()
        val targetLatitude = intent?.getDoubleExtra(EXTRA_TARGET_LATITUDE, Double.NaN) ?: Double.NaN
        val targetLongitude = intent?.getDoubleExtra(EXTRA_TARGET_LONGITUDE, Double.NaN) ?: Double.NaN
        val target = if (targetLatitude.isFinite() && targetLongitude.isFinite()) LatLng(targetLatitude, targetLongitude) else null
        if (bookingId.isBlank() || userId.isBlank() || role.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, notification(bookingId))
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15_000L)
            .setMinUpdateIntervalMillis(8_000L)
            .build()
        callback?.let { fused.removeLocationUpdates(it) }
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    scope.launch {
                        runCatching { repository.publish(bookingId, userId, role, LatLng(location.latitude, location.longitude), target) }
                    }
                }
            }
        }
        fused.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        callback?.let { fused.removeLocationUpdates(it) }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Perjalanan JepretAja", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    private fun notification(bookingId: String): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setContentTitle("Perjalanan sedang dibagikan")
        .setContentText("Lokasi booking #${bookingId.take(8).uppercase()} diperbarui realtime.")
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    companion object {
        const val EXTRA_BOOKING_ID = "bookingId"
        const val EXTRA_USER_ID = "userId"
        const val EXTRA_ROLE = "role"
        const val EXTRA_TARGET_LATITUDE = "targetLatitude"
        const val EXTRA_TARGET_LONGITUDE = "targetLongitude"
        private const val CHANNEL_ID = "live_location"
        private const val NOTIFICATION_ID = 4101
    }
}