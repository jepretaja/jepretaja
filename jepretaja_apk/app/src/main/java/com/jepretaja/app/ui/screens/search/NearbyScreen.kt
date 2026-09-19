package com.jepretaja.app.ui.screens.search

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.jepretaja.app.ui.components.AppAvatar
import com.jepretaja.app.ui.components.AppTopBar
import com.jepretaja.app.ui.components.EmptyState
import com.jepretaja.app.ui.components.PremiumCard
import com.jepretaja.app.ui.components.SkeletonList
import com.jepretaja.app.ui.components.rememberAppTopBarScrollBehavior
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.data.model.CreatorModel

private val radiusOptions = listOf(5.0, 10.0, 25.0, 50.0, 100.0)

/** Nearby (section 20) — permission lokasi nyata + jarak Haversine. */
@Composable
fun NearbyScreen(
    onBack: () -> Unit,
    onCreatorClick: (String) -> Unit,
    viewModel: NearbyViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var radiusKm by remember { mutableStateOf(25.0) }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Pair<CreatorModel, Double>>>(emptyList()) }
    var loadingResults by remember { mutableStateOf(false) }
    var modeMap by remember { mutableStateOf(true) }
    var availabilityFilter by remember { mutableStateOf<String?>(null) }

    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionDenied = true
            return
        }
        permissionDenied = false
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) { lat = loc.latitude; lng = loc.longitude; error = null }
            else error = "Gagal mendapatkan lokasi. Pastikan GPS aktif."
        }.addOnFailureListener { error = "Gagal mendapatkan lokasi: ${it.message}" }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchLocation() else permissionDenied = true
    }

    LaunchedEffect(Unit) { fetchLocation() }

    LaunchedEffect(lat, lng, radiusKm) {
        val currentLat = lat; val currentLng = lng
        if (currentLat != null && currentLng != null) {
            loadingResults = true
            results = viewModel.nearby(currentLat, currentLng, radiusKm)
            loadingResults = false
        }
    }

    val scrollBehavior = rememberAppTopBarScrollBehavior()
    val filteredResults = remember(results, availabilityFilter) {
        results.filter { (creator, _) ->
            when (availabilityFilter) {
                "today", "weekend" -> creator.acceptingBookings && creator.awayUntil == null
                else -> true
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AppTopBar(title = "Creator Terdekat", onBack = onBack, scrollBehavior = scrollBehavior) },
    ) { padding ->
        when {
            permissionDenied -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOff, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Izin lokasi ditolak. Aktifkan untuk memakai fitur Nearby.", color = AppColors.TextSecondary)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary, contentColor = AppColors.OnPrimary),
                    ) { Text("Coba Lagi") }
                }
            }
            error != null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { Text(error!!, color = AppColors.Danger) }
            lat == null || loadingResults -> SkeletonList(count = 7, modifier = Modifier.padding(padding))
            else -> Column(Modifier.padding(padding)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = modeMap,
                        onClick = { modeMap = true },
                        label = { Text("Map") },
                        leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                    )
                    FilterChip(
                        selected = !modeMap,
                        onClick = { modeMap = false },
                        label = { Text("List") },
                        leadingIcon = { Icon(Icons.Default.ViewList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { fetchLocation() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Gunakan lokasi saat ini", tint = AppColors.Primary)
                    }
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(radiusOptions, key = { it }) { r ->
                        FilterChip(
                            selected = radiusKm == r,
                            onClick = { radiusKm = r },
                            label = { Text("${r.toInt()} km") },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                        )
                    }
                    item {
                        FilterChip(
                            selected = availabilityFilter == "today",
                            onClick = { availabilityFilter = if (availabilityFilter == "today") null else "today" },
                            label = { Text("Available today") },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                        )
                    }
                    item {
                        FilterChip(
                            selected = availabilityFilter == "weekend",
                            onClick = { availabilityFilter = if (availabilityFilter == "weekend") null else "weekend" },
                            label = { Text("This weekend") },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                        )
                    }
                }
                Surface(
                    color = AppColors.SurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Within ${radiusKm.toInt()} km", style = MaterialTheme.typography.labelLarge, color = AppColors.TextPrimary)
                        Spacer(Modifier.weight(1f))
                        Text("${filteredResults.size} creator", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                }
                if (filteredResults.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Default.LocationOff,
                            title = "Tidak ada creator di sekitar",
                            description = "Perlebar radius pencarian untuk menemukan lebih banyak creator.",
                        )
                    }
                } else if (modeMap) {
                    NearbyMap(
                        results = filteredResults,
                        currentLat = lat,
                        currentLng = lng,
                        onCreatorClick = onCreatorClick,
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filteredResults, key = { it.first.creatorId }) { (creator, distance) ->
                            PremiumCard(
                                onClick = { onCreatorClick(creator.creatorId) },
                                elevation = 3.dp,
                                contentPadding = PaddingValues(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppAvatar(url = creator.coverUrl, name = creator.displayName, size = 46.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(creator.displayName, style = MaterialTheme.typography.titleSmall)
                                            if (creator.verified) {
                                                Spacer(Modifier.width(4.dp))
                                                Icon(Icons.Default.Verified, contentDescription = null, tint = AppColors.Info, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Text(
                                            "${creator.city ?: "-"} • ★ ${"%.1f".format(creator.rating)}",
                                            style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary,
                                        )
                                        Text(
                                            "Mulai Rp${creator.minPrice ?: 0}",
                                            style = MaterialTheme.typography.labelSmall, color = AppColors.Primary,
                                        )
                                    }
                                    Text(
                                        "%.1f km".format(distance),
                                        color = AppColors.Primary, style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyMap(
    results: List<Pair<CreatorModel, Double>>,
    currentLat: Double?,
    currentLng: Double?,
    onCreatorClick: (String) -> Unit,
) {
    val latitudes = results.mapNotNull { it.first.serviceLat }
    val longitudes = results.mapNotNull { it.first.serviceLng }
    val minLat = latitudes.minOrNull() ?: currentLat ?: 0.0
    val maxLat = latitudes.maxOrNull() ?: currentLat ?: 1.0
    val minLng = longitudes.minOrNull() ?: currentLng ?: 0.0
    val maxLng = longitudes.maxOrNull() ?: currentLng ?: 1.0
    val latRange = (maxLat - minLat).coerceAtLeast(0.01)
    val lngRange = (maxLng - minLng).coerceAtLeast(0.01)

    Box(
        Modifier.fillMaxWidth().height(270.dp).padding(horizontal = 20.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(Color(0xFFEAF1FE)),
    ) {
        Text("Peta area layanan", modifier = Modifier.align(Alignment.TopStart).padding(16.dp), color = AppColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        results.forEach { (creator, _) ->
            val creatorLat = creator.serviceLat ?: minLat
            val creatorLng = creator.serviceLng ?: minLng
            val x = (((creatorLng - minLng) / lngRange).coerceIn(0.08, 0.92)).toFloat()
            val y = (1f - ((creatorLat - minLat) / latRange).coerceIn(0.12, 0.88)).toFloat()
            Box(
                Modifier.fillMaxWidth().fillMaxHeight().align(Alignment.TopStart)
                    .padding(start = (x * 100).dp, top = (y * 210).dp),
            ) {
                Surface(onClick = { onCreatorClick(creator.creatorId) }, shape = androidx.compose.foundation.shape.CircleShape, color = AppColors.Primary, shadowElevation = 5.dp) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        AppAvatar(url = creator.photoUrl, name = creator.displayName, size = 30.dp)
                    }
                }
            }
        }
        Surface(
            color = AppColors.Surface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        ) {
            Text("${results.size} photographer di sekitar", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = AppColors.TextPrimary)
        }
    }
}
