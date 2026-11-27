package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.TwinPost
import com.example.data.model.TwinReaction
import com.example.data.model.TwinStory
import com.example.ui.theme.TwinHeartRed
import com.example.ui.theme.TwinSuccessLight
import com.example.ui.theme.storyBackgroundBrush
import com.example.ui.theme.twinStoryRingBrush
import com.google.firebase.Timestamp
import kotlin.math.abs

fun formatTimestampVi(ts: Timestamp?): String {
    val ms = ts?.toDate()?.time ?: return "vừa xong"
    return formatMillisVi(ms)
}

fun formatMillisVi(ms: Long): String {
    if (ms <= 0L) return "vừa xong"
    val diffSec = abs((System.currentTimeMillis() - ms) / 1000L)
    return when {
        diffSec < 60 -> "vừa xong"
        diffSec < 3600 -> "${diffSec / 60} phút"
        diffSec < 86_400 -> "${diffSec / 3600} giờ"
        diffSec < 2_592_000 -> "${diffSec / 86_400} ngày"
        else -> "${diffSec / 2_592_000} tháng"
    }
}

@Composable
fun TwinAvatar(
    avatarUrl: String,
    name: String,
    size: Dp = 42.dp,
    showStoryRing: Boolean = false,
    online: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        val outerModifier = if (showStoryRing) {
            Modifier
                .matchParentSize()
                .border(2.2.dp, twinStoryRingBrush(), CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
        } else {
            Modifier
                .matchParentSize()
                .clip(CircleShape)
        }

        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Ảnh đại diện $name",
                contentScale = ContentScale.Crop,
                modifier = outerModifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
        } else {
            val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "T"
            Box(
                modifier = outerModifier.background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (online) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(TwinSuccessLight)
            )
        }
    }
}

@Composable
fun TwinPostCard(
    post: TwinPost,
    currentUid: String,
    onReact: (String) -> Unit,
    onOpenComments: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showReactionBar by remember { mutableStateOf(false) }
    val myReactionKey = post.reactions[currentUid]
    val myReaction = TwinReaction.fromKey(myReactionKey)
    val isSaved = currentUid in post.savedBy
    val reactionCount = post.reactions.size
    val commentCount = post.comments.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("twin_post_card_${post.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post Author Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TwinAvatar(
                    avatarUrl = post.authorAvatar,
                    name = post.authorName,
                    size = 42.dp,
                    showStoryRing = true
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorName.ifBlank { "Thành viên Twin" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.feeling.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${post.feeling}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = formatTimestampVi(post.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (post.privacy == "friends") Icons.Outlined.Group else Icons.Outlined.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        if (post.groupName.isNotBlank()) {
                            Text(
                                text = "trong ${post.groupName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (onDelete != null && post.authorUid == currentUid) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_post_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.post_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Market Listing Badge if applicable
            if (post.isMarketPost) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = com.example.data.model.formatVndPrice(post.marketPrice),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (post.marketTitle.isNotBlank()) {
                        Text(
                            text = post.marketTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Text Content
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Attached File Pill (matching web file attachments)
            if (post.fileUrl.isNotBlank()) {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clickable {
                            runCatching {
                                val openIntent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(post.fileUrl)
                                )
                                ctx.startActivity(openIntent)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "📎", fontSize = 16.sp)
                        Text(
                            text = post.fileName.ifBlank { "Tệp đính kèm trên Twin" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Post Image (supports web imageUrls[], images[], and imageUrl)
            val displayImage = post.primaryImageUrl
            if (displayImage.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                AsyncImage(
                    model = displayImage,
                    contentDescription = "Ảnh bài viết của ${post.authorName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.25f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
            }

            // Stats Strip
            if (reactionCount > 0 || commentCount > 0 || post.sharesCount > 0L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (reactionCount > 0) {
                            val topEmojis = post.reactions.values
                                .distinct()
                                .mapNotNull { TwinReaction.fromKey(it)?.emoji }
                                .take(3)
                                .joinToString("")
                            Text(text = topEmojis.ifBlank { "❤️" }, fontSize = 14.sp)
                            Text(
                                text = "$reactionCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (commentCount > 0) {
                            Text(
                                text = "$commentCount bình luận",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable(onClick = onOpenComments)
                            )
                        }
                        if (post.sharesCount > 0L) {
                            Text(
                                text = "${post.sharesCount} chia sẻ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Expandable 6 Twin Reactions Picker
            if (showReactionBar) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TwinReaction.entries.forEach { reaction ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (myReaction == reaction) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        onReact(reaction.key)
                                        showReactionBar = false
                                    }
                            ) {
                                Text(text = reaction.emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { showReactionBar = !showReactionBar },
                    modifier = Modifier.testTag("react_btn_${post.id}")
                ) {
                    if (myReaction != null) {
                        Text(text = myReaction.emoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = myReaction.label,
                            color = if (myReaction == TwinReaction.LOVE) TwinHeartRed else MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.ThumbUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.post_react))
                    }
                }

                TextButton(
                    onClick = onOpenComments,
                    modifier = Modifier.testTag("comment_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.post_comment))
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.testTag("save_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(R.string.post_save),
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("share_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.post_share),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CommentsDialog(
    post: TwinPost,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    val comments = remember(post.comments) { post.parsedComments() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.75f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bình luận (${comments.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
                ) {
                    if (comments.isEmpty()) {
                        item {
                            Text(
                                text = "Chưa có bình luận nào. Hãy là người đầu tiên chia sẻ cảm nghĩ!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        items(comments, key = { it.commentId.ifBlank { "${it.authorUid}_${it.createdAtMillis}" } }) { c ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TwinAvatar(avatarUrl = c.authorAvatar, name = c.authorName, size = 34.dp)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = c.authorName,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = formatMillisVi(c.createdAtMillis),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = c.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("Viết bình luận...") },
                        singleLine = true,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comment_input_field")
                    )
                    IconButton(
                        onClick = {
                            val trimmed = draft.trim()
                            if (trimmed.isNotEmpty()) {
                                onSendComment(trimmed)
                                draft = ""
                            }
                        },
                        modifier = Modifier.testTag("comment_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gửi bình luận",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoryViewerDialog(
    story: TwinStory,
    currentUid: String,
    onToggleLike: () -> Unit,
    onDismiss: () -> Unit
) {
    val liked = currentUid in story.likes
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(24.dp))
                .background(storyBackgroundBrush(story.bgId))
        ) {
            if (story.type == "image" && story.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = story.imageUrl,
                    contentDescription = "Tin 24h của ${story.authorName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.text,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Top bar overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TwinAvatar(avatarUrl = story.authorAvatar, name = story.authorName, size = 36.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Text(
                        text = formatTimestampVi(story.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Đóng tin", tint = Color.White)
                }
            }

            // Bottom overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = story.caption.ifBlank { "${story.likes.size} lượt thả tim" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Thả tim tin",
                        tint = if (liked) TwinHeartRed else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AdminVerifiedIcon() {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "Quản trị viên Twin",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp)
    )
}
