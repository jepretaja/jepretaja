package com.jepretaja.app.ui.screens.profile

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.jepretaja.app.ui.state.AuthViewModel

/**
 * Profil pengguna — titik masuk publik dipanggil dari CustomerRootShell.
 *
 * **Kenapa cuma pemanggil.** File ini sebelumnya menyimpan ~1.250 baris
 * implementasi lama (state, dialog, grid post, semua composable privat) di
 * BAWAH sebuah `return` tanpa syarat — jadi seluruhnya kode mati yang tidak
 * pernah tereksekusi maupun dipakai file lain (fungsi-fungsi itu semua
 * `private`). Implementasi yang sungguh berjalan sudah lama dipindah ke
 * ModernProfileScreen. Dead code itu dihapus di sini; kalau butuh riwayat
 * implementasi lama, lihat versi sebelumnya di git history.
 */
@Composable
fun ProfileScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onMyBookings: () -> Unit,
    onFavorites: () -> Unit,
    onMyReviews: () -> Unit,
    onReports: () -> Unit,
    onNotifications: () -> Unit,
    onHelp: () -> Unit,
    onLoggedOut: () -> Unit,
    authViewModel: AuthViewModel,
    onSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onSaved: () -> Unit,
    onChat: () -> Unit,
    onCreatorStudio: () -> Unit,
    onCreateWork: () -> Unit,
    onPostClick: (String) -> Unit,
    /** [tab]: 0 = Mengikuti, 1 = Pengikut. */
    onFollowList: (Int) -> Unit,
    onMyWorks: () -> Unit,
    onPortfolio: () -> Unit,
    onInterests: () -> Unit,
    onWatchHistory: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    ModernProfileScreen(
        onLogin = onLogin,
        onRegister = onRegister,
        onMyBookings = onMyBookings,
        onFavorites = onFavorites,
        onMyReviews = onMyReviews,
        onReports = onReports,
        onNotifications = onNotifications,
        onHelp = onHelp,
        onChat = onChat,
        onCreatorStudio = onCreatorStudio,
        onInterests = onInterests,
        onWatchHistory = onWatchHistory,
        onFollowList = onFollowList,
        onMyWorks = onMyWorks,
        onPortfolio = onPortfolio,
        onSaved = onSaved,
        onLoggedOut = onLoggedOut,
        onSettings = onSettings,
        onEditProfile = onEditProfile,
        onPostClick = onPostClick,
        onCreateWork = onCreateWork,
        authViewModel = authViewModel,
        viewModel = viewModel,
    )
}
