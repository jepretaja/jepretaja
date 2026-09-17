package com.jepretaja.app.ui.screens.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jepretaja.app.core.theme.AppColors
import com.jepretaja.app.ui.components.AppAvatar
import com.jepretaja.app.ui.state.AuthViewModel
import kotlinx.coroutines.flow.flowOf

@Composable
fun ExploreCommentsSheet(
    postId: String,
    authViewModel: AuthViewModel,
    onLoginRequired: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: ExploreDetailViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val post by viewModel.post.collectAsState()
    var text by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<CommentUi?>(null) }

    LaunchedEffect(postId) {
        viewModel.observePost(postId)
        viewModel.observeComments(postId)
    }

    val following by remember(post?.creatorId, authState.uid) {
        if (post?.creatorId != null && authState.uid != null) {
            viewModel.isFollowing(post!!.creatorId, authState.uid!!)
        } else flowOf(false)
    }.collectAsState(initial = false)
    val canComment = when (post?.commentPolicy) {
        "off" -> post?.creatorId == authState.uid
        "followers" -> following || post?.creatorId == authState.uid
        else -> true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Komentar", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text("${comments.size}", color = AppColors.TextSecondary)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup komentar")
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CommentSort.entries.forEach { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { viewModel.setSort(option) },
                        label = { Text(option.label) },
                        shape = RoundedCornerShape(percent = 50),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.PrimarySoft),
                    )
                }
            }
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (comments.isEmpty()) {
                    item { Text("Belum ada komentar. Jadilah yang pertama.", color = AppColors.TextSecondary, modifier = Modifier.padding(vertical = 28.dp)) }
                }
                items(comments, key = { it.commentId }) { comment ->
                    CommentSheetRow(
                        comment = comment,
                        userId = authState.uid,
                        viewModel = viewModel,
                        postId = postId,
                        onReply = { replyTo = comment },
                        pinned = post?.pinnedCommentId == comment.commentId,
                    )
                }
            }
            replyTo?.let { target ->
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Membalas ${target.authorName}", color = AppColors.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { replyTo = null }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, "Batal membalas") }
                }
            }
            val uid = authState.uid
            if (authState.uid == null) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Masuk untuk ikut berkomentar", color = AppColors.TextSecondary, modifier = Modifier.weight(1f))
                    TextButton(onClick = onLoginRequired) { Text("Masuk") }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        enabled = canComment,
                        placeholder = { Text(if (canComment) "Tulis komentar..." else "Komentar dibatasi creator") },
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.size(8.dp))
                    IconButton(
                        enabled = canComment && text.isNotBlank(),
                        onClick = {
                            uid?.let { userId ->
                                viewModel.addComment(postId, userId, text.trim(), replyTo?.commentId, authState.profile?.name.orEmpty(), authState.profile?.photoUrl)
                            }
                            text = ""
                            replyTo = null
                        },
                    ) { Icon(Icons.AutoMirrored.Filled.Send, "Kirim", tint = AppColors.Primary) }
                }
            }
        }
    }
}

@Composable
private fun CommentSheetRow(
    comment: CommentUi,
    userId: String?,
    viewModel: ExploreDetailViewModel,
    postId: String,
    onReply: () -> Unit,
    pinned: Boolean,
) {
    val liked by remember(comment.commentId, userId) {
        if (userId != null) viewModel.isCommentLiked(comment.commentId, userId) else flowOf(false)
    }.collectAsState(initial = false)
    Column {
        ListItem(
            leadingContent = { AppAvatar(url = comment.authorPhotoUrl, name = comment.authorName, size = 36.dp) },
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.authorName)
                    if (pinned) { Spacer(Modifier.size(5.dp)); Icon(Icons.Default.PushPin, "Disematkan", modifier = Modifier.size(14.dp), tint = AppColors.Primary) }
                }
            },
            supportingContent = { Text(comment.text) },
            trailingContent = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { userId?.let { viewModel.toggleCommentLike(comment.commentId, it) } }, modifier = Modifier.size(30.dp)) {
                        Icon(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Suka komentar", tint = if (liked) AppColors.Danger else AppColors.TextSecondary, modifier = Modifier.size(17.dp))
                    }
                    Text("${comment.likeCount}", style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        Row(Modifier.padding(start = 60.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Balas", color = AppColors.Primary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable(onClick = onReply))
            userId?.takeIf { comment.userId == it }?.let { ownerId ->
                Text("Hapus", color = AppColors.Danger, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable { viewModel.deleteComment(comment.commentId, postId, ownerId) })
            }
        }
        comment.replies.forEach { reply ->
            Row(Modifier.padding(start = 42.dp, top = 6.dp), verticalAlignment = Alignment.Top) {
                AppAvatar(url = reply.authorPhotoUrl, name = reply.authorName, size = 28.dp)
                Column(Modifier.padding(start = 8.dp)) {
                    Text(reply.authorName, style = MaterialTheme.typography.labelLarge)
                    Text(reply.text, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
            }
        }
    }
}
