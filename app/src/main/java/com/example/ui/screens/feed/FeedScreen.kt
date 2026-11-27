package com.example.ui.screens.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.UserPreferences
import com.example.data.model.GitHubRepo
import com.example.data.model.RateLimitInfo
import com.example.ui.components.DeveloperStoryAvatar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.EventSocialCard
import com.example.ui.components.RateLimitBanner
import com.example.ui.components.RepoSocialCard
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseEmerald

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    preferences: UserPreferences,
    rateLimit: RateLimitInfo?,
    onOpenRepo: (owner: String, repo: String) -> Unit,
    onOpenUser: (login: String) -> Unit,
    onOpenProfileSettings: () -> Unit,
    onShareRepo: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedSet = preferences.savedRepos.map { it.fullName.lowercase() }.toSet()
    val followedSet = preferences.followedUsers.map { it.lowercase() }.toSet()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("feed_lazy_column"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Branded Social Top Bar
        item(key = "feed_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        PulseEmerald.copy(alpha = 0.22f),
                                        PulseCyan.copy(alpha = 0.22f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Hub,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "LIVE API",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.feed_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.testTag("feed_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.feed_refresh),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Rate-limit warning banner if low
        if (rateLimit != null && rateLimit.remaining <= 10) {
            item(key = "rate_limit_banner") {
                RateLimitBanner(
                    rateLimit = rateLimit,
                    onOpenSettings = onOpenProfileSettings,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Developer Stories Row
        if (preferences.followedUsers.isNotEmpty()) {
            item(key = "dev_stories") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.feed_stories_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(preferences.followedUsers, key = { it.lowercase() }) { login ->
                            DeveloperStoryAvatar(
                                login = login,
                                avatarUrl = "https://github.com/$login.png?size=160",
                                onClick = { onOpenUser(login) }
                            )
                        }
                    }
                }
            }
        }

        // Feed Mode Segmented Chips
        item(key = "feed_mode_selector") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedModePill(
                    label = stringResource(R.string.feed_tab_trending),
                    icon = Icons.Outlined.Explore,
                    selected = state.mode == FeedMode.TRENDING,
                    onClick = { viewModel.selectMode(FeedMode.TRENDING) },
                    testTag = "feed_mode_trending",
                    modifier = Modifier.weight(1f)
                )
                FeedModePill(
                    label = stringResource(R.string.feed_tab_live),
                    icon = Icons.Outlined.Bolt,
                    selected = state.mode == FeedMode.LIVE_EVENTS,
                    onClick = { viewModel.selectMode(FeedMode.LIVE_EVENTS) },
                    testTag = "feed_mode_live",
                    modifier = Modifier.weight(1f)
                )
                FeedModePill(
                    label = stringResource(R.string.feed_tab_following),
                    icon = Icons.Outlined.Group,
                    selected = state.mode == FeedMode.FOLLOWING,
                    onClick = { viewModel.selectMode(FeedMode.FOLLOWING) },
                    testTag = "feed_mode_following",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Topic Filter Chips when in Trending mode
        if (state.mode == FeedMode.TRENDING) {
            item(key = "feed_topics") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FeedUiState.DEFAULT_TOPICS, key = { it.id }) { topic ->
                        val selected = state.selectedTopic.id == topic.id
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.selectTopic(topic) },
                            label = { Text(topic.label, style = MaterialTheme.typography.labelLarge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("topic_chip_${topic.id}")
                        )
                    }
                }
            }
        }

        // Content Section
        when {
            state.isLoading -> {
                item(key = "feed_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Đang đồng bộ dữ liệu từ GitHub API...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            state.errorMessage != null -> {
                item(key = "feed_error") {
                    ErrorStateView(
                        message = state.errorMessage.orEmpty(),
                        onRetry = { viewModel.refresh() }
                    )
                }
            }

            state.mode == FeedMode.TRENDING -> {
                if (state.repos.isEmpty()) {
                    item(key = "feed_empty_repos") {
                        EmptyStateView(
                            icon = Icons.Outlined.Explore,
                            title = "Chưa có kho mã nguồn nào",
                            subtitle = stringResource(R.string.feed_empty_generic),
                            actionLabel = stringResource(R.string.feed_refresh),
                            onAction = { viewModel.refresh() }
                        )
                    }
                } else {
                    items(state.repos, key = { it.id }) { repo ->
                        RepoSocialCard(
                            repo = repo,
                            isBookmarked = repo.fullName.lowercase() in savedSet,
                            isFollowingOwner = repo.owner.login.lowercase() in followedSet,
                            onOpenRepo = { onOpenRepo(repo.owner.login, repo.name) },
                            onOpenOwner = { onOpenUser(repo.owner.login) },
                            onToggleBookmark = { viewModel.toggleBookmark(repo) },
                            onToggleFollow = { viewModel.toggleFollow(repo.owner.login) },
                            onShare = { onShareRepo(repo) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            state.mode == FeedMode.LIVE_EVENTS -> {
                if (state.liveEvents.isEmpty()) {
                    item(key = "feed_empty_live") {
                        EmptyStateView(
                            icon = Icons.Outlined.Bolt,
                            title = "Chưa có hoạt động mới",
                            subtitle = stringResource(R.string.feed_empty_generic),
                            actionLabel = stringResource(R.string.feed_refresh),
                            onAction = { viewModel.refresh() }
                        )
                    }
                } else {
                    items(state.liveEvents, key = { it.id }) { event ->
                        EventSocialCard(
                            event = event,
                            onOpenRepo = onOpenRepo,
                            onOpenUser = onOpenUser,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            state.mode == FeedMode.FOLLOWING -> {
                if (state.followingEvents.isEmpty()) {
                    item(key = "feed_empty_following") {
                        EmptyStateView(
                            icon = Icons.Outlined.Group,
                            title = "Bảng tin theo dõi đang trống",
                            subtitle = stringResource(R.string.feed_empty_following),
                            actionLabel = stringResource(R.string.feed_refresh),
                            onAction = { viewModel.refresh() }
                        )
                    }
                } else {
                    items(state.followingEvents, key = { it.id }) { event ->
                        EventSocialCard(
                            event = event,
                            onOpenRepo = onOpenRepo,
                            onOpenUser = onOpenUser,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedModePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .height(48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
