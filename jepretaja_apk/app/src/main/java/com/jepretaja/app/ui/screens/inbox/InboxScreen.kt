package com.jepretaja.app.ui.screens.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.util.Formatters
import com.jepretaja.app.data.model.BookingModel
import com.jepretaja.app.data.model.ChatModel
import com.jepretaja.app.data.model.NotificationModel
import com.jepretaja.app.ui.components.AppAvatar
import com.jepretaja.app.ui.components.AppTopBar
import com.jepretaja.app.ui.components.EmptyState
import com.jepretaja.app.ui.components.PremiumCard
import com.jepretaja.app.ui.components.StatusBadge
import com.jepretaja.app.ui.components.rememberAppTopBarScrollBehavior
import com.jepretaja.app.ui.state.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class InboxTab(val label: String) {
    ALL("Semua"), BOOKINGS("Booking"), MESSAGES("Chat")
}

private sealed interface InboxItem {
    val sortTime: Long
    data class Chat(val value: ChatModel, val unread: Int) : InboxItem {
        override val sortTime: Long = value.updatedAt?.toDate()?.time ?: 0L
    }
    data class Booking(val value: BookingModel) : InboxItem {
        override val sortTime: Long = value.createdAt?.toDate()?.time ?: value.date?.toDate()?.time ?: 0L
    }
    data class Notification(val value: NotificationModel) : InboxItem {
        override val sortTime: Long = value.createdAt?.toDate()?.time ?: 0L
    }
}

@Composable
fun InboxScreen(
    onChatClick: (String) -> Unit,
    onBookingClick: (String) -> Unit,
    authViewModel: AuthViewModel,
    inboxViewModel: InboxViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsState()
    val uid = authState.uid
    var tab by remember { mutableStateOf(InboxTab.ALL) }
    var retryKey by remember(uid) { mutableIntStateOf(0) }
    val scrollBehavior = rememberAppTopBarScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AppColors.Background,
        topBar = { AppTopBar(title = "Kotak Masuk", scrollBehavior = scrollBehavior) },
    ) { padding ->
        if (uid == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.NotificationsNone,
                    title = "Masuk untuk membuka kotak masuk",
                    description = "Pesan, booking, dan kabar penting akan terkumpul di sini.",
                )
            }
            return@Scaffold
        }

        val inboxState by remember(uid, retryKey) { inboxViewModel.observe(uid, retryKey) }.collectAsState()
        if (inboxState.loading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Primary)
            }
            return@Scaffold
        }
        inboxState.error?.let { message ->
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = AppColors.Danger, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(message, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { retryKey++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Coba lagi")
                    }
                }
            }
            return@Scaffold
        }

        val chats = inboxState.chats
        val unreadByChat = inboxState.unreadByChat
        val bookings = inboxState.bookings
        val notifications = inboxState.notifications
        val items = remember(chats, unreadByChat, bookings, notifications, tab) {
            buildList {
                if (tab == InboxTab.ALL || tab == InboxTab.MESSAGES) {
                    addAll(chats.map { InboxItem.Chat(it, unreadByChat[it.chatId] ?: 0) })
                }
                if (tab == InboxTab.ALL || tab == InboxTab.BOOKINGS) {
                    addAll(bookings.map { InboxItem.Booking(it) })
                }
                if (tab == InboxTab.ALL) {
                    addAll(notifications.map { InboxItem.Notification(it) })
                }
            }.sortedByDescending { it.sortTime }
        }
        val unreadTotal = unreadByChat.values.sum() + notifications.count { it.readAt == null }
        val activeBookings = bookings.count { it.status !in setOf("completed", "cancelled", "rejected", "reviewed") }

        Column(Modifier.padding(padding).fillMaxSize()) {
            PremiumCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                elevation = 5.dp,
                color = AppColors.Surface,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(AppColors.PrimarySoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (unreadTotal > 0) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = AppColors.Primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (unreadTotal > 0) "$unreadTotal kabar perlu dilihat" else "Semua sudah terbaca",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.W700,
                            color = AppColors.TextPrimary,
                        )
                        Text(
                            "$activeBookings booking aktif · ${chats.size} percakapan",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary,
                        )
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = AppColors.Background,
                contentColor = AppColors.Primary,
                edgePadding = 12.dp,
            ) {
                InboxTab.values().forEach { item ->
                    val count = when (item) {
                        InboxTab.ALL -> unreadTotal
                        InboxTab.MESSAGES -> unreadByChat.values.sum()
                        InboxTab.BOOKINGS -> activeBookings
                    }
                    Tab(
                        selected = tab == item,
                        onClick = { tab = item },
                        text = { Text(if (count > 0) "${item.label} ($count)" else item.label) },
                    )
                }
            }

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = when (tab) {
                            InboxTab.MESSAGES -> Icons.Default.ChatBubbleOutline
                            InboxTab.BOOKINGS -> Icons.Default.CalendarMonth
                            else -> Icons.Default.NotificationsNone
                        },
                        title = when (tab) {
                            InboxTab.MESSAGES -> "Belum ada pesan"
                            InboxTab.BOOKINGS -> "Belum ada booking"
                            InboxTab.ALL -> "Kotak masuk masih kosong"
                        },
                        description = "Kabar baru akan muncul otomatis di sini.",
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items, key = { item ->
                        when (item) {
                            is InboxItem.Chat -> "chat-${item.value.chatId}"
                            is InboxItem.Booking -> "booking-${item.value.bookingId}"
                            is InboxItem.Notification -> "notification-${item.value.notificationId}"
                        }
                    }) { item ->
                        when (item) {
                            is InboxItem.Chat -> {
                                ChatInboxRow(item, onClick = { onChatClick(item.value.chatId) })
                                HorizontalDivider(
                                    color = AppColors.Border,
                                    modifier = Modifier.padding(start = 80.dp),
                                )
                            }
                            is InboxItem.Booking -> BookingInboxRow(item.value, onClick = { onBookingClick(item.value.bookingId) })
                            is InboxItem.Notification -> NotificationInboxRow(item.value, onClick = { inboxViewModel.markRead(item.value.notificationId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInboxRow(item: InboxItem.Chat, onClick: () -> Unit) {
    val chat = item.value
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppAvatar(url = chat.otherPartyPhotoUrl, name = chat.otherPartyName, size = 52.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chat.otherPartyName.ifBlank { "Percakapan" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (item.unread > 0) FontWeight.W700 else FontWeight.W600,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(waktuInbox(chat.updatedAt?.toDate()?.time), style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                chat.lastMessage.ifBlank { "Belum ada pesan" },
                style = MaterialTheme.typography.bodySmall,
                color = if (item.unread > 0) AppColors.TextPrimary else AppColors.TextSecondary,
                fontWeight = if (item.unread > 0) FontWeight.W600 else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.unread > 0) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(AppColors.Primary)
                    .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (item.unread > 99) "99+" else item.unread.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.W700,
                    color = AppColors.OnPrimary,
                )
            }
        }
    }
}

@Composable
private fun BookingInboxRow(booking: BookingModel, onClick: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth(), elevation = 3.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(AppColors.Info.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AppColors.Info)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Booking #${booking.bookingId.takeLast(8).uppercase()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W700,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(waktuInbox(booking.createdAt?.toDate()?.time), style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
                }
                Spacer(Modifier.height(3.dp))
                Text(booking.packageName.ifBlank { "Paket booking" }, style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${booking.date?.let { Formatters.date(it) } ?: "Jadwal belum diatur"} · ${Formatters.currency(booking.total)}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                Spacer(Modifier.height(6.dp))
                Text(statusPembayaran(booking.status), style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            StatusBadge(status = statusBooking(booking.status))
        }
    }
}

@Composable
private fun NotificationInboxRow(notification: NotificationModel, onClick: () -> Unit) {
    val unread = notification.readAt == null
    PremiumCard(modifier = Modifier.fillMaxWidth(), elevation = if (unread) 5.dp else 2.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(if (unread) AppColors.PrimarySoft else AppColors.SurfaceVariant), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = if (unread) AppColors.Primary else AppColors.TextSecondary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notification.title, style = MaterialTheme.typography.titleSmall, fontWeight = if (unread) FontWeight.W700 else FontWeight.W500, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text(waktuInbox(notification.createdAt?.toDate()?.time), style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
                }
                Spacer(Modifier.height(3.dp))
                Text(notification.body, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (unread) Box(Modifier.size(8.dp).clip(CircleShape).background(AppColors.Primary))
        }
    }
}

private fun waktuInbox(millis: Long?): String {
    if (millis == null || millis <= 0L) return ""
    val tanggal = Date(millis)
    val formatHari = SimpleDateFormat("yyyyMMdd", Locale("in", "ID"))
    val format = if (formatHari.format(Date()) == formatHari.format(tanggal)) {
        SimpleDateFormat("HH:mm", Locale("in", "ID"))
    } else {
        SimpleDateFormat("d MMM", Locale("in", "ID"))
    }
    return format.format(tanggal)
}

private fun statusBooking(status: String): String = when (status) {
    "pending_payment" -> "Menunggu pembayaran"
    "paid" -> "Sudah dibayar"
    "confirmed" -> "Dikonfirmasi"
    "upcoming" -> "Akan datang"
    "in_progress" -> "Sedang berjalan"
    "completed" -> "Selesai"
    "cancelled" -> "Dibatalkan"
    "rejected" -> "Ditolak"
    else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun statusPembayaran(status: String): String = when (status) {
    "draft" -> "Pembayaran: Belum dibuat"
    "pending_payment" -> "Pembayaran: Menunggu"
    "paid", "confirmed", "upcoming", "in_progress", "completed", "reviewed", "funds_released" -> "Pembayaran: Lunas"
    "cancelled", "rejected", "refund_requested" -> "Pembayaran: Tidak aktif"
    else -> "Pembayaran: Perlu ditinjau"
}
