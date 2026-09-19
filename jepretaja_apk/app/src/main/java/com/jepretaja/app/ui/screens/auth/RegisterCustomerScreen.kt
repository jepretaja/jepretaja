package com.jepretaja.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.data.model.IndonesianLocations
import com.jepretaja.app.ui.components.BigPrimaryButton
import com.jepretaja.app.ui.components.LocationField
import com.jepretaja.app.ui.state.AuthActionsViewModel

@Composable
fun RegisterCustomerScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AuthActionsViewModel = hiltViewModel(),
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Diaktifkan sekali saja begitu tombol Daftar pertama kali ditekan dengan
    // data belum lengkap — sebelum itu, ladang yang masih kosong belum ditandai
    // merah supaya layar tidak terlihat penuh error saat formnya baru dibuka.
    var submitAttempted by remember { mutableStateOf(false) }
    var provinceExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    val provinceOptions = remember { IndonesianLocations.provinces }
    val cityOptions = remember(province) { IndonesianLocations.citiesForProvince(province) }

    LaunchedEffect(province) {
        if (province.isBlank()) {
            city = ""
            return@LaunchedEffect
        }
        if (cityOptions.isNotEmpty() && (city.isBlank() || city !in cityOptions)) {
            city = cityOptions.first()
        }
    }

    // SEMUA field di bawah ini wajib diisi — bukan cuma nama, email, dan
    // password seperti sebelumnya. Tanpa ini, No. HP/Kota/Provinsi/Alamat bisa
    // dikosongkan begitu saja dan akun tetap berhasil dibuat dengan data
    // kontak yang bolong, padahal field-field itu dibutuhkan booking & creator
    // untuk menghubungi konsumen.
    val formReady = name.isNotBlank() && email.contains("@") && phone.isNotBlank() &&
        province.isNotBlank() && city.isNotBlank() && address.isNotBlank() &&
        password.length >= 8 && password == confirmPassword && termsAccepted

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
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    Modifier.size(52.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(AppColors.PrimarySoft),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(27.dp)) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Buat Akun", style = MaterialTheme.typography.headlineMedium, color = AppColors.TextPrimary)
                    Text("Akun konsumen", style = MaterialTheme.typography.labelMedium, color = AppColors.Primary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Daftar sebagai konsumen untuk mulai memesan sesi foto & video pilihanmu.",
                style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary,
            )
            Spacer(Modifier.height(22.dp))
            Text("Identitas akun", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                name, { name = it }, label = { Text("Nama Lengkap *") }, singleLine = true,
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
                    label = { Text("Kota *") },
                    singleLine = true,
                    enabled = cityOptions.isNotEmpty(),
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
                label = "Alamat Lengkap *",
                placeholder = "Pilih dari lokasi saya atau ketik alamat lengkap",
                isError = submitAttempted && address.isBlank(),
                errorText = if (submitAttempted && address.isBlank()) "Wajib diisi." else null,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                password, { password = it }, label = { Text("Password *") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true,
                isError = submitAttempted && password.length < 8,
                shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(5.dp))
            Text("Minimal 8 karakter", style = MaterialTheme.typography.labelSmall, color = if (password.length >= 8) AppColors.Success else AppColors.TextSecondary)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                confirmPassword, { confirmPassword = it }, label = { Text("Ulangi Password *") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true,
                isError = (confirmPassword.isNotEmpty() && confirmPassword != password) || (submitAttempted && confirmPassword.isBlank()),
                supportingText = {
                    when {
                        confirmPassword.isNotEmpty() && confirmPassword != password -> Text("Password belum sama.")
                        submitAttempted && confirmPassword.isBlank() -> Text("Wajib diisi.", color = AppColors.Danger)
                    }
                },
                shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = termsAccepted, onCheckedChange = { termsAccepted = it })
                Text("Saya menyetujui Terms dan Privacy Policy *", style = MaterialTheme.typography.bodySmall)
            }
            if (submitAttempted && !termsAccepted) {
                Text("Wajib disetujui untuk melanjutkan.", style = MaterialTheme.typography.labelSmall, color = AppColors.Danger)
            }
            error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(28.dp))
            BigPrimaryButton(
                text = if (loading) "Memproses..." else "Daftar",
                loading = loading,
                enabled = !loading,
                onClick = {
                    submitAttempted = true
                    if (!formReady) {
                        error = "Lengkapi semua data bertanda (*) sebelum mendaftar."
                        return@BigPrimaryButton
                    }
                    loading = true; error = null
                    viewModel.registerCustomer(name.trim(), email.trim(), password, phone.trim(), address.trim(), city.trim(), province.trim(), onSuccess = { loading = false; onSuccess() }, onError = { loading = false; error = it })
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
