package com.jepretaja.app.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jepretaja.app.data.model.BookingModel
import com.jepretaja.app.data.model.ChatModel
import com.jepretaja.app.data.model.NotificationModel
import com.jepretaja.app.data.repository.BookingRepository
import com.jepretaja.app.data.repository.ChatRepository
import com.jepretaja.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxState(
    val chats: List<ChatModel> = emptyList(),
    val unreadByChat: Map<String, Int> = emptyMap(),
    val bookings: List<BookingModel> = emptyList(),
    val notifications: List<NotificationModel> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val bookingRepository: BookingRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    @Suppress("UNUSED_PARAMETER")
    fun observe(userId: String, retryKey: Int): StateFlow<InboxState> {
        val customerBookings = bookingRepository.streamCustomerBookings(userId)
        val creatorBookings = bookingRepository.streamCreatorBookings(userId)
        val bookings = combine(customerBookings, creatorBookings) { mine, incoming ->
            (mine + incoming).distinctBy { it.bookingId }.sortedByDescending {
                it.createdAt?.toDate()?.time ?: it.date?.toDate()?.time ?: 0L
            }
        }
        return combine(
            chatRepository.streamChats(userId),
            chatRepository.streamUnreadPerChat(userId),
            bookings,
            notificationRepository.stream(userId),
        ) { chats, unread, bookingList, notifications ->
            InboxState(chats, unread, bookingList, notifications, loading = false)
        }.onStart { emit(InboxState()) }
            .catch { emit(InboxState(loading = false, error = "Kotak masuk gagal dimuat.")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxState())
    }

    fun markRead(notificationId: String) {
        viewModelScope.launch { runCatching { notificationRepository.markRead(notificationId) } }
    }
}