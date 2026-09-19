package com.jepretaja.app.ui.screens.creatorupload

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.core.util.CaptionParser
import com.jepretaja.app.ui.components.AppAvatar
import com.jepretaja.app.ui.components.VideoPlayer

@Composable
internal fun CreatorPostPreviewScreen(
    uri: Uri?,
    isVideo: Boolean,
    coverUri: Uri?,
    title: String,
    description: String,
    category: String,
    location: String,
    packageName: String?,
    packagePrice: Long?,
    creatorName: String,
    onBack: () -> Unit,
    onPublish: () -> Unit,
) {
    val tags = remember(description) { CaptionParser.hashtags(description) }

    Scaffold(
        topBar = { com.jepretaja.app.ui.components.AppTopBar(title = "Preview", onBack = onBack) },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding()
                    .background(AppColors.Surface).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(percent = 50),
                ) { Text("Kembali") }
                Button(
                    onClick = onPublish,
                    modifier = Modifier.weight(1.2f).height(50.dp),
                    shape = RoundedCornerShape(percent = 50),
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Publish")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Text("Seperti inilah karya kamu tampil di Explore", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.Black)) {
                Box(Modifier.fillMaxWidth().aspectRatio(9f / 12f), contentAlignment = Alignment.Center) {
                    when {
                        uri == null -> Text("Media belum dipilih", color = Color.White.copy(alpha = 0.75f))
                        isVideo -> VideoPlayer(url = uri.toString(), playWhenActive = true, muted = false, modifier = Modifier.fillMaxSize())
                        else -> AsyncImage(
                            model = uri,
                            contentDescription = "Preview karya",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppAvatar(url = null, name = creatorName, size = 34.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("@$creatorName", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(description, color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Text("Kategori: $category", color = AppColors.OnPrimary, style = MaterialTheme.typography.labelMedium)
                    if (tags.isNotEmpty()) {
                        Spacer(Modifier.height(5.dp))
                        Text(tags.joinToString(" ") { "#$it" }, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
                    }
                    if (location.isNotBlank()) {
                        Spacer(Modifier.height(5.dp))
                        Text("Lokasi: $location", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text("Like 0", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Text("Komentar 0", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Text("Simpan 0", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (isVideo && coverUri != null) {
                Spacer(Modifier.height(18.dp))
                Text("Cover video", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = coverUri,
                    contentDescription = "Cover video",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 86.dp, height = 116.dp).clip(MaterialTheme.shapes.medium),
                )
            }

            packageName?.let { name ->
                Spacer(Modifier.height(18.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AppColors.PrimarySoft,
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Paket Booking", style = MaterialTheme.typography.labelMedium, color = AppColors.Primary)
                        Spacer(Modifier.height(4.dp))
                        Text(name, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                        packagePrice?.let {
                            Text(com.jepretaja.app.core.util.Formatters.currency(it), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
internal fun PreviewPost(
    uri: Uri?,
    isVideo: Boolean,
    caption: String,
    creatorName: String,
    location: String,
    packageName: String?,
    coverUri: Uri? = null,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.Black)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
            when {
                uri == null -> Text("Pilih foto atau video untuk melihat preview", color = Color.White.copy(alpha = 0.72f))
                isVideo -> Box(Modifier.fillMaxSize()) {
                    if (coverUri != null) {
                        AsyncImage(model = coverUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.Center).size(30.dp),
                    )
                }
                else -> AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Column(Modifier.padding(14.dp)) {
            Text(creatorName, color = Color.White, style = MaterialTheme.typography.titleSmall)
            if (caption.isNotBlank()) {
                Text(caption, color = Color.White.copy(alpha = 0.86f), maxLines = 3, style = MaterialTheme.typography.bodySmall)
            }
            if (location.isNotBlank()) {
                Text("Lokasi: $location", color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelSmall)
            }
            packageName?.let { Text("Booking: $it", color = AppColors.OnPrimary, style = MaterialTheme.typography.labelMedium) }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Like", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Text("Komentar", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Text("Simpan", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Text("Bagikan", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}