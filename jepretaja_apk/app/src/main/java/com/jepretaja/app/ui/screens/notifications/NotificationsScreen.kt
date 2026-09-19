package com.jepretaja.app.ui.screens.notifications

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.ui.components.AppTopBar
import com.jepretaja.app.ui.components.EmptyState
import com.jepretaja.app.ui.components.PremiumCard
import com.jepretaja.app.ui.components.rememberAppTopBarScrollBehavior
import com.jepretaja.app.ui.state.AuthViewModel
import com.jepretaja.app.data.work.UploadQueue
import androidx.work.WorkInfo
/** Jenis notifikasi yang lahir dari interaksi sosial, bukan dari transaksi. */
private val JENIS_SOSIAL = setOf("like", "comment", "follow")
private val JENIS_BOOKING = setOf("booking", "booking_created", "booking_confirmed", "booking_cancelled")
private val JENIS_MESSAGE = setOf("chat", "message")
private val JENIS_PAYMENT = setOf("payment", "refund", "withdrawal")

private val JUDUL_TAB = listOf("All", "Bookings", "Social", "Messages", "Payments", "System")

/** Notification System (section 17) — inbox bertab seperti TikTok. */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel,
    onUploadClick: () -> Unit,
    onNotificationClick: (com.jepretaja.app.data.model.NotificationModel) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val myUid = authState.uid
    val context = androidx.compose.ui.platform.LocalContext.current

    var tab by remember { mutableIntStateOf(0) }
    val scrollBehavior = rememberAppTopBarScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AppTopBar(title = "Notifikasi", onBack = onBack, scrollBehavior = scrollBehavior) },
    ) { padding ->
        if (myUid == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { Text("Masuk untuk melihat notifikasi") }
            return@Scaffold
        }
        val semua by remember(myUid) { viewModel.stream(myUid) }.collectAsStateWithLifecycle(initialValue = emptyList())
        val uploads by remember { UploadQueue.stream(context) }.collectAsStateWithLifecycle(initialValue = emptyList())

        // Tab mengikuti pola inbox TikTok. Kategorinya ditentukan field `type`
        // yang sudah lama ada di NotificationModel tapi tidak pernah dipakai
        // untuk apa pun, sehingga kabar booking, suka, dan komentar dulu
        // tercampur dalam satu daftar panjang.
        val items = remember(semua, tab) {
            when (tab) {
                1 -> semua.filter { it.type in JENIS_BOOKING }
                2 -> semua.filter { it.type in JENIS_SOSIAL }
                3 -> semua.filter { it.type in JENIS_MESSAGE }
                4 -> semua.filter { it.type in JENIS_PAYMENT }
                5 -> semua.filter { it.type !in JENIS_SOSIAL && it.type !in JENIS_BOOKING && it.type !in JENIS_MESSAGE && it.type !in JENIS_PAYMENT }
                else -> semua
            }
        }

        Column(Modifier.padding(padding).fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { viewModel.markAllRead(myUid) }, enabled = semua.any { it.readAt == null }) { Text("Mark all as read") }
        }
        ScrollableTabRow(
            selectedTabIndex = tab,
            containerColor = AppColors.Background,
            contentColor = AppColors.TextPrimary,
            edgePadding = 12.dp,
        ) {
            JUDUL_TAB.forEachIndexed { index, judul ->
                val jumlahBaru = when (index) {
                    1 -> semua.count { it.type in JENIS_BOOKING && it.readAt == null }
                    2 -> semua.count { it.type in JENIS_SOSIAL && it.readAt == null }
                    3 -> semua.count { it.type in JENIS_MESSAGE && it.readAt == null }
                    4 -> semua.count { it.type in JENIS_PAYMENT && it.readAt == null }
                    5 -> semua.count { it.type !in JENIS_SOSIAL && it.type !in JENIS_BOOKING && it.type !in JENIS_MESSAGE && it.type !in JENIS_PAYMENT && it.readAt == null }
                    else -> semua.count { it.readAt == null }
                }
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(if (jumlahBaru > 0) "$judul ($jumlahBaru)" else judul) },
                )
            }
        }

        if (items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.NotificationsNone,
                    title = if (tab == 0) "Belum ada notifikasi" else "Belum ada di kategori ini",
                    description = "Kabar soal booking, suka, komentar, dan pengikut baru akan muncul di sini.",
                )
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (tab == 0 || tab == 5) {
                    items(uploads, key = { "upload_${it.id}" }) { upload ->
                        val (title, body) = when (upload.state) {
                            WorkInfo.State.RUNNING -> "Upload sedang berjalan" to "Media sedang diproses dan diunggah. ${(upload.progress * 100).toInt()}%"
                            WorkInfo.State.SUCCEEDED -> "Upload selesai" to "Postingan siap dilihat di JepretAja."
                            WorkInfo.State.FAILED -> "Upload gagal" to (upload.error ?: "Media gagal diproses. Coba lagi.")
                            WorkInfo.State.CANCELLED -> "Upload dibatalkan" to "Upload belum selesai dan dapat dimulai lagi."
                            else -> "Media siap upload" to "Upload menunggu jaringan atau giliran."
                        }
                        PremiumCard(modifier = Modifier.fillMaxWidth(), elevation = 4.dp, onClick = onUploadClick) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall); Text(body, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary) }
                            }
                        }
                    }
                }
                items(items, key = { it.notificationId }) { n ->
                    val unread = n.readAt == null
                    PremiumCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = if (unread) 5.dp else 2.dp,
                        onClick = { viewModel.markRead(n.notificationId); onNotificationClick(n) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (unread) AppColors.PrimarySoft else AppColors.SurfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Notifications, contentDescription = null,
                                    tint = if (unread) AppColors.Primary else AppColors.TextSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    n.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (unread) FontWeight.W700 else FontWeight.W500,
                                    color = AppColors.TextPrimary,
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(n.body, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            }
                            if (unread) {
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.size(8.dp).clip(CircleShape).background(AppColors.Primary))
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
