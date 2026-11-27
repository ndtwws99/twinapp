package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.GitHubEvent
import com.example.data.model.GitHubRepo
import com.example.data.model.RateLimitInfo
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.MonoHandleStyle
import com.example.ui.theme.PulseAmber
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseViolet
import com.example.ui.theme.languageColor

@Composable
fun DeveloperStoryAvatar(
    login: String,
    avatarUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag("story_avatar_$login")
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(PulseEmerald, PulseCyan, PulseViolet)),
                    shape = CircleShape
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Ảnh đại diện @$login",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "@$login",
            style = MonoHandleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RepoSocialCard(
    repo: GitHubRepo,
    isBookmarked: Boolean,
    isFollowingOwner: Boolean,
    onOpenRepo: () -> Unit,
    onOpenOwner: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleFollow: () -> Unit,
    onShare: () -> Unit,
    showBannerPreview: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("repo_card_${repo.fullName}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post author header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenOwner)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = repo.owner.avatarUrl,
                        contentDescription = "Avatar ${repo.owner.login}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "@${repo.owner.login}",
                            style = MonoHandleStyle.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val relTime = formatRelativeTimeVi(repo.pushedAt ?: repo.updatedAt)
                        Text(
                            text = if (relTime.isNotBlank()) "Cập nhật $relTime" else "Kho mã nguồn công khai",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = onToggleFollow,
                    shape = RoundedCornerShape(999.dp),
                    color = if (isFollowingOwner) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isFollowingOwner) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("follow_chip_${repo.owner.login}")
                ) {
                    Text(
                        text = if (isFollowingOwner) stringResource(R.string.user_following) else stringResource(R.string.user_follow),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isFollowingOwner) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Clickable body -> opens Repository Detail
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenRepo)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = repo.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!repo.description.isNullOrBlank()) {
                    Text(
                        text = repo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (showBannerPreview) {
                    AsyncImage(
                        model = repo.openGraphImageUrl,
                        contentDescription = "Ảnh xem trước ${repo.fullName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.91f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(12.dp)
                            )
                    )
                }

                // Language & Topics
                if (!repo.language.isNullOrBlank() || repo.topics.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!repo.language.isNullOrBlank()) {
                            LanguageDotBadge(language = repo.language)
                        }
                        repo.topics.take(3).forEach { topic ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "#$topic",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Social engagement bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    MetricPill(
                        icon = Icons.Filled.Star,
                        value = formatCompactCount(repo.stargazersCount),
                        tint = PulseAmber
                    )
                    MetricPill(
                        icon = Icons.Outlined.CallSplit,
                        value = formatCompactCount(repo.forksCount),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MetricPill(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        value = formatCompactCount(repo.openIssuesCount),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.testTag("bookmark_button_${repo.fullName}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmarked) stringResource(R.string.repo_bookmarked) else stringResource(R.string.repo_bookmark),
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("share_button_${repo.fullName}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.repo_share),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventSocialCard(
    event: GitHubEvent,
    onOpenRepo: (owner: String, repo: String) -> Unit,
    onOpenUser: (login: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val eventInfo = describeEventVi(event)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("event_card_${event.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (event.repo.owner.isNotBlank() && event.repo.repoName.isNotBlank()) {
                        onOpenRepo(event.repo.owner, event.repo.repoName)
                    }
                }
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = event.actor.avatarUrl,
                    contentDescription = "Avatar @${event.actor.login}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onOpenUser(event.actor.login) }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "@${event.actor.displayLogin ?: event.actor.login}",
                            style = MonoHandleStyle.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { onOpenUser(event.actor.login) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${formatRelativeTimeVi(event.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = eventInfo.actionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = eventInfo.badgeColor.copy(alpha = 0.16f)
                ) {
                    Icon(
                        imageVector = eventInfo.icon,
                        contentDescription = null,
                        tint = eventInfo.badgeColor,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
            }

            // Repo pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = event.repo.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!eventInfo.snippet.isNullOrBlank()) {
                        Text(
                            text = eventInfo.snippet,
                            style = if (event.type == "PushEvent") MonoCodeStyle else MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class EventPresentation(
    val actionText: String,
    val snippet: String?,
    val icon: ImageVector,
    val badgeColor: Color
)

private fun describeEventVi(event: GitHubEvent): EventPresentation {
    val payload = event.payload
    return when (event.type) {
        "PushEvent" -> {
            val count = payload?.size ?: payload?.commits?.size ?: 1
            val branch = payload?.ref?.removePrefix("refs/heads/").orEmpty()
            val firstMsg = payload?.commits?.firstOrNull()?.message?.lineSequence()?.firstOrNull()
            EventPresentation(
                actionText = "vừa đẩy $count commit" + if (branch.isNotBlank()) " lên nhánh $branch" else "",
                snippet = firstMsg,
                icon = Icons.Outlined.Terminal,
                badgeColor = PulseEmerald
            )
        }
        "ReleaseEvent" -> {
            val tag = payload?.release?.tagName.orEmpty()
            val title = payload?.release?.name?.takeIf { it.isNotBlank() } ?: tag
            EventPresentation(
                actionText = "đã phát hành phiên bản mới $tag",
                snippet = listOfNotNull(title.takeIf { it.isNotBlank() }, payload?.release?.body?.take(140)).joinToString(" — "),
                icon = Icons.Outlined.NewReleases,
                badgeColor = PulseEmerald
            )
        }
        "WatchEvent" -> EventPresentation(
            actionText = "đã gắn sao yêu thích kho mã nguồn",
            snippet = null,
            icon = Icons.Outlined.StarOutline,
            badgeColor = PulseAmber
        )
        "ForkEvent" -> EventPresentation(
            actionText = "đã fork kho mã nguồn này",
            snippet = null,
            icon = Icons.Outlined.CallSplit,
            badgeColor = PulseCyan
        )
        "IssuesEvent" -> {
            val verb = when (payload?.action) {
                "closed" -> "đã đóng thảo luận"
                "reopened" -> "đã mở lại thảo luận"
                else -> "đã mở thảo luận mới"
            }
            val issueTitle = payload?.issue?.let { "#${it.number} ${it.title}" }
            EventPresentation(
                actionText = verb,
                snippet = issueTitle,
                icon = Icons.Outlined.BugReport,
                badgeColor = PulseViolet
            )
        }
        "IssueCommentEvent" -> EventPresentation(
            actionText = "đã bình luận trong thảo luận",
            snippet = payload?.comment?.body?.take(150) ?: payload?.issue?.title,
            icon = Icons.Outlined.ChatBubbleOutline,
            badgeColor = PulseCyan
        )
        "PullRequestEvent" -> EventPresentation(
            actionText = "đã ${payload?.action ?: "cập nhật"} Pull Request",
            snippet = null,
            icon = Icons.Outlined.Code,
            badgeColor = PulseViolet
        )
        "CreateEvent" -> EventPresentation(
            actionText = "đã tạo ${payload?.refType ?: "nhánh"} mới ${payload?.ref.orEmpty()}".trim(),
            snippet = null,
            icon = Icons.Outlined.Code,
            badgeColor = PulseEmerald
        )
        else -> EventPresentation(
            actionText = "đã hoạt động trên GitHub",
            snippet = null,
            icon = Icons.Outlined.Code,
            badgeColor = PulseCyan
        )
    }
}

@Composable
fun LanguageDotBadge(
    language: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(languageColor(language))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MetricPill(
    icon: ImageVector,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            style = MonoHandleStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RateLimitBanner(
    rateLimit: RateLimitInfo?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (rateLimit == null || rateLimit.remaining > 10) return
    Surface(
        onClick = onOpenSettings,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "GitHub API còn ${rateLimit.remaining}/${rateLimit.limit} lượt. Nhấn để thêm Token trong Hồ sơ.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Outlined.ErrorOutline,
        title = "Không tải được dữ liệu GitHub",
        subtitle = message,
        actionLabel = stringResource(R.string.action_retry),
        onAction = onRetry,
        modifier = modifier
    )
}
