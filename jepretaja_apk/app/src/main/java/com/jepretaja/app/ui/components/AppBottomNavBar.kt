package com.jepretaja.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.theme.AppSpacing

data class NavTab(val route: String, val label: String, val icon: ImageVector, val iconOutline: ImageVector)

/**
 * Bottom nav datar dengan ritme sederhana seperti Telegram: menempel di bawah
 * layar, tanpa kartu melayang, tanpa tombol bulat besar, dan tiap menu punya
 * bobot visual yang sama.
 *
 * [centerAction] adalah tombol aksi di TENGAH deretan menu. Tamu tidak
 * mengirimkan aksi ini sehingga tetap memiliki empat menu; konsumen memakai
 * Booking, sedangkan creator memakai Unggah Konten.
 */
@Composable
fun AppBottomNavBar(
    tabs: List<NavTab>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    centerAction: (() -> Unit)? = null,
    centerActionLabel: String = "Unggah",
    centerActionIcon: ImageVector = Icons.Default.Add,
) {
    Box(
        modifier
            .fillMaxWidth()
            .zIndex(100f)
            .background(AppColors.Background)
            .navigationBarsPadding()
            .padding(top = 4.dp, bottom = 3.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(AppColors.Surface)
                .padding(horizontal = AppSpacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Aksi utama disisipkan tepat di tengah deretan, bukan ditempel di
            // ujung — posisinya yang simetris membuatnya terbaca sebagai aksi
            // utama, bukan sekadar menu kelima.
            val middle = tabs.size / 2
            tabs.forEachIndexed { index, tab ->
                if (centerAction != null && index == middle) {
                    CenterActionItem(
                        label = centerActionLabel,
                        icon = centerActionIcon,
                        onClick = centerAction,
                        modifier = Modifier.weight(1f),
                    )
                }
                NavItem(
                    tab = tab,
                    selected = currentRoute == tab.route,
                    onClick = { onSelect(tab.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavItem(tab: NavTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val contentColor = if (selected) AppColors.Primary else AppColors.TextSecondary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick,
            )
            .padding(horizontal = 2.dp, vertical = 2.dp),
    ) {
        Icon(
            if (selected) tab.icon else tab.iconOutline,
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier.size(23.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = contentColor, maxLines = 1)
    }
}

/** Aksi tengah bergaya Telegram: menonjol lewat warna, bukan tombol bulat. */
@Composable
private fun CenterActionItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick,
            )
            .padding(horizontal = 2.dp, vertical = 2.dp),
    ) {
        Icon(icon, contentDescription = label, tint = AppColors.Primary, modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.Primary, maxLines = 1)
    }
}
