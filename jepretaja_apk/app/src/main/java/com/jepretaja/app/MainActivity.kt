package com.jepretaja.app

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jepretaja.app.core.navigation.JepretAjaNavGraph
import com.jepretaja.app.core.theme.JepretAjaTheme
import com.jepretaja.app.core.util.ConnectivityObserver
import com.jepretaja.app.ui.components.OfflineBanner
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.jepretaja.app.core.navigation.Routes
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var connectivityObserver: ConnectivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // AndroidManifest mendeklarasikan Theme.JepretAja.Splash sebagai tema
        // permanen Activity ini (dipakai splashscreen compat library untuk
        // android:windowBackground biru). Tanpa baris ini, window background
        // TIDAK PERNAH kembali ke tema normal setelah splash selesai — itu
        // sebabnya background biru "menembus" ke semua layar (login,
        // onboarding, dst), bukan cuma splash sesaat.
        setTheme(R.style.Theme_JepretAja)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.window_background)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.window_background)
        var systemUi = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            systemUi = systemUi or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            systemUi = systemUi or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window.decorView.systemUiVisibility = systemUi
        val notificationRoute = intent.getStringExtra("notification_route")
        setContent {
            JepretAjaTheme {
                val online by connectivityObserver.isOnline.collectAsStateWithLifecycle(initialValue = true)

                // Izin notifikasi HARUS diminta saat runtime sejak Android 13.
                // Sebelumnya POST_NOTIFICATIONS hanya dideklarasikan di manifest
                // dan tidak pernah diminta, sehingga di perangkat Android 13 ke
                // atas seluruh notifikasi (booking, chat, pembayaran) tidak
                // pernah muncul sama sekali — tanpa error apa pun yang bisa
                // dilihat, karena sistem hanya membuangnya diam-diam.
                val peminta = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { /* ditolak pun aplikasi tetap jalan, hanya tanpa notifikasi */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Jangan menutup frame pertama dengan dialog sistem; tampilkan
                        // Home/login dulu agar pembukaan terasa instan.
                        delay(1200)
                        val sudahDiberi = ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!sudahDiberi) peminta.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Column(Modifier.fillMaxSize()) {
                    // Pita koneksi ditaruh di atas seluruh isi aplikasi supaya
                    // berlaku di semua layar sekaligus, bukan ditambahkan satu
                    // per satu di tiap halaman.
                    OfflineBanner(isOnline = online, modifier = Modifier.statusBarsPadding())
                    JepretAjaNavGraph(
                        modifier = Modifier.weight(1f),
                        notificationRoute = notificationRoute,
                    )
                }
            }
        }
    }
}
