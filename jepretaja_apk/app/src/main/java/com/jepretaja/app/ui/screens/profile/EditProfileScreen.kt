package com.jepretaja.app.ui.screens.profile

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.data.model.IndonesianLocations
import com.jepretaja.app.ui.components.AppAvatar
import com.jepretaja.app.ui.components.AppTopBar
import com.jepretaja.app.ui.components.BigPrimaryButton
import com.jepretaja.app.ui.components.SectionHeader
import com.jepretaja.app.ui.components.rememberAppTopBarScrollBehavior
import com.jepretaja.app.ui.state.AuthViewModel

/**
 * Edit profil untuk SEMUA pengguna.
 *
 * Sebelumnya hanya creator yang punya jalan mengubah profilnya (lewat Creator
 * Settings); konsumen sama sekali tidak bisa memperbaiki nama, nomor telepon,
 * atau memasang foto — padahal nama itulah yang dilihat creator saat menerima
 * booking dan membalas chat.
 */
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val profile = authState.profile

    var name by remember(profile?.userId) { mutableStateOf(profile?.name ?: "") }
    var username by remember(profile?.userId) {
        mutableStateOf(profile?.username?.ifBlank { null } ?: profile?.name
            ?.lowercase()
            ?.replace(Regex("[^a-z0-9]"), "")
            ?.take(12)
            ?.ifBlank { "pengguna" }
            ?.plus(profile.userId.takeLast(7))
            ?.take(20)
            ?: "")
    }
    var phone by remember(profile?.userId) { mutableStateOf(profile?.phone ?: "") }
    var dateOfBirth by remember(profile?.userId) { mutableStateOf(profile?.dateOfBirth ?: "") }
    var gender by remember(profile?.userId) { mutableStateOf(profile?.gender ?: "") }
    var address by remember(profile?.userId) { mutableStateOf(profile?.address ?: "") }
    var city by remember(profile?.userId) { mutableStateOf(profile?.city ?: "") }
    var province by remember(profile?.userId) { mutableStateOf(profile?.province ?: "") }
    var kabupaten by remember(profile?.userId) { mutableStateOf(profile?.kabupaten ?: "") }
    var kecamatan by remember(profile?.userId) { mutableStateOf(profile?.kecamatan ?: "") }
    var desa by remember(profile?.userId) { mutableStateOf(profile?.desa ?: "") }
    var postalCode by remember(profile?.userId) { mutableStateOf(profile?.postalCode ?: "") }
    var bio by remember(profile?.userId) { mutableStateOf(profile?.bio ?: "") }
    var pickedPhoto by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var provinceExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }

    val provinceOptions = remember { IndonesianLocations.provinces }
    val cityOptions = remember(province) { IndonesianLocations.citiesForProvince(province) }

    LaunchedEffect(profile?.userId) {
        if (profile?.province?.isNotBlank() == true && province.isBlank()) province = profile.province ?: ""
        if (profile?.city?.isNotBlank() == true && city.isBlank()) city = profile.city ?: ""
    }

    LaunchedEffect(province) {
        if (province.isBlank()) {
            city = ""
            return@LaunchedEffect
        }
        if (cityOptions.isNotEmpty() && city.isBlank()) {
            city = cityOptions.first()
        } else if (cityOptions.isNotEmpty() && city !in cityOptions) {
            city = cityOptions.first()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) pickedPhoto = uri
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            message = null
        }
    }

    val scrollBehavior = rememberAppTopBarScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AppTopBar(title = "Edit Profil", onBack = onBack, scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.align(Alignment.CenterHorizontally).clickable { photoPicker.launch("image/*") }) {
                if (pickedPhoto != null) {
                    AsyncImage(
                        model = pickedPhoto, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(96.dp).clip(CircleShape),
                    )
                } else {
                    AppAvatar(url = profile?.photoUrl, name = profile?.name ?: "?", size = 96.dp)
                }
                Icon(
                    Icons.Default.PhotoCamera, contentDescription = "Ganti foto",
                    tint = AppColors.OnPrimary,
                    modifier = Modifier.align(Alignment.BottomEnd).size(30.dp)
                        .clip(CircleShape).background(AppColors.Primary).padding(6.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Ketuk foto untuk mengganti",
                style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(28.dp))
            SectionHeader("Data Diri")
            Spacer(Modifier.height(12.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama lengkap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.lowercase().replace(" ", "") },
                    label = { Text("Nama pengguna") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("contoh: ahmad_putra") },
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor telepon") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    label = { Text("Tanggal lahir") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("YYYY-MM-DD") },
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = gender,
                    onValueChange = { gender = it },
                    label = { Text("Jenis kelamin") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Laki-laki / Perempuan / Lainnya") },
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = provinceExpanded,
                    onExpandedChange = { provinceExpanded = it },
                ) {
                    OutlinedTextField(
                        value = province,
                        onValueChange = { province = it },
                        label = { Text("Provinsi") },
                        singleLine = true,
                        readOnly = false,
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
                                    city = if (cityOptions.isNotEmpty()) cityOptions.first() else ""
                                    provinceExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = cityExpanded,
                    onExpandedChange = { if (cityOptions.isNotEmpty()) cityExpanded = it },
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Kota") },
                        singleLine = true,
                        readOnly = false,
                        enabled = cityOptions.isNotEmpty(),
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
                    OutlinedTextField(
                        value = kabupaten,
                        onValueChange = { kabupaten = it },
                        label = { Text("Kabupaten / Kota") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: Aceh Utara") },
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = kecamatan,
                        onValueChange = { kecamatan = it },
                        label = { Text("Kecamatan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: Banda Sakti") },
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = desa,
                        onValueChange = { desa = it },
                        label = { Text("Desa / Kelurahan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: Kampung Jawa") },
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = postalCode,
                        onValueChange = { postalCode = it.filter(Char::isDigit).take(5) },
                        label = { Text("Kode pos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: 24351") },
                    )
                    Spacer(Modifier.height(12.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat detail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Contoh: Jl. Mawar No. 12, RT 02 RW 04") },
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio / Deskripsi singkat") },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                // Email sengaja hanya ditampilkan, tidak bisa diubah: mengganti
                // email adalah operasi Firebase Auth tersendiri yang butuh
                // verifikasi ulang, bukan sekadar menulis field di Firestore.
                OutlinedTextField(
                    value = profile?.email ?: "-", onValueChange = {}, enabled = false,
                    label = { Text("Email (tidak bisa diubah)") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(28.dp))
                BigPrimaryButton(
                    text = "Simpan Perubahan",
                    loading = saving,
                    enabled = name.isNotBlank() && username.isNotBlank(),
                    onClick = {
                        saving = true
                        viewModel.save(
                            name = name.trim(),
                            phone = phone.trim().ifBlank { null },
                            photoUri = pickedPhoto,
                            userId = authState.uid,
                        ) { photoUrl, error ->
                            if (error != null) {
                                saving = false
                                message = error
                            } else {
                                authViewModel.updateProfile(
                                    name = name.trim(),
                                    username = username.trim(),
                                    phone = phone.trim().ifBlank { null },
                                    dateOfBirth = dateOfBirth.trim().ifBlank { null },
                                    gender = gender.trim().ifBlank { null },
                                    address = address.trim().ifBlank { null },
                                    city = city.trim().ifBlank { null },
                                    province = province.trim().ifBlank { null },
                                    kabupaten = kabupaten.trim().ifBlank { null },
                                    kecamatan = kecamatan.trim().ifBlank { null },
                                    desa = desa.trim().ifBlank { null },
                                    postalCode = postalCode.trim().ifBlank { null },
                                    bio = bio.trim().ifBlank { null },
                                    photoUrl = photoUrl,
                                ) { saveError ->
                                    saving = false
                                    message = saveError ?: "Profil tersimpan"
                                    if (saveError == null) pickedPhoto = null
                                }
                            }
                        }
                    },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
