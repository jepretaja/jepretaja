package com.jepretaja.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.jepretaja.app.services.LiveLocationForegroundService

@Composable
fun LiveLocationTracker(
    bookingId: String,
    userId: String,
    role: String,
    targetLatitude: Double? = null,
    targetLongitude: Double? = null,
    enabled: Boolean,
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted = it }

    LaunchedEffect(enabled, permissionGranted) {
        if (enabled && !permissionGranted) requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        if (enabled && permissionGranted) {
            val serviceIntent = Intent(context, LiveLocationForegroundService::class.java).apply {
                putExtra(LiveLocationForegroundService.EXTRA_BOOKING_ID, bookingId)
                putExtra(LiveLocationForegroundService.EXTRA_USER_ID, userId)
                putExtra(LiveLocationForegroundService.EXTRA_ROLE, role)
                targetLatitude?.let { putExtra(LiveLocationForegroundService.EXTRA_TARGET_LATITUDE, it) }
                targetLongitude?.let { putExtra(LiveLocationForegroundService.EXTRA_TARGET_LONGITUDE, it) }
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } else if (!enabled) {
            context.stopService(Intent(context, LiveLocationForegroundService::class.java))
        }
    }
}