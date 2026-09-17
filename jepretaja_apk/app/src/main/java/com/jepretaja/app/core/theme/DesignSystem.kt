package com.jepretaja.app.core.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Token layout bersama supaya jarak antar layar tidak dipilih secara ad hoc. */
object AppSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
}

/** Token radius bersama untuk kartu, kontrol, dan tombol. */
object AppRadii {
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val large: Dp = 16.dp
    val pill: Dp = 999.dp
}

/** Tingkatan elevasi yang sengaja dibatasi agar hierarki visual konsisten. */
object AppElevation {
    val card: Dp = 4.dp
    val raised: Dp = 8.dp
    val hero: Dp = 12.dp
}
