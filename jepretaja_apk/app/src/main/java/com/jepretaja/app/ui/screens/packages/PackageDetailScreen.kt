package com.jepretaja.app.ui.screens.packages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.util.Formatters
import com.jepretaja.app.ui.components.BigPrimaryButton
import com.jepretaja.app.ui.components.GradientHeroCard
import com.jepretaja.app.ui.components.PremiumCard
import com.jepretaja.app.ui.components.SectionHeader
import com.jepretaja.app.ui.state.AuthViewModel

/** Package Detail (section 9 & 28). */
@Composable
fun PackageDetailScreen(
    onBack: () -> Unit,
    onBookingClick: (String) -> Unit,
    onLoginRequired: () -> Unit,
    authViewModel: AuthViewModel,
    viewModel: PackageDetailViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsState()
    val pkg by viewModel.pkg.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val notFound by viewModel.notFound.collectAsState()
    var quantity by remember { mutableIntStateOf(1) }
    var selectedAddOns by remember { mutableStateOf(setOf<String>()) }
    var customRequest by remember { mutableStateOf("") }
    var faqOpen by remember { mutableIntStateOf(-1) }

    Scaffold(topBar = {
        com.jepretaja.app.ui.components.AppTopBar(title = "Detail Paket", onBack = onBack)
    }) { padding ->
        when {
            loading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = AppColors.Danger)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.load() }) { Text("Coba Lagi") }
                }
            }
            notFound || pkg == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { Text("Paket tidak ditemukan") }
            else -> {
                val p = pkg!!
                Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(4.dp))
                    GradientHeroCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Text(p.name, style = MaterialTheme.typography.headlineMedium, color = AppColors.OnPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(Formatters.currency(p.price), style = MaterialTheme.typography.displaySmall, color = AppColors.OnPrimary)
                    }
                    Spacer(Modifier.height(24.dp))
                    SectionHeader("Yang Kamu Dapat")
                    Spacer(Modifier.height(12.dp))
                    PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), elevation = 4.dp) {
                        PackageAttributeRow(Icons.Default.Schedule, "Durasi", p.duration)
                        p.personnel?.let { PackageAttributeRow(Icons.Default.Groups, "Personel", it) }
                        p.output?.let { PackageAttributeRow(Icons.Default.Inventory2, "Output", it) }
                    }
                    Spacer(Modifier.height(20.dp))
                    SectionHeader("What's included")
                    Spacer(Modifier.height(8.dp))
                    PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), elevation = 3.dp) {
                        val included = listOfNotNull(p.duration.takeIf { it.isNotBlank() }?.let { "$it coverage" }, p.personnel?.takeIf { it.isNotBlank() }?.let { "$it" }, p.output?.takeIf { it.isNotBlank() })
                        (included.ifEmpty { listOf("Professional photography service", "Edited digital delivery", "Online gallery") }).forEach { item ->
                            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AppColors.Success, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp)); Text(item, color = AppColors.TextPrimary)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), elevation = 3.dp) {
                        Text("What's not included", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("Transport, venue permits, and requests outside the selected package.", color = AppColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(20.dp))
                    SectionHeader("Add-ons")
                    Spacer(Modifier.height(8.dp))
                    PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), elevation = 3.dp) {
                        if (p.addOns.isEmpty()) Text("Tidak ada add-on untuk paket ini.", color = AppColors.TextSecondary)
                        p.addOns.forEach { addOn ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = addOn.addOnId in selectedAddOns, onCheckedChange = { selectedAddOns = if (it) selectedAddOns + addOn.addOnId else selectedAddOns - addOn.addOnId })
                                Text(addOn.name, modifier = Modifier.weight(1f), color = AppColors.TextPrimary)
                                Text("+ ${Formatters.currency(addOn.price)}", color = AppColors.Primary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), elevation = 3.dp) {
                        Text("Jumlah sesi", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { quantity = (quantity - 1).coerceAtLeast(1) }) { Icon(Icons.Default.Remove, contentDescription = "Kurangi jumlah") }
                            Text(quantity.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 14.dp))
                            IconButton(onClick = { quantity = (quantity + 1).coerceAtMost(10) }) { Icon(Icons.Default.Add, contentDescription = "Tambah jumlah") }
                        }
                        OutlinedTextField(value = customRequest, onValueChange = { customRequest = it }, label = { Text("Custom request") }, placeholder = { Text("Ceritakan kebutuhan khususmu") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(20.dp))
                    SectionHeader("Perbandingan paket")
                    Spacer(Modifier.height(8.dp))
                    PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), elevation = 3.dp) {
                        ComparisonRow("Durasi", p.duration.ifBlank { "Menyesuaikan" })
                        ComparisonRow("Delivery time", "3–7 hari kerja")
                        ComparisonRow("Revisi", "1 kali revisi ringan")
                        ComparisonRow("Cancellation", "Sesuai kebijakan creator")
                    }
                    Spacer(Modifier.height(20.dp))
                    SectionHeader("FAQ")
                    Spacer(Modifier.height(8.dp))
                    listOf("Kapan hasil dikirim?" to "Hasil dikirim sesuai estimasi delivery time setelah sesi selesai.", "Apakah bisa request khusus?" to "Bisa. Tulis kebutuhanmu pada custom request sebelum booking.", "Bagaimana kebijakan pembatalan?" to "Pembatalan mengikuti kebijakan creator dan status pembayaran.").forEachIndexed { index, (question, answer) ->
                        PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp), elevation = 2.dp, onClick = { faqOpen = if (faqOpen == index) -1 else index }) {
                            Row(verticalAlignment = Alignment.CenterVertically) { Text(question, modifier = Modifier.weight(1f), color = AppColors.TextPrimary, style = MaterialTheme.typography.titleSmall); Icon(Icons.Default.ExpandMore, contentDescription = null, tint = AppColors.TextSecondary) }
                            if (faqOpen == index) { Spacer(Modifier.height(8.dp)); Text(answer, color = AppColors.TextSecondary, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    SectionHeader("Deskripsi")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        p.description,
                        color = AppColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(32.dp))
                    BigPrimaryButton(
                        text = if (authState.isLoggedIn) "Booking Sekarang" else "Masuk untuk Booking",
                        onClick = {
                            if (authState.isLoggedIn) onBookingClick(p.packageId) else onLoginRequired()
                        },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun PackageAttributeRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(MaterialTheme.shapes.small).background(AppColors.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(label, color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
    }
}

@Composable
private fun ComparisonRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AppColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(value, color = AppColors.TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}
