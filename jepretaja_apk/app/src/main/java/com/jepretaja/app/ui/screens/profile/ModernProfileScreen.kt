package com.jepretaja.app.ui.screens.profile

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.theme.AppSpacing
import com.jepretaja.app.data.model.BookingModel
import com.jepretaja.app.data.model.ExplorePostModel
import com.jepretaja.app.ui.components.AppAvatar
import com.jepretaja.app.ui.components.EmptyState
import com.jepretaja.app.ui.components.GradientHeroCard
import com.jepretaja.app.ui.components.PremiumCard
import com.jepretaja.app.ui.components.QrCode
import com.jepretaja.app.ui.components.SkeletonPostTile
import com.jepretaja.app.ui.components.StatusBadge
import com.jepretaja.app.ui.state.AuthViewModel
import kotlinx.coroutines.flow.flowOf

private enum class ProfileCollection(val label: String) {
    POSTS("Posts"), MEDIA("Media"), SAVED("Saved"), BOOKINGS("Bookings"), REVIEWS("Reviews"), FAVORITES("Favorites"), FOLLOWERS("Followers"), FOLLOWING("Following"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernProfileScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onMyBookings: () -> Unit,
    onFavorites: () -> Unit,
    onMyReviews: () -> Unit,
    onReports: () -> Unit,
    onNotifications: () -> Unit,
    onHelp: () -> Unit,
    onChat: () -> Unit,
    onCreatorStudio: () -> Unit,
    onInterests: () -> Unit,
    onWatchHistory: () -> Unit,
    onFollowList: (Int) -> Unit,
    onMyWorks: () -> Unit,
    onSaved: () -> Unit,
    onLoggedOut: () -> Unit,
    onSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onPostClick: (String) -> Unit,
    onCreateWork: () -> Unit,
    authViewModel: AuthViewModel,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsState()
    val uid = authState.uid
    val creator by remember(uid, authState.isCreator) {
        if (uid != null && authState.isCreator) viewModel.creator(uid) else flowOf(null)
    }.collectAsState(initial = null)
    val posts by remember(uid, authState.isCreator) {
        if (uid != null && authState.isCreator) viewModel.myPosts(uid) else flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val saved by remember(uid) { if (uid != null) viewModel.savedPosts(uid) else flowOf(emptyList()) }
        .collectAsState(initial = emptyList())
    val liked by remember(uid) { if (uid != null) viewModel.likedPosts(uid) else flowOf(emptyList()) }
        .collectAsState(initial = emptyList())
    val bookings by remember(uid) {
        if (uid != null && !authState.isCreator) viewModel.myBookings(uid) else flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val followingCount by remember(uid) {
        if (uid != null) viewModel.followingCount(uid) else flowOf(0)
    }.collectAsState(initial = 0)

    var selected by rememberSaveable { mutableStateOf(ProfileCollection.POSTS) }
    var showShare by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(containerColor = AppColors.Background) { padding ->
        if (!authState.isLoggedIn) {
            GuestProfileModern(modifier = Modifier.padding(padding), onLogin = onLogin, onRegister = onRegister)
            return@Scaffold
        }

        val name = authState.profile?.name?.takeIf { it.isNotBlank() } ?: "Pengguna JepretAja"
        val username = "@" + name.lowercase().replace("[^a-z0-9]".toRegex(), "").take(20).ifBlank { "pengguna" }
        val collection = when (selected) {
            ProfileCollection.POSTS, ProfileCollection.MEDIA -> posts
            ProfileCollection.SAVED -> saved
            else -> emptyList()
        }
        val likes = if (authState.isCreator) posts.sumOf { it.likeCount } else liked.size.toLong()
        val followerCount = creator?.followerCount ?: 0
        val bookingCount = if (authState.isCreator) creator?.totalBookings ?: 0 else bookings.size

        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = { showShare = true }) {
                    Icon(Icons.Default.Share, contentDescription = "Bagikan profil", tint = AppColors.TextPrimary)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = AppColors.TextPrimary)
                }
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AppAvatar(url = authState.profile?.photoUrl, name = name, size = 84.dp, verified = creator?.verified == true)
                Spacer(Modifier.height(AppSpacing.sm))
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W700, color = AppColors.TextPrimary)
                Text(username, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                if (creator?.verified == true) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = AppColors.Info, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Creator Terverifikasi", style = MaterialTheme.typography.labelMedium, color = AppColors.Info)
                    }
                }
                creator?.bio?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(AppSpacing.sm))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = AppSpacing.xxl))
                }
                Spacer(Modifier.height(AppSpacing.md))
                Row(Modifier.padding(horizontal = AppSpacing.xl), horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    OutlinedButton(onClick = onEditProfile, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(42.dp)) {
                        Text("Edit Profil", style = MaterialTheme.typography.labelLarge)
                    }
                    if (authState.isCreator) {
                        Button(onClick = onCreateWork, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(42.dp)) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Upload Karya", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))
            Row(Modifier.fillMaxWidth().padding(horizontal = AppSpacing.xl), horizontalArrangement = Arrangement.SpaceEvenly) {
                ProfileMetric("$followerCount", "Pengikut")
                ProfileMetric("$followingCount", "Mengikuti")
                ProfileMetric("$bookingCount", "Booking")
            }

            creator?.let { profile ->
                Spacer(Modifier.height(AppSpacing.md))
                Column(Modifier.padding(horizontal = AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (profile.categories.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkOutline, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(profile.categories.joinToString(" & "), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
                        }
                    }
                    profile.city?.takeIf { it.isNotBlank() }?.let { city ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(city, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))
            ScrollableTabRow(selectedTabIndex = selected.ordinal, edgePadding = AppSpacing.sm, containerColor = Color.Transparent, divider = { HorizontalDivider(color = AppColors.Border) }) {
                ProfileCollection.entries.forEach { tab ->
                    Tab(selected = selected == tab, onClick = {
                        when (tab) {
                            ProfileCollection.FAVORITES -> onFavorites()
                            ProfileCollection.REVIEWS -> onMyReviews()
                            ProfileCollection.FOLLOWERS -> onFollowList(1)
                            ProfileCollection.FOLLOWING -> onFollowList(0)
                            else -> selected = tab
                        }
                    }, text = { Text(tab.label, style = MaterialTheme.typography.labelLarge) }, icon = { Icon(if (tab == ProfileCollection.POSTS || tab == ProfileCollection.MEDIA) Icons.Default.GridView else if (tab == ProfileCollection.SAVED) Icons.Default.BookmarkBorder else if (tab == ProfileCollection.FAVORITES) Icons.Default.FavoriteBorder else if (tab == ProfileCollection.FOLLOWERS || tab == ProfileCollection.FOLLOWING) Icons.Default.People else Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) })
                }
            }

            Spacer(Modifier.height(AppSpacing.md))
            if (selected == ProfileCollection.BOOKINGS) {
                BookingCollection(bookings = bookings, onOpen = onMyBookings)
            } else if (selected == ProfileCollection.MEDIA) {
                SocialCollectionAction("Media", "Video dan media visual yang kamu bagikan akan muncul di sini.", onPostClick = { posts.firstOrNull()?.let { onPostClick(it.postId) } })
            } else if (selected == ProfileCollection.REVIEWS) {
                SocialCollectionAction("Reviews", "Lihat semua review dan pengalaman booking kamu.", onPostClick = { onMyReviews() })
            } else if (selected == ProfileCollection.FAVORITES) {
                SocialCollectionAction("Favorites", "Creator dan package favoritmu tersimpan di marketplace.", onPostClick = onFavorites)
            } else if (selected == ProfileCollection.FOLLOWERS) {
                SocialCollectionAction("Followers", "Lihat orang yang mengikuti profilmu.", onPostClick = { onFollowList(1) })
            } else if (selected == ProfileCollection.FOLLOWING) {
                SocialCollectionAction("Following", "Lihat creator yang kamu ikuti.", onPostClick = { onFollowList(0) })
            } else if (collection.isEmpty()) {
                EmptyState(
                    icon = if (selected == ProfileCollection.SAVED) Icons.Default.BookmarkBorder else if (selected == ProfileCollection.FAVORITES) Icons.Default.FavoriteBorder else Icons.Default.GridView,
                    title = if (selected == ProfileCollection.POSTS) "Belum ada karya" else "Belum ada konten",
                    description = if (selected == ProfileCollection.POSTS) "Mulai bagikan karya pertama kamu di JepretAja." else "Konten yang kamu pilih akan muncul di sini.",
                    actionLabel = if (selected == ProfileCollection.POSTS && authState.isCreator) "+ Buat Karya" else null,
                    onAction = if (selected == ProfileCollection.POSTS && authState.isCreator) onCreateWork else null,
                )
            } else {
                PostGrid(posts = collection, onPostClick = onPostClick)
            }
            Spacer(Modifier.height(AppSpacing.xxxl))
        }

        if (showShare) {
            val link = if (authState.isCreator && uid != null) "https://jepretaja.app/creator/$uid" else "https://jepretaja.app"
            AlertDialog(
                onDismissRequest = { showShare = false },
                title = { Text("Bagikan profil") },
                text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { QrCode(content = link, modifier = Modifier.size(180.dp)); Spacer(Modifier.height(AppSpacing.sm)); Text(link, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, textAlign = TextAlign.Center) } },
                confirmButton = { TextButton(onClick = { val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Lihat profilku di JepretAja: $link") }; runCatching { context.startActivity(Intent.createChooser(send, "Bagikan lewat")) }; showShare = false }) { Text("Bagikan") } },
                dismissButton = { TextButton(onClick = { showShare = false }) { Text("Tutup") } },
            )
        }
    }
}

@Composable
private fun SocialCollectionAction(title: String, description: String, onPostClick: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.xl), elevation = 3.dp, onClick = onPostClick) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
        Spacer(Modifier.height(5.dp))
        Text(description, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        Spacer(Modifier.height(10.dp))
        Text("Buka koleksi  →", style = MaterialTheme.typography.labelLarge, color = AppColors.Primary)
    }
}

@Composable
private fun RowScope.ProfileMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 52.dp).weight(1f)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W700, color = AppColors.TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary, maxLines = 1)
    }
}

@Composable
private fun GuestProfileModern(modifier: Modifier, onLogin: () -> Unit, onRegister: () -> Unit) {
    Column(modifier.fillMaxSize().padding(AppSpacing.xxl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AppAvatar(url = null, name = "?", size = 76.dp)
        Spacer(Modifier.height(AppSpacing.lg))
        Text("Masuk untuk membuka profil", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
        Spacer(Modifier.height(AppSpacing.sm))
        Text("Simpan karya, ikuti creator, dan lihat aktivitas booking kamu.", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(AppSpacing.lg))
        Button(onClick = onLogin, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Masuk") }
        Spacer(Modifier.height(AppSpacing.sm))
        OutlinedButton(onClick = onRegister, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Daftar") }
    }
}

@Composable
private fun PostGrid(posts: List<ExplorePostModel>, onPostClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        posts.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { post ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.SurfaceVariant)
                            .clickable { onPostClick(post.postId) },
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(post.thumbnailUrl ?: post.mediaUrls.firstOrNull())
                                .size(480, 270)
                                .crossfade(false)
                                .build(),
                            contentDescription = post.caption,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Row(Modifier.align(Alignment.BottomStart).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("${post.likeCount}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun BookingCollection(bookings: List<BookingModel>, onOpen: () -> Unit) {
    if (bookings.isEmpty()) {
        EmptyState(icon = Icons.Default.CalendarMonth, title = "Belum ada booking", description = "Riwayat booking kamu akan muncul di sini.", actionLabel = "Lihat Booking", onAction = onOpen)
    } else {
        Column(Modifier.padding(horizontal = AppSpacing.xl), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            bookings.take(5).forEach { booking ->
                PremiumCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(AppSpacing.md), onClick = onOpen) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(booking.packageName, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary); Text(booking.time, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary) }
                        StatusBadge(booking.status)
                    }
                }
            }
        }
    }
}

