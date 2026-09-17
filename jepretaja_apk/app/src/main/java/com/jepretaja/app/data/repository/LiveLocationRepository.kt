package com.jepretaja.app.data.repository

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import android.location.Location
import com.google.firebase.firestore.FirebaseFirestore
import com.jepretaja.app.core.util.FirestorePaths
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveLocationRepository @Inject constructor(
    private val db: FirebaseFirestore,
) {
    suspend fun publish(
        bookingId: String,
        userId: String,
        role: String,
        point: LatLng,
        target: LatLng?,
    ) {
        val distanceMeters = target?.let {
            FloatArray(1).also { result -> Location.distanceBetween(point.latitude, point.longitude, it.latitude, it.longitude, result) }[0].toDouble()
        }
        val etaSeconds = distanceMeters?.let { (it / 8.0).toLong() }
        val geofenceStatus = distanceMeters?.let { if (it <= 200.0) "arrived" else "en_route" }
        val fields = mapOf(
                "bookingId" to bookingId,
                "userId" to userId,
                "role" to role,
                "latitude" to point.latitude,
                "longitude" to point.longitude,
                "distanceMeters" to distanceMeters,
                "etaSeconds" to etaSeconds,
                "geofenceStatus" to geofenceStatus,
                "updatedAt" to Timestamp.now(),
        )
        db.collection(FirestorePaths.LIVE_LOCATIONS).document("${bookingId}_$userId").set(fields).await()
        db.collection(FirestorePaths.LOCATION_TRACKS).add(
            fields + mapOf(
                "recordedAt" to Timestamp.now(),
                "eventType" to when (geofenceStatus) {
                    "arrived" -> "geofence_arrived"
                    else -> "location_update"
                },
            ),
        ).await()
    }

    suspend fun raiseSos(bookingId: String, userId: String, role: String, message: String) {
        val now = Timestamp.now()
        db.collection(FirestorePaths.SUPPORT_ALERTS).add(
            mapOf(
                "bookingId" to bookingId,
                "userId" to userId,
                "role" to role,
                "type" to "sos",
                "message" to message,
                "status" to "open",
                "createdAt" to now,
            ),
        ).await()
        db.collection(FirestorePaths.LOCATION_TRACKS).add(
            mapOf(
                "bookingId" to bookingId,
                "userId" to userId,
                "role" to role,
                "eventType" to "sos",
                "message" to message,
                "recordedAt" to now,
            ),
        ).await()
    }
}