package com.jepretaja.app.ui.screens.myreports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.util.Formatters
import com.jepretaja.app.data.model.ReportModel
import com.jepretaja.app.ui.components.AppTopBar
import com.jepretaja.app.ui.components.EmptyState
import com.jepretaja.app.ui.components.PremiumCard
import com.jepretaja.app.ui.components.SectionHeader
import com.jepretaja.app.ui.components.StatusBadge
import com.jepretaja.app.ui.components.rememberAppTopBarScrollBehavior
import com.jepretaja.app.ui.state.AuthViewModel

private fun targetTypeLabel(targetType: String): String = when (targetType) {
    "explore_post" -> "Konten Explore"
    "creator" -> "Creator"
    "chat_message" -> "Pesan Chat"
    "review" -> "Review"
    else -> targetType.replace("_", " ").replaceFirstChar { it.uppercase() }
}

/** Laporan Saya (section 28) — laporan konten/pengguna yang pernah diajukan customer ini. */
@Composable
fun MyReportsScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel,
    viewModel: MyReportsViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsState()
    val myUid = authState.uid
    var showReport by remember { mutableStateOf(false) }

    val scrollBehavior = rememberAppTopBarScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AppColors.Background,
        topBar = { AppTopBar(title = "Laporan Saya", onBack = onBack, scrollBehavior = scrollBehavior, actions = { IconButton(onClick = { showReport = true }) { Icon(Icons.Default.Add, "Buat laporan") } }) },
    ) { padding ->
        if (myUid == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.Lock,
                    title = "Masuk untuk melihat laporan Anda",
                    description = "Laporan yang pernah Anda ajukan akan muncul di sini setelah masuk.",
                )
            }
            return@Scaffold
        }
        val reports by remember(myUid) { viewModel.myReports(myUid) }.collectAsState(initial = emptyList())
        if (reports.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.Flag,
                    title = "Anda belum pernah membuat laporan",
                    description = "Laporan konten atau pengguna yang Anda ajukan akan tercatat di sini.",
                )
            }
        } else {
            Column(Modifier.padding(padding).fillMaxSize()) {
                Spacer(Modifier.height(20.dp))
                SectionHeader(title = "Laporan Saya", subtitle = "${reports.size} laporan telah diajukan")
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(reports) { r -> ReportRow(r) }
                }
            }
        }
    }
    if (showReport && myUid != null) {
        ReportSheet(
            submitting = viewModel.submitting.collectAsState().value,
            onDismiss = { showReport = false },
            onSubmit = { type, target, reason -> viewModel.submit(myUid, type, target, reason); showReport = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(submitting: Boolean, onDismiss: () -> Unit, onSubmit: (String, String, String) -> Unit) {
    var targetType by remember { mutableStateOf("explore_post") }
    var targetId by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("spam") }
    val reasons = listOf("spam" to "Spam", "scam" to "Scam", "harassment" to "Harassment", "fake_account" to "Fake account", "copyright" to "Copyright", "inappropriate_content" to "Inappropriate content", "payment_issue" to "Payment issue", "other" to "Other")
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {
            Text("Report", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("What happened?", color = AppColors.TextSecondary)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(targetId, { targetId = it }, label = { Text("Object ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Text("Target", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = targetType == "explore_post", onClick = { targetType = "explore_post" }, label = { Text("Post") })
                FilterChip(selected = targetType == "creator", onClick = { targetType = "creator" }, label = { Text("Creator") })
            }
            Spacer(Modifier.height(8.dp))
            reasons.forEach { (value, label) -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(reason == value, { reason = value }); Text(label) } }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onSubmit(targetType, targetId.trim(), reason) }, enabled = targetId.isNotBlank() && !submitting, modifier = Modifier.fillMaxWidth()) { Text(if (submitting) "Submitting..." else "Submit Report") }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ReportRow(r: ReportModel) {
    PremiumCard(modifier = Modifier.fillMaxWidth(), elevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(targetTypeLabel(r.targetType), style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
            StatusBadge(status = r.status)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            r.reason.replace("_", " ").replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
        )
        r.createdAt?.let {
            Spacer(Modifier.height(8.dp))
            Text(Formatters.dateShort(it), style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
        }
    }
}
