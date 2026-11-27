package com.example.ui.screens.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.UserPreferences
import com.example.data.model.GitHubRepo
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.EventSocialCard
import com.example.ui.components.RepoSocialCard
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.MonoHandleStyle
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel,
    preferences: UserPreferences,
    onBack: () -> Unit,
    onOpenRepo: (owner: String, repo: String) -> Unit,
    onOpenUser: (login: String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onShareRepo: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isFollowing = preferences.followedUsers.any { it.equals(state.username, ignoreCase = true) }
    val savedSet = preferences.savedRepos.map { it.fullName.lowercase() }.toSet()
    val followedSet = preferences.followedUsers.map { it.lowercase() }.toSet()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "@${state.username}",
                        style = MonoHandleStyle.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("user_profile_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    state.user?.htmlUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        IconButton(onClick = { onOpenUrl(url) }) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInBrowser,
                                contentDescription = stringResource(R.string.repo_open_github)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "user_hero") {
                when {
                    state.isUserLoading && state.user == null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    state.userError != null && state.user == null -> {
                        ErrorStateView(message = state.userError.orEmpty(), onRetry = { viewModel.refresh() })
                    }
                    state.user != null -> {
                        val user = state.user!!
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Subtle developer gradient banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    PulseEmerald.copy(alpha = 0.28f),
                                                    PulseCyan.copy(alpha = 0.25f),
                                                    PulseViolet.copy(alpha = 0.25f)
                                                )
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            AsyncImage(
                                                model = user.avatarUrl,
                                                contentDescription = "Avatar @${user.login}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                    .padding(2.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = user.name?.takeIf { it.isNotBlank() } ?: user.login,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "@${user.login}",
                                                    style = MonoHandleStyle,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }

                                        if (isFollowing) {
                                            OutlinedButton(
                                                onClick = { viewModel.toggleFollow() },
                                                modifier = Modifier.testTag("user_profile_follow_button")
                                            ) {
                                                Text(stringResource(R.string.user_following))
                                            }
                                        } else {
                                            Button(
                                                onClick = { viewModel.toggleFollow() },
                                                modifier = Modifier.testTag("user_profile_follow_button")
                                            ) {
                                                Text(stringResource(R.string.user_follow))
                                            }
                                        }
                                    }

                                    if (!user.bio.isNullOrBlank()) {
                                        Text(
                                            text = user.bio,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Metadata lines
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (!user.company.isNullOrBlank()) {
                                            MetaRow(Icons.Outlined.Business, user.company)
                                        }
                                        if (!user.location.isNullOrBlank()) {
                                            MetaRow(Icons.Outlined.LocationOn, user.location)
                                        }
                                        if (!user.blog.isNullOrBlank()) {
                                            MetaRow(Icons.Outlined.Link, user.blog)
                                        }
                                    }

                                    // Follower stats
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ProfileStatBox(
                                            value = formatCompactCount(user.followers),
                                            label = stringResource(R.string.user_followers_label),
                                            modifier = Modifier.weight(1f)
                                        )
                                        ProfileStatBox(
                                            value = formatCompactCount(user.following),
                                            label = stringResource(R.string.user_following_label),
                                            modifier = Modifier.weight(1f)
                                        )
                                        ProfileStatBox(
                                            value = formatCompactCount(user.publicRepos),
                                            label = stringResource(R.string.user_repos_label),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "user_tabs") {
                TabRow(
                    selectedTabIndex = state.activeTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = state.activeTab == UserProfileTab.REPOS,
                        onClick = { viewModel.selectTab(UserProfileTab.REPOS) },
                        text = { Text(stringResource(R.string.user_repos_tab)) },
                        icon = { Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = state.activeTab == UserProfileTab.ACTIVITY,
                        onClick = { viewModel.selectTab(UserProfileTab.ACTIVITY) },
                        text = { Text(stringResource(R.string.user_activity_tab)) },
                        icon = { Icon(Icons.Outlined.Bolt, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            when (state.activeTab) {
                UserProfileTab.REPOS -> {
                    when {
                        state.isReposLoading -> item(key = "u_repos_load") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        state.repos.isEmpty() -> item(key = "u_repos_empty") {
                            EmptyStateView(
                                icon = Icons.Outlined.Code,
                                title = "Chưa có kho mã nguồn công khai",
                                subtitle = "Người dùng này chưa công khai kho mã nguồn nào."
                            )
                        }
                        else -> items(state.repos, key = { it.id }) { repo ->
                            RepoSocialCard(
                                repo = repo,
                                isBookmarked = repo.fullName.lowercase() in savedSet,
                                isFollowingOwner = repo.owner.login.lowercase() in followedSet,
                                onOpenRepo = { onOpenRepo(repo.owner.login, repo.name) },
                                onOpenOwner = { onOpenUser(repo.owner.login) },
                                onToggleBookmark = { viewModel.toggleBookmark(repo) },
                                onToggleFollow = { viewModel.toggleFollow() },
                                onShare = { onShareRepo(repo) },
                                showBannerPreview = false,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                UserProfileTab.ACTIVITY -> {
                    when {
                        state.isEventsLoading -> item(key = "u_ev_load") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        state.events.isEmpty() -> item(key = "u_ev_empty") {
                            EmptyStateView(
                                icon = Icons.Outlined.Bolt,
                                title = "Chưa có hoạt động gần đây",
                                subtitle = "Không ghi nhận hoạt động công khai nào gần đây."
                            )
                        }
                        else -> items(state.events, key = { it.id }) { event ->
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
}

@Composable
private fun MetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileStatBox(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MonoHandleStyle.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
