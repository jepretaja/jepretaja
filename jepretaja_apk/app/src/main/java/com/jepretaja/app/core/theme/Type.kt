package com.jepretaja.app.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Satu keluarga sans-serif menjaga keterbacaan form, kartu, status, dan tombol
// tetap konsisten di berbagai ukuran layar tanpa gaya dekoratif pada headline.
private val AppFont = FontFamily.SansSerif

/** Skala tipografi lengkap (13 role) — sebelumnya hanya 5 role yang
 * dikustomisasi, sisanya diam-diam jatuh ke default Material3 sehingga
 * tampilan tidak konsisten antar layar. */
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W700, fontSize = 40.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W700, fontSize = 32.sp, lineHeight = 38.sp),
    displaySmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W700, fontSize = 26.sp, lineHeight = 32.sp),

    headlineLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W700, fontSize = 24.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W600, fontSize = 21.sp, lineHeight = 27.sp),
    headlineSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W600, fontSize = 19.sp, lineHeight = 25.sp),

    titleLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W800, fontSize = 21.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W700, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W700, fontSize = 15.sp, lineHeight = 21.sp),

    bodyLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W400, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W400, fontSize = 14.5.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W400, fontSize = 13.sp, lineHeight = 18.sp),

    labelLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W700, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W600, fontSize = 12.5.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.W600, fontSize = 11.5.sp, letterSpacing = 0.3.sp),
)
