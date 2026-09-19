package com.jepretaja.app.ui.screens.root

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jepretaja.app.core.navigation.Routes
import com.jepretaja.app.ui.components.AppBottomNavBar
import com.jepretaja.app.ui.components.FiturButuhAkun
import com.jepretaja.app.ui.components.LoginRequiredSheet
import com.jepretaja.app.ui.components.NavTab
import com.jepretaja.app.ui.screens.explore.ExploreScreen
import com.jepretaja.app.ui.screens.home.HomeScreen
import com.jepretaja.app.ui.screens.inbox.InboxScreen
import com.jepretaja.app.ui.screens.profile.ProfileScreen
import com.jepretaja.app.ui.screens.search.SearchFilters
import com.jepretaja.app.ui.state.AuthViewModel
import com.jepretaja.app.ui.copy.LocalJepretAjaCopy

/**
 * Menu bawah mengikuti peran:
 * - tamu: Beranda | Jelajahi | Pesan | Profil
 * - konsumen/creator: Beranda | Jelajahi | aksi utama | Pesan | Profil
 *
 * Chat sengaja tidak lagi menempati satu slot di sini — ia pindah ke pojok
 * kanan atas layar Home. Alasannya: chat adalah tempat yang dituju setelah ada
 * urusan (booking, tanya paket), bukan tujuan sehari-hari seperti empat menu
 * ini. Slot tengah dipakai untuk aksi utama sesuai peran pengguna.
 */
private fun mainTabs(copy: com.jepretaja.app.ui.copy.RoleCopy) = listOf(
    NavTab("tab_home", copy.navHome, Icons.Filled.Home, Icons.Outlined.Home),
    NavTab("tab_explore", copy.navExplore, Icons.Filled.Explore, Icons.Outlined.Explore),
    NavTab("tab_inbox", copy.navInbox, Icons.Filled.Markunread, Icons.Outlined.Markunread),
    NavTab("tab_profile", copy.navProfile, Icons.Filled.Person, Icons.Outlined.Person),
)

/**
 * Shell utama aplikasi, dipakai konsumen, tamu, MAUPUN creator.
 *
 * Semua peran memakai kerangka visual yang sama. Tamu tidak mendapat tombol
 * tengah karena belum punya aksi akun; konsumen mendapat Booking, sedangkan
 * creator mendapat Unggah Konten.
 */
@Composable
fun CustomerRootShell(
    outerNavController: NavHostController,
    authViewModel: AuthViewModel,
) {
    val innerNav = rememberNavController()
    val backStackEntry by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val copy = LocalJepretAjaCopy.current

    // Denyut kehadiran: cukup dipanggil saat shell tampil dan tiap kali akun
    // berubah — pembatas 5 menit di ViewModel yang memutuskan perlu-tidaknya
    // benar-benar menulis.
    LaunchedEffect(authState.uid, authState.isCreator) { authViewModel.tandaiKehadiran() }
    // Menu yang sedang diminta tamu, atau null bila tidak ada ajakan masuk yang
    // sedang tampil. Splash baru melepas pengguna ke sini setelah status auth
    // selesai dibaca, jadi `isLoggedIn == false` di titik ini benar-benar
    // berarti tamu — bukan sekadar "belum sempat dimuat".
    var mintaMasuk by remember { mutableStateOf<FiturButuhAkun?>(null) }
    val tamu = !authState.isLoggedIn

    // Home, Explore, dan Profile tetap terbuka untuk tamu: justru itu yang
    // membuat orang mau mendaftar. Yang dijaga hanya menu yang isinya memang
    // milik satu akun.
    fun bukaBooking() {
        if (tamu) mintaMasuk = FiturButuhAkun.BOOKING else outerNavController.navigate(Routes.MY_BOOKINGS)
    }

    fun bukaChat() {
        if (tamu) mintaMasuk = FiturButuhAkun.CHAT else outerNavController.navigate(Routes.CHAT_LIST)
    }

    fun bukaNotifikasi() {
        if (tamu) mintaMasuk = FiturButuhAkun.NOTIFIKASI else outerNavController.navigate(Routes.NOTIFICATIONS)
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            innerNav,
            startDestination = "tab_home",
            modifier = Modifier.fillMaxSize().padding(bottom = 92.dp),
        ) {
            composable("tab_home") {
                HomeScreen(
                    onSearch = { outerNavController.navigate(Routes.SEARCH) },
                    onNotifications = { bukaNotifikasi() },
                    onChat = { bukaChat() },
                    onCreatorClick = { id -> outerNavController.navigate(Routes.creatorProfile(id)) },
                    onCategoryClick = { category ->
                        outerNavController.currentBackStackEntry?.savedStateHandle?.set("search_filters", SearchFilters(categories = listOf(category)))
                        outerNavController.navigate(Routes.SEARCH_RESULT)
                    },
                    onSeeExplore = { innerNav.navigate("tab_explore") },
                    onBookingClick = { id -> outerNavController.navigate(Routes.bookingDetail(id)) },
                    // Home adalah etalase; klik karya membuka Explore, bukan
                    // langsung masuk ke layar komentar.
                    onPostClick = { innerNav.navigate("tab_explore") },
                    authViewModel = authViewModel,
                )
            }
            composable("tab_explore") {
                ExploreScreen(
                    authViewModel = authViewModel,
                    onLoginRequired = { outerNavController.navigate(Routes.LOGIN) },
                    onSearch = { outerNavController.navigate(Routes.SEARCH) },
                    onCreatorClick = { id -> outerNavController.navigate(Routes.creatorProfile(id)) },
                    onTagClick = { tag, isCategory ->
                        outerNavController.navigate(Routes.tagFeed(tag, isCategory))
                    },
                    onPackageClick = { id -> outerNavController.navigate(Routes.packageDetail(id)) },
                )
            }
            composable("tab_inbox") {
                InboxScreen(
                    onChatClick = { id -> outerNavController.navigate(Routes.chatRoom(id)) },
                    onBookingClick = { id -> outerNavController.navigate(Routes.bookingDetail(id)) },
                    onLogin = { outerNavController.navigate(Routes.LOGIN) },
                    onRegister = { outerNavController.navigate(Routes.REGISTER_CUSTOMER) },
                    authViewModel = authViewModel,
                )
            }
            composable("tab_profile") {
                ProfileScreen(
                    onLogin = { outerNavController.navigate(Routes.LOGIN) },
                    onRegister = { outerNavController.navigate(Routes.REGISTER_CUSTOMER) },
                    onSettings = { outerNavController.navigate(Routes.SETTINGS) },
                    onEditProfile = { outerNavController.navigate(Routes.EDIT_PROFILE) },
                    onSaved = { outerNavController.navigate(Routes.SAVED_POSTS) },
                    onMyBookings = { bukaBooking() },
                    onFavorites = { outerNavController.navigate(Routes.FAVORITES) },
                    onMyReviews = { outerNavController.navigate(Routes.MY_REVIEWS) },
                    onReports = { outerNavController.navigate(Routes.MY_REPORTS) },
                    onNotifications = { bukaNotifikasi() },
                    onHelp = { outerNavController.navigate(Routes.HELP) },
                    onChat = { bukaChat() },
                    onCreatorStudio = { outerNavController.navigate(Routes.CREATOR_DASHBOARD) },
                    onCreateWork = { outerNavController.navigate(Routes.CREATOR_UPLOAD) },
                    // Ubin di grid profil membuka postnya, sama seperti profil TikTok.
                    onPostClick = { postId -> outerNavController.navigate(Routes.exploreDetail(postId)) },
                    onMyWorks = { outerNavController.navigate(Routes.MY_WORKS) },
                    onPortfolio = { outerNavController.navigate(Routes.CREATOR_PORTFOLIO_MANAGEMENT) },
                    onInterests = { outerNavController.navigate(Routes.INTERESTS) },
                    onWatchHistory = { outerNavController.navigate(Routes.WATCH_HISTORY) },
                    onFollowList = { tab ->
                        authState.uid?.let { outerNavController.navigate(Routes.followList(it, tab)) }
                    },
                    onLoggedOut = { outerNavController.navigate(Routes.CHOOSE_ACCESS) { popUpTo(0) } },
                    authViewModel = authViewModel,
                )
            }
        }

        // Explore memakai VerticalPager fullscreen. Bottom nav sengaja berada
        // di layer teratas agar area sentuh menu tetap aktif di atas feed.
        AppBottomNavBar(
            tabs = mainTabs(copy),
            currentRoute = currentRoute,
            onSelect = { route ->
                innerNav.navigate(route) {
                    popUpTo("tab_home") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(20f),
            // Tamu memakai empat menu saja. Konsumen dan creator mendapat satu
            // aksi utama di tengah dengan ikon serta label yang sesuai.
            centerAction = when {
                tamu -> null
                authState.isCreator -> { { outerNavController.navigate(Routes.CREATOR_UPLOAD) } }
                else -> { { outerNavController.navigate(Routes.SEARCH) } }
            },
            centerActionLabel = copy.centerAction,
            centerActionIcon = if (authState.isCreator) Icons.Default.AddAPhoto else Icons.Default.EventAvailable,
        )
    }

    mintaMasuk?.let { fitur ->
        LoginRequiredSheet(
            fitur = fitur,
            onDismiss = { mintaMasuk = null },
            // Lembar ditutup DULU baru berpindah: kalau tidak, ia masih
            // menempel di layar masuk dan ikut terlihat lagi saat pengguna
            // menekan tombol kembali.
            onLogin = { mintaMasuk = null; outerNavController.navigate(Routes.LOGIN) },
            onRegister = { mintaMasuk = null; outerNavController.navigate(Routes.REGISTER_CUSTOMER) },
        )
    }

}
