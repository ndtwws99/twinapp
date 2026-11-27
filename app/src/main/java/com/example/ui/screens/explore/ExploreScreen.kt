package com.example.ui.screens.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.UserPreferences
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.RepoSocialCard
import com.example.ui.theme.MonoHandleStyle

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    preferences: UserPreferences,
    onOpenRepo: (owner: String, repo: String) -> Unit,
    onOpenUser: (login: String) -> Unit,
    onShareRepo: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val savedSet = preferences.savedRepos.map { it.fullName.lowercase() }.toSet()
    val followedSet = preferences.followedUsers.map { it.lowercase() }.toSet()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("explore_lazy_column"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "explore_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.explore_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.explore_search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.updateQuery("") },
                                modifier = Modifier.testTag("explore_clear_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.action_close)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboard?.hide()
                            viewModel.submitSearch()
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = state.target == SearchTarget.REPOSITORIES,
                        onClick = { viewModel.selectTarget(SearchTarget.REPOSITORIES) },
                        label = { Text(stringResource(R.string.explore_mode_repos)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("explore_tab_repos")
                    )
                    FilterChip(
                        selected = state.target == SearchTarget.USERS,
                        onClick = { viewModel.selectTarget(SearchTarget.USERS) },
                        label = { Text(stringResource(R.string.explore_mode_users)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.PersonSearch,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("explore_tab_users")
                    )
                }
            }
        }

        // Quick topic suggestions
        item(key = "quick_topics") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.explore_topics_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ExploreUiState.QUICK_TOPICS, key = { it }) { topic ->
                        SuggestionChip(
                            onClick = { viewModel.applyQuickTopic(topic) },
                            label = {
                                Text(
                                    text = "#$topic",
                                    style = MonoHandleStyle
                                )
                            },
                            modifier = Modifier.testTag("quick_topic_$topic")
                        )
                    }
                }
            }
        }

        when {
            state.isLoading -> {
                item(key = "explore_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            state.errorMessage != null -> {
                item(key = "explore_error") {
                    ErrorStateView(
                        message = state.errorMessage.orEmpty(),
                        onRetry = { viewModel.submitSearch() }
                    )
                }
            }

            state.target == SearchTarget.REPOSITORIES -> {
                if (state.repos.isEmpty() && state.hasSearched) {
                    item(key = "explore_empty_repos") {
                        EmptyStateView(
                            icon = Icons.Outlined.Search,
                            title = "Không có kết quả",
                            subtitle = stringResource(R.string.explore_no_results)
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
                            showBannerPreview = false,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            state.target == SearchTarget.USERS -> {
                if (state.users.isEmpty() && state.hasSearched) {
                    item(key = "explore_empty_users") {
                        EmptyStateView(
                            icon = Icons.Outlined.PersonSearch,
                            title = "Không tìm thấy lập trình viên",
                            subtitle = stringResource(R.string.explore_no_results)
                        )
                    }
                } else {
                    items(state.users, key = { it.id }) { user ->
                        DeveloperSearchCard(
                            user = user,
                            isFollowing = user.login.lowercase() in followedSet,
                            onOpenProfile = { onOpenUser(user.login) },
                            onToggleFollow = { viewModel.toggleFollow(user.login) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperSearchCard(
    user: GitHubUser,
    isFollowing: Boolean,
    onOpenProfile: () -> Unit,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_search_card_${user.login}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenProfile)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar @${user.login}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${user.login}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.type ?: "GitHub Developer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                onClick = onToggleFollow,
                shape = RoundedCornerShape(999.dp),
                color = if (isFollowing) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(
                    1.dp,
                    if (isFollowing) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = if (isFollowing) stringResource(R.string.user_following) else stringResource(R.string.user_follow),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
