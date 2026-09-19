package com.jepretaja.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.data.model.IndonesianLocations
import com.jepretaja.app.ui.components.BigPrimaryButton
import com.jepretaja.app.ui.components.LocationField
import com.jepretaja.app.ui.components.PremiumCard
import com.jepretaja.app.ui.state.AuthActionsViewModel

/** Setelah register, creator berstatus verified=false sampai dokumen
 * diverifikasi Admin (section 8, 14, 20). */
@Composable
fun RegisterCreatorScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthActionsViewModel = hiltViewModel(),
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var termsAccepted by remember { mutableStateOf(false) }
    // Sama seperti RegisterCustomerScreen: baru menandai field kosong dengan
    // merah setelah tombol Daftar pertama kali ditekan.
    var submitAttempted by remember { mutableStateOf(false) }
    var provinceExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    val provinceOptions = remember { IndonesianLocations.provinces }
    val cityOptions = remember(province) { IndonesianLocations.citiesForProvince(province) }

    LaunchedEffect(province) {
        if (province.isBlank()) {
            return@LaunchedEffect
        }
    }

    // SEMUA field wajib diisi — sebelumnya hanya nama, email, dan password
    // yang dicek, sehingga creator bisa lolos daftar tanpa No. HP, kota,
    // provinsi, atau alamat studio, padahal itu semua dibutuhkan klien untuk
    // booking dan tim verifikasi untuk menghubunginya.
    val formReady = name.isNotBlank() && email.contains("@") && phone.isNotBlank() &&
        province.isNotBlank() && city.isNotBlank() && address.isNotBlank() &&
        password.length >= 8 && termsAccepted

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            com.jepretaja.app.ui.components.AppTopBar(title = "", onBack = onBack)
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(AppColors.PrimarySoft),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(27.dp)) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Bergabung Sebagai Creator", style = MaterialTheme.typography.headlineMedium, color = AppColors.TextPrimary)
                    Text("Bangun profil profesionalmu", style = MaterialTheme.typography.labelMedium, color = AppColors.Primary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Tampilkan portofoliomu dan mulai menerima booking dari klien di seluruh Indonesia.",
                style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary,
            )
            Spacer(Modifier.height(22.dp))
            Text("Profil awal", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                name, { name = it }, label = { Text("Nama / Nama Studio *") }, singleLine = true,
                isError = submitAttempted && name.isBlank(),
                supportingText = { if (submitAttempted && name.isBlank()) Text("Wajib diisi.", color = AppColors.Danger) },
                shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                email, { email = it }, label = { Text("Email *") }, singleLine = true,
                isError = submitAttempted && !email.contains("@"),
                supportingText = { if (submitAttempted && !email.contains("@")) Text("Wajib diisi dengan format email yang benar.", color = AppColors.Danger) },
                shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                phone, { phone = it }, label = { Text("No. HP *") }, singleLine = true,
                isError = submitAttempted && phone.isBlank(),
                supportingText = { if (submitAttempted && phone.isBlank()) Text("Wajib diisi.", color = AppColors.Danger) },
                shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            ExposedDropdownMenuBox(
                expanded = provinceExpanded,
                onExpandedChange = { provinceExpanded = it },
            ) {
                OutlinedTextField(
                    value = province,
                    onValueChange = { province = it },
                    label = { Text("Provinsi *") },
                    singleLine = true,
                    isError = submitAttempted && province.isBlank(),
                    supportingText = { if (submitAttempted && province.isBlank()) Text("Wajib dipilih.", color = AppColors.Danger) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = provinceExpanded,
                    onDismissRequest = { provinceExpanded = false },
                ) {
                    provinceOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                province = option
                                provinceExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            ExposedDropdownMenuBox(
                expanded = cityExpanded,
                onExpandedChange = { if (cityOptions.isNotEmpty()) cityExpanded = it },
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Kota Domisili *") },
                    singleLine = true,
                    enabled = true,
                    isError = submitAttempted && city.isBlank(),
                    supportingText = { if (submitAttempted && city.isBlank()) Text("Wajib dipilih.", color = AppColors.Danger) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = cityExpanded && cityOptions.isNotEmpty(),
                    onDismissRequest = { cityExpanded = false },
                ) {
                    cityOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                city = option
                                cityExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LocationField(
                value = address,
                onValueChange = { address = it },
                label = "Alamat Studio / Domisili *",
                placeholder = "Pilih dari lokasi saya atau ketik alamat lengkap",
                isError = submitAttempted && address.isBlank(),
                errorText = if (submitAttempted && address.isBlank()) "Wajib diisi." else null,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                password, { password = it }, label = { Text("Password *") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true,
                isError = submitAttempted && password.length < 8,
                supportingText = { if (submitAttempted && password.length < 8) Text("Minimal 8 karakter.", color = AppColors.Danger) },
                shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text("Profil awal", style = MaterialTheme.typography.labelLarge, color = AppColors.Primary)
            Text("Setelah akun dibuat, lengkapi portfolio, layanan, harga, jadwal, rekening, dan verifikasi dari Creator Studio.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(termsAccepted, { termsAccepted = it })
                Text("Saya menyetujui Terms dan Privacy Policy *", style = MaterialTheme.typography.bodySmall)
            }
            if (submitAttempted && !termsAccepted) {
                Text("Wajib disetujui untuk melanjutkan.", style = MaterialTheme.typography.labelSmall, color = AppColors.Danger)
            }
            Spacer(Modifier.height(16.dp))
            PremiumCard(modifier = Modifier.fillMaxWidth(), elevation = 2.dp, color = AppColors.PrimarySoft) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Setelah daftar, lengkapi dokumen verifikasi di Creator Settings agar akun terverifikasi.",
                        color = AppColors.TextSecondary, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall) }
            BigPrimaryButton(
                text = if (loading) "Memproses..." else "Daftar sebagai Creator",
                loading = loading,
                enabled = !loading,
                onClick = {
                    submitAttempted = true
                    if (!formReady) {
                        error = "Lengkapi semua data bertanda (*) sebelum mendaftar."
                        return@BigPrimaryButton
                    }
                    loading = true; error = null
                    viewModel.registerCreator(name.trim(), email.trim(), password, city.trim(), phone.trim(), address.trim(), province.trim(), onSuccess = { loading = false; onSuccess() }, onError = { loading = false; error = it })
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
