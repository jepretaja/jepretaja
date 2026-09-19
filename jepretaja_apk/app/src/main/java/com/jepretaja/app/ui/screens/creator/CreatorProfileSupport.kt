package com.jepretaja.app.ui.screens.creator

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.util.Formatters
import com.jepretaja.app.data.model.CreatorModel
import com.jepretaja.app.data.model.PortfolioModel
import com.jepretaja.app.data.model.ReviewModel
import com.jepretaja.app.ui.components.AppAvatar
import com.jepretaja.app.ui.components.AppTopBar
import com.jepretaja.app.ui.components.EmptyState
import com.jepretaja.app.ui.components.PremiumCard
import com.jepretaja.app.ui.components.SkeletonBox
import com.jepretaja.app.ui.components.SkeletonList
import com.jepretaja.app.ui.state.AuthViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/* --------------------------------------------------------------------------
 * Sisanya
 * ----------------------------------------------------------------------- */

@Composable
internal fun KerangkaProfil(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(16f / 9f), RoundedCornerShape(0.dp))
        Column(Modifier.padding(20.dp)) {
            SkeletonBox(Modifier.size(108.dp), CircleShape)
            Spacer(Modifier.height(14.dp))
            SkeletonBox(Modifier.width(180.dp).height(24.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.width(110.dp).height(14.dp))
            Spacer(Modifier.height(16.dp))
            SkeletonBox(Modifier.fillMaxWidth().height(120.dp), RoundedCornerShape(18.dp))
        }
        SkeletonList(count = 3)
    }
}

/** Online = aktif dalam 2 menit terakhir. */
internal fun sedangOnline(c: CreatorModel): Boolean {
    val terakhir = c.lastActiveAt?.toDate()?.time ?: return false
    return System.currentTimeMillis() - terakhir < 2 * 60 * 1000
}

internal fun statusKehadiran(c: CreatorModel): String {
    val terakhir = c.lastActiveAt?.toDate()?.time ?: return "Belum pernah aktif"
    val menit = (System.currentTimeMillis() - terakhir) / 60000
    return when {
        menit < 2 -> "Online"
        menit < 60 -> "Aktif $menit menit lalu"
        menit < 60 * 24 -> "Aktif ${menit / 60} jam lalu"
        else -> "Aktif ${menit / 1440} hari lalu"
    }
}

/**
 * Persentase booking yang selesai.
 *
 * Null kalau belum ada booking sama sekali — menampilkan "0% selesai" pada
 * creator baru akan menghukumnya karena belum sempat bekerja, bukan karena
 * kinerjanya buruk.
 */
internal fun tingkatPenyelesaian(c: CreatorModel): Int? {
    if (c.totalBookings < 3) return null
    return (c.completedBookings * 100 / c.totalBookings).coerceIn(0, 100)
}

internal fun durasiSingkat(menit: Int): String = when {
    menit < 60 -> "< $menit menit"
    menit < 60 * 24 -> "< ${menit / 60} jam"
    else -> "> 1 hari"
}
