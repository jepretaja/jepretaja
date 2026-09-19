package com.jepretaja.app.ui.screens.creatordashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jepretaja.app.core.util.AppConstants
import com.jepretaja.app.data.model.PortfolioModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DialogEditAlbum(
    item: PortfolioModel,
    onBatal: () -> Unit,
    onSimpan: (String, String) -> Unit,
) {
    var judul by remember(item.portfolioId) { mutableStateOf(item.title) }
    var kategori by remember(item.portfolioId) { mutableStateOf(item.category) }
    var menuTerbuka by remember(item.portfolioId) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onBatal,
        title = { Text("Edit Album") },
        text = {
            Column {
                OutlinedTextField(
                    judul,
                    { judul = it },
                    label = { Text("Judul") },
                    supportingText = { Text("Judul membantu calon pelanggan mengenali jenis pemotretannya.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = menuTerbuka,
                    onExpandedChange = { menuTerbuka = it },
                ) {
                    OutlinedTextField(
                        value = kategori,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    DropdownMenu(
                        expanded = menuTerbuka,
                        onDismissRequest = { menuTerbuka = false },
                    ) {
                        AppConstants.SERVICE_CATEGORIES.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = { kategori = c; menuTerbuka = false },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSimpan(judul.trim(), kategori) }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onBatal) { Text("Batal") } },
    )
}