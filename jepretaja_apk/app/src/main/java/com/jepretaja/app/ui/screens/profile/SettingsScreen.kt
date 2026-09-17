package com.jepretaja.app.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.ui.components.AppTopBar
import com.jepretaja.app.ui.components.PremiumCard
import com.jepretaja.app.ui.state.AuthViewModel

private const val JEPRETAJA_PUBLIC_URL = "https://jepretaja-website-jepretaja.vercel.app"
private const val SUPPORT_EMAIL = "support@jepretaja.app"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onMyBookings: () -> Unit,
    onNotifications: () -> Unit,
    onFavorites: () -> Unit,
    onWatchHistory: () -> Unit,
    onReports: () -> Unit,
    onHelp: () -> Unit,
    onCreatorBank: () -> Unit,
    onCreatorStudio: () -> Unit,
    onLoggedOut: () -> Unit,
    authViewModel: AuthViewModel,
) {
    val context = LocalContext.current
    val authState by authViewModel.uiState.collectAsState()
    fun openExternalLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { /* no-op: browser may not be available */ }
    }
    var publicAccount by remember(authState.profile?.userId) { mutableStateOf(authState.profile?.publicProfile ?: true) }
    var messagePermission by remember(authState.profile?.userId) { mutableStateOf(authState.profile?.allowMessages ?: true) }
    var activityVisibility by remember(authState.profile?.userId) { mutableStateOf(authState.profile?.showActivity ?: true) }

    LaunchedEffect(authState.profile?.userId, authState.profile?.publicProfile, authState.profile?.allowMessages, authState.profile?.showActivity) {
        publicAccount = authState.profile?.publicProfile ?: true
        messagePermission = authState.profile?.allowMessages ?: true
        activityVisibility = authState.profile?.showActivity ?: true
    }
    var twoStep by remember { mutableStateOf(false) }
    var sound by remember { mutableStateOf(true) }
    var vibration by remember { mutableStateOf(true) }
    var followSystemTheme by remember { mutableStateOf(true) }
    var animations by remember { mutableStateOf(true) }
    var notificationPreferences by remember {
        mutableStateOf(listOf("Pesan", "Booking", "Pembayaran", "Pesanan", "Komentar", "Like", "Followers", "Promo", "Notifikasi sistem").associateWith { true })
    }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var showLogout by remember { mutableStateOf(false) }
    var showDisableAccount by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = { AppTopBar(title = "Pengaturan", onBack = onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            PremiumCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, null, tint = AppColors.Primary, modifier = Modifier.size(42.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(authState.profile?.name ?: "Pengguna JepretAja", style = MaterialTheme.typography.titleMedium)
                        Text(authState.profile?.email ?: "Akun belum tersambung", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                    IconButton(onClick = onEditProfile) { Icon(Icons.Default.Person, "Edit profil") }
                }
            }

            SettingsSection("Creator", Icons.Default.Dashboard) {
                if (authState.isCreator) {
                    SettingRow(
                        Icons.Default.Dashboard,
                        "Creator Studio",
                        "Kelola karya, booking, paket, dan pendapatan",
                        onCreatorStudio,
                    )
                } else {
                    SettingRow(
                        Icons.Default.Dashboard,
                        "Daftar sebagai Creator",
                        "Tampilkan karya dan terima booking dari pelanggan",
                        onClick = {
                            authViewModel.becomeCreator { pesan ->
                                if (pesan == null) {
                                    authViewModel.refreshProfileThen(onCreatorStudio)
                                } else {
                                    actionMessage = pesan
                                }
                            }
                        },
                    )
                }
            }

            SettingsSection("Akun", Icons.Default.Person) {
                SettingRow(Icons.Default.Person, "Edit profil", "Nama, email, nomor HP", onEditProfile)
                SettingRow(Icons.Default.Lock, "Ubah password", "Kirim tautan ubah password ke email", { authViewModel.resetPassword { actionMessage = it } })
                SettingToggle("PIN keamanan", "Minta verifikasi tambahan saat login", twoStep) { twoStep = it }
                SettingRow(Icons.Default.VerifiedUser, "Verifikasi email", "Kirim ulang email verifikasi akun", { authViewModel.resendVerificationEmail { actionMessage = it } })
                SettingRow(Icons.Default.Devices, "Perangkat yang login", "Buka detail keamanan aplikasi", {
                    runCatching {
                        context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    }.onFailure { actionMessage = "Detail perangkat tidak tersedia." }
                })
                SettingRow(Icons.Default.Logout, "Keluar dari semua perangkat", "Akhiri sesi akun ini", { showLogout = true })
            }

            SettingsSection("Privasi & Keamanan", Icons.Default.PrivacyTip) {
                SettingToggle("Akun publik", "Orang dapat menemukan profilmu", publicAccount) {
                    publicAccount = it
                    authViewModel.updatePrivacySettings(publicProfile = it, allowMessages = messagePermission, showActivity = activityVisibility)
                }
                SettingToggle("Siapa yang dapat mengirim pesan", "Izinkan pesan dari pengguna lain", messagePermission) {
                    messagePermission = it
                    authViewModel.updatePrivacySettings(publicProfile = publicAccount, allowMessages = it, showActivity = activityVisibility)
                }
                SettingToggle("Tampilkan aktivitas", "Bagikan aktivitas terbaru di profil", activityVisibility) {
                    activityVisibility = it
                    authViewModel.updatePrivacySettings(publicProfile = publicAccount, allowMessages = messagePermission, showActivity = it)
                }
                SettingRow(Icons.Default.Block, "Pengguna diblokir", "Kelola blokir dari ruang chat", { actionMessage = "Buka ruang chat pengguna lalu pilih menu Blokir. Daftar blokir berlaku langsung di seluruh aplikasi." })
            }

            SettingsSection("Notifikasi", Icons.Default.NotificationsNone) {
                notificationPreferences.forEach { (label, enabled) ->
                    SettingToggle(label, "Notifikasi $label", enabled) {
                        notificationPreferences = notificationPreferences.toMutableMap().also { it[label] = !enabled }
                    }
                }
                SettingToggle("Suara notifikasi", "Putar suara saat ada notifikasi", sound) { sound = it }
                SettingToggle("Getar", "Getar saat ada notifikasi", vibration) { vibration = it }
                SettingRow(Icons.Default.NotificationsNone, "Buka pusat notifikasi", "Lihat semua notifikasi", onNotifications)
                SettingRow(Icons.Default.SettingsSuggest, "Pengaturan notifikasi perangkat", "Atur izin notifikasi Android", {
                    runCatching {
                        context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                        })
                    }.onFailure { actionMessage = "Pengaturan notifikasi perangkat tidak tersedia." }
                })
            }

            SettingsSection("Tampilan", Icons.Default.DarkMode) {
                SettingToggle("Mengikuti sistem", "Gunakan tema perangkat", followSystemTheme) { followSystemTheme = it }
                SettingRow(Icons.Default.DarkMode, "Mode terang / gelap", "Aplikasi mengikuti mode perangkat", { actionMessage = "Tema aplikasi mengikuti pengaturan terang/gelap perangkat secara otomatis." })
                SettingRow(Icons.Default.Language, "Ukuran teks", "Sesuaikan keterbacaan", { actionMessage = "Ukuran teks dapat diatur dari pengaturan Tampilan perangkat." })
                SettingToggle("Animasi", "Gunakan transisi dan gerak antarlayar", animations) { animations = it }
            }

            SettingsSection("Booking & Pembayaran", Icons.Default.Payment) {
                SettingRow(Icons.Default.Payment, "Metode pembayaran", "Metode dipilih saat pembayaran booking", onMyBookings)
                SettingRow(Icons.Default.History, "Riwayat pembayaran", "Invoice dan status transaksi", onMyBookings)
                SettingRow(Icons.Default.Sell, "Booking aktif & riwayat booking", "Lihat semua pesanan", onMyBookings)
                SettingRow(Icons.Default.ReportProblem, "Refund", "Ajukan dari detail booking terkait", onMyBookings)
                SettingRow(Icons.Default.AccountCircle, "Rekening / e-wallet Creator", "Tujuan pencairan pendapatan", onCreatorBank)
            }

            SettingsSection("Konten", Icons.Default.BookmarkBorder) {
                SettingRow(Icons.Default.SettingsSuggest, "Pengaturan upload", "Kualitas dan preferensi unggah", {
                    if (authState.isCreator) onCreatorStudio()
                    else authViewModel.becomeCreator { pesan -> actionMessage = pesan ?: "Mode creator aktif. Buka Creator Studio untuk mulai mengunggah." }
                })
                SettingRow(Icons.Default.PrivacyTip, "Privasi posting", "Atur siapa yang dapat melihat postingan saat mengunggah", {
                    if (authState.isCreator) onCreatorStudio()
                    else authViewModel.becomeCreator { pesan -> actionMessage = pesan ?: "Daftar sebagai creator untuk mengatur privasi posting." }
                })
                SettingRow(Icons.Default.BookmarkBorder, "Konten tersimpan", "Buka koleksi tersimpan", onFavorites)
                SettingRow(Icons.Default.History, "Riwayat aktivitas", "Lihat aktivitas tontonan akun", onWatchHistory)
            }

            SettingsSection("Bantuan", Icons.Default.HelpOutline) {
                SettingRow(Icons.Default.HelpOutline, "Pusat bantuan & FAQ", "Jawaban untuk pertanyaan umum", onHelp)
                SettingRow(Icons.Default.ReportProblem, "Laporkan masalah", "Kirim laporan teknis", {
                    val subject = Uri.encode("Laporkan masalah aplikasi JepretAja")
                    val body = Uri.encode("Halo tim JepretAja,\n\nSaya ingin melaporkan masalah pada aplikasi:\n- Nama akun: ${authState.profile?.name ?: "-"}\n- Email: ${authState.profile?.email ?: "-"}\n- Detail masalah: \n")
                    openExternalLink("mailto:$SUPPORT_EMAIL?subject=$subject&body=$body")
                })
                SettingRow(Icons.Default.Block, "Laporkan pengguna", "Bantu menjaga komunitas tetap aman", onReports)
                SettingRow(Icons.Default.SupportAgent, "Hubungi CS", "Dapatkan bantuan dari tim JepretAja", {
                    val subject = Uri.encode("Bantuan JepretAja")
                    val body = Uri.encode("Halo tim JepretAja,\n\nSaya membutuhkan bantuan terkait akun / booking / karya saya.\n\nDetail:\n")
                    openExternalLink("mailto:$SUPPORT_EMAIL?subject=$subject&body=$body")
                })
                SettingRow(Icons.Default.Info, "Syarat & ketentuan", "Aturan penggunaan layanan", { openExternalLink("$JEPRETAJA_PUBLIC_URL/syarat") })
                SettingRow(Icons.Default.PrivacyTip, "Kebijakan privasi", "Cara data digunakan dan dilindungi", { openExternalLink("$JEPRETAJA_PUBLIC_URL/privasi") })
            }

            SettingsSection("Tentang", Icons.Default.Info) {
                SettingRow(Icons.Default.Info, "Tentang JepretAja", "Platform foto dan creator lokal", { openExternalLink(JEPRETAJA_PUBLIC_URL) })
                SettingRow(Icons.Default.VerifiedUser, "Versi aplikasi", "Versi terpasang saat ini", { actionMessage = "JepretAja ${com.jepretaja.app.BuildConfig.VERSION_NAME} (build ${com.jepretaja.app.BuildConfig.VERSION_CODE})" })
                SettingRow(Icons.Default.Info, "Lisensi", "Lisensi open-source yang digunakan", { actionMessage = "JepretAja menggunakan AndroidX, Jetpack Compose, Firebase, Coil, WorkManager, dan pustaka open-source lainnya." })
                SettingRow(Icons.Default.SettingsSuggest, "Cek pembaruan", "Pastikan aplikasi tetap terbaru", { openExternalLink("$JEPRETAJA_PUBLIC_URL/downloads") })
            }

            SettingsSection("Akun", Icons.Default.Security) {
                SettingRow(Icons.Default.Security, "Nonaktifkan akun", "Sembunyikan akun sementara", { showDisableAccount = true })
                SettingRow(Icons.Default.DeleteOutline, "Hapus akun", "Penghapusan permanen dan tidak dapat dibatalkan", { showDeleteAccount = true }, destructive = true)
                SettingRow(Icons.Default.Logout, "Logout", "Keluar dari perangkat ini", { showLogout = true }, destructive = true)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    actionMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { actionMessage = null },
            title = { Text("JepretAja") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { actionMessage = null }) { Text("Mengerti") } },
        )
    }
    if (showDisableAccount) {
        AlertDialog(
            onDismissRequest = { showDisableAccount = false },
            title = { Text("Nonaktifkan akun?") },
            text = { Text("Akun Anda akan disembunyikan sementara dan sesi saat ini akan ditutup.") },
            confirmButton = {
                Button(onClick = {
                    showDisableAccount = false
                    authViewModel.disableAccount { message ->
                        actionMessage = message
                        if (message?.contains("berhasil", ignoreCase = true) == true) {
                            onLoggedOut()
                        }
                    }
                }) { Text("Nonaktifkan") }
            },
            dismissButton = { TextButton(onClick = { showDisableAccount = false }) { Text("Batal") } },
        )
    }
    if (showDeleteAccount) {
        AlertDialog(
            onDismissRequest = { showDeleteAccount = false },
            title = { Text("Hapus akun secara permanen?") },
            text = { Text("Tindakan ini menghapus data akun, profil, dan konten yang terkait. Proses tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccount = false
                        authViewModel.deleteAccount { result ->
                            if (result.pesan == null) {
                                actionMessage = "Akun berhasil dihapus."
                                onLoggedOut()
                            } else {
                                actionMessage = result.pesan
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger),
                ) { Text("Hapus Akun") }
            },
            dismissButton = { TextButton(onClick = { showDeleteAccount = false }) { Text("Batal") } },
        )
    }
    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("Logout dari JepretAja?") },
            text = { Text("Sesi di perangkat ini akan diakhiri.") },
            confirmButton = { Button(onClick = { showLogout = false; authViewModel.logout(); onLoggedOut() }) { Text("Logout") } },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Icon(icon, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = AppColors.Primary, modifier = Modifier.padding(start = 8.dp))
        }
        PremiumCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, destructive: Boolean = false) {
    ListItem(
        headlineContent = { Text(title, color = if (destructive) AppColors.Danger else AppColors.TextPrimary) },
        supportingContent = { Text(subtitle, color = AppColors.TextSecondary) },
        leadingContent = { Icon(icon, null, tint = if (destructive) AppColors.Danger else AppColors.Primary) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = AppColors.TextSecondary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}