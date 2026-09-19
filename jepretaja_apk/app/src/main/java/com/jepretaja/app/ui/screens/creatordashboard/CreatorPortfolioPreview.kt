package com.jepretaja.app.ui.screens.creatordashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.data.model.PortfolioModel
import com.jepretaja.app.ui.components.VideoPlayer

@Composable
internal fun PratinjauLayarPenuh(item: PortfolioModel, onTutup: () -> Unit) {
    var indeks by remember(item.portfolioId) { mutableIntStateOf(0) }
    val media = item.media
    val video = item.type == "video"

    Dialog(onDismissRequest = onTutup, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f))) {
            if (video) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    VideoPlayer(
                        url = media.getOrNull(indeks).orEmpty(),
                        playWhenActive = false,
                        muted = false,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    )
                }
            } else {
                AsyncImage(
                    model = media.getOrNull(indeks),
                    contentDescription = item.title.ifBlank { "Karya portfolio" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(vertical = 90.dp),
                )
            }

            IconButton(onClick = onTutup, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
            }

            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
                if (media.size > 1 && !video) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(media.size) { i ->
                            val url = media[i]
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (i == indeks) 2.dp else 0.dp,
                                        color = if (i == indeks) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .clickable { indeks = i },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                val t = tampilanStatus(item.status)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(t.ikon, contentDescription = null, tint = t.warna, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(t.label, style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
                if (item.title.isNotBlank()) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
                if (item.category.isNotBlank()) {
                    Text(item.category, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}